/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Feb 22, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.Node;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.6
 */
public class DefaultFileStoreHandler implements FileStoreHandler {

    private final Node m_node;
    private final UUID m_storeUUID;
    private File m_baseDirInWorkflowFolder;
    private File m_baseDir;
    private DuplicateChecker m_duplicateChecker;
    private FileStoreHandlerRepository m_fileStoreHandlerRepository;
    private int m_nextIndex = 0;

    /**
     *  */
    DefaultFileStoreHandler(final Node node, final UUID storeUUID) {
        if (node == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_node = node;
        m_storeUUID = storeUUID;
    }

    public void setBaseDir(final File baseDir) {
        if (!baseDir.isDirectory()) {
            throw new IllegalStateException(
                    "Base directory of file store to node " + m_node.getName()
                    + " does not exist: " + baseDir.getAbsolutePath());
        }
        m_baseDir = baseDir;
    }

    void setFileStoreHandlerRepository(final FileStoreHandlerRepository repo) {
        m_fileStoreHandlerRepository = repo;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_fileStoreHandlerRepository;
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        if (m_baseDir != null) {
            FileUtil.deleteRecursively(m_baseDir);
        }
        m_fileStoreHandlerRepository.removeFileStoreHandler(this);
        m_fileStoreHandlerRepository = null;
    }

    /** @return the baseDir */
    public File getBaseDir() {
        return m_baseDir;
    }

    /** @return the storeUUID */
    public UUID getStoreUUID() {
        return m_storeUUID;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileStore getFileStore(final FileStoreKey key) {
        FileStoreHandler ownerHandler;
        if (key.getStoreUUID().equals(m_storeUUID)) {
            ownerHandler = this;
        } else {
            final FileStoreHandlerRepository repo =
                m_fileStoreHandlerRepository;
            if (repo != null) {
                ownerHandler = repo.getHandler(m_storeUUID);
            } else {
                throw new IllegalStateException(
                        "No file store handler repository set");
            }
        }
        if (!(ownerHandler instanceof DefaultFileStoreHandler)) {
            throw new IllegalStateException(String.format(
                    "Owner file store handler \"%s\" to file store key \"%s\" "
                    + "is not of expected type %s",
                    ownerHandler, key,
                    DefaultFileStoreHandler.class.getSimpleName()));
        }
        try {
            DefaultFileStoreHandler owner = (DefaultFileStoreHandler)ownerHandler;
            return owner.getFileStoreInternal(key);
        } catch (IOException e) {
            throw new IllegalStateException("Could not init file store \""
                    + ownerHandler + "\"", e);
        }
    }

    private FileStore getFileStoreInternal(final FileStoreKey key)
        throws IOException {
        assert Thread.holdsLock(this);
        assert key.getStoreUUID().equals(getStoreUUID());
        if (getBaseDir() == null && m_baseDirInWorkflowFolder == null) {
            throw new IllegalStateException(
                    "No file stores in \"" + toString() + "\"");
        }
        if (m_baseDirInWorkflowFolder != null) {
            assert m_baseDir == null;
            ensureInitBaseDirectory();
            File source = m_baseDirInWorkflowFolder;
            m_baseDirInWorkflowFolder = null;
            FileUtil.copyDir(source, m_baseDir);
        }
        return new FileStore(this, key);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(m_storeUUID.toString());
        b.append(" (").append(m_node.getName()).append(": ");
        if (m_baseDir == null) {
            b.append("<no directory>");
        } else {
            b.append(m_baseDir.getAbsolutePath());
        }
        b.append(")");
        return b.toString();
    }

    public synchronized FileStore createFileStore(final String name)
        throws IOException {
        if (name == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (name.startsWith(".")) {
            throw new IOException("Name must not start with a dot: \""
                    + name + "\"");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new IOException("Invalid file name, must not contain (back)"
                    + " slash: \"" + name + "\"");
        }
        if (m_duplicateChecker == null) {
            throw new IllegalStateException("File store on node "
                    + m_node.getName() + " is read only");
        }
        try {
            m_duplicateChecker.addKey(name);
        } catch (IOException e) {
            throw new IllegalStateException(e.getClass().getSimpleName()
                    + " while checking for duplicate names", e);
        }
        ensureInitBaseDirectory();
        FileStoreKey key = new FileStoreKey(m_storeUUID, m_nextIndex, name);
        m_nextIndex++;
        FileStore fs = new FileStore(this, key);
        return fs;
    }

    private void ensureInitBaseDirectory() throws IOException {
        assert Thread.holdsLock(this);
        if (m_baseDir == null) {
            StringBuilder baseDirName = new StringBuilder("knime_fs-");
            String nodeName = m_node.getName();
            // delete special chars
            nodeName = nodeName.replaceAll("[()-]", "");
            // non-word chars by '_'
            nodeName = nodeName.replaceAll("\\W", "_");
            baseDirName.append(nodeName);
            baseDirName.append("-").append(getStoreUUID().toString());
            m_baseDir = FileUtil.createTempDir(baseDirName.toString());
        }
    }

    public synchronized void close() {
        if (m_duplicateChecker == null) {
            return;
        }
        try {
            m_duplicateChecker.checkForDuplicates();
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Duplicate file store "
                    + "name encountered: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException(e.getClass().getSimpleName()
                    + " while checking for duplicate names", e);
        }
    }

    public static final FileStoreHandler createNewHandler(final Node node,
            final FileStoreHandlerRepository fileStoreHandlerRepository) {
        DefaultFileStoreHandler result = new DefaultFileStoreHandler(node, UUID.randomUUID());
        fileStoreHandlerRepository.addFileStoreHandler(result);
        result.setFileStoreHandlerRepository(fileStoreHandlerRepository);
        result.m_duplicateChecker = new DuplicateChecker();
        return result;
    }

    public static final FileStoreHandler restore(final Node node,
            final UUID uuid,
            final FileStoreHandlerRepository fileStoreHandlerRepository,
            final File inWorkflowDirectory) {
        DefaultFileStoreHandler fileStoreHandler = new DefaultFileStoreHandler(node, uuid);
        fileStoreHandlerRepository.addFileStoreHandler(fileStoreHandler);
        fileStoreHandler.setFileStoreHandlerRepository(fileStoreHandlerRepository);
        fileStoreHandler.m_baseDirInWorkflowFolder = inWorkflowDirectory;
        return fileStoreHandler;
    }

}

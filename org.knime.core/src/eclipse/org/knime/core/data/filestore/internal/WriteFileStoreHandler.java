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
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LRUCache;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.6
 */
public class WriteFileStoreHandler implements IWriteFileStoreHandler {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WriteFileStoreHandler.class);

    /** File organization of file stores. There are {@value #FOLDER_LEVEL} levels of sub folders in the temp dir,
     * each folder contains {@value #FILES_PER_FOLDER} sub folders or files (in the leaf folders). A file store
     * file is then located in, e.g. &lt;filestore_dir>/000/000/000/file1.bin */
    public static final int FILES_PER_FOLDER = 1000;
    /** See {@link #FILES_PER_FOLDER}. */
    public static final int FOLDER_LEVEL = 2;

    private static int MAX_NR_FILES = (int)Math.pow(FILES_PER_FOLDER, FOLDER_LEVEL + 1);

    private final String m_name;
    private final UUID m_storeUUID;
    private File m_baseDirInWorkflowFolder;
    private File m_baseDir;
    private InternalDuplicateChecker m_duplicateChecker;
    private FileStoreHandlerRepository m_fileStoreHandlerRepository;
    private LRUCache<FileStoreKey, FileStoreKey> m_createdFileStoreKeys;
    private int m_nextIndex = 0;


    /**
     *  */
    public WriteFileStoreHandler(final String name, final UUID storeUUID) {
        if (name == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_name = name;
        m_storeUUID = storeUUID;
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final FileStoreHandlerRepository fileStoreHandlerRepository) {
        fileStoreHandlerRepository.addFileStoreHandler(this);
        m_fileStoreHandlerRepository = fileStoreHandlerRepository;
    }

    public void setBaseDir(final File baseDir) {
        if (!baseDir.isDirectory()) {
            throw new IllegalStateException(
                    "Base directory of file store to node " + m_name
                    + " does not exist: " + baseDir.getAbsolutePath());
        }
        m_baseDir = baseDir;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_fileStoreHandlerRepository;
    }

    /** {@inheritDoc} */
    @Override
    public void clearAndDispose() {
        if (m_fileStoreHandlerRepository != null) {
            m_fileStoreHandlerRepository.removeFileStoreHandler(this);
            m_fileStoreHandlerRepository = null;
        }
        if (m_baseDir != null) {
            StringBuilder b = new StringBuilder("Disposing file store \"");
            b.append(toString()).append("\"");
            if (FileUtil.deleteRecursively(m_baseDir)) {
                b.append(" - folder successfully deleted");
                LOGGER.debug(b.toString());
            } else {
                b.append(" - folder not or only partially deleted");
                LOGGER.warn(b.toString());
            }
        }
    }

    /** @return the baseDir */
    public File getBaseDir() {
        return m_baseDir;
    }

    /** @return the storeUUID */
    @Override
    public final UUID getStoreUUID() {
        return m_storeUUID;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs) {
        final FileStoreKey key = FileStoreUtil.getFileStoreKey(fs);
        if (getOwnerHandler(key) == null) {
            return copyFileStore(fs);
        }
        return key;
    }

    synchronized FileStoreKey copyFileStore(final FileStore fs) {
        FileStoreKey key = FileStoreUtil.getFileStoreKey(fs);
        if (m_createdFileStoreKeys == null) {
            LOGGER.debug("Duplicating file store objects - file store handler id "
                    + key.getStoreUUID() + " is unknown to " + m_fileStoreHandlerRepository.getClass().getName());
            LOGGER.debug("Dump of valid file store handlers follows, omitting further log output");
            m_fileStoreHandlerRepository.printValidFileStoreHandlersToLogDebug();
            m_createdFileStoreKeys = new LRUCache<FileStoreKey, FileStoreKey>(10000);
        }
        FileStoreKey local = m_createdFileStoreKeys.get(key);
        if (local != null) {
            return local;
        }
        FileStore newStore;
        try {
            newStore = createFileStoreInternal(getNextIndex() + "_" + key.getName(), null, -1);
            FileUtil.copy(fs.getFile(), newStore.getFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed copying file stores to local handler", e);
        }
        final FileStoreKey newKey = FileStoreUtil.getFileStoreKey(newStore);
        m_createdFileStoreKeys.put(key, newKey);
        return newKey;
    }

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(final FileStoreKey key) {
        IFileStoreHandler ownerHandler = getOwnerHandler(key);
        if (!(ownerHandler instanceof WriteFileStoreHandler)) {
            throw new IllegalStateException(String.format(
                    "Owner file store handler \"%s\" to file store key \"%s\" "
                    + "is not of expected type %s",
                    ownerHandler, key,
                    WriteFileStoreHandler.class.getSimpleName()));
        }
        try {
            WriteFileStoreHandler owner = (WriteFileStoreHandler)ownerHandler;
            return owner.getFileStoreInternal(key);
        } catch (IOException e) {
            throw new IllegalStateException("Could not init file store \""
                    + ownerHandler + "\"", e);
        }
    }

    /**
     * @param key
     * @return */
    IFileStoreHandler getOwnerHandler(final FileStoreKey key) {
        IFileStoreHandler ownerHandler;
        final UUID keyStoreUUID = key.getStoreUUID();
        if (keyStoreUUID.equals(m_storeUUID)) {
            ownerHandler = this;
        } else {
            FileStoreHandlerRepository repo = m_fileStoreHandlerRepository;
            if (repo == null) {
                throw new IllegalStateException(
                        "No file store handler repository set");
            }
            ownerHandler = repo.getHandler(keyStoreUUID);
        }
        return ownerHandler;
    }

    private synchronized FileStore getFileStoreInternal(final FileStoreKey key)
        throws IOException {
        assert key.getStoreUUID().equals(getStoreUUID());
        if (getBaseDir() == null && m_baseDirInWorkflowFolder == null) {
            throw new IllegalStateException(
                    "No file stores in \"" + toString() + "\"");
        }
        if (m_baseDirInWorkflowFolder != null) {
            assert m_baseDir == null;
            ensureInitBaseDirectory();
            LOGGER.debug(String.format("Restoring file store directory \"%s\" from \"%s\"",
                    toString(), m_baseDirInWorkflowFolder));
            File source = m_baseDirInWorkflowFolder;
            m_baseDirInWorkflowFolder = null;
            FileUtil.copyDir(source, m_baseDir);
        }
        return FileStoreUtil.createFileStore(this, key);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(m_storeUUID.toString());
        b.append(" (").append(m_name).append(": ");
        if (m_baseDir == null) {
            b.append("<no directory>");
        } else {
            b.append(m_baseDir.getAbsolutePath());
        }
        b.append(")");
        return b.toString();
    }

    public synchronized FileStore createFileStore(final String name) throws IOException {
        addToDuplicateChecker(name);
        return createFileStoreInternal(name, null, -1);
    }

    /**
     * @param name
     * @throws IOException */
    void addToDuplicateChecker(final String name) throws IOException {
        if (m_duplicateChecker == null) {
            throw new IllegalStateException("File store on node " + m_name + " is read only/closed");
        }
        m_duplicateChecker.add(name);
    }

    FileStore createFileStoreInternal(final String name,
            final byte[] nestedLoopPath, final int iterationIndex) throws IOException {
        assert Thread.holdsLock(this);
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
        FileStoreKey key = new FileStoreKey(m_storeUUID, m_nextIndex, nestedLoopPath, iterationIndex, name);
        ensureInitBaseDirectory();
        if (m_nextIndex > MAX_NR_FILES) {
            throw new IOException("Maximum number of files stores reached: " + MAX_NR_FILES);
        }
        getParentDir(m_nextIndex, true);
        m_nextIndex++;
        FileStore fs = FileStoreUtil.createFileStore(this, key);
        return fs;
    }

    /** @return the nextIndex */
    public int getNextIndex() {
        return m_nextIndex;
    }

    public File getParentDir(final int indexArg, final boolean create) {
        int index = indexArg / FILES_PER_FOLDER; // bottom most dir also contains many files
        File parentDir = m_baseDir;
        String[] subFolderNames = new String[FOLDER_LEVEL];
        for (int level = 0; level < FOLDER_LEVEL; level++) {
            int modulo = index % FILES_PER_FOLDER;
            subFolderNames[FOLDER_LEVEL - level - 1] = String.format("%03d", modulo);
            index = index / FILES_PER_FOLDER;
        }
        for (int level = 0; level < FOLDER_LEVEL; level++) {
            parentDir = new File(parentDir, subFolderNames[level]);
        }
        if (create && !parentDir.isDirectory()) {
            if (!parentDir.mkdirs()) {
                LOGGER.error("Failed to create directory \"" + parentDir.getAbsolutePath() + "\"");
            }
        }
        return parentDir;
    }


    private void ensureInitBaseDirectory() throws IOException {
        assert Thread.holdsLock(this);
        if (m_baseDir == null) {
            StringBuilder baseDirName = new StringBuilder("knime_fs-");
            String nodeName = m_name;
            // delete special chars
            nodeName = nodeName.replaceAll("[()-]", "");
            nodeName = nodeName.replaceAll(":", "-");
            // non-word chars by '_'
            nodeName = nodeName.replaceAll("\\W", "_");
            baseDirName.append(nodeName).append("-");
            m_baseDir = FileUtil.createTempDir(baseDirName.toString());
            LOGGER.debug("Assigning temp directory to file store \"" + toString() + "\"");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        if (exec == null) {
            throw new NullPointerException("ExecutionContext must not be null.");
        }
        open();
    }

    /** Assigns duplicate checker which marks this FSH as open. */
    void open() {
        m_duplicateChecker = new InternalDuplicateChecker();
    }

    public void close() {
        if (m_duplicateChecker != null) {
            m_duplicateChecker.close();
            m_duplicateChecker = null;
        }
    }

    public static final IFileStoreHandler restore(final String name,
            final UUID uuid,
            final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository,
            final File inWorkflowDirectory) {
        WriteFileStoreHandler fileStoreHandler = new WriteFileStoreHandler(name, uuid);
        fileStoreHandler.addToRepository(fileStoreHandlerRepository);
        fileStoreHandler.m_baseDirInWorkflowFolder = inWorkflowDirectory;
        return fileStoreHandler;
    }

}

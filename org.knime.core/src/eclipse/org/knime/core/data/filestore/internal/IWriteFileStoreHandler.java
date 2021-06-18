/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 11, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface IWriteFileStoreHandler extends IFileStoreHandler {

    public FileStore createFileStore(final String name) throws IOException;

    /**
     * Creates a file store also using loop information (for the {@link FileStoreKey}).
     *
     * NOTE: doesn't do duplicate checking!
     *
     * @param name
     * @param nestedLoopPath
     * @param iterationIndex
     * @return the new file store
     * @throws IOException
     */
    public FileStore createFileStore(String name, int[] nestedLoopPath, int iterationIndex) throws IOException;

    public void open(final ExecutionContext exec);

    public void addToRepository(final IDataRepository repository);

    public void close();

    public void ensureOpenAfterLoad() throws IOException;

    /**
     * @param fs
     * @param flushCallback TODO
     * @return */
    public FileStoreKey translateToLocal(FileStore fs, FlushCallback flushCallback);

    public boolean mustBeFlushedPriorSave(final FileStore fs);

    /**
     * @return the store id, never <code>null</code>. If {@link #isReference()} is <code>true</code>, it returns the
     *         store id of the referenced file store handler
     */
    public UUID getStoreUUID();

    /**
     * @return the file store's base directory or <code>null</code> if this handler just references another one (i.e.
     *         {@link #isReference()} is <code>true</code>) or there have been no file stores creates by the respective
     *         node
     */
    default File getBaseDir() {
        if (isReference()) {
            return null;
        } else {
            throw new IllegalStateException(
                "IMPLEMENTATION ERROR: Implementing class must overwrite the 'getBaseDir'-method");
        }
    }

    /**
     * Tells if this file store handler just references another one (i.e. another file store handler of another node)
     * and doesn't create file stores itself. This file store, e.g., won't be persisted with the node neither will it be
     * added to the {@link IDataRepository}.
     *
     * @return <code>true</code> if this file store handler just references one of another node, otherwise
     *         <code>false</code>
     */
    boolean isReference();
}

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
 *   Jul 17, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.node.ExecutionContext;


/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public class ROWriteFileStoreHandler extends EmptyFileStoreHandler implements IWriteFileStoreHandler {

    /**
     * @param dataRepository */
    public ROWriteFileStoreHandler(final IDataRepository dataRepository) {
        super(dataRepository);
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStore(final String name) throws IOException {
        throw new IllegalStateException("read only file store handler");
    }

    /** {@inheritDoc} */
    @Override
    public FileStore createFileStore(final String name, final int[] nestedLoopPath, final int iterationIndex)
        throws IOException {
        throw new IllegalStateException("read only file store handler");
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        throw new IllegalStateException("read only file store handler");
    }

    /** {@inheritDoc} */
    @Override
    public void addToRepository(final IDataRepository repository) {
        throw new IllegalStateException("read only file store handler");
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        throw new IllegalStateException("read only file store handler");
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey translateToLocal(final FileStore fs, final FlushCallback flushCallback) {
        return FileStoreUtil.getFileStoreKey(fs);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustBeFlushedPriorSave(final FileStore fs) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public UUID getStoreUUID() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void ensureOpenAfterLoad() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReference() {
        return true;
    }
}

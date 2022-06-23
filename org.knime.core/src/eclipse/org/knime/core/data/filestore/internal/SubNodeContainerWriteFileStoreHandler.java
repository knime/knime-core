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
 *   Jul 10, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.util.UUID;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public class SubNodeContainerWriteFileStoreHandler extends AbstractReferenceWriteFileStoreHandler {

//    private final FlowSubnodeScopeContext m_subNodeContext;
    private FileStoresInLoopCache m_fileStoresInLoopCache;

    private FileStoresInLoopCache m_endNodeCacheWithKeysToPersist;

    /**
     * If this loop start file store handler just references another one.
     */
    private final boolean m_referencesAnotherFileStoreHandler;

    /**
     * Initializes a new loop start file store handler with the given store id. All file stores in the loop will be
     * created at the node 'owning' this file store handler.
     *
     * @param container owning node
     */
    public SubNodeContainerWriteFileStoreHandler(final SubNodeContainer container) {
        super(new WriteFileStoreHandler(container.getNameWithID(), UUID.randomUUID()));
        m_referencesAnotherFileStoreHandler = false;
    }

    /**
     * Initializes a new loop start file store handler that references the provided one. I.e. in that case it is a
     * 'reference file store handler', {@link IWriteFileStoreHandler#isReference()} returns <code>true</code> and the
     * actual file stores are created in the by the referenced file store handler.
     *
     * @param fsh the file store handler to be referenced (and delegated the most calls to)
     * @param container owning node
     */
    public SubNodeContainerWriteFileStoreHandler(final IWriteFileStoreHandler fsh,
        final SubNodeContainer container) {
        super(fsh);
        m_referencesAnotherFileStoreHandler = true;
    }

    /** {@inheritDoc} */
    @Override
    public void open(final ExecutionContext exec) {
        ((WriteFileStoreHandler)getDelegate()).open();
        m_fileStoresInLoopCache = new FileStoresInLoopCache(exec);
    }

    public void onSubNodeContainerFinished(final FileStoresInLoopCache endNodeCacheWithKeysToPersist) {
        m_endNodeCacheWithKeysToPersist = endNodeCacheWithKeysToPersist;
    }

    @Override
    public void clearAndDispose() {
        super.clearAndDispose();
        if (m_endNodeCacheWithKeysToPersist != null) {
            m_endNodeCacheWithKeysToPersist.dispose();
            m_endNodeCacheWithKeysToPersist = null;
        }
        if (m_fileStoresInLoopCache != null) {
            m_fileStoresInLoopCache.dispose();
            m_fileStoresInLoopCache = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReference() {
        return m_referencesAnotherFileStoreHandler;
    }

}

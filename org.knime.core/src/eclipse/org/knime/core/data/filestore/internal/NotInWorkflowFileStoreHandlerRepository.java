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
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.util.UUID;

import org.knime.core.node.NodeLogger;

/** Fallback repository that is used when the node is run outside the workflow manager,
 * for instance in the testing environment or using a 3rd party executor.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class NotInWorkflowFileStoreHandlerRepository extends FileStoreHandlerRepository {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NotInWorkflowFileStoreHandlerRepository.class);

    private IFileStoreHandler m_fileStoreHandler;

    /** {@inheritDoc} */
    @Override
    public void addFileStoreHandler(final IWriteFileStoreHandler writableFileStoreHandler) {
        assert m_fileStoreHandler == null : "Already assigned";
        m_fileStoreHandler = writableFileStoreHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void removeFileStoreHandler(final IWriteFileStoreHandler writableFileStoreHandler) {
        assert m_fileStoreHandler == writableFileStoreHandler;
    }

    /** {@inheritDoc} */
    @Override
    public IFileStoreHandler getHandler(final UUID storeHandlerUUID) {
        if (m_fileStoreHandler instanceof IWriteFileStoreHandler) {
            IWriteFileStoreHandler defFileStoreHandler =
                (IWriteFileStoreHandler)m_fileStoreHandler;
            if (defFileStoreHandler.getStoreUUID().equals(storeHandlerUUID)) {
                return defFileStoreHandler;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public IFileStoreHandler getHandlerNotNull(final UUID storeHandlerUUID) {
        if (!(m_fileStoreHandler instanceof IWriteFileStoreHandler)) {
            throw new IllegalStateException("no file stores in repository to \"" + m_fileStoreHandler + "\"");
        }
        IWriteFileStoreHandler defFileStoreHandler = (IWriteFileStoreHandler)m_fileStoreHandler;
        if (!defFileStoreHandler.getStoreUUID().equals(storeHandlerUUID)) {
            throw new IllegalStateException("Unknown file store id \""
                    + storeHandlerUUID + "\"; only \"" + defFileStoreHandler.getStoreUUID()
                    + "\" is valid in repository to \"" + m_fileStoreHandler + "\"");
        }
        return defFileStoreHandler;
    }

    /** {@inheritDoc} */
    @Override
    void printValidFileStoreHandlersToLogDebug() {
        if (m_fileStoreHandler instanceof IWriteFileStoreHandler) {
            LOGGER.debug("Single file store handler " + ((IWriteFileStoreHandler)m_fileStoreHandler).getStoreUUID());
        } else {
            LOGGER.debug("No writable file store handler set");
        }
    }
}

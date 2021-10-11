/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 */
package org.knime.core.data.v2.value.cell;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.WriteValue;

/**
 * Abstract implementation of a DataCell based WriteValue.
 * Handles file store cells.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractDataCellWriteValue implements WriteValue<DataCell> {
    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    AbstractDataCellWriteValue(final IDataRepository repository, final IWriteFileStoreHandler fsHandler) {
        m_fsHandler = fsHandler;
        m_dataRepository = repository;
    }

    @Override
    public final void setValue(final DataCell cell) {
        if (cell instanceof FileStoreCell) {
            final FileStoreCell fsCell = (FileStoreCell)cell;

            // handle loops
            if (mustBeFlushedPriorSave(fsCell)) {
                try {
                    final FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
                    final FileStoreKey[] fileStoreKeys = new FileStoreKey[fileStores.length];

                    for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                        fileStoreKeys[fileStoreIndex] =
                            m_fsHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
                    }

                    // update file store keys without calling post-construct.
                    FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository, false);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        // NB: Missing Value checks is expected to happen before cell is actually written. See RowWriteAccess.
        setValueImpl(cell);
    }

    protected abstract void setValueImpl(final DataCell cell);

    // TODO why do we need to flush? problem with heap cache!
    private boolean mustBeFlushedPriorSave(final FileStoreCell cell) {
        final FileStore[] fileStores = FileStoreUtil.getFileStores(cell);
        for (FileStore fs : fileStores) {
            if (m_fsHandler.mustBeFlushedPriorSave(fs)) {
                return true;
            }
        }
        return false;
    }
}
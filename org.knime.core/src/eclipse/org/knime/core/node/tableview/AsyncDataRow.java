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
 *
 * History
 *   Aug 27, 2018 (hornm): created
 */
package org.knime.core.node.tableview;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

/**
 * A row that is not immediately available and requires some time to be loaded (e.g. from a remote endpoint). Class is
 * intended for the UI only!
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 3.7
 */
public class AsyncDataRow implements DataRow {

    private int m_numCells;
    private CompletableFuture<DataRow> m_futureRow;
    private long m_rowIndex;

    /**
     * Creates a new async data row.
     *
     * @param rowIndex the row index of this row
     * @param numCells the number of cells in the row
     * @param futureRow the row still requires time for loading - will be used as soon as the loading is done
     */
    public AsyncDataRow(final long rowIndex, final int numCells, final CompletableFuture<DataRow> futureRow) {
        m_rowIndex = rowIndex;
        m_numCells = numCells;
        m_futureRow = futureRow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumCells() {
        return m_numCells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowKey getKey() {
        if (m_futureRow.isCompletedExceptionally()) {
            return new RowKey("FAILED LOADING");
        } else if (m_futureRow.isDone()) {
            return m_futureRow.getNow(null).getKey();
        } else {
            return new RowKey("Loading ... (" + m_rowIndex + ")");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final int index) {
        if (m_futureRow.isDone() && !m_futureRow.isCompletedExceptionally()) {
            return m_futureRow.getNow(null).getCell(index);
        } else {
            return new DataCell() {

                @Override
                public String toString() {
                    return "";
                }

                @Override
                public int hashCode() {
                    return 0;
                }

                @Override
                protected boolean equalsDataCell(final DataCell dc) {
                    return false;
                }
            };
        }
    }
}

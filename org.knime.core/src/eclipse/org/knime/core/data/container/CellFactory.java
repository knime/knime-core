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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.container;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.ExecutionMonitor;

/**
 * Factory for a ColumnRearranger to provide new columns which are, e.g. appended to a given table.
 *
 * <p>
 * Note, it's strongly recommend to extend the abstract classes {@link AbstractCellFactory} and
 * {@link SingleCellFactory} instead of implementing this interface. These classes also enable the parallel processing
 * of the input table and access to a {@link org.knime.core.data.filestore.FileStoreFactory}.
 *
 * @see org.knime.core.data.container.ColumnRearranger
 * @see org.knime.core.data.container.ColumnRearranger#append(CellFactory)
 * @author Bernd Wiswedel, University of Konstanz
 * @noimplement This interface is not intended to be implemented by clients, use {@link AbstractCellFactory} or
 *              {@link SingleCellFactory} instead
 */
public interface CellFactory {
    /**
     * Get the new cells for a given row. These cells are incorporated into the existing row. The way it is done is
     * defined through the ColumnRearranger using this object.
     *
     * @param row The row of interest.
     * @return The new cells to that row.
     * @throws org.knime.core.node.KNIMEException.KNIMERuntimeException If there is no mapping available (unchecked to
     *             be API compatible)
     * @noreference This method is not intended to be referenced by clients, instead call
     *              {@link #getCells(DataRow, long)}
     */
    default DataCell[] getCells(final DataRow row) {
        throw new NotImplementedException("No implementation for either of the CellFactory#getCells method provided.");
    }

    /**
     * Get the new cells for a given row. These cells are incorporated into the existing row. The way it is done is
     * defined through the ColumnRearranger using this object.
     *
     * @param row The row of interest.
     * @param rowIndex the index of the row of interest
     * @return The new cells to that row.
     * @throws org.knime.core.node.KNIMEException.KNIMERuntimeException If there is no mapping available (unchecked to
     *             be API compatible)
     * @since 5.0
     */
    default DataCell[] getCells(final DataRow row, final long rowIndex) {
        return getCells(row);
    }

    /**
     * The column specs for the cells that are generated in the getCells() method. This method is only called once,
     * there is no need to cache the return value. The length of the returned array must match the length of the array
     * returned by the getCells(DataRow) method and also the types must match, i.e. the type of the respective
     * DataColumnSpec must be of the same type or a super type of the cell as returned by getCells(DataRow).
     *
     * @return The specs to the newly created cells.
     */
    DataColumnSpec[] getColumnSpecs();

    /**
     * Indicates whether calls to {@link #getCells(DataRow)} change the state of an instance.
     * E.g. an index is incremented or a Map updated.<br>
     * Defaults to true.
     *
     * @return whether a call to {@link #getCells(DataRow)} can change the state of this instance
     * @since 4.6
     */
    default boolean hasState() {
        return true;
    }

    /**
     * Returns the indices of the columns needed by the {@link #getCells(DataRow)} method. If all columns are needed or
     * it isn't known which columns are needed, then {@link Optional#empty()} can be returned (this is also the
     * default).
     *
     * It is highly recommended to overwrite this method because it allows the backend to only load the required columns
     * which can lead to dramatic performance increases for wide tables.
     *
     * @return the indices of the columns needed by the CellFactory or {@link Optional#empty()} if all columns are
     *         needed.
     * @since 4.6
     */
    default Optional<int[]> getRequiredColumns() {
        return Optional.empty();
    }

    /**
     * This method is called when a row has been processed. It allows the implementor to set progress in the execution
     * monitor and also some meaningful progress message.
     * <p>
     * Note, you don't need to check <code>exec.checkCanceled()</code> in the implementation as this is done in the
     * calling class.
     *
     * @param curRowNr The number of the row just processed
     * @param rowCount The total number of rows.
     * @param lastKey The row's key.
     * @param exec The execution monitor to report progress to.
     * @deprecated use/implement {@link #setProgress(long, long, RowKey, ExecutionMonitor)} instead which supports more
     *             than {@link Integer#MAX_VALUE} rows
     */
    @Deprecated
    void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec);

    /**
     * This method is called when a row has been processed. It allows the implementor to set progress in the execution
     * monitor and also some meaningful progress message.
     * <p>
     * Note, you don't need to check <code>exec.checkCanceled()</code> in the implementation as this is done in the
     * calling class.
     *
     * @param curRowNr The number of the row just processed
     * @param rowCount The total number of rows.
     * @param lastKey The row's key.
     * @param exec The execution monitor to report progress to.
     * @since 3.0
     */
    default void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
        setProgress((int) curRowNr, KnowsRowCountTable.checkRowCount(rowCount), lastKey, exec);
    }
}

/*
 *
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
 * History
 *   Jun 20, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;

/**
 * Convenience implementation of a cell factory with one new column.
 *
 * The cells can be produced concurrently, see {@link Parallelization} for details.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class SingleCellFactory extends AbstractCellFactory {

    /**
     * Create new cell factory that provides one column given by newColSpec. The calculation is done sequentially (no
     * parallel processing of input).
     *
     * @param newColSpec The spec of the new column.
     * @see #SingleCellFactory(Parallelization, DataColumnSpec)
     */
    protected SingleCellFactory(final DataColumnSpec newColSpec) {
        super(newColSpec);
    }

    /**
     * Create new cell factory that provides one column given by newColSpec. The calculation is done sequentially (no
     * parallel processing of input).
     *
     * @param parallelziation The parallelization mode. See {@link Parallelization}.
     * @param newColSpec The spec of the new column.
     */
    protected SingleCellFactory(final Parallelization parallelziation, final DataColumnSpec newColSpec) {
        super(parallelziation, newColSpec);
    }

    /** Create new cell factory that provides one column given by newColSpec.
     * @param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param newColSpec The spec of the new column.
     * @see #setParallelProcessing(boolean)
     * @since 2.5
     * @deprecated
     */
    @Deprecated(since = "5.4")
    protected SingleCellFactory(final boolean processConcurrently,
            final DataColumnSpec newColSpec) {
        super(processConcurrently, newColSpec);
    }

    /** Create new cell factory that provides one column given by newColSpec.
     * @param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param workerCount see {@link #setParallelProcessing(boolean, int, int)}
     * @param maxQueueSize see {@link #setParallelProcessing(boolean, int, int)}
     * @param newColSpec The spec of the new column.
     * @see #setParallelProcessing(boolean, int, int)
     * @since 2.5
     * @deprecated
     */
    @Deprecated(since = "5.4")
    protected SingleCellFactory(final boolean processConcurrently,
            final int workerCount, final int maxQueueSize,
            final DataColumnSpec newColSpec) {
        super(processConcurrently, workerCount, maxQueueSize, newColSpec);
    }

    @Override
    public DataCell[] getCells(final DataRow row, final long rowIndex) {
        return new DataCell[] { getCell(row, rowIndex) };
    }

    /**
     * Called from getCells. Return the single cell to be returned.
     * @param row The reference row.
     * @return The new cell.
     */
    public DataCell getCell(final DataRow row) {
        throw new NotImplementedException(
            "No implementation for either of the SingleCellFactory#getCell methods provided.");
    }

    /**
     * Called from {@link #getCells(DataRow, long)}.
     * Overwrite this method if you need access to the row index, otherwise overwrite {@link #getCell(DataRow)}
     *
     * @param row the input row
     * @param rowIndex the index of input row
     * @return the new cell
     * @since 5.0
     */
    public DataCell getCell(final DataRow row, final long rowIndex) {
        return getCell(row);
    }

}

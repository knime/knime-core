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
 *   Sep 2, 2011 (wiswedel): created
 */
package org.knime.core.data.sort;

import java.util.Collection;
import java.util.Comparator;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Table sorter for sorting {@link BufferedDataTable} objects. The returned
 * table is also of class {@link BufferedDataTable}. In comparison to
 * {@link DataTableSorter} this class will take advantage of certain flow
 * properties when dealing with table objects, for instance it will respect
 * the node's memory policy or better handle blob objects (which will be
 * referenced in case of {@link BufferedDataTable} but copied for any other
 * type of table).
 *
 * <p>Usage: Client implementations will initialize this object with the table
 * to be sorted, set properties using the varies set-methods (defaults are
 * generally fine) and finally call the {@link #sort(ExecutionContext)} method.
 *
 * <p>For details on the sorting mechanism see the <a href="package.html">
 * package description</a>.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class BufferedDataTableSorter extends TableSorter {

    /** Used to create temporary and final output table. */
    private ExecutionContext m_execContext;

    /** Inits table sorter using the sorting according to
     * {@link #setSortColumns(Collection, boolean[])}.
     *
     * @param inputTable The table to sort
     * @param inclList Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @param sortAscending Passed on to
     * {@link #setSortColumns(Collection, boolean[])}.
     * @throws NullPointerException If any argument is null.
     * @throws IllegalArgumentException If arguments are inconsistent.
     */
    public BufferedDataTableSorter(final BufferedDataTable inputTable,
            final Collection<String> inclList, final boolean[] sortAscending) {
        super(inputTable, inputTable.getRowCount(), inclList, sortAscending);
    }

    /** Inits sorter on argument table with given row comparator.
     * @param inputTable Table to sort.
     * @param rowComparator Passed to {@link #setRowComparator(Comparator)}. */
    public BufferedDataTableSorter(final BufferedDataTable inputTable,
            final Comparator<DataRow> rowComparator) {
        super(inputTable, inputTable.getRowCount(), rowComparator);
    }

    /** Sorts the table passed in the constructor according to the settings
     * and returns the sorted output table.
     * @param ctx To report progress & create temporary and final output tables.
     * @return The sorted output.
     * @throws CanceledExecutionException If canceled. */
    public BufferedDataTable sort(final ExecutionContext ctx)
            throws CanceledExecutionException {
        if (ctx == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_execContext = ctx;
        try {
            return (BufferedDataTable)super.sortInternal(ctx);
        } finally {
            m_execContext = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    DataContainer createDataContainer(final DataTableSpec spec,
            final boolean forceOnDisk) {
        return m_execContext.createDataContainer(
                spec, true, forceOnDisk ? 0 : -1);
    }

}

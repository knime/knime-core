/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   10.06.2014 (Marcel Hanser): created
 */
package org.knime.core.data.sort;

import java.util.Comparator;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 * Column-wise table sorter which uses the {@link DataTableSorter} internally. If a {@link ExecutionContext} is
 * available the {@link ColumnBufferedDataTableSorter} is the more appropriate and flexible choice. The result contains
 * defined columns only.
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public class ColumnDataTableSorter extends AbstractColumnTableSorter {

    /**
     * The constructor is identical to {@link #ColumnDataTableSorter(DataTableSpec, int, String...)} with
     * {@link DataTableSpec#getColumnNames()} as the last input.
     *
     * @param spec the spec
     * @param rowsCount the amount of rows of the data table, if known -1 otherwise
     * @throws InvalidSettingsException if arguments are inconsistent.
     * @throws NullPointerException if any argument is null.
     */
    public ColumnDataTableSorter(final DataTableSpec spec, final int rowsCount) //
        throws InvalidSettingsException {
        this(spec, rowsCount, spec.getColumnNames());
    }

    /**
     * @param spec the spec
     * @param rowsCount the row count
     * @param columnsToSort the columns to sort
     * @throws InvalidSettingsException thrown the spec and the columns to sort are incompatible
     */
    public ColumnDataTableSorter(final DataTableSpec spec, final int rowsCount, final String... columnsToSort)
        throws InvalidSettingsException {
        super(spec, rowsCount, columnsToSort);
    }

    /**
     * A {@link SortingDescription} defines a set of columns which should be packaged and sorted together and also the
     * comparison function. This enables a very fine granular sorting configuration.
     *
     * @param spec the spec
     * @param rowsCount the row count
     * @param sortingDescriptions the columns to sort
     * @throws InvalidSettingsException thrown the spec and the columns to sort are incompatible
     */
    public ColumnDataTableSorter(final DataTableSpec spec, final int rowsCount,
        final SortingDescription... sortingDescriptions) throws InvalidSettingsException {
        super(spec, rowsCount, sortingDescriptions);
    }

    /**
     * Performs the sorting and calls the result consumer with each row of the sorted result.
     *
     * @param dataTable the data table
     * @param executionMonitor the execution monitor
     * @param resultConsumer the result listener
     * @throws CanceledExecutionException the canceled execution exception
     */
    @Override
    public void sort(final DataTable dataTable, final ExecutionMonitor executionMonitor,
        final SortingConsumer resultConsumer) throws CanceledExecutionException {
        super.sort(dataTable, executionMonitor, resultConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    AbstractTableSorter createTableSorter(final long rowCount, final DataTableSpec spec,
        final Comparator<DataRow> rowComparator) {
        return new DataTableSorter(rowCount, spec, rowComparator);
    }
}

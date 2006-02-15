/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.filter.column;

import java.util.ArrayList;

import de.unikn.knime.base.data.filter.column.FilterColumnTable;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * The model for the column filter which extracts certain columns from the input
 * <code>DataTable</code> using a list of columns to exclude.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
final class FilterColumnNodeModel extends NodeModel {

    /**
     * The input port used here.
     */
    static final int INPORT = 0;

    /**
     * The output port used here.
     */
    static final int OUTPORT = 0;

    /**
     * the excluded settings.
     */
    static final String KEY = "exclude";

    /*
     * List contains the data cells to exclude.
     */
    private final ArrayList<DataCell> m_list;

    /**
     * Creates a new filter model with one and in- and output.
     * 
     * @see #reset
     */
    FilterColumnNodeModel() {
        super(1, 1);
        m_list = new ArrayList<DataCell>();
    }

    /**
     * Resets the internal list of columns to exclude.
     */
    protected void reset() {

    }

    /**
     * Creates a new <code>ColumnFilterTable</code> and returns it.
     * 
     * @param data The table for which to create the filtered output table
     * @param exec The execution monitor.
     * @return The filtered table.
     * @throws Exception if current settings are invalid
     * 
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] data,
            final ExecutionMonitor exec) throws Exception {

        assert (data != null && data.length == 1 && data[INPORT] != null);

        final DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        final DataTableSpec outSpec = createOutDataTableSpec(inSpec);
        final DataCell[] cols = new DataCell[outSpec.getNumColumns()];
        for (int c = 0; c < cols.length; c++) {
            cols[c] = outSpec.getColumnSpec(c).getName();
        }
        return new DataTable[] {new FilterColumnTable(data[INPORT], cols)};
    }

    /**
     * Excludes a number of columns from the input spec and generates a new
     * ouput spec.
     * 
     * @param inSpecs The input table spec.
     * @return outSpecs The output table spec with some excluded columns.
     * 
     * @throws InvalidSettingsException If the selected column is not available
     *             in the DataTableSpec.
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs != null);

        return new DataTableSpec[] {createOutDataTableSpec(inSpecs[INPORT])};

    }

    /*
     * Creates the output data table spec according to the current settings.
     * Throws an InvalidSettingsException if colums are specified that don't
     * exist in the input table spec. 
     */
    private DataTableSpec createOutDataTableSpec(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        assert inSpec != null;
        // check if excluded columns exist
        if (m_list.isEmpty()) {
            return inSpec;
        }
        // check if all specified columns exist in the input spec
        for (DataCell name : m_list) {
            if (!inSpec.containsName(name)) {
                throw new InvalidSettingsException("Column '" + name
                        + "' not found.");
            }
        }
        // counter for included columns
        int j = 0;
        // compose list of included column indices
        // which are the original minus the excluded ones
        final int[] columns = new int[inSpec.getNumColumns() - m_list.size()];
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            // if exclude does not contain current column name
            if (!m_list.contains(inSpec.getColumnSpec(i).getName())) {
                columns[j] = i;
                j++;
            }
        }
        assert (j == columns.length);
        // return the new spec
        return FilterColumnTable.createFilterTableSpec(inSpec, columns);

    }

    /**
     * Writes number of filtered columns, and the names as <code>DataCell</code>
     * to the given settings.
     * 
     * @param settings The object to save the settings into.
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        settings.addDataCellArray(KEY, m_list.toArray(new DataCell[0]));
    }

    /**
     * Reads the filtered columns.
     * 
     * @param settings to read from.
     * @throws InvalidSettingsException If the settings does not contain the
     *             size or a particular column key.
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        // clear exclude column list
        m_list.clear();
        // get list of excluded columns
        DataCell[] columns = settings.getDataCellArray(KEY, 
                m_list.toArray(new DataCell[0]));
        for (int i = 0; i < columns.length; i++) {
            m_list.add(columns[i]);
        }

    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        // true because the filter model does not care if there are columns to
        // exclude are available
    }

} // FilterColumnNodeModel

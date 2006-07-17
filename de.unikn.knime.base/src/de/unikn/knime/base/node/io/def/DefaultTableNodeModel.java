/*
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
package de.unikn.knime.base.node.io.def;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.def.DefaultTable;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * This is the model for the DefaultTable node. It only holds the DefaultTable.
 * It provides similar cosntructors as the DefaultTable.
 * 
 * @author ohl University of Konstanz
 */
public class DefaultTableNodeModel extends NodeModel {

    private final DataTable m_table;

    /**
     * @see de.unikn.knime.core.data.def.DefaultTable
     * @param rows see DefaultTable constructor
     * @param columnNames see DefaultTable constructor
     * @param columnTypes see DefaultTable constructor
     */
    public DefaultTableNodeModel(final DataRow[] rows,
            final String[] columnNames, final DataType[] columnTypes) {
        this(rows, new DataTableSpec(columnNames, columnTypes));
    }

    /**
     * Also this constructor is available in <code>DefaultTable</code>.
     * @param rows Passed to constructor of <code>DefaultTable</code>
     * @param spec Passed to constructor of <code>DefaultTable</code>
     * @see de.unikn.knime.core.data.def.DefaultTable#DefaultTable( DataRow[],
     *      DataTableSpec)
     */
    public DefaultTableNodeModel(final DataRow[] rows, final DataTableSpec spec)
    {
        super(0, 1); // tell the super we need no input and one output
        m_table = new DefaultTable(rows, spec);
    }

    /**
     * @see de.unikn.knime.core.data.def.DefaultTable
     * @param data see DefaultTable constructor
     * @param rowHeader see DefaultTable constructor
     * @param colHeader see DefaultTable constructor
     */
    public DefaultTableNodeModel(final Object[][] data,
            final String[] rowHeader, final String[] colHeader) {
        super(0, 1); // tell'em we need no input but one output please

        m_table = new DefaultTable(data, rowHeader, colHeader);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    protected BufferedDataTable[] execute(
            final BufferedDataTable[] data, final ExecutionContext exec) 
            throws Exception {
        BufferedDataTable out = 
            BufferedDataTable.createBufferedDataTable(m_table, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        // we don't destroy our static datatable.
    }

    /**
     * The standard table can always provide a DataTableSpec, that's why
     * it is also executable: returns true (so to say).
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        return new DataTableSpec[]{m_table.getDataTableSpec()}; 
    }
    
    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
            throws InvalidSettingsException {
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    protected void validateSettings(final NodeSettingsRO settings) 
            throws InvalidSettingsException {
    }
    
}

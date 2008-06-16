/*
 * ---------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.io.def;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model for the {@link org.knime.core.data.def.DefaultTable}
 * node. It only holds the {@link org.knime.core.data.def.DefaultTable}.
 * It provides similar cosntructors as the
 * {@link org.knime.core.data.def.DefaultTable}.
 * 
 * @author ohl University of Konstanz
 */
public class DefaultTableNodeModel extends NodeModel {

    private final DataTable m_table;

    /**
     * @see org.knime.core.data.def.DefaultTable
     * @param rows see DefaultTable constructor
     * @param columnNames see DefaultTable constructor
     * @param columnTypes see DefaultTable constructor
     */
    public DefaultTableNodeModel(final DataRow[] rows,
            final String[] columnNames, final DataType[] columnTypes) {
        this(rows, new DataTableSpec(columnNames, columnTypes));
    }

    /**
     * Also this constructor is available in
     * {@link org.knime.core.data.def.DefaultTable}.
     * 
     * @param rows Passed to constructor of
     *            {@link org.knime.core.data.def.DefaultTable}
     * @param spec Passed to constructor of
     *            {@link org.knime.core.data.def.DefaultTable}
     * @see org.knime.core.data.def.DefaultTable#DefaultTable( DataRow[],
     *      DataTableSpec)
     */
    public DefaultTableNodeModel(final DataRow[] rows, final DataTableSpec spec) {
        super(0, 1); // tell the super we need no input and one output
        m_table = new DefaultTable(rows, spec);
    }

    /**
     * @see org.knime.core.data.def.DefaultTable
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
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable out = exec.createBufferedDataTable(m_table, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // we don't destroy our static datatable.
    }

    /**
     * The standard table can always provide a table spec, that's why it is also
     * executable: returns <code>true</code> (so to say).
     * 
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        return new DataTableSpec[]{m_table.getDataTableSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }
}

/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Implements the <code>NodeModel</code> for the ScatterPlot node. It stores
 * two selected columns and the number of rows to display. Zoom factor and dot
 * size are adjusted and stored by the view. Also, the dots to be displayed by
 * the plotter are stored here. They will be extracted from the input data table
 * when the model is executed.
 * 
 * @author ohl University of Konstanz
 */
public class ScatterPlotNodeModel extends NodeModel {

    /**
     * Settings object key for the starting row.
     */
    public static final String CFGKEY_FROMROW = "FromRow";

    /**
     * Settings object key for the number of row to display.
     */
    public static final String CFGKEY_ROWCNT = "RowCount";

    /* the numbers the plotter starts with, to be executable w/o dlg */
    private static final int DEFAULT_FIRSTROW = 1;

    private static final int DEFAULT_ROWCOUNT = 20000;

    private static final String FILE_NAME = "scatterplotterData.zip";

    /**
     * the range of rows we are supposed to display.
     */
    private int m_firstRow;

    private int m_numRows;

    // the data rows we are looking at
    private DataArray m_rows;

    /**
     * Creates a new Scatterplot model.
     */
    public ScatterPlotNodeModel() {

        super(1, 0); // we need one input, no outputs.
        m_firstRow = DEFAULT_FIRSTROW;
        m_numRows = DEFAULT_ROWCOUNT;
        m_rows = null;
    }

    /**
     * The execute function refreshs the contents of the RowInfo container. The
     * model will create new RowInfos (as many as set in the dialog)
     * 
     * @see org.knime.core.node.NodeModel
     *      #execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        assert inData != null;
        assert inData.length == 1;
        if ((m_firstRow < 1) || (m_numRows < 1)) {
            throw new IllegalStateException("Setup the model's settings "
                    + "before you execute it.");
        }

        BufferedDataTable table = inData[0];

        int nrOfRows = Math.min(table.getRowCount(), m_numRows);
        m_rows = new DefaultDataArray(table, m_firstRow, nrOfRows, exec);

        return new BufferedDataTable[]{};
    }

    /**
     * releases the current data table and clears the model.
     */
    @Override
    public void reset() {
        m_rows = null;
    }

    /**
     * returns the content of the model.
     * 
     * @return the current collection of dots to be plotted.
     */
    public DataArray getRowsContainer() {
        return m_rows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // We don't need to create a DataTableSpec as we have no outputs.
        // Just return the 'executable' status

        if (m_firstRow < 1) {
            throw new InvalidSettingsException("First data row to plot not"
                    + " specified.");
        }
        if (m_numRows < 1) {
            throw new InvalidSettingsException(
                    "Amount of data rows to plot not" + " specified.");
        }
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (m_firstRow > 0) {
            settings.addInt(CFGKEY_FROMROW, m_firstRow);
        }
        if (m_numRows > 0) {
            settings.addInt(CFGKEY_ROWCNT, m_numRows);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettings(settings, /* validateOnly= */true);
    }

    private void readSettings(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {

        // read the values into local variables first
        int first = settings.getInt(CFGKEY_FROMROW);
        int count = settings.getInt(CFGKEY_ROWCNT);

        // check their correctness and completeness.
        String msg = "";
        if (first < 1) {
            msg += "The first data vector to display must be specified.\n";
        }
        if (count < 1) {
            msg += "The number of data vectors to display must be specified.\n";
        }
        if (!msg.equals("")) {
            throw new InvalidSettingsException(msg);
        }

        // now take them over - if we are supposed to.
        if (!validateOnly) {
            m_firstRow = first;
            m_numRows = count;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File data = new File(internDir, FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(data);
        int rowCount = table.getRowCount();
        m_rows = new DefaultDataArray(table, 1, rowCount, exec);
    }

    /**
     * {@inheritDoc}
     */
@Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_rows != null) {
            File rows = new File(internDir, FILE_NAME);
            DataContainer.writeToZip(m_rows, rows, exec);
        }
    }
}

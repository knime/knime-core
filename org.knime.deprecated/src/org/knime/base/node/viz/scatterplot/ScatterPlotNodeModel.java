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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

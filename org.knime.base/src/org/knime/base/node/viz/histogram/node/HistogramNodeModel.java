/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.InteractiveHistogramPlotter;
import org.knime.base.node.viz.histogram.InteractiveHistogramProperties;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * The NodeModel class of the interactive histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(HistogramNodeModel.class);
    
    private static final String CFG_DATA = "histogramData";

    private static final String CFG_SETTINGS = "histogramSettings";

    /** The Rule2DPlotter is the core of the view. */
    private InteractiveHistogramPlotter m_plotter;

    /**
     * The <code>HistogramProps</code> class which holds the properties dialog
     * elements.
     */
    private InteractiveHistogramProperties m_properties;

    private static final int INITIAL_WIDTH = 300;

    /**
     * Used to store the attribute column name in the settings.
     */
    static final String CFGKEY_X_COLNAME = "HistogramXColName";

    /** The <code>BufferedDataTable</code> of the input port. */
    private DataTable m_data;

    /** The name of the x column. */
    private String m_xColName;
    /**
     * The constructor.
     */
    protected HistogramNodeModel() {
        super(1, 0); // one input, no outputs
        m_data = null;
        setAutoExecutable(true);
        // m_attrColName = null;
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_X_COLNAME, m_xColName);
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        /*
         * try { settings.getString(CFGKEY_ATTRCOLNAME); } catch
         * (InvalidSettingsException e) { throw new
         * InvalidSettingsException("Attribute column " + "not specified"); }
         */
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_xColName = settings.getString(CFGKEY_X_COLNAME);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, CFG_DATA);
        m_data = DataContainer.readFromZip(f);
        File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
        FileInputStream in = new FileInputStream(settingsFile);
        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        try {
            m_xColName = settings.getString(CFGKEY_X_COLNAME);
        } catch (InvalidSettingsException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals( java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, CFG_DATA);
        DataContainer.writeToZip(m_data, f, exec);
        NodeSettings settings = new NodeSettings(CFG_SETTINGS);
        settings.addString(CFGKEY_X_COLNAME, m_xColName);
        File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
        FileOutputStream out = new FileOutputStream(settingsFile);
        settings.saveToXML(out);
    }

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        LOGGER.info(
                "Entering execute(inData, exec) of class HistogramNodeModel.");
        // create the data object
        m_data = inData[0];
        DataTableSpec tableSpec = m_data.getDataTableSpec();
        String selectedXCol = m_xColName;
        if (selectedXCol == null && tableSpec.getNumColumns() > 0) {
            // set the first column of the table as default x column
            selectedXCol = tableSpec.getColumnSpec(0).getName();
        }
        // create the properties panel
        m_properties = 
            new InteractiveHistogramProperties(AggregationMethod.COUNT);
        // create the plotter
        m_plotter = new InteractiveHistogramPlotter(INITIAL_WIDTH, 
                tableSpec, m_properties, getInHiLiteHandler(0), selectedXCol);
        m_plotter.setBackground(ColorAttr.getBackground());
        if (m_data != null) {
            exec.setMessage("Adding data rows to histogram...");
            final int noOfRows = inData[0].getRowCount();
            final double progressPerRow = 1.0 / noOfRows;
            double progress = 0.0;
            for (DataRow row : m_data) {
                m_plotter.addDataRow(row);
                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to histogram...");
                exec.checkCanceled();
            }
            exec.setMessage("Creating histogram data");
            m_plotter.lastDataRowAdded();
        }
        LOGGER.info(
                "Exiting execute(inData, exec) of class HistogramNodeModel.");
        return new BufferedDataTable[0];
    }

    /**
     * @return the data of the input port.
     */
    public DataTable getData() {
        return m_data;
    }

    /*
     * protected String getSelectedColumnName() { return m_attrColName; }
     */

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_data = null;
        m_plotter = null;
        m_properties = null;
    }

    /**
     * @return the plotter
     */
    protected AbstractHistogramPlotter getPlotter() {
        return m_plotter;
    }
    
    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        /*
         * if ((m_attrColName == null) || (m_attrColName.length() == 0)) { throw
         * new InvalidSettingsException("Attribute column must be" + "
         * specified."); } int colIdx =
         * inSpecs[0].findColumnIndex(m_attrColName); if (colIdx < 0) { throw
         * new InvalidSettingsException("Specified attribute column" + " not in
         * input table"); }
         */
        return new DataTableSpec[0];
    }
}

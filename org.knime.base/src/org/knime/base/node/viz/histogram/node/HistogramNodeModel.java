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
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.interactive.InteractiveHistogramProperties;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * The NodeModel class of the interactive histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(HistogramNodeModel.class);
    
//    private static final String CFG_DATA = "histogramData";
    
    /**Default number of rows to use.*/
    protected static final int DEFAULT_NO_OF_ROWS = 2500;
    
    private static final String CFG_SETTINGS = "histogramSettings";
    
    /**Settings name of the number of rows.*/
    protected static final String CFGKEY_NO_OF_ROWS = "noOfRows";
    

    /**
     * Used to store the attribute column name in the settings.
     */
    static final String CFGKEY_X_COLNAME = "HistogramXColName";

    /** The Rule2DPlotter is the core of the view. */
    private InteractiveHistogramPlotter m_plotter;

    /**
     * The <code>HistogramProps</code> class which holds the properties dialog
     * elements.
     */
    private InteractiveHistogramProperties m_properties;

    /** The <code>BufferedDataTable</code> of the input port. */
    private DataTable m_data;

    /** The name of the x column. */
    private final SettingsModelString m_xColName = new SettingsModelString(
            HistogramNodeModel.CFGKEY_X_COLNAME, "");
    
    private final SettingsModelInteger m_noOfRows = new SettingsModelInteger(
            HistogramNodeModel.CFGKEY_NO_OF_ROWS, 
            HistogramNodeModel.DEFAULT_NO_OF_ROWS);
    /**
     * The constructor.
     */
    protected HistogramNodeModel() {
        super(1, 0); // one input, no outputs
        m_data = null;
        //setAutoExecutable(true);
        // m_attrColName = null;
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_noOfRows.saveSettingsTo(settings);
        m_xColName.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        m_noOfRows.validateSettings(settings);
        m_xColName.validateSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_noOfRows.loadSettingsFrom(settings);
        } catch (Exception e) {
            // In case of older nodes the row number is not available
        }
        m_xColName.loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
//        final File f = new File(nodeInternDir, CFG_DATA);
//        m_data = DataContainer.readFromZip(f);
        final File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
        final FileInputStream in = new FileInputStream(settingsFile);
        final NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        try {
            m_noOfRows.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e1) {
            m_noOfRows.setIntValue(DEFAULT_NO_OF_ROWS);
        }
        try {
            m_xColName.loadSettingsFrom(settings);
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
            final ExecutionMonitor exec) throws IOException {
//        final File f = new File(nodeInternDir, CFG_DATA);
//        DataContainer.writeToZip(m_data, f, exec);
        final NodeSettings settings = new NodeSettings(CFG_SETTINGS);
        m_noOfRows.saveSettingsTo(settings);
        m_xColName.saveSettingsTo(settings);
        final File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
        final FileOutputStream out = new FileOutputStream(settingsFile);
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
        final DataTableSpec tableSpec = m_data.getDataTableSpec();
        String selectedXCol = m_xColName.getStringValue();
        if (selectedXCol == null && tableSpec.getNumColumns() > 0) {
            // set the first column of the table as default x column
            selectedXCol = tableSpec.getColumnSpec(0).getName();
        }
        // create the properties panel
        m_properties = 
            new InteractiveHistogramProperties(AggregationMethod.COUNT);
        // create the plotter
        m_plotter = new InteractiveHistogramPlotter(tableSpec, m_properties, 
                getInHiLiteHandler(0), selectedXCol);
        m_plotter.setBackground(ColorAttr.getBackground());
        if (m_data != null) {
            final int selectedNoOfRows = m_noOfRows.getIntValue();
            final int noOfRows = inData[0].getRowCount();
            if ((selectedNoOfRows) < noOfRows) {
                setWarningMessage("Only the first " + selectedNoOfRows + " of " 
                        + noOfRows + " rows are displayed.");
            }
            exec.setMessage("Adding data rows to histogram...");
            final double progressPerRow = 1.0 / noOfRows;
            double progress = 0.0;
            final RowIterator rowIterator = m_data.iterator();
            for (int i = 0; i < selectedNoOfRows && rowIterator.hasNext(); i++) {
                final DataRow row = rowIterator.next();
                m_plotter.addDataRow(row);
                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to histogram...");
                exec.checkCanceled();
            }
            exec.setProgress(1.0, "Histogram finished.");
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        return new DataTableSpec[0];
    }
}

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
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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

    /**Default number of rows to use.*/
    protected static final int DEFAULT_NO_OF_ROWS = 2500;

    /**Settings name for the take all rows select box.*/
    protected static final String CFGKEY_ALL_ROWS = "allRows";
    
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
            CFGKEY_X_COLNAME, "");
    
    private final SettingsModelInteger m_noOfRows = new SettingsModelInteger(
            CFGKEY_NO_OF_ROWS, DEFAULT_NO_OF_ROWS);
    
    private final SettingsModelBoolean m_allRows = new SettingsModelBoolean(
            CFGKEY_ALL_ROWS, false);
    
    /**
     * The constructor.
     */
    protected HistogramNodeModel() {
        super(1, 0); // one input, no outputs
        m_data = null;
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
        setAutoExecutable(true);
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_allRows.saveSettingsTo(settings);
        m_noOfRows.saveSettingsTo(settings);
        m_xColName.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        final SettingsModelBoolean model = 
            m_allRows.createCloneWithValidatedValue(settings);
        if (!model.getBooleanValue()) {
            //read the spinner value only if the user hasn't selected to 
            //retrieve all values
            m_noOfRows.validateSettings(settings);
        }
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
            m_allRows.loadSettingsFrom(settings);
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
            final ExecutionMonitor exec) {
        //not necessary since all dialog settings are saved anyway.
        //This method is only needed if we have created internal structures
        //which are necessary to create the view!!!
//        final File f = new File(nodeInternDir, CFG_DATA);
//        m_data = DataContainer.readFromZip(f);
//        final File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
//        final FileInputStream in = new FileInputStream(settingsFile);
//        final NodeSettingsRO settings = NodeSettings.loadFromXML(in);
//        try {
//            m_noOfRows.loadSettingsFrom(settings);
//            m_totalNoOfRows.loadSettingsFrom(settings);
//        } catch (InvalidSettingsException e1) {
//            m_noOfRows.setIntValue(DEFAULT_NO_OF_ROWS);
//            m_totalNoOfRows.setIntValue(DEFAULT_NO_OF_ROWS);
//        }
//        try {
//            m_xColName.loadSettingsFrom(settings);
//        } catch (InvalidSettingsException e) {
//            throw new IOException(e.getMessage());
//        }
    }

    /**
     * @throws CanceledExecutionException 
     * @see org.knime.core.node.NodeModel#saveInternals( java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //not necessary since all dialog settings are saved automatically 
        //with the workspace and reloaded when the user opens it again.
        //This method is only needed if we have created internal structures
        //which are necessary to create the view!!!
//        final File f = new File(nodeInternDir, CFG_DATA);
//        DataContainer.writeToZip(m_data, f, exec);
//        final NodeSettings settings = new NodeSettings(CFG_SETTINGS);
//        m_totalNoOfRows.saveSettingsTo(settings);
//        m_noOfRows.saveSettingsTo(settings);
//        m_xColName.saveSettingsTo(settings);
//        final File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
//        final FileOutputStream out = new FileOutputStream(settingsFile);
//        settings.saveToXML(out);
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
        if (inData == null || inData[0] == null) {
            throw new Exception("No data table available!");
        }
        // create the data object
        m_data = inData[0];
        final int rowCount = inData[0].getRowCount();
        if (m_allRows.getBooleanValue()) {
            //set the actual number of rows in the selected number of rows
            //object since the user wants to display all rows
            m_noOfRows.setIntValue(rowCount);
        }
        final String selectedXCol = m_xColName.getStringValue();
        // create the plotter
        createPlotter(m_data, rowCount, exec, selectedXCol);
        LOGGER.info(
                "Exiting execute(inData, exec) of class HistogramNodeModel.");
        return new BufferedDataTable[0];
    }

    /**
     * @param inData
     * @param exec
     * @param tableSpec
     * @param selectedXCol
     * @throws CanceledExecutionException
     */
    private void createPlotter(final DataTable dataTable, final int noOfRows,
            final ExecutionContext exec, final String selectedXCol) 
    throws CanceledExecutionException {
        final DataTableSpec tableSpec = dataTable.getDataTableSpec();
        // create the properties panel
        m_properties = 
            new InteractiveHistogramProperties(AggregationMethod.COUNT);
        m_plotter = new InteractiveHistogramPlotter(tableSpec, m_properties, 
                getInHiLiteHandler(0), selectedXCol);
        m_plotter.setBackground(ColorAttr.getBackground());
        if (dataTable != null) {
            final int selectedNoOfRows = m_noOfRows.getIntValue();
            //final int noOfRows = inData[0].getRowCount();
            if ((selectedNoOfRows) < noOfRows) {
                setWarningMessage("Only the first " + selectedNoOfRows + " of " 
                        + noOfRows + " rows are displayed.");
            }
            exec.setMessage("Adding data rows to histogram...");
            final double progressPerRow = 1.0 / noOfRows;
            double progress = 0.0;
            final RowIterator rowIterator = dataTable.iterator();
            for (int i = 0; i < selectedNoOfRows && rowIterator.hasNext();
                i++) {
                final DataRow row = rowIterator.next();
                m_plotter.addDataRow(row);
                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to histogram...");
                exec.checkCanceled();
            }
            exec.setProgress(1.0, "Histogram finished.");
            m_plotter.lastDataRowAdded();
        }
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        if (inSpecs == null || inSpecs[0] == null) {
            throw new InvalidSettingsException("No input spec available.");
        }
        final DataTableSpec spec = inSpecs[0];
        if (spec.getNumColumns() < 2) {
            throw new IllegalArgumentException(
                    "Input table should have at least 2 columns.");
        }
        final String xCol = m_xColName.getStringValue();
        if (!spec.containsName(xCol)) {
            if (spec.getNumColumns() > 0) {
                // set the first column of the table as default x column
                m_xColName.setStringValue(spec.getColumnSpec(0).getName());
            } else {
                throw new InvalidSettingsException(
                    "No column found in table specification.");
            }
        }
        return new DataTableSpec[0];
    }
}

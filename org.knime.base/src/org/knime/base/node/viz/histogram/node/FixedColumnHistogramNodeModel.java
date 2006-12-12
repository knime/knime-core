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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import java.io.File;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.impl.fixed.FixedColumnHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.fixed.FixedColumnHistogramProperties;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
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
 * The NodeModel class of the histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedColumnHistogramNodeModel.class);
    
    /**Default number of rows to use.*/
    protected static final int DEFAULT_NO_OF_ROWS = 5000;
    
    /**Settings name for the take all rows select box.*/
    protected static final String CFGKEY_ALL_ROWS = "allRows";
    
    /**Settings name of the number of rows.*/
    protected static final String CFGKEY_NO_OF_ROWS = "noOfRows";
    
    /**Settings name of the x column name.*/
    protected static final String CFGKEY_X_COLNAME = "xColumn";
    /**Settings name of the aggregation column name.*/
    protected static final String CFGKEY_AGGR_COLNAME = "aggrColumn";

    private SettingsModelString m_xColName = new SettingsModelString(
            FixedColumnHistogramNodeModel.CFGKEY_X_COLNAME, "");
    
    private SettingsModelString m_aggrColName = new SettingsModelString(
            FixedColumnHistogramNodeModel.CFGKEY_AGGR_COLNAME, "");
    
    private final SettingsModelInteger m_noOfRows = new SettingsModelInteger(
            FixedColumnHistogramNodeModel.CFGKEY_NO_OF_ROWS, 
            FixedColumnHistogramNodeModel.DEFAULT_NO_OF_ROWS);


    private final SettingsModelBoolean m_allRows = new SettingsModelBoolean(
            CFGKEY_ALL_ROWS, false);
    
    /** The Rule2DPlotter is the core of the view. */
    private FixedColumnHistogramPlotter m_plotter;

    /**
     * The <code>HistogramProps</code> class which holds the properties dialog
     * elements.
     */
    private FixedColumnHistogramProperties m_properties;

    /**
     * The constructor.
     */
    protected FixedColumnHistogramNodeModel() {
        super(1, 0); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
        setAutoExecutable(true);
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
        m_aggrColName.validateSettings(settings);
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
        m_aggrColName.loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_allRows.saveSettingsTo(settings);
        m_noOfRows.saveSettingsTo(settings);
        m_xColName.saveSettingsTo(settings);
        m_aggrColName.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //not necessary since all dialog settings are saved automatically 
        //with the workspace and reloaded when the user opens it again.
        //This method is only needed if we have created internal structures
        //which are necessary to create the view!!!
//        File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
//        FileInputStream in = new FileInputStream(settingsFile);
//        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
//        try {
//            m_noOfRows.loadSettingsFrom(settings);
//        } catch (InvalidSettingsException e1) {
//            //to prevent problems with older work flows
//            m_noOfRows.setIntValue(DEFAULT_NO_OF_ROWS);
//        }
//        try {
//            m_xColName.loadSettingsFrom(settings);
//            m_aggrColName.loadSettingsFrom(settings);
//        } catch (InvalidSettingsException e) {
//            throw new IOException(e.getMessage());
//        }
    }

    /**
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
//        NodeSettings settings = new NodeSettings(CFG_SETTINGS);
//        m_noOfRows.saveSettingsTo(settings);
//        m_xColName.saveSettingsTo(settings);
//        m_aggrColName.saveSettingsTo(settings);
//        File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
//        FileOutputStream out = new FileOutputStream(settingsFile);
//        settings.saveToXML(out);
    }

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        LOGGER.info("Entering execute(inData, exec) of class "
                + "FixedColumnHistogramNodeModel.");
        // create the data object
        BufferedDataTable data = inData[0];
        // create the properties panel
        m_properties = 
            new FixedColumnHistogramProperties(AggregationMethod.COUNT);
        final String xCol = m_xColName.getStringValue();
        final String aggrCol = m_aggrColName.getStringValue();
        // create the plotter
        m_plotter = new FixedColumnHistogramPlotter(data.getDataTableSpec(), 
                m_properties, getInHiLiteHandler(0), xCol, aggrCol);
        m_plotter.setBackground(ColorAttr.getBackground());
        final int rowCount = data.getRowCount();
        if (m_allRows.getBooleanValue()) {
            //set the actual number of rows in the selected number of rows
            //object since the user wants to display all rows
            m_noOfRows.setIntValue(rowCount);
        }
        final int selectedNoOfRows = m_noOfRows.getIntValue();
        if ((selectedNoOfRows) < rowCount) {
            setWarningMessage("Only the first " + selectedNoOfRows + " of " 
                    + rowCount + " rows are displayed.");
        }
        exec.setMessage("Adding data rows to histogram...");
        final double progressPerRow = 1.0 / rowCount;
        double progress = 0.0;
        final RowIterator rowIterator = data.iterator();
        for (int i = 0; i < selectedNoOfRows && rowIterator.hasNext(); i++) {
            final DataRow row = rowIterator.next();
            m_plotter.addDataRow(row);
            progress += progressPerRow;
            exec.setProgress(progress, "Adding data rows to histogram...");
            exec.checkCanceled();
        }
        exec.setProgress(1.0, "Histogram finished.");
        m_plotter.lastDataRowAdded();
        LOGGER.info("Exiting execute(inData, exec) of class "
                + "FixedColumnHistogramNodeModel.");
        return new BufferedDataTable[0];
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_plotter = null;
        m_properties = null;
    }

    /**
     * @return the plotter
     */
    protected FixedColumnHistogramPlotter getPlotter() {
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
            //if the input table has only two columns where only one column
            //is numerical select these two columns as default columns
            //if both are numeric we don't know which one the user wants as
            //aggregation column and which one as x column
            if (spec.getNumColumns() == 2) {
                final DataType type0 = spec.getColumnSpec(0).getType();
                final DataType type1 = spec.getColumnSpec(1).getType();
                if (type0.isCompatible(StringValue.class) 
                        && type1.isCompatible(DoubleValue.class)) {
                    m_xColName.setStringValue(spec.getColumnSpec(0).getName());
                    m_aggrColName.setStringValue(
                            spec.getColumnSpec(1).getName());
                } else if (type0.isCompatible(DoubleValue.class) 
                        && type1.isCompatible(StringValue.class)) {
                    m_xColName.setStringValue(spec.getColumnSpec(1).getName());
                    m_aggrColName.setStringValue(
                            spec.getColumnSpec(0).getName());
                } else {
                    throw new InvalidSettingsException(
                    "Please define the x column name.");
                }
            } else {
                throw new InvalidSettingsException(
                        "Please define the x column name.");
            }
        }
        final String aggrCol = m_aggrColName.getStringValue();
        if (!spec.containsName(aggrCol)) {
            throw new InvalidSettingsException(
                    "Please define the aggregation column name.");
        }
        return new DataTableSpec[0];
    }
}

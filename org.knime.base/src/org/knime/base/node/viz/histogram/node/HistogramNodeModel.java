/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import java.awt.Color;
import java.io.File;
import java.util.Iterator;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.HistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.HistogramDataRow;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
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

    /** The <code>BufferedDataTable</code> of the input port. */
    private DataTable m_data;

    /**The histogram data model which holds all information.*/
    private HistogramDataModel m_model;
    
    private DataTableSpec m_tableSpec;
    
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
        createHistogramModel(m_data, rowCount, exec, selectedXCol);
        LOGGER.info(
                "Exiting execute(inData, exec) of class HistogramNodeModel.");
        return new BufferedDataTable[0];
    }

    private void createHistogramModel(final DataTable dataTable, 
            final int noOfRows, final ExecutionContext exec, 
            final String selectedXCol) 
    throws CanceledExecutionException {
        m_tableSpec = dataTable.getDataTableSpec();
        if (m_tableSpec == null) {
            throw new IllegalArgumentException(
                    "Table specification shouldn't be null");
        }
        final int xColIdx = m_tableSpec.findColumnIndex(selectedXCol);
        if (xColIdx < 0) {
            throw new IllegalArgumentException(
                    "Selected x column not found: " + selectedXCol);
        }
        final DataColumnSpec xColSpec = m_tableSpec.getColumnSpec(xColIdx);
        if (xColSpec == null) {
            throw new IllegalArgumentException(
                    "No column specification found for selected x column:" 
                    + selectedXCol);
        }
        //get the column specification for the first numerical column 
        //as aggregation column
        final int numColumns = m_tableSpec.getNumColumns();
        
        ColorColumn aggrColumn = null;
        for (int i = 0; i < numColumns; i++) {
            final DataColumnSpec spec = m_tableSpec.getColumnSpec(i);
            if (spec.getType().isCompatible(DoubleValue.class)) {
                aggrColumn = new ColorColumn(Color.CYAN, i, spec.getName());
                break;
            }
        }
        if (aggrColumn == null) {
            throw new IllegalArgumentException(
                    "No numeric column found in table specification");
        }
        final int aggrColIdx = aggrColumn.getColumnIndex();
        m_model = new HistogramDataModel(HistogramDataModel.DEFAULT_NO_OF_BINS,
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout(), xColSpec, aggrColumn);
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
                final Color color = 
                    m_tableSpec.getRowColor(row).getColor(false, false);
                final HistogramDataRow histoRow = new HistogramDataRow(
                        row.getKey(), color, row.getCell(xColIdx),
                        row.getCell(aggrColIdx));
                m_model.addDataRow(histoRow);
                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to histogram...");
                exec.checkCanceled();
            }
            exec.setProgress(1.0, "Histogram finished.");
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
        m_model = null;
    }

    /**
     * @return the histogram data model
     */
    protected HistogramDataModel getHistogramModelClone() {
        if (m_model == null) {
            return null;
        }
        return m_model.clone();
    }
    /**
     * @return the {@link DataTableSpec} of the input table
     */
    protected DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    protected Iterator<DataRow> getRows(){
        if (m_data == null) {
            return null;
        }
        return m_data.iterator();
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
            throw new InvalidSettingsException(
                    "Input table should have at least 2 columns.");
        }
        // if we have nominal columns without possible values
        for (DataColumnSpec colSpec : spec) {

            if (!colSpec.getType().isCompatible(DoubleValue.class) 
                    && colSpec.getDomain().getValues() == null) {
                throw new InvalidSettingsException(
                        "Found nominal column without possible values: "
                        + colSpec.getName() 
                        + " Please use DomainCalculator or ColumnFilter node!");
            }
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

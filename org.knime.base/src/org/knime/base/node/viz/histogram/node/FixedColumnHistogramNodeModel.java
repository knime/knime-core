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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.impl.fixed.FixedColumnHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.fixed.FixedColumnHistogramProperties;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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


/**
 * The NodeModel class of the histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedColumnHistogramNodeModel.class);
    
    private static final String CFG_SETTINGS = "fixedColumnHistogramSettings";
    
    /**Settings name of the x column name.*/
    protected static final String CFGKEY_X_COLNAME = "xColumn";
    /**Settings name of the aggregation column name.*/
    protected static final String CFGKEY_AGGR_COLNAME = "aggrColumn";

    private String m_xColName;
    
    private String m_aggrColName;

    /** The Rule2DPlotter is the core of the view. */
    private FixedColumnHistogramPlotter m_plotter;

    /**
     * The <code>HistogramProps</code> class which holds the properties dialog
     * elements.
     */
    private FixedColumnHistogramProperties m_properties;

    private static final int INITIAL_WIDTH = 300;

    /**
     * The constructor.
     */
    protected FixedColumnHistogramNodeModel() {
        super(1, 0); // one input, no outputs
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_X_COLNAME, m_xColName);
        settings.addString(CFGKEY_AGGR_COLNAME, m_aggrColName);
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String xCol = settings.getString(CFGKEY_X_COLNAME);
        if (xCol == null || xCol.length() < 1) {
            throw new InvalidSettingsException(
                    "The x column needs to be defined.");
        }
        String aggrCol = settings.getString(CFGKEY_AGGR_COLNAME);
        if (aggrCol == null || aggrCol.length() < 1) {
            throw new InvalidSettingsException(
                    "The aggregation column needs to be defined.");
        }
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_xColName = settings.getString(CFGKEY_X_COLNAME);
        m_aggrColName = settings.getString(CFGKEY_AGGR_COLNAME);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
        FileInputStream in = new FileInputStream(settingsFile);
        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        try {
            m_xColName = settings.getString(CFGKEY_X_COLNAME);
            m_aggrColName = settings.getString(CFGKEY_AGGR_COLNAME);
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
        NodeSettings settings = new NodeSettings(CFG_SETTINGS);
        settings.addString(CFGKEY_X_COLNAME, m_xColName);
        settings.addString(CFGKEY_AGGR_COLNAME, m_aggrColName);
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
        LOGGER.info("Entering execute(inData, exec) of class "
                + "FixedColumnHistogramNodeModel.");
        // create the data object
        BufferedDataTable data = inData[0];
        // create the properties panel
        m_properties = 
            new FixedColumnHistogramProperties(AggregationMethod.COUNT);
        // create the plotter
        m_plotter = new FixedColumnHistogramPlotter(INITIAL_WIDTH, 
                data.getDataTableSpec(), m_properties, getInHiLiteHandler(0), 
                m_xColName, m_aggrColName);
        m_plotter.setBackground(ColorAttr.getBackground());
        exec.setMessage("Adding data rows to histogram...");
        final int noOfRows = inData[0].getRowCount();
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        for (DataRow row : data) {
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
        //check the internal variables if they are valid
        if (m_xColName == null || m_xColName.length() < 1) {
            throw new InvalidSettingsException(
                    "Please define the x column.");
        }

        if (m_aggrColName == null || m_aggrColName.length() < 1) {
            throw new InvalidSettingsException(
                    "Please define the aggregation column.");
        }
        return new DataTableSpec[0];
    }
}

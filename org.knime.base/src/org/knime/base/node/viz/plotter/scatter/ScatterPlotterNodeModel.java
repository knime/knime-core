/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   14.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterNodeModel extends NodeModel implements DataProvider {
    
    private int m_firstRow = START;
    
    private int m_lastRow = END;
    
    private boolean m_antialias;
    
    private DataArray m_dataModel;
    
    private ScatterPlotter m_plotter;
    
    /**
     * One input: the data to display.
     *
     */
    public ScatterPlotterNodeModel() {
        super(1, 0);
        m_plotter = new ScatterPlotter();
        m_plotter.setAntialiasing(false);
    }

    
    /** Config key for the first row. */
    public static final String CFGKEY_FROMROW = "startRow";
    
    /** Config key for the number of rows. */
    public static final String CFGKEY_ROWCNT = "nrOfRows";
    
    /** Config key for dis- or enabling antialiasing. */
    public static final String CFG_ANTIALIAS = "antialias";
    
    private static final String FILE_NAME = "datamodel";
    
    private static final String SETTINGS_NAME = "settings";
    
    /**
     * 
     * @return the local plotter instance.
     */
    public ScatterPlotter getPlotter() {
        return m_plotter;
    }
    
    /**
     * @see org.knime.core.node.NodeModel#configure(
     * org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * @see org.knime.core.node.NodeModel#execute(
     * org.knime.core.node.BufferedDataTable[], 
     * org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_dataModel = new DefaultDataArray(inData[0], m_firstRow, m_lastRow, 
                exec); 
        return new BufferedDataTable[]{};
    }
    
    
    
    

    /**
     * @see org.knime.core.node.NodeModel#setInHiLiteHandler(int, 
     * org.knime.core.node.property.hilite.HiLiteHandler)
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        assert inIndex == 0;
        super.setInHiLiteHandler(inIndex, hiLiteHdl);
        m_plotter.setHiLiteHandler(hiLiteHdl);
    }

    /**
     * @see org.knime.base.node.viz.plotter.DataProvider
     * #getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        assert index == index;
        return m_dataModel;
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(
     * java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File settingsFile  = new File(nodeInternDir, SETTINGS_NAME);
        NodeSettingsRO settings = NodeSettings.loadFromXML(
                new FileInputStream(settingsFile));
        try {
        m_firstRow = settings.getInt(CFGKEY_FROMROW);
        m_lastRow = settings.getInt(CFGKEY_ROWCNT);
        File f = new File(nodeInternDir, FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(f); 
        m_dataModel = new DefaultDataArray(table, m_firstRow, m_lastRow);
        } catch (InvalidSettingsException ise) {
            throw new IOException("Settings weren't complete or missing");
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_firstRow = settings.getInt(CFGKEY_FROMROW);
        m_lastRow = settings.getInt(CFGKEY_ROWCNT);
        m_antialias = settings.getBoolean(CFG_ANTIALIAS);
        m_plotter.setAntialiasing(m_antialias);
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_dataModel = null;
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(
     * java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        NodeSettings settings = new NodeSettings(SETTINGS_NAME);
        settings.addInt(CFGKEY_FROMROW, m_firstRow);
        settings.addInt(CFGKEY_ROWCNT, m_lastRow);
        File settingsFile = new File(nodeInternDir, SETTINGS_NAME);
        settings.saveToXML(new FileOutputStream(settingsFile));
        File f = new File(nodeInternDir, FILE_NAME);
        DataContainer.writeToZip(m_dataModel, f, exec);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     * org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(CFGKEY_FROMROW, m_firstRow);
        settings.addInt(CFGKEY_ROWCNT, m_lastRow);
        settings.addBoolean(CFG_ANTIALIAS, m_antialias);
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getInt(CFGKEY_FROMROW);
        settings.getInt(CFGKEY_ROWCNT);
        settings.getBoolean(CFG_ANTIALIAS);
    }

}

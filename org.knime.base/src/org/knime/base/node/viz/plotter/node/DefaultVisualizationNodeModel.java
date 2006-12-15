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
 *   20.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
 * Implementation of a {@link org.knime.core.node.NodeModel} which provides all
 * functionality that is needed for a default plotter implementation. That is:
 * converting the incoming data of inport 0 into a 
 * {@link org.knime.base.node.util.DataArray} with the maximum number of rows 
 * specified in the {@link DefaultVisualizationNodeDialog}, loading and saving 
 * this {@link org.knime.base.node.util.DataArray} in the <code>load</code>- and
 * <code>saveInternals</code> and providing it via the
 * {@link #getDataArray(int)} method of the 
 * {@link org.knime.base.node.viz.plotter.DataProvider} interface.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultVisualizationNodeModel extends NodeModel implements 
    DataProvider {
    
    private DataArray m_input;
    
    private int m_last = END;
    
    /** Config key for the last displayed row. */
    public static final String CFG_END = "end";
    
    /** Config key for dis- or enabling antialiasing. */
    public static final String CFG_ANTIALIAS = "antialias";
    
    private static final String FILE_NAME = "internals";
    
    private boolean m_antialiasing;
    
    private int[] m_excludedColumns;
    
    
    /**
     * Creates a {@link org.knime.core.node.NodeModel} with one data inport and 
     * no outport.
     */
    public DefaultVisualizationNodeModel() {
        super(1, 0);
    }
    
    /**
     * Constructor for extending classes to define an arbitrary number of
     * in- and outports.
     * 
     * @param nrInports number of data inports
     * @param nrOutports number of data outports
     */
    public DefaultVisualizationNodeModel(final int nrInports, 
            final int nrOutports) {
        super(nrInports, nrOutports);
    }


    /**
     * @see org.knime.base.node.viz.plotter.DataProvider
     * #getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        return m_input;
    }


    /**
     * True, if antialiasing should be used, false otherwise.
     * @return true, if antialiasing should be used, false otherwise
     */
    public boolean antiAliasingOn() {
        return m_antialiasing;
    }

    /**
     * All nominal columns without possible values or with more than 60
     * possible values are ignored. A warning is set if so.
     * 
     * @see org.knime.core.node.NodeModel#configure(
     * org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // first filter out those nominal columns with
        // possible values == null
        // or possibleValues.size() > 60
        List<Integer>excludedCols = new ArrayList<Integer>();
        int currColIdx = 0;
        for (DataColumnSpec colSpec : inSpecs[0]) {
            // nominal value
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                if (colSpec.getDomain().hasValues() &&
                        colSpec.getDomain().getValues().size() > 60) {
                    excludedCols.add(currColIdx);
                } else if (!colSpec.getDomain().hasValues()) {
                    excludedCols.add(currColIdx);
                }
                
            }
            currColIdx++;
        }
        m_excludedColumns = new int[excludedCols.size()];
        for (int i = 0; i < excludedCols.size(); i++) {
            m_excludedColumns[i] = excludedCols.get(i);
        }
        if (excludedCols.size() > 0) {
            setWarningMessage("Nominal columns without possible values or with "
                    + "more than 60 possible values are ignored!");   
        }
        return new DataTableSpec[0];
    }

    /**
     * Converts the input data at inport 0 into a 
     * {@link org.knime.base.node.util.DataArray} with maximum number of rows as
     * defined in the {@link DefaultVisualizationNodeDialog}. Thereby nominal 
     * columns are irgnored whose possible values are null or more than 60.
     * 
     * @see org.knime.core.node.NodeModel#execute(
     * org.knime.core.node.BufferedDataTable[], 
     * org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // first filter out those nominal columns with
        // possible values == null
        // or possibleValues.size() > 60
        DataTable filter = new FilterColumnTable(inData[0], false, 
                m_excludedColumns);
        m_input = new DefaultDataArray(filter, 1, m_last, exec);
        if ((m_last) < inData[0].getRowCount()) {
            setWarningMessage("Only the rows from 0 to " + m_last 
                    + "are displayed.");
        }
        return new BufferedDataTable[0];
    }

    /**
     * Loads the converted {@link org.knime.base.node.util.DataArray}.
     * 
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(f);
        m_input = new DefaultDataArray(table, 1, m_last, exec);
    }

    /**
     * Loads the maximum number of rows.
     * 
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_last = settings.getInt(CFG_END);
        m_antialiasing = settings.getBoolean(CFG_ANTIALIAS);
    }

    /**
     * Sets the input data <code>null</code>.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_excludedColumns = null;
        m_input = null;
    }

    /**
     * Saves the converted {@link org.knime.base.node.util.DataArray}.
     * 
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        DataContainer.writeToZip(m_input, f, exec);
    }

    /**
     * Saves the maximum number of rows.
     * 
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     * org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(CFG_END, m_last);
        settings.addBoolean(CFG_ANTIALIAS, m_antialiasing);
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getInt(CFG_END);
        settings.getBoolean(CFG_ANTIALIAS);
    }

}

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

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataColumnSpec;
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
    
    private static final String FILE_NAME = "internals";
    
    
    /**
     * Creates a {@link org.knime.core.node.NodeModel} with one data inport and 
     * no outport.
     */
    public DefaultVisualizationNodeModel() {
        super(1, 0);
    }


    /**
     * @see org.knime.base.node.viz.plotter.DataProvider
     * #getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        return m_input;
    }



    /**
     * Checks if the nominal values have possible values.
     * 
     * @see org.knime.core.node.NodeModel#configure(
     * org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        for (DataColumnSpec colSpec : inSpecs[0]) {
            // if we have nominal columns without possible values
            if (!colSpec.getType().isCompatible(DoubleValue.class) 
                    && colSpec.getDomain().getValues() == null) {
                throw new InvalidSettingsException(
                        "Found nominal column without possible values: "
                        + colSpec.getName() 
                        + " Please use DomainCalculator or ColumnFilter!");
            }
        }
        return new DataTableSpec[0];
    }

    /**
     * Converts the input data at inport 0 into a 
     * {@link org.knime.base.node.util.DataArray} with maximum number of rows as
     * defined in the {@link DefaultVisualizationNodeDialog}.
     * @see org.knime.core.node.NodeModel#execute(
     * org.knime.core.node.BufferedDataTable[], 
     * org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_input = new DefaultDataArray(inData[0], 1, m_last, exec);
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
    }

    /**
     * Sets the input data <code>null</code>.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
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
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
//        settings.getInt(CFG_START);
        settings.getInt(CFG_END);
    }

}

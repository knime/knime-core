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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.test;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.line.LinePlotter;
import org.knime.base.node.viz.plotter.parcoord.ParallelCoordinatesPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
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
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TestNodeModel extends NodeModel implements DataProvider {
    
    private DataArray m_input;
    
    private AbstractPlotter[] m_plotter;
    
    private static final String FILE_NAME = "testInternals";
    
    /**
     * 
     *
     */
    public TestNodeModel() {
        super(1, 0);
        m_plotter = new AbstractPlotter[]{new ScatterPlotter(), 
                new LinePlotter(), new ParallelCoordinatesPlotter()};
    }
    

    /**
     * 
     * @return the plotter.
     */
    public AbstractPlotter[] getPlotter() {
        return m_plotter;
    }
    

    /**
     * @see org.knime.base.node.viz.plotter.DataProvider#
     * getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        return m_input;
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
        m_input = new DefaultDataArray(inData[0], START, END);
        return new BufferedDataTable[]{};
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        ContainerTable table = DataContainer.readFromZip(f);
        m_input = new DefaultDataArray(table, START, END);

    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {

    }
    
    

    /**
     * @see org.knime.core.node.NodeModel#setInHiLiteHandler(int,
     *      org.knime.core.node.property.hilite.HiLiteHandler)
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        if (hiLiteHdl != null) {
            super.setInHiLiteHandler(inIndex, hiLiteHdl);
            if (m_plotter != null) {
                for (AbstractPlotter plotter : m_plotter) {
                    plotter.setHiLiteHandler(hiLiteHdl);
                    hiLiteHdl.addHiLiteListener(plotter);
                }
            }
        }
    }




    /**
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
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     * org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     * org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

}

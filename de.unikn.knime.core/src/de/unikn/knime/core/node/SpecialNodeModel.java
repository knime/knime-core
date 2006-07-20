/*
 * -------------------------------------------------------------------
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
 */
package de.unikn.knime.core.node;

import java.io.File;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * This special node model is an extension of a normal node model that allows to
 * implement nodes that do "special" things like managing meta workflows.
 * 
 * <b>This class is not intended for regular use by data processing nodes!</b>
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class SpecialNodeModel extends NodeModel {
    private Node m_node;

    private WorkflowManager m_internalWFM;

    /**
     * Creates a new model with the given number of data, and predictor in- and
     * outputs.
     * 
     * @param nrDataIns The number of <code>DataTable</code> elements expected
     *            as inputs.
     * @param nrDataOuts The number of <code>DataTable</code> objects expected
     *            at the output.
     * @param nrPredParamsIns The number of <code>ModelContent</code> elements
     *            available as inputs.
     * @param nrPredParamsOuts The number of <code>ModelContent</code> objects
     *            available at the output.
     * @throws NegativeArraySizeException If the number of in- or outputs is
     *             smaller than zero.
     */
    public SpecialNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
    }

    /**
     * Creates a new model with the given number of in- and outputs.
     * 
     * @param nrDataIns Number of data inputs.
     * @param nrDataOuts Number of data outputs.
     */
    public SpecialNodeModel(final int nrDataIns, final int nrDataOuts) {
        super(nrDataIns, nrDataOuts);
    }

    /**
     * Sets the node in which this model is contained.
     * 
     * @param node the node
     */
    final void setNode(final Node node) {
        if (m_node != null) {
            throw new IllegalStateException("This node for this model has"
                    + " already been set");
        }
        m_node = node;
    }

    /**
     * Returns the data input port with the given index.
     * 
     * @param index the index of the data input port
     * @return a data input port
     */
    protected final DataInPort getDataInPort(final int index) {
        return (DataInPort)m_node.getInPort(index);
    }

    /**
     * Returns the data output port with the given index.
     * 
     * @param index the index of the data output port
     * @return a data output port
     */
    protected final DataOutPort getDataOutPort(final int index) {
        return (DataOutPort)m_node.getOutPort(index);
    }

    /**
     * Returns the predictor input port with the given index.
     * 
     * @param index the index of the predictor input port
     * @return a predictor input port
     */
    protected final ModelContentInPort getModelContentInPort(final int index) {
        return (ModelContentInPort)m_node.getInPort(index + getNrDataIns());
    }

    /**
     * Returns the predictor output port with the given index.
     * 
     * @param idx the index of the predictor output port
     * @return a predictor output port
     */
    protected final ModelContentOutPort getModelContentOutPort(final int idx) {
        return (ModelContentOutPort)m_node.getOutPort(idx + getNrDataOuts());
    }

    /**
     * This method is called, if a new data table is available at an input port.
     * 
     * @param table the new data table
     * @param inPortID the ID of the data port
     */
    protected void inportHasNewDataTable(final BufferedDataTable table,
            final int inPortID) {
        // nothing to do for this class here
    }

    /**
     * This method is called, if a new data table spec is available at an input
     * port.
     * 
     * @param spec the new data table spec
     * @param inPortID the ID of the data port
     */
    protected void inportHasNewTableSpec(final DataTableSpec spec,
            final int inPortID) {
        // nothing to do for this class here
    }

    /**
     * This method is called if a new connection at an input port is added.
     * 
     * @param inPortID the port id that has been connected
     */
    protected void inportHasNewConnection(final int inPortID) {
        // nothing to do for this class here
    }

    /**
     * This method is called if an input port has been disconnected.
     * 
     * @param inPortID the port id that just has been disconnected
     */
    protected void inportWasDisconnected(final int inPortID) {
        // nothing to do for this class here
    }

    /**
     * Sets the internal workflow manager. If it has already been set an
     * exception is thrown.
     * 
     * @param wfm the internal workflow manager
     * @throws IllegalStateException if the workflow manager has already been
     *             set
     */
    void setInternalWFM(final WorkflowManager wfm)
    throws IllegalStateException {
        if (m_internalWFM != null) {
            throw new IllegalStateException("Internal workflow manager has"
                    + " already been set");
        }
        m_internalWFM = wfm;
    }

    /**
     * Returns the internal workflow manager.
     * 
     * @return a workflow manager that is a child of the main manager
     */
    protected WorkflowManager internalWFM() {
        return m_internalWFM;
    }

    /**
     * Returns the node progress monitor inside the execution monitor.
     * 
     * @param exec an execution monitor
     * @return the wrapped node progress monitor
     */
    protected static final NodeProgressMonitor getNodeProgressMonitor(
            final ExecutionMonitor exec) {
        return exec.getProgressMonitor();
    }

    /**
     * Validates the specified settings in the model and then loads them into
     * it.
     * 
     * @param nodeFile the node file
     * @param settings The settings to read.
     * @param exec an execution monitor
     * @throws InvalidSettingsException If the load iof the validated settings
     *             fails.
     */
    final void loadSettingsFrom(final File nodeFile,
            final NodeSettingsRO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException {
        validateSettings(nodeFile, settings);
        loadValidatedSettingsFrom(nodeFile, settings, exec);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO setts)
            throws InvalidSettingsException {
        loadValidatedSettingsFrom(null, setts, null);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected final void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        validateSettings(null, settings);
    }

    /**
     * Validates the settings in the passed <code>NodeSettings</code> object.
     * The specified settings should be checked for completeness and
     * consistencty. It must be possible to load a settings object validated
     * here without any exception in the
     * <code>#loadValidatedSettings(NodeSettings)</code> method. The method
     * must not change the current settings in the model - it is supposed to
     * just check them. If some settings are missing, invalid, inconsistent, or
     * just not right throw an exception with a message useful to the user.
     * 
     * @param nodeDir the node file
     * @param settings The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    protected abstract void validateSettings(final File nodeDir,
            final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the settings of this model.
     * 
     * @param nodeDir the directory for the node
     * @param settings a settings object
     * @param exec an execution monitor to report progress for long-running
     *            saves
     */
    protected abstract void saveSettingsTo(final File nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec);

    /**
     * Loads the validated settings for this model.
     * 
     * @param nodeDir the directory of the node
     * @param settings a settings object
     * @param exec an execution monitor to report progress for long-running
     *            loads
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected abstract void loadValidatedSettingsFrom(final File nodeDir,
            final NodeSettingsRO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException;

    /**
     * Resets the node. If you use this method make sure that no loop is
     * created.
     */
    protected final void resetMyself() {
        m_node.resetAndConfigure();
    }
}

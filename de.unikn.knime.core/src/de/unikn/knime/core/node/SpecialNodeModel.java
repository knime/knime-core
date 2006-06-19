/* Created on Jun 1, 2006 10:45:44 AM by thor
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

import de.unikn.knime.core.node.workflow.WorkflowManager;


/**
 * This special node model is an extension of a normal node model that allows
 * to implement nodes that do "special" things like managing meta workflows.
 * 
 * <b>This class is not intended for regular use by data processing nodes!</b>
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class SpecialNodeModel extends NodeModel {
    private Node m_node;
    
    /**
     * Creates a new model with the given number of data, and predictor in- and
     * outputs.
     * 
     * @param nrDataIns The number of <code>DataTable</code> elements expected
     *            as inputs.
     * @param nrDataOuts The number of <code>DataTable</code> objects expected
     *            at the output.
     * @param nrPredParamsIns The number of <code>PredictorParams</code>
     *            elements available as inputs.
     * @param nrPredParamsOuts The number of <code>PredictorParams</code>
     *            objects available at the output.
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
    protected final PredictorInPort getPredictorInPort(final int index) {
        return (PredictorInPort)m_node.getInPort(index + getNrDataIns());
    }

    
    /**
     * Returns the predictor output port with the given index.
     * 
     * @param index the index of the predictor output port
     * @return a predictor output port
     */
    protected final PredictorOutPort getPredictorOutPort(final int index) {
        return (PredictorOutPort)m_node.getOutPort(index + getNrDataOuts());
    }
    
    
    /**
     * This method is called, if a new data table is available at an input port.
     * 
     * @param inPortID the ID of the data port
     */
    protected void inportHasNewDataTable(final int inPortID) {
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
    
    
    protected final WorkflowManager createSubManager() {
        return m_node.getWorkflowManager().createSubManager();
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
}

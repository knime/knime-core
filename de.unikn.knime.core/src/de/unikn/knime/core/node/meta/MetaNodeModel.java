/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * History
 *   17.11.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import java.util.ArrayList;
import java.util.Vector;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowExecutor;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * Preliminary version of the MetaNodeModel that holds a MetaWorkflow. The 
 * mode of operation is as follows: an empty <code>WorkFlowManager</code> is 
 * initialized with a given number of <code>MetaInputNode</code>s and <code>
 * MetaOutputNode</code>s. configure and execute manually set the <code>
 * DataTableSpec</code>s resp. the <code>DataTable</code>s in the
 * MetaInputNodes and retrieve the outputs from the MetaOutputNodes in the
 * inner workflow. 
 * 
 * @author cebron, University of Konstanz
 */
public class MetaNodeModel extends NodeModel implements WorkflowListener {

    /**
     * Key to store the workflow manager in the settings.
     */
    public static final String WORKFLOW_KEY = "workflow";

    /*
     * The inner WorkflowManager
     */
    private WorkflowManager m_workflowmanager;

    /*
     * References to the MetaInputNodeContainers in the inner workflow.
     */
    private MetaInputNodeContainer[] m_metaInContainer;

    /*
     * References to the MetaOutputNodeContainers in the inner workflow.
     */
    private MetaOutputNodeContainer[] m_metaOutContainer;

    /*
     * The listeners that are interested in node state changes.
     */
    private final ArrayList<NodeStateListener> m_stateListeners;

    
    
    
    
    MetaNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
        m_workflowmanager = new WorkflowManager();
        m_workflowmanager.addListener(this);
        // create MetaInput nodes, add them to the workflow
        m_metaInContainer = new MetaInputNodeContainer[nrDataIns];
        for (int i = 0; i < nrDataIns; i++) {
            m_metaInContainer[i] = (MetaInputNodeContainer)m_workflowmanager
                    .createNode(new MetaInputNodeFactory());
        }

        // create MetaOutput nodes, add them to the workflow
        m_metaOutContainer = new MetaOutputNodeContainer[nrDataOuts];
        for (int i = 0; i < nrDataOuts; i++) {
            m_metaOutContainer[i] = (MetaOutputNodeContainer)m_workflowmanager
                    .createNode(new MetaOutputNodeFactory());
        }

        m_stateListeners = new ArrayList<NodeStateListener>();

    }
    

    /**
     * The number of inputs and outputs must be provided, the corresponding
     * <code>MetaInputNode</code>s and <code>MetaOutputNode</code>s are created 
     * in the inner workflow.
     * @param nrIns number of input nodes.
     * @param nrOuts number of output nodes.
     */
    MetaNodeModel(final int nrIns, final int nrOuts) {
        this(nrIns, nrOuts, 0, 0);
    }

    /**
     * The inSpecs are manually set in the <code>MetaInputNode</code>s. The
     * resulting outSpecs from the <code>MetaOutputNode</code>s are returned.
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (m_metaInContainer.length == inSpecs.length);
        // redirect inSpecs in the inner workflow
        for (int i = 0; i < m_metaInContainer.length; i++) {
            m_metaInContainer[i].setInTableSpec(inSpecs[i]);
            /*
             * trigger a configure manually in order to have the new
             * DataTableSpec available at the outport of the MetaInputNode.
             */
            m_metaInContainer[i].configureNode();
        }

        // collect all output specs
        DataTableSpec[] outspecs = new DataTableSpec[m_metaOutContainer.length];
        for (int i = 0; i < m_metaOutContainer.length; i++) {
            outspecs[i] = m_metaOutContainer[i].getOutTableSpec();
        }
        return outspecs;
    }

    /**
     * During execute, the inData <code>DataTables</code> are passed on to the 
     * <code>MetaInputNode</code>s.
     * The inner workflow gets executed and the output <code>DataTable</code>s 
     * from the <code>MetaOutputNode</code>s are returned.
     * @see NodeModel#execute(DataTable[], ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        assert (m_metaInContainer.length == inData.length);
        exec.setMessage("Translating input");
        // translate input
        for (int i = 0; i < m_metaInContainer.length; i++) {
            m_metaInContainer[i].setInDataTable(inData[i]);
        }

        exec.setMessage("Executing inner workflow");
        m_workflowmanager.prepareForExecAllNodes();
        WorkflowExecutor executor = new WorkflowExecutor(m_workflowmanager);
        executor.executeAll();

        // translate output
        exec.setMessage("Collecting output");
        DataTable[] out = new DataTable[m_metaOutContainer.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = m_metaOutContainer[i].getOutDataTable();
        }
        return out;
    }

    /**
     * Loads the Meta Workflow from the settings. Internal references to the
     * MetaInput and MetaOuput - Nodes are updated.
     * 
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        if (settings.containsKey(WORKFLOW_KEY)) {
            m_workflowmanager = WorkflowManager.load(settings
                    .getConfig(WORKFLOW_KEY));
            m_workflowmanager.addListener(this);
            NodeContainer[] allnodes = m_workflowmanager.getNodes();
            Vector<MetaInputNodeContainer> incontainer = new Vector<MetaInputNodeContainer>();
            Vector<MetaOutputNodeContainer> outcontainer = new Vector<MetaOutputNodeContainer>();
            // collect all MetaInputNode and MetaOutputNode containers.
            for (NodeContainer con : allnodes) {
                if (con instanceof MetaInputNodeContainer) {
                    incontainer.add((MetaInputNodeContainer)con);
                } else if (con instanceof MetaOutputNodeContainer) {
                    outcontainer.add((MetaOutputNodeContainer)con);
                }
            }
            m_metaInContainer = incontainer.toArray(m_metaInContainer);
            m_metaOutContainer = outcontainer.toArray(m_metaOutContainer);
        }
    }

    /**
     * A reset at the MetaInputNodes of the inner workflow is triggered in order
     * to reset all nodes in the inner workflow. 
     * @see NodeModel#reset()
     */
    protected void reset() {
        if (m_metaInContainer != null) {
            for (MetaInputNodeContainer myinputContainer : m_metaInContainer) {
                myinputContainer.resetNode();
            }
        }
    }

    /**
     * Stores the inner workflow in the settings.
     * 
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        NodeSettings conf = settings.addConfig(WORKFLOW_KEY);
        m_workflowmanager.save(conf);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {

    }

    /**
     * Returns the workflow manager representing the meta-workflow.
     * 
     * @return the meta-workflow manager for this meta-node
     */
    public WorkflowManager getMetaWorkflowManager() {
        return m_workflowmanager;
    }

    /**
     * Reacts on a workflow event of the underlying workflow manager of this
     * meta workflow model.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener#workflowChanged
     *      (de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {

        if (event.getEventType() == WorkflowEvent.NODE_EXTRAINFO_CHANGED) {
            notifyStateListeners(new NodeStatus(
                    NodeStatus.STATUS_EXTRA_INFO_CHANGED));
        } else if (event.getEventType() == WorkflowEvent.CONNECTION_ADDED) {
            notifyStateListeners(new NodeStatus(
                    NodeStatus.STATUS_EXTRA_INFO_CHANGED));
        } else if (event.getEventType() == WorkflowEvent.CONNECTION_REMOVED) {
            notifyStateListeners(new NodeStatus(
                    NodeStatus.STATUS_EXTRA_INFO_CHANGED));
        } else if (event.getEventType() == WorkflowEvent.CONNECTION_EXTRAINFO_CHANGED) {
            notifyStateListeners(new NodeStatus(
                    NodeStatus.STATUS_EXTRA_INFO_CHANGED));
        }
    }

    /**
     * Notifies all state listeners that the state of this meta node model has
     * changed.
     * 
     * @param state <code>NodeStateListener</code>
     */
    private void notifyStateListeners(final NodeStatus state) {
        // for all listeners
        for (int i = 0; i < m_stateListeners.size(); i++) {
            NodeStateListener listener = m_stateListeners.get(i);
            listener.stateChanged(state, -1);
        }
    }

    /**
     * Adds a state listener to this <code>MetaNodeModel</code>. Ignored, if
     * the listener is already registered.
     * 
     * @param listener The listener to add
     */
    public void addStateListener(final NodeStateListener listener) {
        if (!m_stateListeners.add(listener)) {
            // add logger
        }
    }
}

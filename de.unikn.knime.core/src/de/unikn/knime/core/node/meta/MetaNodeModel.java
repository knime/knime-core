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
 * History
 *   17.11.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.DataOutPort;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.SpecialNodeModel;
import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowExecutor;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class MetaNodeModel extends SpecialNodeModel
    implements WorkflowListener, NodeStateListener {
    private static final String WORKFLOW_KEY = "workflow";
    private static final String INOUT_CONNECTIONS_KEY = "inOutConnections";
    
    private WorkflowManager m_internalWFM;
    private WorkflowExecutor m_workflowExecutor;

    private final NodeContainer[] m_dataInContainer, m_dataOutContainer;
    private final NodeContainer[] m_modelInContainer, m_modelOutContainer;
    private boolean m_resetFromInterior;
    
    /*
     * The listeners that are interested in node state changes.
     */
    private final ArrayList<NodeStateListener> m_stateListeners;

    private final NodeFactory m_myFactory;
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(MetaNodeModel.class);
    
    /**
     * Creates a new new meta node model.
     * 
     * @param nrDataIns the number of data input ports
     * @param nrDataOuts the number of data output ports
     * @param nrPredParamsIns the number of predictor param input ports
     * @param nrPredParamsOuts the number of predictor param output ports
     * @param factory the factory that created this model
     */
    MetaNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts,
            final MetaNodeFactory factory) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);

        m_dataInContainer = new NodeContainer[nrDataIns];
        m_dataOutContainer = new NodeContainer[nrDataOuts];
        m_modelInContainer = new NodeContainer[nrPredParamsIns];
        m_modelOutContainer = new NodeContainer[nrPredParamsOuts];
        m_myFactory = factory;        
        m_stateListeners = new ArrayList<NodeStateListener>();
    }
    

    /**
     * The number of inputs and outputs must be provided, the corresponding
     * <code>MetaInputNode</code>s and <code>MetaOutputNode</code>s are created 
     * in the inner workflow.
     * 
     * @param nrIns number of input nodes.
     * @param nrOuts number of output nodes.
     * @param f the factory that created this model
     */
    MetaNodeModel(final int nrIns, final int nrOuts, final MetaNodeFactory f) {
        this(nrIns, nrOuts, 0, 0, f);
    }

    
    
    /**
     * The inSpecs are manually set in the <code>MetaInputNode</code>s. The
     * resulting outSpecs from the <code>MetaOutputNode</code>s are returned.
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {        
        if (m_internalWFM == null) {
            m_internalWFM = getResponsibleWorkflowManager().createSubManager();
            m_internalWFM.addListener(this);
            addInOutNodes();
            m_workflowExecutor = new WorkflowExecutor(m_internalWFM);
        }
        
        // collect all output specs
        DataTableSpec[] outspecs = new DataTableSpec[m_dataOutContainer.length];
        for (int i = 0; i < outspecs.length; i++) {
            if (m_dataOutContainer[i] != null) {
                outspecs[i] = ((DataOutPort) m_dataOutContainer[i]
                    .getOutPorts().get(0)).getDataTableSpec();
            }
        }
        return outspecs;
    }

    /**
     * During execute, the inData <code>DataTables</code> are passed on to the 
     * <code>MetaInputNode</code>s.
     * The inner workflow gets executed and the output <code>DataTable</code>s 
     * from the <code>MetaOutputNode</code>s are returned.
     * @see de.unikn.knime.core.node.NodeModel
     *  #execute(DataTable[], ExecutionMonitor)
     */
    @Override
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        exec.setMessage("Executing inner workflow");
        m_workflowExecutor.executeAll();

        // translate output
        exec.setMessage("Collecting output");
        DataTable[] out = new DataTable[m_dataOutContainer.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = ((DataOutPort) m_dataOutContainer[i]
                 .getOutPorts().get(0)).getDataTable();
            if (out[i] == null) {
                System.out.println();
            }
        }
        return out;
    }

    /**
     * Loads the Meta Workflow from the settings. Internal references to the
     * MetaInput and MetaOuput - Nodes are updated.
     * 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        m_internalWFM.clear();
        addInOutNodes();
        
        // load the "internal" workflow
        if (settings.containsKey(WORKFLOW_KEY)) {
            m_internalWFM.load(settings.getConfig(WORKFLOW_KEY));
        }
        
        addInOutConnections(settings.getConfig(INOUT_CONNECTIONS_KEY));
    }

    private void addInOutNodes() {
        LOGGER.debug("Adding in- and output nodes");
        for (int i = 0; i < m_dataInContainer.length; i++) {
            final int temp = i; 
            m_dataInContainer[i] = m_internalWFM.addNewNode(
                new MetaInputNodeFactory(1, 0) {
                    @Override
                    public String getOutportDescription(final int index) {
                        return m_myFactory.getInportDescription(temp);
                    }
                });
        }

        for (int i = 0; i < m_dataOutContainer.length; i++) {
            final int temp = i;
            m_dataOutContainer[i] = m_internalWFM.addNewNode(
                new MetaOutputNodeFactory(1, 0) {
                    @Override
                    public String getInportDescription(final int index) {
                        return m_myFactory.getOutportDescription(temp);
                    }
                });
            m_dataOutContainer[i].addListener(this);
        }

        for (int i = 0; i < m_modelInContainer.length; i++) {
            final int temp = i;
            m_modelInContainer[i] = m_internalWFM.addNewNode(
                new MetaInputNodeFactory(0, 1) {
                    @Override
                    public String getPredParamOutDescription(final int index) {
                        return m_myFactory.getPredParamInDescription(temp);
                    }                        
                });
        }

        for (int i = 0; i < m_modelOutContainer.length; i++) {
            final int temp = i;
            m_modelOutContainer[i] = m_internalWFM.addNewNode(
                new MetaOutputNodeFactory(0, 1) {
                    @Override
                    public String getPredParamInDescription(final int index) {
                        return m_myFactory.getPredParamOutDescription(temp);
                    }                        
                });
            m_modelOutContainer[i].addListener(this);
        }        
    }
    
    
    private void addInOutConnections(final NodeSettings connections)
    throws InvalidSettingsException {
        LOGGER.debug("Adding in- and output connections");
        for (String key : connections) {
            if (key.startsWith("connection_")) {
                int[] conn = connections.getIntArray(key);
                m_internalWFM.addConnection(conn[0], conn[1], conn[2],
                        conn[3]);
            }
        }        
    }
    
    /**
     * A reset at the MetaInputNodes of the inner workflow is triggered in order
     * to reset all nodes in the inner workflow.
     *  
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        if (!m_resetFromInterior) {
            for (NodeContainer nc : m_dataInContainer) {
                nc.reset();
            }
            
            for (NodeContainer nc : m_modelInContainer) {
                nc.reset();
            }
        }
    }

    /**
     * Stores the inner workflow in the settings.
     * 
     * @see de.unikn.knime.core.node.NodeModel#saveSettingsTo(NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        NodeSettings connections = settings.addConfig(INOUT_CONNECTIONS_KEY);
        
        int count = 0;
        int[] conn = new int[4];
        
        for (NodeContainer nc : m_dataInContainer) {
            List<ConnectionContainer> conncon =
                m_internalWFM.getOutgoingConnectionsAt(nc, 0);
            for (ConnectionContainer cc : conncon) {
                conn[0] = cc.getSource().getID();
                conn[1] = cc.getSourcePortID();
                conn[2] = cc.getTarget().getID();
                conn[3] = cc.getTargetPortID();
                
                connections.addIntArray("connection_dataIn_" + count++, conn);
            }
        }
        
        for (NodeContainer nc : m_dataOutContainer) {
            ConnectionContainer cc =
                m_internalWFM.getIncomingConnectionAt(nc, 0);
            if (cc != null) {
                conn[0] = cc.getSource().getID();
                conn[1] = cc.getSourcePortID();
                conn[2] = cc.getTarget().getID();
                conn[3] = cc.getTargetPortID();
                
                connections.addIntArray("connection_dataOut_" + count++, conn);
            }
        }

        for (NodeContainer nc : m_modelInContainer) {
            List<ConnectionContainer> conncon =
                m_internalWFM.getOutgoingConnectionsAt(nc, 0);
            for (ConnectionContainer cc : conncon) {
                conn[0] = cc.getSource().getID();
                conn[1] = cc.getSourcePortID();
                conn[2] = cc.getTarget().getID();
                conn[3] = cc.getTargetPortID();
                
                connections.addIntArray("connection_modelIn_" + count++, conn);
            }
        }
        
        for (NodeContainer nc : m_modelOutContainer) {
            ConnectionContainer cc =
                m_internalWFM.getIncomingConnectionAt(nc, 0);
            if (cc != null) {
                conn[0] = cc.getSource().getID();
                conn[1] = cc.getSourcePortID();
                conn[2] = cc.getTarget().getID();
                conn[3] = cc.getTargetPortID();
                
                connections.addIntArray("connection_modelOut_" + count++, conn);
            }
        }

        Set<NodeContainer> omitNodes = new HashSet<NodeContainer>();
        for (NodeContainer nc : m_dataInContainer) {
            omitNodes.add(nc);
        }
        for (NodeContainer nc : m_dataOutContainer) {
            omitNodes.add(nc);
        }
        for (NodeContainer nc : m_modelInContainer) {
            omitNodes.add(nc);
        }
        for (NodeContainer nc : m_modelOutContainer) {
            omitNodes.add(nc);
        }
        
        
        NodeSettings conf = settings.addConfig(WORKFLOW_KEY);
        m_internalWFM.save(conf, omitNodes);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#validateSettings(NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        // maybe do some sanity checks?
    }

    /**
     * Returns the workflow manager representing the meta-workflow.
     * 
     * @return the meta-workflow manager for this meta-node
     */
    public WorkflowManager getMetaWorkflowManager() {
        if (m_internalWFM == null) {
            m_internalWFM = getResponsibleWorkflowManager().createSubManager();
            m_internalWFM.addListener(this);
        }
        
        return m_internalWFM;
    }

    /**
     * Reacts on a workflow event of the underlying workflow manager of this
     * meta workflow model.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener#workflowChanged
     *      (de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {

        if (event instanceof WorkflowEvent.NodeExtrainfoChanged) {
            notifyStateListeners(new NodeStatus.ExtrainfoChanged());
        } else if (event instanceof WorkflowEvent.ConnectionAdded) {
            notifyStateListeners(new NodeStatus.ExtrainfoChanged());
        } else if (event instanceof WorkflowEvent.ConnectionRemoved) {
            notifyStateListeners(new NodeStatus.ExtrainfoChanged());
        } else if (event instanceof WorkflowEvent.ConnectionExtrainfoChanged) {
            notifyStateListeners(new NodeStatus.ExtrainfoChanged());
        }
    }

    /**
     * Notifies all state listeners that the state of this meta node model has
     * changed.
     * 
     * @param state <code>NodeStateListener</code>
     */
    private void notifyStateListeners(final NodeStatus state) {
        for (NodeStateListener l : m_stateListeners) {
            l.stateChanged(state, -1);
        }
    }


    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #inportHasNewConnection(int)
     */
    @Override
    protected void inportHasNewConnection(final int inPortID) {
        LOGGER.debug("Adding new dummy connection for input #" + inPortID); 
        super.inportHasNewConnection(inPortID);
        
        NodeContainer myCont = getNodeContainer();
        
        ConnectionContainer cc = getResponsibleWorkflowManager()
            .getIncomingConnectionAt(myCont, inPortID);        
        NodeContainer outCont = cc.getSource();
        int outPortID = cc.getSourcePortID();
        
        if (inPortID < getNrDataIns()) {
            m_internalWFM.addConnection(outCont, outPortID,
                    m_dataInContainer[inPortID], 0);
        } else {
            m_internalWFM.addConnection(outCont, outPortID,
                    m_modelInContainer[inPortID - getNrDataIns()], 0);
        }
    }


    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel#inportHasNewDataTable(int)
     */
    @Override
    protected void inportHasNewDataTable(final int inPortID) {
        LOGGER.debug("Executing input node #" + inPortID);
        super.inportHasNewDataTable(inPortID);
        // m_dataInContainer[inPortID].startExecution(null);
    }


    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel#inportWasDisconnected(int)
     */
    @Override
    protected void inportWasDisconnected(final int inPortID) {
        LOGGER.debug("Resetting input node #" + inPortID);
        super.inportWasDisconnected(inPortID);
        // m_dataInContainer[inPortID].reset();
        if (inPortID < getNrDataIns()) {
            ConnectionContainer cc = m_internalWFM.getIncomingConnectionAt(
                    m_dataInContainer[inPortID], 0);
            m_internalWFM.removeConnectionIfExists(cc);
        } else {
            ConnectionContainer cc = m_internalWFM.getIncomingConnectionAt(
                    m_dataInContainer[inPortID - getNrDataIns()], 0);
            m_internalWFM.removeConnectionIfExists(cc);            
        }
    }


    /**
     * @see de.unikn.knime.core.node.NodeStateListener
     *  #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
     */
    public void stateChanged(final NodeStatus state, final int id) { 
        if (state instanceof NodeStatus.Reset) {
            // one of the output nodes has been reset => put myself into
            // "not executed" status

            if (getNodeContainer() != null) {
                // during initialization the node container is not yet set
                m_resetFromInterior = true;
                try {
                    getNodeContainer().reset();
                } finally {
                    m_resetFromInterior = false;
                }
            }
        }
    }
}

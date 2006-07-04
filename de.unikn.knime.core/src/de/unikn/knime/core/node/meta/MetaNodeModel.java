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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.KNIMEConstants;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.NodeView;
import de.unikn.knime.core.node.SpecialNodeModel;
import de.unikn.knime.core.node.workflow.ConnectionContainer;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowInExecutionException;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * 
 * 
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class MetaNodeModel extends SpecialNodeModel
    implements WorkflowListener, NodeStateListener {
    private static final String INOUT_CONNECTIONS_KEY = "inOutConnections";
    
    private final NodeContainer[] m_dataInContainer, m_dataOutContainer;
    private final NodeContainer[] m_modelInContainer, m_modelOutContainer;
    private final MetaInputModel[] m_dataInModels;
    private final MetaOutputModel[] m_dataOutModels;    
    private final MetaInputModel[] m_modelInModels;
    private final NodeModel[] m_modelOutModels;
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
     * @param outerDataIns the number of data input ports
     * @param outerDataOuts the number of data output ports
     * @param outerPredParamsIns the number of predictor param input ports
     * @param outerPredParamsOuts the number of predictor param output ports
     * @param factory the factory that created this model
     */
    protected MetaNodeModel(final int outerDataIns, final int outerDataOuts,
            final int outerPredParamsIns, final int outerPredParamsOuts,
            final MetaInputModel[] dataInModels,
            final MetaInputModel[] modelInModels,
            final MetaOutputModel[] dataOutModels,
            final MetaOutputModel[] modelOutModels,
            final MetaNodeFactory factory) {
        super(outerDataIns, outerDataOuts, outerPredParamsIns,
                outerPredParamsOuts);

        m_dataInContainer = new NodeContainer[dataInModels.length];
        m_dataOutContainer = new NodeContainer[dataOutModels.length];
        m_modelInContainer = new NodeContainer[modelInModels.length];
        m_modelOutContainer = new NodeContainer[modelOutModels.length];

        m_dataInModels = dataInModels;        
        m_dataOutModels = dataOutModels;
        m_modelInModels = modelInModels;        
        m_modelOutModels = modelOutModels;
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
    protected MetaNodeModel(final int nrIns, final int nrOuts,
            final MetaNodeFactory f) {
        this(nrIns, nrOuts, 0, 0, createDefaultDataInputModels(nrIns),
                new MetaInputModel[0], createDefDataOutputModels(nrOuts),
                new MetaOutputModel[0], f);
    }

        
    private static MetaInputModel[] createDefaultDataInputModels(final int c) {
        MetaInputModel[] m = new MetaInputModel[c];
        for (int i = 0; i < c; i++) {
            m[i] = new DataInputNodeModel();
        }
        return m;
    }

    
    private static MetaOutputModel[] createDefDataOutputModels(final int c) {
        MetaOutputModel[] m = new MetaOutputModel[c];
        for (int i = 0; i < c; i++) {
            m[i] = new DataOutputNodeModel();
        }
        return m;
    }

    
    /**
     * The inSpecs are manually set in the <code>MetaInputNode</code>s. The
     * resulting outSpecs from the <code>MetaOutputNode</code>s are returned.
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {        
        createInternalWFM();
        
        if (!m_resetFromInterior) {
            for (int i = 0; (i < m_dataInModels.length)
                && (i < inSpecs.length); i++) {
                m_dataInModels[i].setDataTableSpec(inSpecs[i]);
                internalWFM().configureNode(m_dataInContainer[i].getID());
            }
                        
            for (int i = inSpecs.length; i < m_dataInModels.length; i++) {
                m_dataInModels[i].setDataTableSpec(inSpecs[inSpecs.length - 1]);
                internalWFM().configureNode(m_dataInContainer[i].getID());
            }
        }
        
        // collect all output specs
        DataTableSpec[] outspecs = new DataTableSpec[m_dataOutContainer.length];
        final int min = Math.min(outspecs.length, m_dataOutContainer.length);
        for (int i = 0; i < min; i++) {
            if (m_dataOutContainer[i] != null) {
                outspecs[i] = m_dataOutModels[i].getDataTableSpec();
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
        
        KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(new Runnable() {
            public void run() {
                internalWFM().executeAll(true);
            }
        });
        exec.checkCanceled();
        
        // translate output
        exec.setMessage("Collecting output");
        DataTable[] out = new DataTable[m_dataOutContainer.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = m_dataOutModels[i].getDataTable();
        }
        return out;
    }

    protected String getOutportDescription(final int inputNodeIndex) {
        return m_myFactory.getInportDescription(inputNodeIndex);
    }
    
    protected String getPredParamOutDescription(final int inputNodeIndex) {
        return m_myFactory.getPredParamInDescription(inputNodeIndex);
    }
    

    protected String getPredParamInDescription(final int outputNodeIndex) {
        return m_myFactory.getPredParamOutDescription(outputNodeIndex);
    }
        
    protected String getInportDescription(final int outputNodeIndex) {
        return m_myFactory.getOutportName(outputNodeIndex);
    }
    
    
    /**
     * Adds the input and output nodes to the workflow. 
     */
    private void addInOutNodes() {
        LOGGER.debug("Adding in- and output nodes");
        for (int i = 0; i < m_dataInContainer.length; i++) {
            final int temp = i;
            m_dataInContainer[i] = internalWFM().addNewNode(new NodeFactory() {
                @Override
                public NodeModel createNodeModel() {
                    return m_dataInModels[temp];
                }

                @Override
                protected int getNrNodeViews() {
                    return 0;
                }

                @Override
                public NodeView createNodeView(final int viewIndex,
                        final NodeModel nodeModel) {
                    return null;
                }

                @Override
                protected boolean hasDialog() {
                    return false;
                }

                @Override
                protected NodeDialogPane createNodeDialogPane() {
                    return null;
                }

                @Override
                public String getOutportDescription(final int index) {
                    return MetaNodeModel.this.getOutportDescription(temp);
                }                
            });
        }

        for (int i = 0; i < m_dataOutContainer.length; i++) {
            final int temp = i;
            m_dataOutContainer[i] = internalWFM().addNewNode(new NodeFactory() {
                @Override
                public NodeModel createNodeModel() {
                    return m_dataOutModels[temp];
                }

                @Override
                protected int getNrNodeViews() {
                    return 0;
                }

                @Override
                public NodeView createNodeView(final int viewIndex,
                        final NodeModel nodeModel) {
                    return null;
                }

                @Override
                protected boolean hasDialog() {
                    return false;
                }

                @Override
                protected NodeDialogPane createNodeDialogPane() {
                    return null;
                }

                @Override
                public String getInportDescription(final int index) {
                    return MetaNodeModel.this.getInportDescription(temp);
                }

            });
            m_dataOutContainer[i].addListener(this);
        }

        for (int i = 0; i < m_modelInContainer.length; i++) {
            final int temp = i;
            m_modelInContainer[i] = internalWFM().addNewNode(new NodeFactory() {
                @Override
                public NodeModel createNodeModel() {
                    return m_modelInModels[temp];
                }

                @Override
                protected int getNrNodeViews() {
                    return 0;
                }

                @Override
                public NodeView createNodeView(final int viewIndex,
                        final NodeModel nodeModel) {
                    return null;
                }

                @Override
                protected boolean hasDialog() {
                    return false;
                }

                @Override
                protected NodeDialogPane createNodeDialogPane() {
                    return null;
                }

                @Override
                public String getPredParamOutDescription(final int index) {
                    return MetaNodeModel.this.getPredParamOutDescription(temp);
                }
            });
        }

        for (int i = 0; i < m_modelOutContainer.length; i++) {
            final int temp = i;
            m_modelOutContainer[i] = internalWFM().addNewNode(new NodeFactory() {
                @Override
                public NodeModel createNodeModel() {
                    return m_modelOutModels[temp];
                }

                @Override
                protected int getNrNodeViews() {
                    return 0;
                }

                @Override
                public NodeView createNodeView(final int viewIndex,
                        final NodeModel nodeModel) {
                    return null;
                }

                @Override
                protected boolean hasDialog() {
                    return false;
                }

                @Override
                protected NodeDialogPane createNodeDialogPane() {
                    return null;
                }
                
                @Override
                public String getPredParamInDescription(final int index) {
                    return MetaNodeModel.this.getPredParamInDescription(temp);
                }
            });
            m_modelOutContainer[i].addListener(this);
        }        
    }
    
    
    /**
     * Adds the connection from and to the in-/output nodes.
     * 
     * @param connections the settings in which the connections are stored
     * @throws InvalidSettingsException if one of the settings is invalid
     * @throws WorkflowInExecutionException 
     */
    private void addInOutConnections(final NodeSettings connections)
    throws InvalidSettingsException, WorkflowInExecutionException {
        LOGGER.debug("Adding in- and output connections");
        for (String key : connections) {
            if (key.startsWith("connection_")) {
                int[] conn = connections.getIntArray(key);
                internalWFM().addConnection(conn[0], conn[1], conn[2],
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
        if (!m_resetFromInterior && (internalWFM() != null)) {
            try {
                internalWFM().resetAndConfigureAll();
            } catch (WorkflowInExecutionException ex) {
                LOGGER.error("Could not reset meta node", ex);
            }
        }
    }

 
    /**
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #saveSettingsTo(java.io.File, de.unikn.knime.core.node.NodeSettings,
     *  de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveSettingsTo(final File nodeFile,
            final NodeSettings settings, final ExecutionMonitor exec) {
        if (internalWFM() == null) { return; }
        
        NodeSettings connections = settings.addConfig(INOUT_CONNECTIONS_KEY);
        
        int count = 0;
        int[] conn = new int[4];
        
        for (NodeContainer nc : m_dataInContainer) {
            List<ConnectionContainer> conncon =
                internalWFM().getOutgoingConnectionsAt(nc, 0);
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
                internalWFM().getIncomingConnectionAt(nc, 0);
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
                internalWFM().getOutgoingConnectionsAt(nc, 0);
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
                internalWFM().getIncomingConnectionAt(nc, 0);
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
        
        
        try {
            File f = new File(nodeFile.getParentFile(), "workflow.knime");
            f.createNewFile();
            internalWFM().save(f, omitNodes);
        } catch (IOException ex) {
            LOGGER.error(ex);
        } catch (CanceledExecutionException ex) {
            LOGGER.error(ex);
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not save meta node", ex);
        }
    }


    /**
     * Returns the workflow manager representing the meta-workflow.
     * 
     * @return the meta-workflow manager for this meta-node
     */
    public WorkflowManager getMetaWorkflowManager() {
        createInternalWFM();
        
        return internalWFM();
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
     *  #inportHasNewDataTable(DataTable, int)
     */
    @Override
    protected void inportHasNewDataTable(final DataTable table,
            final int inPortID) {
        createInternalWFM();
        m_dataInModels[inPortID].setDataTable(table);

        if (inPortID == getNrDataIns() - 1) {
            for (int i = 0; i < m_dataInModels.length; i++) {
                m_dataInModels[i].setDataTable(table);
            }
        }    
    }


    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #inportHasNewTableSpec(de.unikn.knime.core.data.DataTableSpec, int)
     */
    @Override
    protected void inportHasNewTableSpec(final DataTableSpec spec, 
            final int inPortID) {
        createInternalWFM();
        m_dataInModels[inPortID].setDataTableSpec(spec);
        
        try {
            internalWFM().resetAndConfigureNode(
                    m_dataInContainer[inPortID].getID());
            
            if (inPortID == getNrDataIns() - 1) {
                for (int i = 0; i < m_dataInModels.length; i++) {
                    m_dataInModels[i].setDataTableSpec(spec);
                    internalWFM().resetAndConfigureNode(
                            m_dataInContainer[i].getID());                
                }
            }
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not reset meta input nodes", ex);
        }
    }

    
    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel#inportWasDisconnected(int)
     */
    @Override
    protected void inportWasDisconnected(final int inPortID) {
        createInternalWFM();
        LOGGER.debug("Resetting input node #" + inPortID);
        super.inportWasDisconnected(inPortID);

        try {
            if (inPortID < getNrDataIns()) {
                m_dataInModels[inPortID].setDataTable(null);
                internalWFM().resetAndConfigureNode(
                        m_dataInContainer[inPortID].getID());
            } else {
                m_modelInModels[inPortID - getNrDataIns()]
                                .setPredictorParams(null);
                internalWFM().resetAndConfigureNode(
                        m_modelInContainer[inPortID - getNrDataIns()].getID());
            }
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not reset meta input nodes", ex);
        }
    }


    /**
     * Returns the node container for a data input node.
     * 
     * @param index the index of the data input node
     * @return a node container
     */
    protected final NodeContainer dataInContainer(final int index) {
        return m_dataInContainer[index];
    }


    /**
     * Returns the node container for a data output node.
     * 
     * @param index the index of the data output node
     * @return a node container
     */
    protected final NodeContainer dataOutContainer(final int index) {
        return m_dataOutContainer[index];
    }

    
    /**
     * Returns the node container for a model input node.
     * 
     * @param index the index of the model input node
     * @return a node container
     */
    protected final NodeContainer modelInContainer(final int index) {
        return m_modelInContainer[index];
    }
    
    protected final MetaInputModel dataInModel(final int index) {
        return m_dataInModels[index];
    }

    protected final MetaOutputModel dataOutModel(final int index) {
        return m_dataOutModels[index];
    }
    
    
    /**
     * Returns the node container for a model output node.
     * 
     * @param index the index of the model output node
     * @return a node container
     */
    protected final NodeContainer modelOutContainer(final int index) {
        return m_modelOutContainer[index];
    }
    
   
    private boolean m_wfmInitialized;
    private void createInternalWFM() {
        if (!m_wfmInitialized) {
            internalWFM().addListener(this);
            addInOutNodes();
            m_wfmInitialized = true;
        }
    }

    
    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #loadValidatedSettingsFrom(java.io.File,
     *  de.unikn.knime.core.node.NodeSettings,
     *  de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadValidatedSettingsFrom(final File nodeFile,
            final NodeSettings settings, final ExecutionMonitor exec)
            throws InvalidSettingsException {
        createInternalWFM();
        // load the "internal" workflow
        File f = new File(nodeFile.getParentFile(),
                WorkflowManager.WORKFLOW_FILE);
        if (f.exists() && f.isFile()) {
            try {
                internalWFM().clear();
                addInOutNodes();
    
                try {
                    internalWFM().load(f);
                } catch (IOException ex) {
                    throw new InvalidSettingsException("Could not load internal"
                            + " workflow");
                } catch (CanceledExecutionException ex) {
                    throw new InvalidSettingsException("Loading of internal"
                            + " workflow has been interrupted by user");
                }
                addInOutConnections(settings.getConfig(INOUT_CONNECTIONS_KEY));
            } catch (WorkflowInExecutionException ex) {
                LOGGER.error("Could not load internal workflow", ex);
            }
        }
    }


    /**
     * @see de.unikn.knime.core.node.NodeStateListener
     *  #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
     */
    public void stateChanged(final NodeStatus state, final int id) {
        if (state instanceof NodeStatus.Reset) {
            m_resetFromInterior = true;
            try {
                resetMyself();
            } finally {
                m_resetFromInterior = false;
            }
        }
    }


    /**
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #validateSettings(java.io.File, de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final File nodeFile,
            final NodeSettings settings) throws InvalidSettingsException {
        //         
    }    
}

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

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.KNIMEConstants;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.PredictorParams;
import de.unikn.knime.core.node.SpecialNodeModel;
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
    
    private final NodeContainer[] m_dataInContainer, m_dataOutContainer;
    private final NodeContainer[] m_modelInContainer, m_modelOutContainer;
    private final DataInputNodeModel[] m_dataInModels;
    private final DataOutputNodeModel[] m_dataOutModels;    
    private final ModelInputNodeModel[] m_modelInModels;
    private final ModelOutputNodeModel[] m_modelOutModels;
    private boolean m_resetFromInterior;
    
    /*
     * The listeners that are interested in node state changes.
     */
    private final ArrayList<NodeStateListener> m_stateListeners;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(MetaNodeModel.class);

    
    /**
     * Creates a new new meta node model.
     * 
     * @param dataIns the number of data input ports
     * @param dataOuts the number of data output ports
     * @param predParamsIns the number of predictor param input ports
     * @param predParamsOuts the number of predictor param output ports
     * @param factory the factory that created this model
     */
    protected MetaNodeModel(final int dataIns, final int dataOuts,
            final int predParamsIns, final int predParamsOuts) {
        super(dataIns, dataOuts, predParamsIns, predParamsOuts);

        m_dataInContainer = new NodeContainer[dataIns];
        m_dataOutContainer = new NodeContainer[dataOuts];
        m_modelInContainer = new NodeContainer[predParamsIns];
        m_modelOutContainer = new NodeContainer[predParamsOuts];

        m_dataInModels = new DataInputNodeModel[dataIns];
        m_dataOutModels = new DataOutputNodeModel[dataOuts];
        m_modelInModels = new ModelInputNodeModel[predParamsIns];        
        m_modelOutModels = new ModelOutputNodeModel[predParamsOuts];        
        m_stateListeners = new ArrayList<NodeStateListener>();
    }

            
    
    /**
     * The inSpecs are manually set in the <code>MetaInputNode</code>s. The
     * resulting outSpecs from the <code>MetaOutputNode</code>s are returned.
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {        
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
                if (outspecs[i] == null) {
                    throw new InvalidSettingsException("Inner workflow is not"
                            + " fully connected yet");
                }
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


    /**
     * Adds the input and output nodes to the workflow. 
     */
    protected void addInternalNodes() {
        LOGGER.debug("Adding in- and output nodes");
        for (int i = 0; i < m_dataInContainer.length; i++) {
            m_dataInContainer[i] =
                internalWFM().addNewNode(new DataInputNodeFactory());
            m_dataInContainer[i].setCustomName("External data input " + i);
        }

        for (int i = 0; i < m_dataOutContainer.length; i++) {
            m_dataOutContainer[i] =
                internalWFM().addNewNode(new DataOutputNodeFactory());
            m_dataOutContainer[i].addListener(this);
            m_dataOutContainer[i].setCustomName("Data collector " + i);
        }

        for (int i = 0; i < m_modelInContainer.length; i++) {
            m_modelInContainer[i] =
                internalWFM().addNewNode(new ModelInputNodeFactory());
            m_modelInContainer[i].setCustomName("External model input " + i);
        }

        for (int i = 0; i < m_modelOutContainer.length; i++) {
            m_modelOutContainer[i] =
                internalWFM().addNewNode(new ModelOutputNodeFactory());
            m_modelOutContainer[i].addListener(this);
            m_modelOutContainer[i].setCustomName("Model collector " + i);
        }
        
        for (NodeContainer cont : internalWFM().getNodes()) {
            cont.addListener(this);
        }        
    }
    
    
    /**
     * Subclasses should override this (empty) method if they insert additional
     * nodes and add connections between the meta input/output nodes here.
     */
    protected void addInternalConnections() {
        // nothing to do for this class here
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
        try {
            int[] ids = new int[m_dataInContainer.length];
            for (int i = 0; i < m_dataInContainer.length; i++) {
                ids[i] = m_dataInContainer[i].getID();
            }
            settings.addIntArray("dataInContainerIDs", ids);
            
            ids = new int[m_dataOutContainer.length];
            for (int i = 0; i < m_dataOutContainer.length; i++) {
                ids[i] = m_dataOutContainer[i].getID();
            }
            settings.addIntArray("dataOutContainerIDs", ids);
            
            
            ids = new int[m_modelInContainer.length];
            for (int i = 0; i < m_modelInContainer.length; i++) {
                ids[i] = m_modelInContainer[i].getID();
            }
            settings.addIntArray("modelInContainerIDs", ids);
            
            ids = new int[m_modelOutContainer.length];
            for (int i = 0; i < m_modelOutContainer.length; i++) {
                ids[i] = m_modelOutContainer[i].getID();
            }
            settings.addIntArray("modelOutContainerIDs", ids);

            File f = new File(nodeFile.getParentFile(), "workflow.knime");
            f.createNewFile();
            internalWFM().save(f, exec);
        } catch (IOException ex) {
            LOGGER.error(ex);
        } catch (CanceledExecutionException ex) {
            LOGGER.error(ex);
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not save meta node", ex);
        }
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
        m_dataInModels[inPortID].setDataTable(table);
    }


    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel
     *  #inportHasNewTableSpec(de.unikn.knime.core.data.DataTableSpec, int)
     */
    @Override
    protected void inportHasNewTableSpec(final DataTableSpec spec, 
            final int inPortID) {
        m_dataInModels[inPortID].setDataTableSpec(spec);
        
        try {
            internalWFM().resetAndConfigureNode(
                    m_dataInContainer[inPortID].getID());
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not reset meta input nodes", ex);
        }
    }

    
    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel#inportWasDisconnected(int)
     */
    @Override
    protected void inportWasDisconnected(final int inPortID) {
        LOGGER.debug("Resetting input node #" + inPortID);
        super.inportWasDisconnected(inPortID);

        try {
            m_dataInModels[inPortID].setDataTable(null);
            internalWFM().resetAndConfigureNode(
                    m_dataInContainer[inPortID].getID());
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not reset meta input nodes", ex);
        }
    }


    private boolean m_wfmInitialized;
    /** 
     * @see de.unikn.knime.core.node.SpecialNodeModel#internalWFM()
     */
    @Override
    protected WorkflowManager internalWFM() {
        if (!m_wfmInitialized) {
            super.internalWFM().addListener(this);
            m_wfmInitialized = true;

            addInternalNodes();
            addInternalConnections();
            retrieveInOutModels();
        }
        
        return super.internalWFM();
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
        File f = new File(nodeFile.getParentFile(),
                WorkflowManager.WORKFLOW_FILE);
        if (f.exists() && f.isFile()) {
            try {
                internalWFM().clear();
                    
                try {
                    internalWFM().load(f);
                } catch (IOException ex) {
                    throw new InvalidSettingsException("Could not load internal"
                            + " workflow");
                } catch (CanceledExecutionException ex) {
                    throw new InvalidSettingsException("Loading of internal"
                            + " workflow has been interrupted by user");
                }
                
            } catch (WorkflowInExecutionException ex) {
                LOGGER.error("Could not load internal workflow", ex);
            }
            int[] ids = settings.getIntArray("dataInContainerIDs", new int[0]);
            for (int i = 0; i < ids.length; i++) {
                m_dataInContainer[i] =
                    internalWFM().getNodeContainerById(ids[i]);
            }
                        
            ids = settings.getIntArray("dataOutContainerIDs", new int[0]);
            for (int i = 0; i < ids.length; i++) {
                m_dataOutContainer[i] =
                    internalWFM().getNodeContainerById(ids[i]);
            }
            
            ids = settings.getIntArray("modelInContainerIDs", new int[0]);
            for (int i = 0; i < ids.length; i++) {
                m_modelInContainer[i] =
                    internalWFM().getNodeContainerById(ids[i]);
            }

            ids = settings.getIntArray("modelOutContainerIDs", new int[0]);
            for (int i = 0; i < ids.length; i++) {
                m_modelOutContainer[i] =
                    internalWFM().getNodeContainerById(ids[i]);
            }
            
            for (NodeContainer cont : internalWFM().getNodes()) {
                cont.addListener(this);
            }
        }
        retrieveInOutModels();
    }


    private void retrieveInOutModels() {
        for (int i = 0; i < m_dataInContainer.length; i++) {
            m_dataInContainer[i].retrieveModel(this);
            
            // If a node is loaded from disk the I/O nodes are created before
            // the workflow is loaded. Thus the data table from the predecessor
            // node is written into the wrong model. Therefore we copy the
            // tables and specs from the "old" model into the new model
            if (m_dataInModels[i] != null) {
                ((DataInputNodeModel)m_receivedModel)
                    .setDataTable(m_dataInModels[i].getDataTable()); 
                ((DataInputNodeModel)m_receivedModel)
                    .setDataTableSpec(m_dataInModels[i].getDataTableSpec()); 
            }
            m_dataInModels[i] = (DataInputNodeModel)m_receivedModel;
        }

        for (int i = 0; i < m_dataOutContainer.length; i++) {
            m_dataOutContainer[i].retrieveModel(this);
            m_dataOutModels[i] = (DataOutputNodeModel)m_receivedModel;
        }
        
        for (int i = 0; i < m_modelInContainer.length; i++) {
            m_modelInContainer[i].retrieveModel(this);
            m_modelInModels[i] = (ModelInputNodeModel)m_receivedModel;
        }

        for (int i = 0; i < m_modelOutContainer.length; i++) {
            m_modelOutContainer[i].retrieveModel(this);
            m_modelOutModels[i] = (ModelOutputNodeModel)m_receivedModel;
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
    
    
    protected NodeModel m_receivedModel;
    
    /**
     * Receives a node model. This method is called from the node upon calling
     * {@link de.unikn.knime.core.node.Node#retrieveModel(MetaNodeModel)}.
     * 
     * @param model a node model
     */
    public void receiveModel(final NodeModel model) {
        m_receivedModel = model;
    }


    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadPredictorParams(int, de.unikn.knime.core.node.PredictorParams)
     */
    @Override
    protected void loadPredictorParams(final int index,
            final PredictorParams predParams) throws InvalidSettingsException {
        m_modelInModels[index].setPredictorParams(predParams);
    }

    
    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #savePredictorParams(int, de.unikn.knime.core.node.PredictorParams)
     */
    @Override
    protected void savePredictorParams(final int index,
            final PredictorParams predParams) throws InvalidSettingsException {
        m_modelOutModels[index].getPredictorParams().copyTo(predParams);
    }


    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #loadInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }


    /** 
     * @see de.unikn.knime.core.node.NodeModel
     *  #saveInternals(java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }
    
    protected final DataOutputNodeModel dataOutModel(final int index) {
        return m_dataOutModels[index];
    }
}

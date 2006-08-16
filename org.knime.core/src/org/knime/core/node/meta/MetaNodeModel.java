/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   17.11.2005 (cebron): created
 */
package org.knime.core.node.meta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeStateListener;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.SpecialNodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowInExecutionException;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * This model is the heart of all meta workflows. It is reposnsible for
 * executing the inner workflow, for collecting its results and for the
 * communication with the outer flow. Therefore it needs some special
 * functionality and is therefore derived from
 * {@link org.knime.core.node.SpecialNodeModel}. This model is intended to be
 * subclassed by other nodes that execute some inner workflow, like cross
 * validation or boosting.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class MetaNodeModel extends SpecialNodeModel implements
        WorkflowListener, NodeStateListener {

    private final NodeContainer[] m_dataInContainer, m_dataOutContainer;

    private final NodeContainer[] m_modelInContainer, m_modelOutContainer;

    private final DataInputNodeModel[] m_dataInModels;

    private final DataOutputNodeModel[] m_dataOutModels;

    private final ModelInputNodeModel[] m_modelInModels;

    private final ModelOutputNodeModel[] m_modelOutModels;

    private boolean m_resetFromInterior;

    static {
        // this if for backwards compatibility with release 1.0.0
        NodeFactory.addLoadedFactory(DataInputNodeFactory.class);
        NodeFactory.addLoadedFactory(DataOutputNodeFactory.class);
        NodeFactory.addLoadedFactory(ModelInputNodeFactory.class);
        NodeFactory.addLoadedFactory(ModelOutputNodeFactory.class);
    }

    /*
     * The listeners that are interested in node state changes.
     */
    private final ArrayList<NodeStateListener> m_stateListeners;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MetaNodeModel.class);

    /**
     * Creates a new new meta node model.
     * 
     * @param dataIns the number of data input ports
     * @param dataOuts the number of data output ports
     * @param predParamsIns the number of predictor param input ports
     * @param predParamsOuts the number of predictor param output ports
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
     * 
     * @see org.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (!m_resetFromInterior) {
            for (int i = 0; (i < m_dataInModels.length) && (i < inSpecs.length); i++) {
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
                if (!m_dataOutContainer[i].isFullyConnected()) {
                    throw new InvalidSettingsException("Internal workflow "
                            + "is not fully connected, please open the meta"
                            + "workflow editor.");  
                }
            }
        }
        return outspecs;
    }

    private boolean m_innerExecCanceled;

    /**
     * During execute, the inData <code>DataTables</code> are passed on to the
     * <code>MetaInputNode</code>s. The inner workflow gets executed and the
     * output <code>DataTable</code>s from the <code>MetaOutputNode</code>s
     * are returned.
     * 
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        exec.setMessage("Executing inner workflow");

        KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(new Runnable() {
            public void run() {
                internalWFM().executeAll(true);
            }
        });
        if (m_innerExecCanceled) {
            throw new CanceledExecutionException("Inner node canceled");
        }
        exec.checkCanceled();

        // translate output
        exec.setMessage("Collecting output");
        BufferedDataTable[] out = new BufferedDataTable[m_dataOutContainer.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = m_dataOutModels[i].getBufferedDataTable();
        }
        return out;
    }

    /**
     * Adds the input and output nodes to the workflow.
     */
    protected void addInternalNodes() {
        LOGGER.debug("Adding in- and output nodes");
        for (int i = 0; i < m_dataInContainer.length; i++) {
            m_dataInContainer[i] = internalWFM().addNewNode(
                    new DataInputNodeFactory());
            m_dataInContainer[i].setCustomName("External data input " + i);
            m_dataInContainer[i].setDeletable(false);
        }

        for (int i = 0; i < m_dataOutContainer.length; i++) {
            m_dataOutContainer[i] = internalWFM().addNewNode(
                    new DataOutputNodeFactory());
            m_dataOutContainer[i].addListener(this);
            m_dataOutContainer[i].setCustomName("Data collector " + i);
            m_dataOutContainer[i].setDeletable(false);
        }

        for (int i = 0; i < m_modelInContainer.length; i++) {
            m_modelInContainer[i] = internalWFM().addNewNode(
                    new ModelInputNodeFactory());
            m_modelInContainer[i].setCustomName("External model input " + i);
            m_modelInContainer[i].setDeletable(false);
        }

        for (int i = 0; i < m_modelOutContainer.length; i++) {
            m_modelOutContainer[i] = internalWFM().addNewNode(
                    new ModelOutputNodeFactory());
            m_modelOutContainer[i].addListener(this);
            m_modelOutContainer[i].setCustomName("Model collector " + i);
            m_modelOutContainer[i].setDeletable(false);
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
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_innerExecCanceled = false;
        if (!m_resetFromInterior && (internalWFM() != null)) {
            try {
                m_resetFromInterior = true;
                internalWFM().resetAndConfigureAll();
            } catch (WorkflowInExecutionException ex) {
                LOGGER.error("Could not reset meta node", ex);
            } finally {
                m_resetFromInterior = false;
            }
        }
    }

    /**
     * Returns if the {@link #reset()} is called because of an internal
     * trigger or from the outside.
     * 
     * @return <code>true</code> if the reset is triggered internally,
     * <code>false</code> if it comes from the outside
     */
    protected final boolean resetFromInterior() {
        return m_resetFromInterior;
    }
    
    /**
     * @see org.knime.core.node.SpecialNodeModel #saveSettingsTo(java.io.File,
     *      NodeSettingsWO, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveSettingsTo(final File nodeDir,
            final NodeSettingsWO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        if (internalWFM() == null) {
            return;
        }
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

            File f = new File(nodeDir, "workflow.knime");
            f.createNewFile();
            internalWFM().save(f, exec.getProgressMonitor());
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
     * @see org.knime.core.node.workflow.WorkflowListener#workflowChanged
     *      (org.knime.core.node.workflow.WorkflowEvent)
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
        } else if (event instanceof WorkflowEvent.NodeAdded) {
            ((NodeContainer)event.getNewValue()).addListener(this);
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
     * @see org.knime.core.node.SpecialNodeModel
     *      #inportHasNewDataTable(BufferedDataTable, int)
     */
    @Override
    protected void inportHasNewDataTable(final BufferedDataTable table,
            final int inPortID) {
        m_dataInModels[inPortID].setBufferedDataTable(table);
    }

    /**
     * @see org.knime.core.node.SpecialNodeModel
     *      #inportHasNewTableSpec(org.knime.core.data.DataTableSpec, int)
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
     * @see org.knime.core.node.SpecialNodeModel#inportWasDisconnected(int)
     */
    @Override
    protected void inportWasDisconnected(final int inPortID) {
        LOGGER.debug("Resetting input node #" + inPortID);
        super.inportWasDisconnected(inPortID);

        try {
            m_dataInModels[inPortID].setBufferedDataTable(null);
            internalWFM().resetAndConfigureNode(
                    m_dataInContainer[inPortID].getID());
        } catch (WorkflowInExecutionException ex) {
            LOGGER.error("Could not reset meta input nodes", ex);
        }
    }

    private boolean m_wfmInitialized;

    /**
     * @see org.knime.core.node.SpecialNodeModel#internalWFM()
     */
    @Override
    protected final WorkflowManager internalWFM() {
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
     * @see org.knime.core.node.SpecialNodeModel
     *      #loadValidatedSettingsFrom(java.io.File, NodeSettingsRO,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadValidatedSettingsFrom(final File nodeDir,
            final NodeSettingsRO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        if (nodeDir != null) {
            File f = new File(nodeDir, WorkflowManager.WORKFLOW_FILE);
            if (f.exists() && f.isFile()) {
                try {
                    internalWFM().clear();

                    try {
                        internalWFM().load(f, new DefaultNodeProgressMonitor());
                    } catch (IOException ex) {
                        throw new InvalidSettingsException(
                                "Could not load internal workflow");
                    } catch (CanceledExecutionException ex) {
                        throw new InvalidSettingsException(
                                "Loading of internal"
                                        + " workflow has been interrupted by user");
                    }

                } catch (WorkflowInExecutionException ex) {
                    LOGGER.error("Could not load internal workflow", ex);
                }
                int[] ids = settings.getIntArray("dataInContainerIDs",
                        new int[0]);
                for (int i = 0; i < ids.length; i++) {
                    m_dataInContainer[i] = internalWFM().getNodeContainerById(
                            ids[i]);
                    m_dataInContainer[i].setDeletable(false);
                }

                ids = settings.getIntArray("dataOutContainerIDs", new int[0]);
                for (int i = 0; i < ids.length; i++) {
                    m_dataOutContainer[i] = internalWFM().getNodeContainerById(
                            ids[i]);
                    m_dataOutContainer[i].setDeletable(false);
                }

                ids = settings.getIntArray("modelInContainerIDs", new int[0]);
                for (int i = 0; i < ids.length; i++) {
                    m_modelInContainer[i] = internalWFM().getNodeContainerById(
                            ids[i]);
                    m_modelInContainer[i].setDeletable(false);
                }

                ids = settings.getIntArray("modelOutContainerIDs", new int[0]);
                for (int i = 0; i < ids.length; i++) {
                    m_modelOutContainer[i] = internalWFM()
                            .getNodeContainerById(ids[i]);
                    m_modelOutContainer[i].setDeletable(false);
                }

                for (NodeContainer cont : internalWFM().getNodes()) {
                    cont.addListener(this);
                }
            }
            retrieveInOutModels();
        }
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
                        .setBufferedDataTable(m_dataInModels[i]
                                .getBufferedDataTable());
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
     * @see org.knime.core.node.NodeStateListener
     *      #stateChanged(org.knime.core.node.NodeStatus, int)
     */
    public void stateChanged(final NodeStatus state, final int id) {
        boolean outNode = false;
        for (NodeContainer nc : m_dataOutContainer) {
            if ((nc != null) && (nc.getID() == id)) {
                outNode = true;
                break;
            }
        }

        for (NodeContainer nc : m_modelOutContainer) {
            if ((nc != null) && (nc.getID() == id)) {
                outNode = true;
                break;
            }
        }

        if (((state instanceof NodeStatus.Reset) || (state instanceof NodeStatus.Configured))
                && outNode && !m_resetFromInterior) {
            m_resetFromInterior = true;
            try {
                resetMyself();
            } finally {
                m_resetFromInterior = false;
            }
        } else if (state instanceof NodeStatus.ExecutionCanceled) {
            m_innerExecCanceled = true;
        }
    }

    /**
     * @see org.knime.core.node.SpecialNodeModel #validateSettings(java.io.File,
     *      NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final File nodeFile,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        //         
    }

    private NodeModel m_receivedModel;

    /**
     * Receives a node model. This method is called from the node upon calling
     * {@link org.knime.core.node.Node#retrieveModel(MetaNodeModel)}.
     * 
     * @param model a node model
     */
    public void receiveModel(final NodeModel model) {
        m_receivedModel = model;
    }

    /**
     * @see org.knime.core.node.NodeModel #loadModelContent(int, ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        m_modelInModels[index].setModelContent(predParams);
    }

    /**
     * @see org.knime.core.node.NodeModel #saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        m_modelOutModels[index].getModelContent().copyTo(predParams);
    }

    /**
     * @see org.knime.core.node.NodeModel #loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    /**
     * @see org.knime.core.node.NodeModel #saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * Returns the data output model at the given index.
     * 
     * @param index the index
     * @return a data ouput model
     */
    protected final DataOutputNodeModel dataOutModel(final int index) {
        return m_dataOutModels[index];
    }

    /**
     * Returns the model output model at the given index.
     * 
     * @param index the index
     * @return a model ouput model
     */
    protected final ModelOutputNodeModel modelOutModel(final int index) {
        return m_modelOutModels[index];
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save here
    }

    /**
     * Checks if a node in the inner workflow has been canceled.
     * 
     * @return <code>true</code> if the inner workflow has been canceled,
     *         <code>false</code> otherwise
     */
    protected final boolean innerExecCanceled() {
        return m_innerExecCanceled;
    }
}

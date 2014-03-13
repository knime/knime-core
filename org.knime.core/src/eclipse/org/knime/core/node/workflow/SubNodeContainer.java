/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Oct 1, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.NodePropertyChangedEvent.NodeProperty;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.core.node.workflow.virtual.VirtualNodeInput;
import org.knime.core.node.workflow.virtual.VirtualPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.VirtualPortObjectInNodeModel;
import org.knime.core.node.workflow.virtual.VirtualPortObjectOutNodeFactory;
import org.knime.core.node.workflow.virtual.VirtualPortObjectOutNodeModel;
import org.w3c.dom.Element;

/** Implementation of a {@link NodeContainer} holding a set of nodes via a {@link WorkflowManager}
 * and acting like a {@link NativeNodeContainer} to the outside.
 *
 * @noreference Not to be used by clients.
 * @author M. Berthold & B. Wiswedel
 * @since 2.9
 */
public final class SubNodeContainer extends SingleNodeContainer {

    private WorkflowManager m_wfm;

    private NodeInPort[] m_inports;
    private NodeContainerOutPort[] m_outports;
    /** Keeps outgoing information (specs, objects, HiLiteHandlers...). */
    static class Output {
        String name = "none";
        PortType type;
        PortObjectSpec spec;
        PortObject object;
        HiLiteHandler hiliteHdl;
        String summary = "no summary";
    }
    private Output[] m_outputs;

    private int m_virtualInNodeIDSuffix;
    private int m_virtualOutNodeIDSuffix;

    private FlowObjectStack m_incomingStack;
    private FlowObjectStack m_outgoingStack;

    /** Load workflow from persistor.
     *
     * @param parent ...
     * @param id ...
     * @param persistor ...
     */
    SubNodeContainer(final WorkflowManager parent, final NodeID id, final SubNodeContainerPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        WorkflowPersistor workflowPersistor = persistor.getWorkflowPersistor();
        m_wfm = new WorkflowManager(parent, id, workflowPersistor);

        WorkflowPortTemplate[] inPortTemplates = persistor.getInPortTemplates();
        WorkflowPortTemplate[] outPortTemplates = persistor.getOutPortTemplates();
        m_outports = new NodeContainerOutPort[outPortTemplates.length];
        for (int i = 0; i < outPortTemplates.length; i++) {
            WorkflowPortTemplate t = outPortTemplates[i];
            m_outports[i] = new NodeContainerOutPort(this, t.getPortType(), t.getPortIndex());
            m_outports[i].setPortName(t.getPortName());
        }
        m_outputs = new Output[m_outports.length];
        for (int i = 0; i < m_outputs.length; i++) {
            m_outputs[i] = new Output();
        }
        m_inports = new NodeInPort[inPortTemplates.length];
        m_virtualInNodeIDSuffix = persistor.getVirtualInNodeIDSuffix();
        m_virtualOutNodeIDSuffix = persistor.getVirtualOutNodeIDSuffix();
        PortType[] inTypes = new PortType[inPortTemplates.length];
        for (int i = 0; i < inPortTemplates.length; i++) {
            inTypes[i] = inPortTemplates[i].getPortType();
            m_inports[i] = new NodeInPort(i, inTypes[i]);
        }
    }

    /**
     * Create new SubNode from existing Metanode (=WorkflowManager).
     *
     * @param parent ...
     * @param id ...
     * @param content ...
     * @param name The name of the sub node
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id, final WorkflowManager content,
        final String name) {
        super(parent, id);
        // Create new, internal workflow manager:
        m_wfm = new WorkflowManager(parent, id, new PortType[]{}, new PortType[]{},
        /*isProject=*/true, parent.getContext(), name);
        // and copy content
        WorkflowCopyContent c = new WorkflowCopyContent();
        c.setAnnotation(content.getWorkflowAnnotations().toArray(new WorkflowAnnotation[0]));
        c.setNodeIDs(content.getWorkflow().getNodeIDs().toArray(new NodeID[0]));
        c.setIncludeInOutConnections(false);
        WorkflowPersistor wp = content.copy(c);
        WorkflowCopyContent wcc = m_wfm.paste(wp);
        // create map of NodeIDs for quick lookup/search
        Collection<NodeContainer> ncs = content.getNodeContainers();
        NodeID[] orgIDs = new NodeID[ncs.size()];
        int j = 0;
        for (NodeContainer nc : ncs) {
            orgIDs[j] = nc.getID();
            j++;
        }
        NodeID[] newIDs = wcc.getNodeIDs();
        Map<NodeID, NodeID> oldIDsHash = new HashMap<NodeID, NodeID>();
        for (j = 0; j < orgIDs.length; j++) {
            oldIDsHash.put(orgIDs[j], newIDs[j]);
        }
        // initialize NodeContainer inports
        // (metanodes don't have hidden variable port 0, SingleNodeContainers do!)
        m_inports = new NodeInPort[content.getNrInPorts() + 1];
        PortType[] inTypes = new PortType[content.getNrInPorts()];
        for (int i = 0; i < content.getNrInPorts(); i++) {
            inTypes[i] = content.getInPort(i).getPortType();
            m_inports[i + 1] = new NodeInPort(i + 1, inTypes[i]);
        }
        m_inports[0] = new NodeInPort(0, FlowVariablePortObject.TYPE_OPTIONAL);
        // initialize NodeContainer outports
        // (metanodes don't have hidden variable port 0, SingleNodeContainers do!)
        m_outports = new NodeContainerOutPort[content.getNrOutPorts() + 1];
        PortType[] outTypes = new PortType[content.getNrOutPorts()];
        m_outputs = new Output[content.getNrOutPorts() + 1];
        for (int i = 0; i < content.getNrOutPorts(); i++) {
            outTypes[i] = content.getOutPort(i).getPortType();
            m_outputs[i + 1] = new Output();
            m_outputs[i + 1].type = content.getOutPort(i).getPortType();
            m_outports[i + 1] = new NodeContainerOutPort(this, m_outputs[i + 1].type, i + 1);
        }
        m_outputs[0] = new Output();
        m_outputs[0].type = FlowVariablePortObject.TYPE;
        m_outports[0] = new NodeContainerOutPort(this, FlowVariablePortObject.TYPE, 0);
        // add virtual in/out nodes and connect them
        NodeID inNodeID = m_wfm.addNode(new VirtualPortObjectInNodeFactory(inTypes));
        for (ConnectionContainer cc : content.getWorkflow().getConnectionsBySource(content.getID())) {
            m_wfm.addConnection(inNodeID, cc.getSourcePort() + 1, oldIDsHash.get(cc.getDest()), cc.getDestPort());
        }
        m_virtualInNodeIDSuffix = inNodeID.getIndex();
        NodeID outNodeID = m_wfm.addNode(new VirtualPortObjectOutNodeFactory(outTypes));
        for (ConnectionContainer cc : content.getWorkflow().getConnectionsByDest(content.getID())) {
            m_wfm.addConnection(oldIDsHash.get(cc.getSource()), cc.getSourcePort(), outNodeID, cc.getDestPort() + 1);
        }
        m_virtualOutNodeIDSuffix = outNodeID.getIndex();


        int xmin = Integer.MAX_VALUE;
        int ymin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymax = Integer.MIN_VALUE;
        for (NodeContainer nc : m_wfm.getNodeContainers()) {
            NodeUIInformation uii = nc.getUIInformation();
            if (uii != null) {
                int[] bounds = uii.getBounds();
                if (bounds.length >= 2) {
                    xmin = Math.min(bounds[0], xmin);
                    ymin = Math.min(bounds[1], ymin);
                    xmax = Math.max(bounds[0], xmax);
                    ymax = Math.max(bounds[1], ymax);
                }
            }
        }

        // move virtual in/out nodes
        NodeContainer inNode = m_wfm.getNodeContainer(getVirtualInNodeID());
        int x = xmin - 100;
        int y = (ymin + ymax) / 2;
        inNode.setUIInformation(new NodeUIInformation(x, y, 0, 0, true));
        NodeContainer outNode = m_wfm.getNodeContainer(getVirtualOutNodeID());
        x = xmax + 100;
        outNode.setUIInformation(new NodeUIInformation(x, y, 0, 0, true));

        setInternalState(m_wfm.getInternalState());
    }

    /* -------------------- Subnode specific -------------- */

    /** @return the inportNodeModel */
    NativeNodeContainer getVirtualInNode() {
        return (NativeNodeContainer)m_wfm.getNodeContainer(getVirtualInNodeID());
    }

    /** @return the inportNodeModel */
    VirtualPortObjectInNodeModel getVirtualInNodeModel() {
        return (VirtualPortObjectInNodeModel)getVirtualInNode().getNodeModel();
    }

    /** @return the outportNodeModel */
    NativeNodeContainer getVirtualOutNode() {
        return (NativeNodeContainer)m_wfm.getNodeContainer(getVirtualOutNodeID());
    }

    /** @return the outportNodeModel */
    VirtualPortObjectOutNodeModel getVirtualOutNodeModel() {
        return (VirtualPortObjectOutNodeModel)getVirtualOutNode().getNodeModel();
    }

    /** @return the inNodeID
     * @since 2.10 */
    public NodeID getVirtualInNodeID() {
        return new NodeID(m_wfm.getID(), m_virtualInNodeIDSuffix);
    }

    /** @return the outNodeID
     * @since 2.10 */
    public NodeID getVirtualOutNodeID() {
        return new NodeID(m_wfm.getID(), m_virtualOutNodeIDSuffix);
    }

    /* -------------------- NodeContainer info properties -------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        // TODO return real description.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        // TODO return useful Icons
        return SubNodeContainer.class.getResource("virtual/empty.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        // TODO create and return matching icon
        return NodeType.Subnode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_wfm.getName();
    }

    /**
     * @param name The new name
     * @since 2.10
     */
    public void setName(final String name) {
        m_wfm.setName(name);
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.Name);
    }

    /* ------------------- Specific Interna ------------------- */

    /**
     * @return underlying workflow.
     */
    public WorkflowManager getWorkflowManager() {
        return m_wfm;
    }

    /* -------------------- Dialog Handling ------------------ */

    private NodeDialogPane m_nodeDialogPane;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        int c = NodeExecutionJobManagerPool.getNumberOfJobManagersFactories();
        return c > 1 || m_wfm.findNodes(DialogNode.class, true).size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs, final PortObject[] inData)
        throws NotConfigurableException {
        NodeDialogPane dialogPane = getDialogPane();
        // find all dialog nodes and update subnode dialog
        @SuppressWarnings("rawtypes")
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        ((SubNodeDialogPane)dialogPane).setDialogNodes(nodes);
        NodeSettings settings = new NodeSettings("subnode_settings");
        saveSettings(settings);
        Node.invokeDialogInternalLoad(dialogPane, settings, inSpecs, inData,
            new FlowObjectStack(getID()),
            new CredentialsProvider(this, m_wfm.getCredentialsStore()),
            getParent().isWriteProtected());
        return dialogPane;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPane() {
        if (m_nodeDialogPane == null) {
            if (hasDialog()) {
                // create sub node dialog with dialog nodes
                m_nodeDialogPane = new SubNodeDialogPane();
                // job managers tab
                if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() > 1) {
                    // TODO: set the SplitType depending on the nodemodel
                    SplitType splitType = SplitType.USER;
                    m_nodeDialogPane.addJobMgrTab(splitType);
                }
                Node.addMiscTab(m_nodeDialogPane);
            } else {
                throw new IllegalStateException("Workflow has no dialog");
            }
        }
        return m_nodeDialogPane;
    }

    /**subNodeID
     * {@inheritDoc}
     */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        return true;
    }

    /* -------------------- Views ------------------ */

    // TODO: enable view handling!
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeViewName(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<NodeModel> getNodeView(final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveView() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInteractiveWebView() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V
        getInteractiveView() {
        return null;
    }

    /* -------------- Configuration/Execution ------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    void performReset() {
        setNodeMessage(NodeMessage.NONE);
        setVirtualOutputIntoOutport();
        m_wfm.resetAllNodesInWFM();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean performConfigure(final PortObjectSpec[] rawInSpecs, final NodeConfigureHelper nch) {
        assert rawInSpecs.length == m_inports.length;
        // copy specs into underlying WFM inports
        // (skip port 0 - SingleNodeContains know about the hidden var port, NodeModels don't!)
        PortObject[] inSpecs = new PortObject[rawInSpecs.length - 1];
        for (int i = 0; i < inSpecs.length; i++) {
            inSpecs[i] = InactiveBranchPortObject.INSTANCE;
        }
        getVirtualInNodeModel().setVirtualNodeInput(new VirtualNodeInput(inSpecs, 0));
        // and launch a configure on entire sub workflow
        // TODO this should more properly call only configure - reset is handled elsewhere!
        m_wfm.resetAndConfigureAll();
        // retrieve results and copy specs to outports
        if (getVirtualOutNodeModel().getOutSpecs() != null) {
            for (int i = 1; i < m_outputs.length; i++) {
                m_outputs[i].spec = getVirtualOutNodeModel().getOutSpecs()[i - 1];
            }
        }
        m_outputs[0].spec = FlowVariablePortObjectSpec.INSTANCE;
        setInternalState(InternalNodeContainerState.CONFIGURED);
//        setInternalState(m_wfm.getInternalState());
        // TODO return status - configure may fail ;-)
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionStatus performExecuteNode(final PortObject[] rawInObjects) {
        setNodeMessage(NodeMessage.NONE);
        assert rawInObjects.length == m_inports.length;
        // copy objects into underlying WFM inports
        // (skip port 0 - SingleNodeContains know about the hidden var port, NodeModels don't!)
        PortObject[] inObjects = new PortObject[rawInObjects.length - 1];
        for (int i = 0; i < inObjects.length; i++) {
            inObjects[i] = rawInObjects[i + 1 ];
        }
        getVirtualInNodeModel().setVirtualNodeInput(new VirtualNodeInput(inObjects, 0));
        // and launch execute on entire sub workflow
        m_wfm.executeAll();
        boolean isCanceled = false;
        try {
            m_wfm.waitWhileInExecution(-1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            m_wfm.cancelExecution(); // TODO - ideally done via parent but that is orphaned?
            isCanceled = true;
        }
        boolean allExecuted = m_wfm.getInternalState().isExecuted();
        if (allExecuted) {
            setVirtualOutputIntoOutport();
        } else if (isCanceled) {
            setNodeMessage(new NodeMessage(Type.WARNING, "Execution canceled"));
        } else {
            setNodeMessage(new NodeMessage(Type.ERROR, "Not all contained nodes are executed:\n"
                    + m_wfm.printNodeSummary(m_wfm.getID(), 0)));
        }
        return allExecuted ? NodeContainerExecutionStatus.SUCCESS : NodeContainerExecutionStatus.FAILURE;
    }

    /** */
    private void setVirtualOutputIntoOutport() {
        // retrieve results and copy to outports
        PortObject[] internalOutputs = getVirtualOutNodeModel().getOutObjects();
        for (int i = 1; i < m_outputs.length; i++) {
            m_outputs[i].spec = internalOutputs == null ? null : internalOutputs[i - 1].getSpec();
            m_outputs[i].object = internalOutputs == null ? null : internalOutputs[i - 1];
        }
        m_outputs[0].spec = internalOutputs == null ? null : FlowVariablePortObjectSpec.INSTANCE;
        m_outputs[0].object = internalOutputs == null ? null : FlowVariablePortObject.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionContext createExecutionContext() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionResult createExecutionResult(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------- Ports and related stuff --------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrInPorts() {
        return m_inports.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeInPort getInPort(final int index) {
        return m_inports[index];
    }

    /**
     * @param portTypes Types of the new ports
     * @since 2.10
     */
    public void setInPorts(final PortType[] portTypes) {
        m_inports = new NodeInPort[portTypes.length + 1];
        for (int i = 0; i < portTypes.length; i++) {
            m_inports[i + 1] = new NodeInPort(i + 1, portTypes[i]);
        }
        NodeContainer oldVNode = m_wfm.getNodeContainer(getVirtualInNodeID());
        m_inports[0] = new NodeInPort(0, FlowVariablePortObject.TYPE_OPTIONAL);
        m_wfm.removeNode(getVirtualInNodeID());
        m_virtualInNodeIDSuffix = m_wfm.addNode(new VirtualPortObjectInNodeFactory(portTypes)).getIndex();
        NodeContainer newVNode = m_wfm.getNodeContainer(getVirtualInNodeID());
        newVNode.setUIInformation(oldVNode.getUIInformation());
        m_wfm.setDirty();
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.MetaNodePorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrOutPorts() {
        return m_outports.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeOutPort getOutPort(final int index) {
        return m_outports[index];
    }

    /**
     * @param portTypes Types of the new ports
     * @since 2.10
     */
    public void setOutPorts(final PortType[] portTypes) {
        m_outputs = new Output[portTypes.length + 1];
        m_outports = new NodeContainerOutPort[portTypes.length + 1];
        for (int i = 0; i < portTypes.length; i++) {
            m_outputs[i + 1] = new Output();
            m_outputs[i + 1].type = portTypes[i];
            m_outports[i + 1] = new NodeContainerOutPort(this, portTypes[i], i + 1);
        }
        m_outputs[0] = new Output();
        m_outputs[0].type = FlowVariablePortObject.TYPE;
        m_outports[0] = new NodeContainerOutPort(this, FlowVariablePortObject.TYPE, 0);
        NodeContainer oldVNode = m_wfm.getNodeContainer(getVirtualOutNodeID());
        m_wfm.removeNode(getVirtualOutNodeID());
        m_virtualOutNodeIDSuffix = m_wfm.addNode(new VirtualPortObjectOutNodeFactory(portTypes)).getIndex();
        NodeContainer newVNode = m_wfm.getNodeContainer(getVirtualOutNodeID());
        newVNode.setUIInformation(oldVNode.getUIInformation());
        m_wfm.setDirty();
        setDirty();
        notifyNodePropertyChangedListener(NodeProperty.MetaNodePorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void cleanOutPorts(final boolean isLoopRestart) {
        for (Output o : m_outputs) {
            o.spec = null;
            o.object = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getOutputType(final int portIndex) {
        return m_outputs[portIndex].type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getOutputSpec(final int portIndex) {
        return m_outputs[portIndex].spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getOutputObject(final int portIndex) {
        return m_outputs[portIndex].object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputObjectSummary(final int portIndex) {
        return "SubNode Output: " + m_outputs[portIndex].summary;
    }

    /* ------------- HiLite Support ---------------- */

    /* TODO: enable if this ever makes sense. */

    /**
     * {@inheritDoc}
     */
    @Override
    void setInHiLiteHandler(final int index, final HiLiteHandler hdl) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutputHiLiteHandler(final int portIndex) {
        return null;
    }

    /* ------------------ Load&Save ------------------------- */

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    boolean performAreModelSettingsValid(final NodeSettingsRO modelSettings) {
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            NodeID id = entry.getKey();
            String nodeID = Integer.toString(id.getIndex());
            if (modelSettings.containsKey(nodeID)) {
                try {
                    NodeSettingsRO conf = modelSettings.getNodeSettings(nodeID);
                    DialogNode node = entry.getValue();
                    node.getDialogValue().validateSettings(conf);
                } catch (InvalidSettingsException e) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    void performLoadModelSettingsFrom(final NodeSettingsRO modelSettings) throws InvalidSettingsException {
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            NodeID id = entry.getKey();
            String nodeID = Integer.toString(id.getIndex());
            if (modelSettings.containsKey(nodeID)) {
                NodeSettingsRO conf = modelSettings.getNodeSettings(nodeID);
                NodeSettingsWO oldSettings = new NodeSettings(nodeID);
                DialogNode node = entry.getValue();
                node.getDialogValue().saveToNodeSettings(oldSettings);
                if (!conf.equals(oldSettings)) {
                    node.getDialogValue().loadFromNodeSettings(conf);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
        final Map<Integer, BufferedDataTable> tblRep, final FlowObjectStack inStack, final ExecutionMonitor exec,
        final LoadResult loadResult, final boolean preserveNodeMessage) throws CanceledExecutionException {
        SubNodeContainerPersistor subNodePersistor = (SubNodeContainerPersistor)nodePersistor;
        WorkflowPersistor workflowPersistor = subNodePersistor.getWorkflowPersistor();
        // TODO pass in a filter input stack
        m_wfm.loadContent(workflowPersistor, tblRep, inStack, exec, loadResult, preserveNodeMessage);
        setVirtualOutputIntoOutport();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    void performSaveModelSettingsTo(final NodeSettings modelSettings) {
        Map<NodeID, DialogNode> nodes = m_wfm.findNodes(DialogNode.class, false);
        for (Map.Entry<NodeID, DialogNode> entry : nodes.entrySet()) {
            String nodeID = Integer.toString(entry.getKey().getIndex());
            NodeSettingsWO subSettings = modelSettings.addNodeSettings(nodeID);
            entry.getValue().getDialogValue().saveToNodeSettings(subSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeContainerPersistor getCopyPersistor(final HashMap<Integer, ContainerTable> tableRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository, final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        return new CopySubNodeContainerPersistor(this,
            tableRep, fileStoreHandlerRepository, preserveDeletableFlags, isUndoableDeleteCommand);
    }

    /* -------------------- Credentials/Stacks ------------------ */

    /**
     * {@inheritDoc}
     */
    @Override
    void performSetCredentialsProvider(final CredentialsProvider cp) {
        // TODO needed once we want to support workflow credentials
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CredentialsProvider getCredentialsProvider() {
        // TODO needed once we want to support workflow credentials
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setFlowObjectStack(final FlowObjectStack st, final FlowObjectStack outgoingStack) {
        m_incomingStack = st;
        m_outgoingStack = outgoingStack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        return m_incomingStack;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowObjectStack getOutgoingFlowObjectStack() {
        return m_outgoingStack;
    }

    /* -------------- SingleNodeContainer methods without meaningful equivalent --------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInactive() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInactiveBranchConsumer() {
        return false;
    }

    /** Implementation of {@link WorkflowManager#getSubnodeInputPortInfo(NodeID)}.
     * @return ...
     */
    MetaPortInfo[] getInputPortInfo() {
        WorkflowManager wfm = getWorkflowManager();
        Workflow wfmFlow = wfm.getWorkflow();
        NodeContainer inNode = wfm.getNodeContainer(getVirtualInNodeID());

        List<MetaPortInfo> result = new ArrayList<MetaPortInfo>(inNode.getNrOutPorts());
        for (int i = 0; i < inNode.getNrOutPorts(); i++) {
            int insideCount = 0;
            for (ConnectionContainer cc : wfmFlow.getConnectionsBySource(
                    getVirtualInNodeID())) {
                if (cc.getSourcePort() == i) {
                    // could also be a through connection
                    insideCount += 1;
                }
            }
            boolean hasOutsideConnection = false;
            for (ConnectionContainer outCC : getParent().getWorkflow().getConnectionsByDest(getID())) {
                if (outCC.getDestPort() == i) {
                    hasOutsideConnection = true;
                    break;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = inNode.getOutPort(i).getPortType();
            if (hasOutsideConnection || insideCount > 0) {
                isConnected = true;
                if (hasOutsideConnection && insideCount > 0) {
                    message = "Connected to one upstream node and "
                        + insideCount + " downstream node(s)";
                } else if (hasOutsideConnection) {
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + insideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result.add(new MetaPortInfo(portType, isConnected, message, i));
        }
        return result.toArray(new MetaPortInfo[result.size()]);
    }

    /** Implementation of {@link WorkflowManager#getSubnodeOutputPortInfo(NodeID)}.
     * @return ...
     */
    MetaPortInfo[] getOutputPortInfo() {
        WorkflowManager wfm = getWorkflowManager();
        Workflow wfmFlow = wfm.getWorkflow();
        NodeContainer outNode = wfm.getNodeContainer(getVirtualOutNodeID());

        List<MetaPortInfo> result = new ArrayList<MetaPortInfo>(outNode.getNrInPorts());
        for (int i = 0; i < outNode.getNrInPorts(); i++) {
            boolean hasInsideConnection = false;
            for (ConnectionContainer cc : wfmFlow.getConnectionsByDest(getVirtualOutNodeID())) {
                if (cc.getDestPort() == i) {
                    hasInsideConnection = true;
                    break;
                }
            }
            int outsideCount = 0;
            for (ConnectionContainer outCC : getParent().getWorkflow().getConnectionsBySource(getID())) {
                if (outCC.getSourcePort() == i) {
                    outsideCount += 1;
                }
            }
            String message;
            boolean isConnected;
            PortType portType = outNode.getInPort(i).getPortType();
            if (hasInsideConnection || outsideCount > 0) {
                isConnected = true;
                if (hasInsideConnection && outsideCount > 0) {
                    message = "Connected to one upstream node and " + outsideCount + " downstream node(s)";
                } else if (hasInsideConnection) {
                    // could also be a through conn but we ignore here
                    message = "Connected to one upstream node";
                } else {
                    message = "Connected to " + outsideCount + " downstream node(s)";
                }
            } else {
                isConnected = false;
                message = null;
            }
            result.add(new MetaPortInfo(portType, isConnected, message, i));
        }
        return result.toArray(new MetaPortInfo[result.size()]);
    }

}

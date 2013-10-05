/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.interactive.WebViewTemplate;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
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
 * @author M. Berthold & B. Wiswedel
 * @since 2.9
 */
public class SubNodeContainer extends SingleNodeContainer {

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

    private VirtualPortObjectInNodeModel m_inportNodeModel;
    private VirtualPortObjectOutNodeModel m_outportNodeModel;

    private FlowObjectStack m_incomingStack;
    private FlowObjectStack m_outgoingStack;

    /** Create new, empty SubNodeContainer.
     *
     * @param parent ...
     * @param id ...
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id) {
        super(parent, id);
        // TODO: create empty WFM
    }

    /** Load workflow from persistor.
     *
     * @param parent ...
     * @param id ...
     * @param persistor ...
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id, final NodeContainerMetaPersistor persistor) {
        super(parent, id, persistor);
        // TODO load content from persistor
    }

    /** Create new SubNode from existing Metanode (=WorkflowManager).
     *
     * @param parent ...
     * @param id ...
     * @param content ...
     */
    public SubNodeContainer(final WorkflowManager parent, final NodeID id, final WorkflowManager content) {
        super(parent, id);
        // Create new, internal workflow manager:
        m_wfm = new WorkflowManager(getParent(), id, new PortType[]{}, new PortType[]{},
            /*isProject=*/true, content.getContext(), "This is a SubNode");
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
        // initialize NodeContainer ports
        m_inports = new NodeInPort[content.getNrInPorts() + 1];
        PortType[] inTypes = new PortType[content.getNrInPorts()];
        for (int i = 0; i < content.getNrInPorts(); i++) {
            inTypes[i] = content.getInPort(i).getPortType();
            m_inports[i + 1] = new NodeInPort(i + 1, inTypes[i]);
        }
        inTypes[0] = FlowVariablePortObject.TYPE;
        m_inports[0] = new NodeInPort(0, inTypes[0]);

        m_outports = new NodeContainerOutPort[content.getNrOutPorts()];
        PortType[] outTypes = new PortType[content.getNrOutPorts()];
        m_outputs = new Output[content.getNrOutPorts() + 1];
        for (int i = 0; i < content.getNrOutPorts(); i++) {
            outTypes[i] = content.getOutPort(i).getPortType();
            m_outputs[i + 1] = new Output();
            m_outputs[i + 1].type = content.getOutPort(i).getPortType();
            m_outports[i + 1] = new NodeContainerOutPort(this, i);
        }
        m_outputs[0] = new Output();
        m_outputs[0].type = FlowVariablePortObject.TYPE;
        m_outports[0] = new NodeContainerOutPort(this, 0);

        // add virtual in/out nodes and connect them
        NodeID inNodeID = m_wfm.addNode(new VirtualPortObjectInNodeFactory(inTypes));
        m_inportNodeModel = (VirtualPortObjectInNodeModel)
                                    ((NativeNodeContainer)m_wfm.getNodeContainer(inNodeID)).getNodeModel();
        for (ConnectionContainer cc : content.getWorkflow().getConnectionsBySource(content.getID())) {
            m_wfm.addConnection(inNodeID, cc.getSourcePort() + 1, oldIDsHash.get(cc.getDest()), cc.getDestPort());
        }
        NodeID outNodeID = m_wfm.addNode(new VirtualPortObjectOutNodeFactory(outTypes));
        m_outportNodeModel = (VirtualPortObjectOutNodeModel)
                                    ((NativeNodeContainer)m_wfm.getNodeContainer(outNodeID)).getNodeModel();
        for (ConnectionContainer cc : content.getWorkflow().getConnectionsByDest(content.getID())) {
            m_wfm.addConnection(oldIDsHash.get(cc.getDest()), cc.getSourcePort(), outNodeID, cc.getDestPort() + 1);
        }
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        // TODO create and return matching icon
        return NodeType.Other;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        // TODO well...
        return "I am a SubNode";
    }

    /* ------------------- Specific Interna ------------------- */

    /**
     * @return underlying workflow.
     */
    public WorkflowManager getWorkflowManager() {
        return m_wfm;
    }

    /* -------------------- Dialog Handling ------------------ */

    // TODO: enable dialog handling!

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs, final PortObject[] inData)
        throws NotConfigurableException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeDialogPane getDialogPane() {
        return null;
    }

    /**
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
    public <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent>> V getInteractiveView() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebViewTemplate getInteractiveWebViewTemplate() {
        return null;
    }

    /* -------------- Configuration/Execution ------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    void performReset() {
        m_wfm.resetAllNodesInWFM();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean performConfigure(final PortObjectSpec[] inSpecs, final NodeConfigureHelper nch) {
        assert inSpecs.length == m_inports.length;
        // copy specs into underlying WFM inports
        PortObject[] inObjects = new PortObject[inSpecs.length];
        for (int i = 0; i < inSpecs.length; i++) {
            inObjects[i] = InactiveBranchPortObject.INSTANCE;
        }
        m_inportNodeModel.setVirtualNodeInput(new VirtualNodeInput(inObjects, 0));
        // and launch a configure on entire sub workflow
        // TODO this should more properly call only configure - reset is handled elsewhere!
        m_wfm.resetAndConfigureAll();
        // retrieve results and copy specs to outports
        PortObject[] outputs = m_outportNodeModel.getOutObjects();
        for (int i = 0; i < outputs.length; i++) {
            m_outputs[i].spec = outputs[i].getSpec();
        }
        // TODO return status - configure may fail ;-)
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerExecutionStatus performExecuteNode(final PortObject[] inObjects) {
        assert inObjects.length == m_inports.length;
        // copy objects into underlying WFM inports
        m_inportNodeModel.setVirtualNodeInput(new VirtualNodeInput(inObjects, 0));
        // and launch execute on entire sub workflow
        m_wfm.executeAll();
        // retrieve results and copy to outports
        PortObject[] outputs = m_outportNodeModel.getOutObjects();
        for (int i = 0; i < outputs.length; i++) {
            m_outputs[i].spec = outputs[i].getSpec();
            m_outputs[i].object = outputs[i];
        }
        return NodeContainerExecutionStatus.SUCCESS;
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
    @Override
    boolean performAreModelSettingsValid(final NodeSettingsRO modelSettings) {
        // TODO once dialogs are supported
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performLoadModelSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO  once dialogs are supported
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
        final Map<Integer, BufferedDataTable> tblRep, final FlowObjectStack inStack, final ExecutionMonitor exec,
        final LoadResult loadResult, final boolean preserveNodeMessage) throws CanceledExecutionException {
        // TODO needed for load/save...
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void performSaveModelSettingsTo(final NodeSettings modelSettings) {
        // TODO  once dialogs are supported
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeContainerPersistor getCopyPersistor(final HashMap<Integer, ContainerTable> tableRep,
        final FileStoreHandlerRepository fileStoreHandlerRepository, final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        // TODO needed for copy...
        return null;
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

}

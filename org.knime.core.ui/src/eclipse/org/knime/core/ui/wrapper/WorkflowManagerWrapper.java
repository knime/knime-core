/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   Oct 13, 2016 (hornm): created
 */
package org.knime.core.ui.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowInPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.WorkflowOutPortUI;
import org.knime.core.util.Pair;

/**
 * Implements the {@link WorkflowManagerUI} interface by simply wrapping the {@link WorkflowManager} implementation.
 *
 * For all return types that implement an interface (from core.api), another wrapper instance is returned.
 *
 * @author Martin Horn, University of Konstanz
 */
public final class WorkflowManagerWrapper extends NodeContainerWrapper<WorkflowManager> implements WorkflowManagerUI{

    private final WorkflowManager m_delegate;

    /**
     * @param delegate the wfm to delegate all the calls to
     */
    private WorkflowManagerWrapper(final WorkflowManager delegate) {
        super(delegate);
        m_delegate = delegate;
    }

    /**
     * Wraps the object via {@link Wrapper#wrapOrGet(Object, java.util.function.Function)}.
     *
     * @param wfm the object to be wrapped
     * @return a new wrapper or a already existing one
     */
    public static final WorkflowManagerWrapper wrap(final WorkflowManager wfm) {
        return (WorkflowManagerWrapper)Wrapper.wrapOrGet(wfm, o -> new WorkflowManagerWrapper(o));
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getReentrantLockInstance()
     */
    @Override
    public ReentrantLock getReentrantLockInstance() {
        return m_delegate.getReentrantLockInstance();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#isLockedByCurrentThread()
     */
    @Override
    public boolean isLockedByCurrentThread() {
        return m_delegate.isLockedByCurrentThread();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getProjectWFM()
     */
    @Override
    public WorkflowManagerUI getProjectWFM() {
        return WorkflowManagerWrapper.wrap(m_delegate.getProjectWFM());
    }

    /**
     * @param id
     * @see org.knime.core.node.workflow.WorkflowManager#removeProject(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public void removeProject(final NodeID id) {
        m_delegate.removeProject(id);
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canRemoveNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canRemoveNode(final NodeID nodeID) {
        return m_delegate.canRemoveNode(nodeID);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#isProject()
     */
    @Override
    public boolean isProject() {
        return m_delegate.isProject();
    }

    /**
     * @param source
     * @param sourcePort
     * @param dest
     * @param destPort
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#addConnection(org.knime.core.node.workflow.NodeID, int, org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public ConnectionContainerUI addConnection(final NodeID source, final int sourcePort, final NodeID dest, final int destPort) {
        return ConnectionContainerWrapper.wrap(m_delegate.addConnection(source, sourcePort, dest, destPort));
    }

    /**
     * @param source
     * @param sourcePort
     * @param dest
     * @param destPort
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canAddConnection(org.knime.core.node.workflow.NodeID, int, org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public boolean canAddConnection(final NodeID source, final int sourcePort, final NodeID dest, final int destPort) {
        return m_delegate.canAddConnection(source, sourcePort, dest, destPort);
    }

    /**
     * @param source
     * @param sourcePort
     * @param dest
     * @param destPort
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canAddNewConnection(org.knime.core.node.workflow.NodeID, int, org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public boolean canAddNewConnection(final NodeID source, final int sourcePort, final NodeID dest, final int destPort) {
        return m_delegate.canAddNewConnection(source, sourcePort, dest, destPort);
    }

    /**
     * @param cc
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canRemoveConnection(org.knime.core.ConnectionContainerUI.node.workflow.IConnectionContainer)
     */
    @Override
    public boolean canRemoveConnection(final ConnectionContainerUI cc) {
        return m_delegate.canRemoveConnection(Wrapper.unwrapCC(cc));
    }

    /**
     * @param cc
     * @see org.knime.core.node.workflow.WorkflowManager#removeConnection(org.knime.core.ConnectionContainerUI.node.workflow.IConnectionContainer)
     */
    @Override
    public void removeConnection(final ConnectionContainerUI cc) {
        m_delegate.removeConnection(Wrapper.unwrapCC(cc));
    }

    /**
     * @param id
     * @param portIdx
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getOutgoingConnectionsFor(org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public Set<ConnectionContainerUI> getOutgoingConnectionsFor(final NodeID id, final int portIdx) {
        return m_delegate.getOutgoingConnectionsFor(id, portIdx).stream().map(cc -> ConnectionContainerWrapper.wrap(cc)).collect(Collectors.toSet());
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getOutgoingConnectionsFor(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public Set<ConnectionContainerUI> getOutgoingConnectionsFor(final NodeID id) {
        return m_delegate.getOutgoingConnectionsFor(id).stream().map(cc -> ConnectionContainerWrapper.wrap(cc)).collect(Collectors.toSet());
    }

    /**
     * @param id
     * @param portIdx
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getIncomingConnectionFor(org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public ConnectionContainerUI getIncomingConnectionFor(final NodeID id, final int portIdx) {
        return ConnectionContainerWrapper.wrap(m_delegate.getIncomingConnectionFor(id, portIdx));
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getIncomingConnectionsFor(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public Set<ConnectionContainerUI> getIncomingConnectionsFor(final NodeID id) {
        return m_delegate.getIncomingConnectionsFor(id).stream().map(cc -> ConnectionContainerWrapper.wrap(cc)).collect(Collectors.toSet());
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getConnection(org.knime.core.def.node.workflow.ConnectionID)
     */
    @Override
    public ConnectionContainerUI getConnection(final ConnectionID id) {
        return ConnectionContainerWrapper.wrap(m_delegate.getConnection(id));
    }

    /**
     * @param metaNodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getMetanodeInputPortInfo(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public MetaPortInfo[] getMetanodeInputPortInfo(final NodeID metaNodeID) {
        return m_delegate.getMetanodeInputPortInfo(metaNodeID);
    }

    /**
     * @param metaNodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getMetanodeOutputPortInfo(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public MetaPortInfo[] getMetanodeOutputPortInfo(final NodeID metaNodeID) {
        return m_delegate.getMetanodeOutputPortInfo(metaNodeID);
    }

    /**
     * @param subNodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getSubnodeInputPortInfo(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public MetaPortInfo[] getSubnodeInputPortInfo(final NodeID subNodeID) {
        return m_delegate.getSubnodeInputPortInfo(subNodeID);
    }

    /**
     * @param subNodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getSubnodeOutputPortInfo(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public MetaPortInfo[] getSubnodeOutputPortInfo(final NodeID subNodeID) {
        return m_delegate.getSubnodeOutputPortInfo(subNodeID);
    }

    /**
     * @param subFlowID
     * @param newPorts
     * @see org.knime.core.node.workflow.WorkflowManager#changeMetaNodeInputPorts(org.knime.core.node.workflow.NodeID, org.knime.core.def.node.port.MetaPortInfo[])
     */
    @Override
    public void changeMetaNodeInputPorts(final NodeID subFlowID, final MetaPortInfo[] newPorts) {
        m_delegate.changeMetaNodeInputPorts(subFlowID, newPorts);
    }

    /**
     * @param subFlowID
     * @param newPorts
     * @see org.knime.core.node.workflow.WorkflowManager#changeMetaNodeOutputPorts(org.knime.core.node.workflow.NodeID, org.knime.core.def.node.port.MetaPortInfo[])
     */
    @Override
    public void changeMetaNodeOutputPorts(final NodeID subFlowID, final MetaPortInfo[] newPorts) {
        m_delegate.changeMetaNodeOutputPorts(subFlowID, newPorts);
    }

    /**
     * @param subFlowID
     * @param newPorts
     * @see org.knime.core.node.workflow.WorkflowManager#changeSubNodeInputPorts(org.knime.core.node.workflow.NodeID, org.knime.core.def.node.port.MetaPortInfo[])
     */
    @Override
    public void changeSubNodeInputPorts(final NodeID subFlowID, final MetaPortInfo[] newPorts) {
        m_delegate.changeSubNodeInputPorts(subFlowID, newPorts);
    }

    /**
     * @param subFlowID
     * @param newPorts
     * @see org.knime.core.node.workflow.WorkflowManager#changeSubNodeOutputPorts(org.knime.core.node.workflow.NodeID, org.knime.core.def.node.port.MetaPortInfo[])
     */
    @Override
    public void changeSubNodeOutputPorts(final NodeID subFlowID, final MetaPortInfo[] newPorts) {
        m_delegate.changeSubNodeOutputPorts(subFlowID, newPorts);
    }

    /**
     * @deprecated
     * @see org.knime.core.node.workflow.WorkflowManager#resetAll()
     */
    @Deprecated
    @Override
    public void resetAll() {
        m_delegate.resetAll();
    }

    /**
     *
     * @see org.knime.core.node.workflow.WorkflowManager#resetAndConfigureAll()
     */
    @Override
    public void resetAndConfigureAll() {
        m_delegate.resetAndConfigureAll();
    }

    /**
     * @param ids
     * @see org.knime.core.node.workflow.WorkflowManager#executeUpToHere(org.knime.core.node.workflow.NodeID[])
     */
    @Override
    public void executeUpToHere(final NodeID... ids) {
        m_delegate.executeUpToHere(ids);
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canReExecuteNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canReExecuteNode(final NodeID id) {
        return m_delegate.canReExecuteNode(id);
    }

    /**
     * @param id
     * @see org.knime.core.node.workflow.WorkflowManager#saveNodeSettingsToDefault(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public void saveNodeSettingsToDefault(final NodeID id) {
        m_delegate.saveNodeSettingsToDefault(id);
    }

    /**
     * @param id
     * @throws InterruptedException
     * @see org.knime.core.node.workflow.WorkflowManager#executePredecessorsAndWait(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public void executePredecessorsAndWait(final NodeID id) throws InterruptedException {
        m_delegate.executePredecessorsAndWait(id);
    }

    /**
     * @param subNodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canExpandSubNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public String canExpandSubNode(final NodeID subNodeID) {
        return m_delegate.canExpandSubNode(subNodeID);
    }

    /**
     * @param wfmID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canExpandMetaNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public String canExpandMetaNode(final NodeID wfmID) {
        return m_delegate.canExpandMetaNode(wfmID);
    }

    /**
     * @param orgIDs
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canCollapseNodesIntoMetaNode(org.knime.core.node.workflow.NodeID[])
     */
    @Override
    public String canCollapseNodesIntoMetaNode(final NodeID[] orgIDs) {
        return m_delegate.canCollapseNodesIntoMetaNode(orgIDs);
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canResetNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canResetNode(final NodeID nodeID) {
        return m_delegate.canResetNode(nodeID);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canResetContainedNodes()
     */
    @Override
    public boolean canResetContainedNodes() {
        return m_delegate.canResetContainedNodes();
    }

    /**
     * @param id
     * @see org.knime.core.node.workflow.WorkflowManager#resetAndConfigureNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public void resetAndConfigureNode(final NodeID id) {
        m_delegate.resetAndConfigureNode(id);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canConfigureNodes()
     */
    @Override
    public boolean canConfigureNodes() {
        return m_delegate.canConfigureNodes();
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canExecuteNodeDirectly(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canExecuteNodeDirectly(final NodeID nodeID) {
        return m_delegate.canExecuteNodeDirectly(nodeID);
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canExecuteNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canExecuteNode(final NodeID nodeID) {
        return m_delegate.canExecuteNode(nodeID);
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canCancelNode(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canCancelNode(final NodeID nodeID) {
        return m_delegate.canCancelNode(nodeID);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canCancelAll()
     */
    @Override
    public boolean canCancelAll() {
        return m_delegate.canCancelAll();
    }

    /**
     * @param nodeID
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canSetJobManager(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canSetJobManager(final NodeID nodeID) {
        return m_delegate.canSetJobManager(nodeID);
    }

    /**
     *
     * @see org.knime.core.node.workflow.WorkflowManager#shutdown()
     */
    @Override
    public void shutdown() {
        m_delegate.shutdown();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#executeAllAndWaitUntilDone()
     */
    @Override
    public boolean executeAllAndWaitUntilDone() {
        return m_delegate.executeAllAndWaitUntilDone();
    }

    /**
     * @return
     * @throws InterruptedException
     * @see org.knime.core.node.workflow.WorkflowManager#executeAllAndWaitUntilDoneInterruptibly()
     */
    @Override
    public boolean executeAllAndWaitUntilDoneInterruptibly() throws InterruptedException {
        return m_delegate.executeAllAndWaitUntilDoneInterruptibly();
    }

    /**
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     * @see org.knime.core.node.workflow.WorkflowManager#waitWhileInExecution(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean waitWhileInExecution(final long time, final TimeUnit unit) throws InterruptedException {
        return m_delegate.waitWhileInExecution(time, unit);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canExecuteAll()
     */
    @Override
    public boolean canExecuteAll() {
        return m_delegate.canExecuteAll();
    }

    /**
     *
     * @see org.knime.core.node.workflow.WorkflowManager#executeAll()
     */
    @Override
    public void executeAll() {
        m_delegate.executeAll();
    }

    /**
     * @param prefix
     * @param indent
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#printNodeSummary(org.knime.core.node.workflow.NodeID, int)
     */
    @Override
    public String printNodeSummary(final NodeID prefix, final int indent) {
        return m_delegate.printNodeSummary(prefix, indent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<NodeContainerUI> getNodeContainers() {
        return m_delegate.getNodeContainers().stream().map(nc -> wrap(nc)).collect(Collectors.toList());
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getConnectionContainers()
     */
    @Override
    public Collection<ConnectionContainerUI> getConnectionContainers() {
        return m_delegate.getConnectionContainers().stream().map(cc -> ConnectionContainerWrapper.wrap(cc)).collect(Collectors.toList());
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNodeContainer(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public NodeContainerUI getNodeContainer(final NodeID id) {
        return wrap(m_delegate.getNodeContainer(id));
    }

    /**
     * @param id
     * @param subclass
     * @param failOnError
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNodeContainer(org.knime.core.node.workflow.NodeID, java.lang.Class, boolean)
     */
    @Override
    public <T> T getNodeContainer(final NodeID id, final Class<T> subclass, final boolean failOnError) {
        return m_delegate.getNodeContainer(id, subclass, failOnError);
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#containsNodeContainer(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean containsNodeContainer(final NodeID id) {
        return m_delegate.containsNodeContainer(id);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#containsExecutedNode()
     */
    @Override
    public boolean containsExecutedNode() {
        return m_delegate.containsExecutedNode();
    }

    /**
     * @return
     * @deprecated
     * @see org.knime.core.node.workflow.WorkflowManager#getNodeErrorMessages()
     */
    @Deprecated
    @Override
    public List<NodeMessage> getNodeErrorMessages() {
        return m_delegate.getNodeErrorMessages();
    }

    /**
     * @param types
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNodeMessages(org.knime.core.node.workflow.NodeMessage.Type[])
     */
    @Override
    public List<Pair<String, NodeMessage>> getNodeMessages(final Type... types) {
        return m_delegate.getNodeMessages(types);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#isWriteProtected()
     */
    @Override
    public boolean isWriteProtected() {
        return m_delegate.isWriteProtected();
    }

    /**
     * @param recurse
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getLinkedMetaNodes(boolean)
     */
    @Override
    public List<NodeID> getLinkedMetaNodes(final boolean recurse) {
        return m_delegate.getLinkedMetaNodes(recurse);
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#canUpdateMetaNodeLink(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean canUpdateMetaNodeLink(final NodeID id) {
        return m_delegate.canUpdateMetaNodeLink(id);
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#hasUpdateableMetaNodeLink(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public boolean hasUpdateableMetaNodeLink(final NodeID id) {
        return m_delegate.hasUpdateableMetaNodeLink(id);
    }

    /**
     * @param password
     * @param hint
     * @throws NoSuchAlgorithmException
     * @see org.knime.core.node.workflow.WorkflowManager#setWorkflowPassword(java.lang.String, java.lang.String)
     */
    @Override
    public void setWorkflowPassword(final String password, final String hint) throws NoSuchAlgorithmException {
        m_delegate.setWorkflowPassword(password, hint);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#isUnlocked()
     */
    @Override
    public boolean isUnlocked() {
        return m_delegate.isUnlocked();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getPasswordHint()
     */
    @Override
    public String getPasswordHint() {
        return m_delegate.getPasswordHint();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#isEncrypted()
     */
    @Override
    public boolean isEncrypted() {
        return m_delegate.isEncrypted();
    }

    /**
     * @param out
     * @return
     * @throws IOException
     * @see org.knime.core.node.workflow.WorkflowManager#cipherOutput(java.io.OutputStream)
     */
    @Override
    public OutputStream cipherOutput(final OutputStream out) throws IOException {
        return m_delegate.cipherOutput(out);
    }

    /**
     * @param fileName
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getCipherFileName(java.lang.String)
     */
    @Override
    public String getCipherFileName(final String fileName) {
        return m_delegate.getCipherFileName(fileName);
    }

    /**
     * @param listener
     * @see org.knime.core.node.workflow.WorkflowManager#addListener(org.knime.core.def.node.workflow.WorkflowListener)
     */
    @Override
    public void addListener(final WorkflowListener listener) {
        m_delegate.addListener(listener);
    }

    /**
     * @param listener
     * @see org.knime.core.node.workflow.WorkflowManager#removeListener(org.knime.core.def.node.workflow.WorkflowListener)
     */
    @Override
    public void removeListener(final WorkflowListener listener) {
        m_delegate.removeListener(listener);
    }

    /**
     *
     * @see org.knime.core.node.workflow.WorkflowManager#setAutoSaveDirectoryDirtyRecursivly()
     */
    @Override
    public void setAutoSaveDirectoryDirtyRecursivly() {
        m_delegate.setAutoSaveDirectoryDirtyRecursivly();
    }

    /**
     * @param name
     * @see org.knime.core.node.workflow.WorkflowManager#setName(java.lang.String)
     */
    @Override
    public void setName(final String name) {
        m_delegate.setName(name);
    }

    /**
     * @param newName
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#renameWorkflowDirectory(java.lang.String)
     */
    @Override
    public boolean renameWorkflowDirectory(final String newName) {
        return m_delegate.renameWorkflowDirectory(newName);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNameField()
     */
    @Override
    public String getNameField() {
        return m_delegate.getNameField();
    }

    /**
     * @param editorInfo
     * @see org.knime.core.node.workflow.WorkflowManager#setEditorUIInformation(org.knime.core.def.node.workflow.EditorUIInformation)
     */
    @Override
    public void setEditorUIInformation(final EditorUIInformation editorInfo) {
        m_delegate.setEditorUIInformation(editorInfo);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getEditorUIInformation()
     */
    @Override
    public EditorUIInformation getEditorUIInformation() {
        return m_delegate.getEditorUIInformation();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNrWorkflowIncomingPorts()
     */
    @Override
    public int getNrWorkflowIncomingPorts() {
        return m_delegate.getNrWorkflowIncomingPorts();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNrWorkflowOutgoingPorts()
     */
    @Override
    public int getNrWorkflowOutgoingPorts() {
        return m_delegate.getNrWorkflowOutgoingPorts();
    }

    /**
     * @param i
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getWorkflowIncomingPort(int)
     */
    @Override
    public NodeOutPortWrapper getWorkflowIncomingPort(final int i) {
        return NodeOutPortWrapper.wrap(m_delegate.getWorkflowIncomingPort(i));
    }

    /**
     * @param i
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getWorkflowOutgoingPort(int)
     */
    @Override
    public NodeInPortWrapper getWorkflowOutgoingPort(final int i) {
        return NodeInPortWrapper.wrap(m_delegate.getWorkflowOutgoingPort(i));
    }

    /**
     * @param inPortsBarUIInfo
     * @see org.knime.core.node.workflow.WorkflowManager#setInPortsBarUIInfo(org.knime.core.def.node.workflow.NodeUIInformation)
     */
    @Override
    public void setInPortsBarUIInfo(final NodeUIInformation inPortsBarUIInfo) {
        m_delegate.setInPortsBarUIInfo(inPortsBarUIInfo);
    }

    /**
     * @param outPortsBarUIInfo
     * @see org.knime.core.node.workflow.WorkflowManager#setOutPortsBarUIInfo(org.knime.core.def.node.workflow.NodeUIInformation)
     */
    @Override
    public void setOutPortsBarUIInfo(final NodeUIInformation outPortsBarUIInfo) {
        m_delegate.setOutPortsBarUIInfo(outPortsBarUIInfo);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getInPortsBarUIInfo()
     */
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        return m_delegate.getInPortsBarUIInfo();
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getOutPortsBarUIInfo()
     */
    @Override
    public NodeUIInformation getOutPortsBarUIInfo() {
        return m_delegate.getOutPortsBarUIInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowInPortUI getInPort(final int index) {
        return WorkflowInPortWrapper.wrap(m_delegate.getInPort(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowOutPortUI getOutPort(final int index) {
        return WorkflowOutPortWrapper.wrap(m_delegate.getOutPort(index));
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getWorkflowAnnotations()
     */
    @Override
    public Collection<WorkflowAnnotation> getWorkflowAnnotations() {
        return m_delegate.getWorkflowAnnotations();
    }



    /**
     * @param annotation
     * @see org.knime.core.node.workflow.WorkflowManager#addWorkflowAnnotation(org.knime.core.WorkflowAnnotation.node.workflow.IWorkflowAnnotation)
     */
    @Override
    public void addWorkflowAnnotation(final WorkflowAnnotation annotation) {
        m_delegate.addWorkflowAnnotation(annotation);
    }

    /**
     * @param annotation
     * @see org.knime.core.node.workflow.WorkflowManager#bringAnnotationToFront(org.knime.core.UIWorkflowAnnotation.node.workflow.IWorkflowAnnotation)
     */
    @Override
    public void bringAnnotationToFront(final WorkflowAnnotation annotation) {
        m_delegate.bringAnnotationToFront(annotation);
    }

    /**
     * @param annotation
     * @see org.knime.core.node.workflow.WorkflowManager#sendAnnotationToBack(org.knime.core.UIWorkflowAnnotation.node.workflow.IWorkflowAnnotation)
     */
    @Override
    public void sendAnnotationToBack(final WorkflowAnnotation annotation) {
        m_delegate.sendAnnotationToBack(annotation);
    }

    /**
     * @param evt
     * @see org.knime.core.node.workflow.WorkflowManager#nodeUIInformationChanged(org.knime.core.def.node.workflow.NodeUIInformationEvent)
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        m_delegate.nodeUIInformationChanged(evt);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getNodeAnnotations()
     */
    @Override
    public List<NodeAnnotation> getNodeAnnotations() {
        return m_delegate.getNodeAnnotations();
    }

    /**
     * @param id
     * @param cl
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#castNodeModel(org.knime.core.node.workflow.NodeID, java.lang.Class)
     */
    @Override
    public <T> T castNodeModel(final NodeID id, final Class<T> cl) {
        return m_delegate.castNodeModel(id, cl);
    }

    /**
     * @param nodeModelClass
     * @param recurse
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#findNodes(java.lang.Class, boolean)
     */
    @Override
    public <T> Map<NodeID, T> findNodes(final Class<T> nodeModelClass, final boolean recurse) {
        return m_delegate.findNodes(nodeModelClass, recurse);
    }

    /**
     * @param id
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#findNodeContainer(org.knime.core.node.workflow.NodeID)
     */
    @Override
    public NodeContainerWrapper findNodeContainer(final NodeID id) {
        return NodeContainerWrapper.wrap(m_delegate.findNodeContainer(id));
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getInputNodes()
     */
    @Override
    public Map<String, ExternalNodeData> getInputNodes() {
        return m_delegate.getInputNodes();
    }

    /**
     * @param input
     * @throws InvalidSettingsException
     * @see org.knime.core.node.workflow.WorkflowManager#setInputNodes(java.util.Map)
     */
    @Override
    public void setInputNodes(final Map<String, ExternalNodeData> input) throws InvalidSettingsException {
        m_delegate.setInputNodes(input);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getExternalOutputs()
     */
    @Override
    public Map<String, ExternalNodeData> getExternalOutputs() {
        return m_delegate.getExternalOutputs();
    }

    /**
     * @param name
     * @see org.knime.core.node.workflow.WorkflowManager#removeWorkflowVariable(java.lang.String)
     */
    @Override
    public void removeWorkflowVariable(final String name) {
        m_delegate.removeWorkflowVariable(name);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.WorkflowManager#getContext()
     */
    @Override
    public WorkflowContext getContext() {
        return m_delegate.getContext();
    }

    /**
     *
     * @see org.knime.core.node.workflow.WorkflowManager#notifyTemplateConnectionChangedListener()
     */
    @Override
    public void notifyTemplateConnectionChangedListener() {
        m_delegate.notifyTemplateConnectionChangedListener();
    }

}

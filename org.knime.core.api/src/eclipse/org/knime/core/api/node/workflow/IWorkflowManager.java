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
 *   Sep 26, 2016 (hornm): created
 */
package org.knime.core.api.node.workflow;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.api.node.port.MetaPortInfo;
import org.knime.core.api.node.port.PortTypeUID;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.Pair;

/**
 *
 * @author Martin Horn, KNIME.com
 */
public interface IWorkflowManager extends INodeContainer{

//    /** The root of everything, a workflow with no in- or outputs.
//     * This workflow holds the top level projects. */
//    WorkflowManager ROOT = new WorkflowManager(null, null, NodeID.ROOTID, new PortType[0], new PortType[0], true, null,
//        "ROOT", Optional.empty(), Optional.empty(), Optional.empty());

//    /** The root of all metanodes that are part of the node repository, for instance x-val metanode.
//     * @noreference This field is not intended to be referenced by clients.
//     * @since 3.2 */
//    // this used to be part of UI code but moved into core because creation of child instance locks ROOT,
//    // which should be done with care.
//    // Problems with loading full repository when fully qualified name of node can't be loaded in
//    //   org.knime.core.node.workflow.FileNativeNodeContainerPersistor.loadNodeFactory(String)
//    WorkflowManager META_NODE_ROOT =
//        ROOT.createAndAddProject("KNIME MetaNode Repository", new WorkflowCreationHelper());

//    /** {@inheritDoc}
//     * @since 3.1 */
//    WorkflowLock lock();

//    /** Like {@link #lock()} just that it assert that the lock is already held by the calling thread. Used in private
//     * methods that need to be called while locked.
//     * @return The lock instance.
//     * @since 3.1
//     */
//    WorkflowLock assertLock();

    /** {@inheritDoc}
     * @since 3.1 */
    ReentrantLock getReentrantLockInstance();

    /** {@inheritDoc}
     * @since 3.1 */
    boolean isLockedByCurrentThread();

//    /** {@inheritDoc} */
//    NodeContainerParent getDirectNCParent();

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    IWorkflowManager getProjectWFM();

//    /** Create new project - which is the same as creating a new subworkflow
//     * at this level with no in- or outports.
//     * @param name the name of the workflow (<code>null</code> value is ok)
//     * @param creationHelper a workflow creation helper instance, must not be <code>null</code>
//     * @return newly created workflow
//     * @since 2.8
//     */
//    WorkflowManager createAndAddProject(String name, WorkflowCreationHelper creationHelper);

    /** Remove a project - the same as remove node but we make sure it really
     * looks like a project (i.e. has no in- or outports).
     *
     * @param id of the project to be removed.
     */
    void removeProject(NodeID id);

    /** Uses given Factory UID to create a new node and then adds new node to the
     * workflow manager. We will automatically find the next available free
     * index for the new node within the given prefix.
     *
     * @param factoryUID the unique identifier for the NodeFactory used to create the new node
     * @return newly created (unique) NodeID
     */
    NodeID createAndAddNode(NodeFactoryUID factoryUID);

    /** Create new Node based on given factory uid and add to workflow.
     *
     * @param factory ...
     * @return unique ID of the newly created and inserted node.
     * @since 2.9
     */
    NodeID addNode(NodeFactoryUID factoryUID);

//    /**
//     * @param factory ...
//     * @param context the context provided by the framework (e.g. the URL of the file that was dragged on the canvas)
//     * @return the node id of the created node.
//     */
//    NodeID addNodeAndApplyContext(NodeFactory<?> factory, NodeCreationContext context);

    /** Check if specific node can be removed (i.e. is not currently being
     * executed or waiting to be).
     *
     * @param nodeID id of node to be removed
     * @return true if node can safely be removed.
     */
    boolean canRemoveNode(NodeID nodeID);

    /** Remove node if possible. Throws an exception if node is "busy" and can
     * not be removed at this time. If the node does not exist, this method
     * returns without exception.
     *
     * @param nodeID id of node to be removed
     */
    void removeNode(NodeID nodeID);

    /** Creates new metanode. We will automatically find the next available
     * free index for the new node within this workflow.
     * @param inPorts types of external inputs (going into this workflow)
     * @param outPorts types of external outputs (exiting this workflow)
     * @param name Name of the workflow (null values will be handled)
     * @return newly created <code>WorkflowManager</code>
     */
    IWorkflowManager createAndAddSubWorkflow(PortTypeUID[] inPorts, PortTypeUID[] outPorts, String name);

    /** Returns true if this workflow manager is a project (which usually means
     * that the parent is {@link #ROOT}). It returns false if this workflow
     * is only a metanode in another metanode or project.
     * @return This property.
     * @since 2.6 */
    boolean isProject();

    /** Add new connection - throw Exception if the same connection
     * already exists.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @return newly created Connection object
     * @throws IllegalArgumentException if connection already exists
     */
    IConnectionContainer addConnection(NodeID source, int sourcePort, NodeID dest, int destPort);

    /** Check if a new connection can be added.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @return true if connection can be added.
     */
    boolean canAddConnection(NodeID source, int sourcePort, NodeID dest, int destPort);

    /**
     * @param source ID of the source node
     * @param sourcePort Index of the sources port
     * @param dest ID of the destination node
     * @param destPort Index of the destination port
     * @return true if the connection can be added, false otherwise
     * @since 2.6
     */
    boolean canAddNewConnection(NodeID source, int sourcePort, NodeID dest, int destPort);

    /** Check if a connection can safely be removed.
     *
     * @param cc connection
     * @return true if connection cc is removable.
     */
    boolean canRemoveConnection(IConnectionContainer cc);

    /** Remove connection.
     *
     * @param cc connection
     */
    void removeConnection(IConnectionContainer cc);

    /**
     * Returns the set of outgoing connections for the node with the passed id
     * at the specified port.
     *
     * @param id id of the node of interest
     * @param portIdx port index of that node
     * @return all outgoing connections for the passed node at the specified
     *  port
     */
    Set<IConnectionContainer> getOutgoingConnectionsFor(NodeID id, int portIdx);

    /** Get all outgoing connections for a node.
     * @param id The requested node
     * @return All current outgoing connections in a new set.
     * @throws IllegalArgumentException If the node is unknown or null.
     */
    Set<IConnectionContainer> getOutgoingConnectionsFor(NodeID id);

    /**
     * Returns the incoming connection of the node with the passed node id at
     * the specified port.
     * @param id id of the node of interest
     * @param portIdx port index
     * @return incoming connection at that port of the given node or null if it
     *     doesn't exist
     */
    IConnectionContainer getIncomingConnectionFor(NodeID id, int portIdx);

    /** Get all incoming connections for a node.
     * @param id The requested node
     * @return All current incoming connections in a new set.
     * @throws IllegalArgumentException If the node is unknown or null.
     */
    Set<IConnectionContainer> getIncomingConnectionsFor(NodeID id);

    /**
     * Gets a connection by id.
     * @param id of the connection to return
     * @return the connection with the specified id
     */
    IConnectionContainer getConnection(ConnectionID id);

    /** Get information on input ports of the argument (meta) node. It's used
     * by the routines that allow the user to change the port information
     * (add, delete, move).
     * @param metaNodeID The argument node
     * @return the metanode's port info.
     * @throws IllegalArgumentException If the node is invalid.
     * @since 2.6 */
    MetaPortInfo[] getMetanodeInputPortInfo(NodeID metaNodeID);

    /** Get information on output ports of the argument (meta) node. Similar
     * to {@link #getMetanodeInputPortInfo(NodeID)}.
     * @param metaNodeID ...
     * @return ...
     * @throws IllegalArgumentException If the node is invalid.
     * @since 2.6 */
    MetaPortInfo[] getMetanodeOutputPortInfo(NodeID metaNodeID);

    /** Get information on input ports of the argument (sub) node. It's used
     * by the routines that allow the user to change the port information
     * (add, delete, move).
     * @param subNodeID The argument node
     * @return the sub node's port info.
     * @throws IllegalArgumentException If the node is invalid.
     * @since 2.10 */
    MetaPortInfo[] getSubnodeInputPortInfo(NodeID subNodeID);

    /** Get information on output ports of the argument (sub) node. Similar
     * to {@link #getSubnodeInputPortInfo(NodeID)}.
     * @param subNodeID ...
     * @return ...
     * @throws IllegalArgumentException If the node is invalid.
     * @since 2.10 */
    MetaPortInfo[] getSubnodeOutputPortInfo(NodeID subNodeID);

    /**
     * @param subFlowID ID of the subflow
     * @param newPorts The new ports
     * @since 2.6
     */
    void changeMetaNodeInputPorts(NodeID subFlowID, MetaPortInfo[] newPorts);

    /**
     * @param subFlowID ID of the subflow
     * @param newPorts The new ports
     * @since 2.6
     */
    void changeMetaNodeOutputPorts(NodeID subFlowID, MetaPortInfo[] newPorts);

    /**
     * @param subFlowID ID of the subflow
     * @param newPorts The new ports
     * @since 2.10
     */
    void changeSubNodeInputPorts(NodeID subFlowID, MetaPortInfo[] newPorts);

    /**
     * @param subFlowID ID of the subflow
     * @param newPorts The new ports
     * @since 2.10
     */
    void changeSubNodeOutputPorts(NodeID subFlowID, MetaPortInfo[] newPorts);

//    /** Load Settings into specified node.
//     *
//     * @param id of node
//     * @param settings to be load by node
//     * @throws InvalidSettingsException if settings are wrong
//     * @throws IllegalArgumentException if node does not exist
//     */
//    void loadNodeSettings(NodeID id, NodeSettingsRO settings) throws InvalidSettingsException;

//    /**
//     * write node settings into Settings object.
//     *
//     * @param id of node
//     * @param settings to be saved to
//     * @throws InvalidSettingsException thrown if nonsense is written
//     */
//    void saveNodeSettings(NodeID id, NodeSettingsWO settings) throws InvalidSettingsException;

//    /** Gets for a set of nodes their (overlapping) node settings. This is currently only the job manager but
//     * might contain also the memory settings in the future. If the nodes have different settings (e.g. job managers),
//     * the result will represent a default (e.g. a null job manager).
//     *
//     * <p>Used from a GUI action that allows the user to modify the settings for multiple selected nodes.
//     * @param ids The nodes of interest.
//     * @return The settings ... as far as they overlap
//     * @noreference This method is not intended to be referenced by clients.
//     * @since 2.7
//     */
//    NodeContainerSettings getCommonSettings(NodeID... ids);

//    /** Counterpart to {@link #getCommonSettings(NodeID...)}. It applies the same settings to all
//     * argument nodes.
//     * @param settings ...
//     * @param ids ...
//     * @throws InvalidSettingsException If not possible (settings may be applied to half of the nodes)
//     * @since 2.7
//     */
//    void applyCommonSettings(NodeContainerSettings settings, NodeID... ids) throws InvalidSettingsException;

    /** Resets and freshly configures all nodes in this workflow.
     * @deprecated Use {@link #resetAndConfigureAll()} instead
     */
    @Deprecated
    void resetAll();

    /** Resets and freshly configures all nodes in this workflow. */
    void resetAndConfigureAll();

    /** mark these nodes and all not-yet-executed predecessors for execution.
     * They will be marked first, queued when all inputs are available and
     * finally executed.
     *
     * @param ids node ids to mark
     */
    void executeUpToHere(NodeID... ids);

    /**
     * @param id ...
     * @return true if node can be re-executed.
     * @throws IllegalArgumentException if node is not of proper type.
     * @since 2.8
     */
    boolean canReExecuteNode(NodeID id);

//    /** Reexecute given node. This required an executed InteractiveNodeModel.
//     * Side effects:
//     *  - a reset/configure of executed successors.
//     *
//     * @param id the node
//     * @param vc the view content to be loaded into the node before re-execution
//     * @param useAsNewDefault true if the view content is to be used as new node settings
//     * @param rec callback object for user interaction (do you really want to reset...)
//     * @since 2.10
//     */
//    void reExecuteNode(NodeID id, ViewContent vc, boolean useAsNewDefault, ReexecutionCallback rec);

    /** Called by views of {@link InteractiveNode interactive nodes}. It will take the settings of the NodeModel
     * and save them in the {@link SingleNodeContainerSettings} so that they become the default for the next execution.
     * @param id The node in question.
     * @since 2.8
     */
    void saveNodeSettingsToDefault(NodeID id);

//    /** Creates lazy and returns an instance that controls the wizard execution of this workflow. These controller
//     * are not meant to be used by multiple clients (only one steps back/forth in the workflow), though this is not
//     * asserted by the returned controller object.
//     * @return A controller for the wizard execution (a new or a previously created and modified instance).
//     * @throws IllegalStateException If this workflow is not a project.
//     * @since 2.10
//     */
//    WizardExecutionController getWizardExecutionController();

//    /** Execute workflow until nodes of the given class - those will
//     * usually be QuickForm or view nodes requiring user interaction.
//     *
//     * @param <T> ...
//     * @param nodeModelClass the interface of the "stepping" nodes
//     * @param filter ...
//     * @since 2.7
//     */
//    <T> void stepExecutionUpToNodeType(Class<T> nodeModelClass, NodeModelFilter<T> filter);

    /** Attempts to execute all nodes upstream of the argument node. The method
     * waits (until either all predecessors are executed or there is no further
     * chance to execute anything).
     *
     * @param id The node whose upstream nodes need to be executed.
     * @throws InterruptedException If thread is canceled during waiting
     * (has no affect on the workflow execution).
     * @since 2.6*/
    void executePredecessorsAndWait(NodeID id) throws InterruptedException;

    /** Check if we can expand the selected metanode into a set of nodes in
     * this WFM.
     * This essentially checks if the nodes can be moved (=deleted from
     * the original WFM) or if they are executed
     *
     * @param subNodeID the id of the metanode to be expanded
     * @return null of ok otherwise reason (String) why not
     * @since 2.10
     */
    String canExpandSubNode(NodeID subNodeID);

    /** Check if we can expand the selected metanode into a set of nodes in
     * this WFM.
     * This essentially checks if the nodes can be moved (=deleted from
     * the original WFM) or if they are executed
     *
     * @param wfmID the id of the metanode to be expanded
     * @return null of ok otherwise reason (String) why not
     */
    String canExpandMetaNode(NodeID wfmID);

    /** Expand the selected metanode into a set of nodes in
     * this WFM and remove the old metanode.
     *
     * @param wfmID the id of the metanode to be expanded
     * @return copied content containing nodes and annotations
     * @throws IllegalArgumentException if expand cannot be done
     */
    WorkflowCopyContent expandMetaNode(NodeID wfmID) throws IllegalArgumentException;

//    /** Expand the selected subnode into a set of nodes in this WFM and remove the old metanode.
//     *
//     * @param nodeID ID of the node containing the sub workflow
//     * @return copied content containing nodes and annotations
//     * @throws IllegalStateException if expand cannot be done
//     * @since 2.12
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    ExpandSubnodeResult expandSubWorkflow(NodeID nodeID) throws IllegalStateException;

//    /** Convert the selected metanode into a subnode.
//     *
//     * @param wfmID the id of the metanode to be converted.
//     * @return ID to the created sub node.
//     * @since 2.10
//     */
//    MetaNodeToSubNodeResult convertMetaNodeToSubNode(NodeID wfmID);

//    /** Unwrap a selected subnode into a metanode.
//     * @param subnodeID Subnode to unwrap.
//     * @return The result object for undo.
//     * @throws IllegalStateException If it cannot perform the operation (e.g. node executing)
//     * @since 3.1
//     */
//    SubNodeToMetaNodeResult convertSubNodeToMetaNode(NodeID subnodeID);

    /** Check if we can collapse selected set of nodes into a metanode.
     * This essentially checks if the nodes can be moved (=deleted from
     * the original WFM), if they are executed, or if moving them would
     * result in cycles in the original WFM (outgoing connections fed
     * back into inports of the new Metanode).
     *
     * @param orgIDs the ids of the nodes to be moved to the new metanode.
     * @return null or reason why this cannot be done as string.
     */
    String canCollapseNodesIntoMetaNode(NodeID[] orgIDs);

//    /**
//     * Collapse selected set of nodes into a metanode. Make sure connections from and to nodes not contained in this set
//     * are passed through appropriate ports of the new metanode.
//     *
//     * @param orgIDs the ids of the nodes to be moved to the new metanode.
//     * @param orgAnnos the workflow annotations to be moved
//     * @param name of the new metanode
//     * @return newly create metanode
//     * @throws IllegalArgumentException if collapse cannot be done
//     */
//    CollapseIntoMetaNodeResult collapseIntoMetaNode(NodeID[] orgIDs, WorkflowAnnotationID[] orgAnnos, String name);

    /**
     * Check if a node can be reset, meaning that it is executed and all of
     * its successors are idle or executed as well. We do not want to mess
     * with executing chains.
     *
     * @param nodeID the id of the node
     * @return true if the node can safely be reset.
     */
    boolean canResetNode(NodeID nodeID);

    /** {@inheritDoc} */
    boolean canResetContainedNodes();

    /** Reset node and all executed successors of a specific node and
     * launch configure storm.
     *
     * @param id of first node in chain to be reset.
     */
    void resetAndConfigureNode(NodeID id);

    /** {@inheritDoc}
     * @since 2.11*/
    boolean canConfigureNodes();

    /** Check if a node can be executed directly.
     *
     * @param nodeID id of node
     * @return true if node is configured and all immediate predecessors are executed.
     * @since 2.9
     */
    boolean canExecuteNodeDirectly(NodeID nodeID);

    /** Check if a node can be executed either directly or via chain of nodes that
         * include an executable node.
        *
        * @param nodeID id of node
        * @return true if node can be executed.
        */
    boolean canExecuteNode(NodeID nodeID);

    /** Check if a node can be cancelled individually.
        *
        * @param nodeID id of node
        * @return true if node can be cancelled
        *
        */
    boolean canCancelNode(NodeID nodeID);

    /** @return true if all nodes in this workflow / metanode can be canceled.
        * @since 3.1 */
    boolean canCancelAll();

    /**
     * Cancel execution of the given NodeContainer.
     *
     * @param nc node to be canceled
     */
    void cancelExecution(INodeContainer nc);

    /**
     * Pause loop execution of the given NodeContainer (=loop end).
     *
     * @param nc node to be canceled
     */
    void pauseLoopExecution(INodeContainer nc);

    /** Resume operation of a paused loop. Depending on the flag we
     * either step (= run only one iteration and pause again) or run
     * until the loop is finished.
     *
     * @param nc The node container
     * @param oneStep If execution should only be resumed by one step
     */
    void resumeLoopExecution(INodeContainer nc, boolean oneStep);

    /** Is the node with the given ID ready to take a new job manager. This
     * is generally true if the node is currently not executing.
     * @param nodeID The node in question.
     * @return Whether it's save to invoke the
     * {@link #setJobManager(NodeID, NodeExecutionJobManager)} method.
     */
    boolean canSetJobManager(NodeID nodeID);

    /** Sets a new job manager on the node with the given ID.
     * @param nodeID The node in question.
     * @param jobMgr uid The new job manager (may be null to use parent's one).
     * @throws IllegalStateException If the node is not ready
     * @throws IllegalArgumentException If the node is unknown
     * @see #canSetJobManager(NodeID)
     */
    void setJobManager(NodeID nodeID, JobManagerUID jobMgr);

    /** Attempts to cancel or running nodes in preparation for a removal of
     * this node (or its parent) from the root. Executing nodes, which can be
     * disconnected from the execution (e.g. remote cluster execution) are
     * disconnected if their status has been saved before.
     */
    void shutdown();

    /**
    * Convenience method: execute all and wait for execution to be done. This method silently swallows
    * {@link InterruptedException} and returns the state at the time of interrupt -- the interrupt state of the calling
    * thread is reset then so use with caution.
    *
    * @return true if execution was successful
    * @see #executeAllAndWaitUntilDoneInterruptibly()
    */
    boolean executeAllAndWaitUntilDone();

    /**
     * Execute all nodes and wait until workflow reaches a stable state.
     * @return true if execution was successful
     * @throws InterruptedException If the calling thread is interrupted. This will not cancel the execution of
     * the workflow.
     * @since 3.2
     */
    boolean executeAllAndWaitUntilDoneInterruptibly() throws InterruptedException;

    /** Causes the current thread to wait until the the workflow has reached
     * a non-executing state unless a given timeout elapsed.
     * @param time the maximum time to wait
     *       (0 or negative for waiting infinitely)
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}. It returns
     *         {@code true} if the time argument is 0 or negative.
     * @throws InterruptedException if the current thread is interrupted
     */
    boolean waitWhileInExecution(long time, TimeUnit unit) throws InterruptedException;

    /** Called by execute-all action to (attempt to) execute all nodes in the workflow. This is true when there is
     * at least one node that is executable (even though the state of the wfm is idle).
     * @return that property
     * @since 2.10 */
    boolean canExecuteAll();

    /** (Try to) Execute all nodes in the workflow. This method only marks the end nodes for execution and then returns
     * immediately. If a job manager is set on the WFM this one will run the execution. In any case this method
     * returns immediately and does not wait for the execution to finish.
     * @see #executeAllAndWaitUntilDone() */
    void executeAll();

    /** {@inheritDoc} */
    @Override
    boolean hasDialog();

    /** {@inheritDoc} */
    @Override
    boolean areDialogAndNodeSettingsEqual();

//    /**
//     * @return global table repository for this WFM.
//     * @since 3.1
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    HashMap<Integer, ContainerTable> getGlobalTableRepository();

//    /** @return the fileStoreHandlerRepository for this metanode or project.
//     * @since 3.1
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    WorkflowFileStoreHandlerRepository getFileStoreHandlerRepository();

//    /** Merges the incoming flow object stacks and sets it into the argument node. Called prior configuration and
//     * externally via the streaming executor.
//     *
//     * <p>
//     * This method is private API but has public scope to enable the streaming
//     * executor to propagate flow variable control into nodes prior (streaming) execution.
//     * @param snc The node to inject into - must exist in workflow
//     * @param sos The upstream stacks.
//     * @return The merged stack as set into the node.
//     * @throws IllegalFlowObjectStackException e.g. conflicting loops.
//     * @since 2.12
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    FlowObjectStack createAndSetFlowObjectStackFor(SingleNodeContainer snc, FlowObjectStack[] sos)
//        throws IllegalFlowObjectStackException;

//    /**
//     * @param id
//     * @return current set of PortObjectSpecs of the given node
//     * @since 2.12
//     */
//    PortObjectSpec[] getNodeInputSpecs(NodeID id);

    /** Produce summary of node.
     *
     * @param prefix if containing node/workflow
     * @param indent number of leading spaces
     * @return string
     */
    String printNodeSummary(NodeID prefix, int indent);

    /** {@inheritDoc} */
    @Override
    String toString();

//    /** {@inheritDoc} */
//    Collection<INodeContainer> getNodeContainers();

    /**
     * @return collection of ConnectionContainer in this WFM
     */
    Collection<IConnectionContainer> getConnectionContainers();

    /**
     * @param id node ID
     * @return NodeContainer for given ID
     */
    INodeContainer getNodeContainer(NodeID id);

    /** Get contained node container and cast to argument class. Throws exception if it not exists or not implementing
     * requested class unless the flag is false.
     * @param <T> The interface or subclass the {@link NodeContainer} is expected to implement.
     * @param id The node to retrieve.
     * @param subclass the expected sub class, usually sub-classes of {@link NodeContainer} but could also be
     *        implementing interface.
     * @param failOnError Fails if node is not found or not of expected type. Otherwise it just prints a DEBUG message.
     * @return The node..
     * @throws IllegalArgumentException If node is not found or of the expected type and the flag is true.
     * @since 2.10
     * @noreference This method is not intended to be referenced by clients (only used in core and testing plugin). */
    <T> T getNodeContainer(NodeID id, Class<T> subclass, boolean failOnError);

    /** Does the workflow contain a node with the argument id?
     * @param id The id in question.
     * @return true if there is node with the given id, false otherwise.
     */
    boolean containsNodeContainer(NodeID id);

    /** {@inheritDoc} */
    boolean containsExecutedNode();

    /**
     * @return list of errors messages (list empty if none exist).
     * @deprecated Use {@link #getNodeMessages(Type...)} instead.
     */
    @Deprecated
    List<NodeMessage> getNodeErrorMessages();

    /** Get all node messages, recursively collected from all contained.
     * @param types A list of messge types (e.g. all errors and warnings). Argument must not be empty,
     *        null or contain null values
     * @return list of errors messages (list empty if none exist).
     * @throws IllegalArgumentException If argument is invalid.
     * @since 2.11
     */
    List<Pair<String, NodeMessage>> getNodeMessages(NodeMessage.Type... types);

//    /** Return list of nodes that are part of the same scope as the given one.
//     * List will contain anchor node alone if there is no scope around it.
//     *
//     * @param anchor node
//     * @return list of nodes.
//     * @since 2.8
//     */
//    List<INodeContainer> getNodesInScope(SingleNodeContainer anchor);

//    /**
//     * @param anchor The anchor
//     * @return List of node containers
//     * @since 2.8
//     */
//    List<INodeContainer> getNodesInScopeOLD(SingleNodeContainer anchor);

    /**
     * {@inheritDoc}
     */
    boolean isWriteProtected();

//    /** @return the templateInformation */
//    MetaNodeTemplateInformation getTemplateInformation();

//    /** {@inheritDoc} */
//    void setTemplateInformation(MetaNodeTemplateInformation tI);

    /** The list of contained metanodes that are linked metanodes. If recurse flag is set, each metanode is checked
     * recursively.
     * @param recurse ...
     * @return list of node ids, ids not necessarily direct childs of this WFM!
     * @since 2.6
     */
    List<NodeID> getLinkedMetaNodes(boolean recurse);

//    /** {@inheritDoc}
//     * @noreference This method is not intended to be referenced by clients. */
//    Map<NodeID, NodeContainerTemplate> fillLinkedTemplateNodesList(Map<NodeID, NodeContainerTemplate> mapToFill,
//        boolean recurse, boolean stopRecursionAtLinkedMetaNodes);

//    /**
//     * Query the template to the linked metanode with the given ID and check whether a newer version is available.
//     *
//     * @param id The ID of the linked metanode
//     * @param loadHelper The load helper to load the template
//     * @return true if a newer revision is available, false if not or this is not a metanode link.
//     * @throws IOException If that fails (template not accessible)
//     */
//    boolean checkUpdateMetaNodeLink(NodeID id, WorkflowLoadHelper loadHelper) throws IOException;

    /** Returns true if the argument node is a valid metanode link and is not
     * executing and has no successor in execution. Used from the GUI to enable
     * or disable the update action. It does not test whether there is a newer
     * version of the metanode available. It may also return true even if the
     * metanode is executed or contains executed nodes.
     * @param id The metanode in question.
     * @return The above described property. */
    boolean canUpdateMetaNodeLink(NodeID id);

    /** Returns true when the metanode for the given ID is a link or contains links, which have the update status set
     * (doesn't actually check on remote side but uses cached information). It assumes
     * {@link #checkUpdateMetaNodeLink(NodeID, WorkflowLoadHelper)} has been called before.
     *
     * <p>This method is used by an UI action and is not meant as public API.
     * @param id The id to the metanode.
     * @return if the ID is unknown or there are no metanodes with the appropriate update flag.
     * @since 2.9
     */
    boolean hasUpdateableMetaNodeLink(NodeID id);

//    /**
//     * Update link metanodes with the given ID.
//     *
//     * @param id The ids of the metanode (must be existing metanode and must be a link).
//     * @param exec The execution monitor used to load a copy of the template
//     * @param loadHelper A load helper.
//     * @return The load result of the newly inserted link copy.
//     * @throws CanceledExecutionException If canceled
//     * @throws IllegalArgumentException If the node does not exist or is not a metanode.
//     */
//    NodeContainerTemplateLinkUpdateResult updateMetaNodeLink(NodeID id, ExecutionMonitor exec,
//        WorkflowLoadHelper loadHelper) throws CanceledExecutionException;

//    /** {@inheritDoc}
//     * @noreference This method is not intended to be referenced by clients. */
//    void updateMetaNodeLinkInternalRecursively(ExecutionMonitor exec, WorkflowLoadHelper loadHelper,
//        Map<URI, NodeContainerTemplate> visitedTemplateMap, NodeContainerTemplateLinkUpdateResult loadRes)
//        throws Exception;

//    /**
//     * Update metanode links (recursively finding all metanodes but not updating metanodes in metanodes).
//     * @param lH Load helper.
//     * @param failOnLoadError If to fail if there errors updating the links
//     * @param exec Progress monitor
//     * @return The update summary
//     * @throws CanceledExecutionException If canceled
//     * @throws IOException Special errors during update (not accessible)
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    NodeContainerTemplateLinkUpdateResult updateMetaNodeLinks(WorkflowLoadHelper lH, boolean failOnLoadError,
//        ExecutionMonitor exec) throws IOException, CanceledExecutionException;

//    /** {@inheritDoc} */
//    MetaNodeTemplateInformation saveAsTemplate(File directory, ExecutionMonitor exec)
//        throws IOException, CanceledExecutionException, LockFailedException;

//    /** Sets the argument template info on the node with the given ID. The node
//     * must be a valid metanode contained in this workflow.
//     * @param id The id of the node to change (must be an existing metanode).
//     * @param templateInformation the templateInformation to set
//     * @return The old template info associated with the node.
//     * @throws NullPointerException If either argument is null.
//     * @throws IllegalArgumentException If the id does not represent a
//     * valid metanode. */
//    MetaNodeTemplateInformation setTemplateInformation(NodeID id, MetaNodeTemplateInformation templateInformation);

    /** Set password on this metanode. See {@link WorkflowCipher} for details
     * on what is protected/locked.
     * @param password The new password (or null to always unlock)
     * @param hint The hint/copyright.
     * @throws NoSuchAlgorithmException If encryption fails. */
    void setWorkflowPassword(String password, String hint) throws NoSuchAlgorithmException;

//    /** {@inheritDoc}
//     * @noreference This method is not intended to be referenced by clients.
//     * @since 2.10 */
//    WorkflowCipher getWorkflowCipher();

    /** @return see {@link WorkflowCipher#isUnlocked()}. */
    boolean isUnlocked();

    /** @return see {@link WorkflowCipher#getPasswordHint()}. */
    String getPasswordHint();

//    /** @param prompt The prompt
//     * @return see {@link WorkflowCipher#unlock(WorkflowCipherPrompt)}. */
//    boolean unlock(WorkflowCipherPrompt prompt);

    /** @return see {@link WorkflowCipher#isEncrypted()}. */
    boolean isEncrypted();

    /** {@inheritDoc} */
    OutputStream cipherOutput(OutputStream out) throws IOException;

    /** {@inheritDoc} */
    String getCipherFileName(String fileName);

    /**
     * Add listener to list.
     *
     * @param listener new listener
     */
    void addListener(WorkflowListener listener);

    /**
     * Remove listener.
     * @param listener listener to be removed
     */
    void removeListener(WorkflowListener listener);

//    /** Copies the nodes with the given ids from the argument workflow manager
//     * into this wfm instance. All nodes wil be reset (and configured id
//     * possible). Connections among the nodes are kept.
//     * @param sourceManager The wfm to copy from
//     * @param content The content to copy (must exist in sourceManager)
//     * @return Inserted NodeIDs and annotations.
//     */
//    WorkflowCopyContent copyFromAndPasteHere(IWorkflowManager sourceManager, WorkflowCopyContent content);

//    /** Copy the given content.
//     * @param content The content to copy (must exist).
//     * @return A workflow persistor hosting the node templates, ready to be
//     * used in the {@link #paste(WorkflowPersistor)} method.
//     */
//    WorkflowPersistor copy(WorkflowCopyContent content);

//    /** Copy the nodes with the given ids.
//     * @param isUndoableDeleteCommand <code>true</code> if the returned persistor is used
//     * in the delete command (which supports undo). This has two effects:
//     * <ol>
//     *   <li>It keeps the locations of the node's directories (e.g.
//     *   &lt;workflow&gt;/File Reader (#xy)/). This is true if the copy serves
//     *   as backup of an undoable delete command (undoable = undo enabled).
//     *   If it is undone, the directories must not be cleared before the
//     *   next save (in order to keep the drop folder)
//     *   </li>
//     *   <li>The returned persistor will insert a reference to the contained
//     *   workflow annotations instead of copying them (enables undo on previous
//     *   move or edit commands.
//     *   </li>
//     * </ol>
//     * @param content The content to copy (must exist).
//     * @return A workflow persistor hosting the node templates, ready to be
//     * used in the {@link #paste(WorkflowPersistor)} method.
//     */
//    WorkflowPersistor copy(boolean isUndoableDeleteCommand, WorkflowCopyContent content);

//    /** Pastes the contents of the argument persistor into this wfm.
//     * @param persistor The persistor created with
//     * {@link #copy(WorkflowCopyContent)} method.
//     * @return The new node ids of the inserted nodes and the annotations in a
//     *         dedicated object.
//     */
//    WorkflowCopyContent paste(WorkflowPersistor persistor);

//    /** Get working folder associated with this WFM. May be null if
//     * not saved yet.
//     * @return working directory.
//     */
//    ReferencedFile getWorkingDir();

//    /** @return the authorInformation or null if not saved yet.
//     * @noreference This method is not intended to be referenced by clients.
//     * @since 2.8
//     */
//    AuthorInformation getAuthorInformation();

//    /** {@inheritDoc} */
//    WorkflowExecutionResult createExecutionResult(ExecutionMonitor exec) throws CanceledExecutionException;

//    /** {@inheritDoc} */
//    void loadExecutionResult(NodeContainerExecutionResult result, ExecutionMonitor exec, LoadResult loadResult);

//    /**
//     * Loads the workflow contained in the directory as node into this workflow
//     * instance. Loading a whole new project is usually done using
//     * {@link WorkflowManager#loadProject(File, ExecutionMonitor, WorkflowLoadHelper)}
//     * .
//     *
//     * @param directory to load from
//     * @param exec For progress/cancellation (currently not supported)
//     * @param loadHelper callback to load credentials and such (if available)
//     *            during load of the underlying <code>SingleNodeContainer</code>
//     *            (may be null).
//     * @param keepNodeMessages Whether to keep the messages that are associated
//     *            with the nodes in the loaded workflow (mostly false but true
//     *            when remotely computed results are loaded).
//     * @return A workflow load result, which also contains the loaded workflow.
//     * @throws IOException If errors reading the "important" files fails due to
//     *             I/O problems (file not present, e.g.)
//     * @throws InvalidSettingsException If parsing the "important" files fails.
//     * @throws CanceledExecutionException If canceled.
//     * @throws UnsupportedWorkflowVersionException If the version of the
//     *             workflow is unknown (future version)
//     * @throws LockFailedException if the flow can't be locked for opening
//     */
//    WorkflowLoadResult load(File directory, ExecutionMonitor exec, WorkflowLoadHelper loadHelper,
//        boolean keepNodeMessages) throws IOException, InvalidSettingsException, CanceledExecutionException,
//        UnsupportedWorkflowVersionException, LockFailedException;

//    /**
//     * Loads the content of the argument persistor into this node.
//     *
//     * @param persistor The persistor containing the node(s) to be loaded as children to this node.
//     * @param exec For progress/cancellation (currently not supported)
//     * @param keepNodeMessages Whether to keep the messages that are associated with the nodes in the loaded workflow
//     *            (mostly false but true when remotely computed results are loaded).
//     * @return A workflow load result, which also contains the loaded node(s).
//     * @throws IOException If errors reading the "important" files fails due to I/O problems (file not present, e.g.)
//     * @throws InvalidSettingsException If parsing the "important" files fails.
//     * @throws CanceledExecutionException If canceled.
//     * @throws UnsupportedWorkflowVersionException If the version of the workflow is unknown (future version)
//     */
//    WorkflowLoadResult load(FileWorkflowPersistor persistor, ExecutionMonitor exec, boolean keepNodeMessages)
//        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException;

//    /** Implementation of {@link #load(FileWorkflowPersistor, ExecutionMonitor, boolean)}.
//     * @noreference This method is not intended to be referenced by clients. */
//    void load(TemplateNodeContainerPersistor persistor, MetaNodeLinkUpdateResult result, ExecutionMonitor exec,
//        boolean keepNodeMessages)
//        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException;

//    /**
//     * Saves the workflow to a new location, setting the argument directory as the new NC dir. It will first copy the
//     * "old" directory, point the NC dir to the new location and then do an incremental save.
//     *
//     * @param directory new directory, not null
//     * @param exec The execution monitor
//     * @throws IOException If an IO error occured
//     * @throws CanceledExecutionException If the execution was canceled
//     * @throws LockFailedException If locking failed
//     * @since 2.9
//     */
//    void saveAs(File directory, ExecutionMonitor exec)
//        throws IOException, CanceledExecutionException, LockFailedException;

//    /**
//     * @param directory The directory to save in
//     * @param exec The execution monitor
//     * @param isSaveData ...
//     * @throws IOException If an IO error occured
//     * @throws CanceledExecutionException If the execution was canceled
//     * @throws LockFailedException If locking failed
//     */
//    void save(File directory, ExecutionMonitor exec, boolean isSaveData)
//        throws IOException, CanceledExecutionException, LockFailedException;

//    /**
//     * @param directory The directory to save in
//     * @param exec The execution monitor
//     * @param saveHelper ...
//     * @throws IOException If an IO error occured
//     * @throws CanceledExecutionException If the execution was canceled
//     * @throws LockFailedException If locking failed
//     * @since 2.10
//     */
//    void save(File directory, WorkflowSaveHelper saveHelper, ExecutionMonitor exec)
//        throws IOException, CanceledExecutionException, LockFailedException;

    /** Marks the workflow and all nodes contained as dirty in the auto-save location.
     * @noreference This method is not intended to be referenced by clients.
     * @since 2.10 */
    void setAutoSaveDirectoryDirtyRecursivly();

    /** {@inheritDoc} */
    @Override
    void setDirty();

    /** {@inheritDoc} */
    @Override
    int getNrInPorts();

    /** {@inheritDoc} */
    @Override
    IWorkflowInPort getInPort(int index);

    /** {@inheritDoc} */
    @Override
    IWorkflowOutPort getOutPort(int index);

    /** {@inheritDoc} */
    @Override
    int getNrOutPorts();

    /** Set new name of this workflow or null to reset the name (will then
     * return the workflow directory in {@link #getName()} or null if this flow
     * has not been saved yet).
     * @param name The new name or null
     */
    void setName(String name);

    /** Renames the underlying workflow directory to the new name.
     * @param newName The name of the directory.
     * @return Whether that was successful.
     * @throws IllegalStateException If the workflow has not been saved yet
     * (has no corresponding node directory).
     */
    boolean renameWorkflowDirectory(String newName);

//    /** Get reference to credentials store used to persist name/passwords.
//     * @return password store associated with this workflow/meta-node.
//     */
//    CredentialsStore getCredentialsStore();

//    /** Update user/password fields in the credentials store assigned to the
//     * workflow and update the node configuration.
//     * @param credentialsList the list of credentials to be updated. It will
//     *  find matching credentials in this workflow and update their fields.
//     * @throws IllegalArgumentException If any of the credentials is unknown
//     */
//    void updateCredentials(Credentials... credentialsList);

    /** Get the name of the workflow. If none has been set, a name is derived
     * from the workflow directory name. If no directory has been set, a static
     * string is returned. This method never returns null.
     * {@inheritDoc} */
    @Override
    String getName();

    /** @return the name set in the constructor or via {@link #setName(String)}.
     * In comparison to {@link #getName()} this method does not use the workflow
     * directory name if no other name is set.
     */
    String getNameField();

    /** {@inheritDoc} */
    @Override
    int getNrNodeViews();

//    /** {@inheritDoc} */
//    NodeView<NodeModel> getNodeView(int i);

    /** {@inheritDoc} */
    @Override
    String getNodeViewName(int i);

    /** {@inheritDoc} */
    @Override
    boolean hasInteractiveView();

    /** {@inheritDoc} */
    @Override
    boolean hasInteractiveWebView();

    /** {@inheritDoc} */
    @Override
    String getInteractiveViewName();

//    /** {@inheritDoc} */
//    <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V
//        getInteractiveView();

    /**
     * Stores the editor specific settings. Stores a reference to the object. Does not create a copy.
     * @param editorInfo the settings to store
     * @since 2.6
     */
    void setEditorUIInformation(EditorUIInformation editorInfo);

    /**
     * Returns the editor specific settings. Returns a reference to the object. Does not create a copy.
     * @return the editor settings currently stored
     * @since 2.6
     */
    EditorUIInformation getEditorUIInformation();

//    /** The version as read from workflow.knime file during load (or <code>null</code> if not loaded but newly created).
//     * @return the workflow {@link LoadVersion}
//     * @since 3.3 */
//    LoadVersion getLoadVersion();

//    /** {@inheritDoc} */
//    NodeType getType();

    /** {@inheritDoc} */
    @Override
    URL getIcon();

    /**
     * @return The number of incoming ports
     */
    int getNrWorkflowIncomingPorts();

    /**
     * @return The number of outgoing ports
     */
    int getNrWorkflowOutgoingPorts();

//    /**
//     * @param i Index of the port
//     * @return The incoming port at the given index
//     */
//    NodeOutPort getWorkflowIncomingPort(int i);

//    /**
//     * @param i Index of the port
//     * @return The outgoing port at the given index
//     */
//    NodeInPort getWorkflowOutgoingPort(int i);

    /** Set UI information for workflow's input ports
     * (typically aligned as a bar).
     * @param inPortsBarUIInfo The new UI info.
     */
    void setInPortsBarUIInfo(NodeUIInformation inPortsBarUIInfo);

    /** Set UI information for workflow's output ports
     * (typically aligned as a bar).
     * @param outPortsBarUIInfo The new UI info.
     */
    void setOutPortsBarUIInfo(NodeUIInformation outPortsBarUIInfo);

    /** Get UI information for workflow input ports.
     * @return the ui info or null if not set.
     * @see #setInPortsBarUIInfo(UIInformation)
     */
    NodeUIInformation getInPortsBarUIInfo();

    /** Get UI information for workflow output ports.
     * @return the ui info or null if not set.
     * @see #setOutPortsBarUIInfo(UIInformation)
     */
    NodeUIInformation getOutPortsBarUIInfo();

    /* Private routine which assembles a stack of workflow variables all
     * the way to the top of the workflow hierarchy.
     */
//    /**
//     * {@inheritDoc}
//     */
//    void pushWorkflowVariablesOnStack(FlowObjectStack sos);

//    /** Get read-only access on the current workflow variables.
//     * @return the current workflow variables, never null.
//     */
//    List<FlowVariable> getWorkflowVariables();

//    /** {@inheritDoc} */
//    FlowObjectStack getFlowObjectStack();

//    /** Set new workflow variables. All nodes within
//     * this workflow will have access to these variables.
//     * The method may change in future versions or removed entirely (bug 1937).
//     *
//     * @param newVars new variables to be set
//     * @param skipReset if false the workflow will be re-configured
//     */
//    void addWorkflowVariables(boolean skipReset, FlowVariable... newVars);

    /**
     * @return read-only collection of all currently registered annotations. The returned collection is sorted according
     *         to the order of the associated {@link WorkflowAnnotationID}s of each {@link IWorkflowAnnotation}.
     */
    Collection<IWorkflowAnnotation> getWorkflowAnnotations();

    /**
     * @return read-only collection of ids of all currently registered annotations.
     */
    Collection<WorkflowAnnotationID> getWorkflowAnnotationIDs();

    /**
     * @param wfaID the id of the request workflow annotation
     * @return the workflow annotation associated with the given key or <code>null</code> it doesn't exist
     */
    IWorkflowAnnotation getWorkflowAnnotation(WorkflowAnnotationID wfaID);

    /** Add new workflow annotation, sets the workflow annotation id, fire events.
     * @param annotation to add
     * @throws IllegalArgumentException If annotation already registered. */
    /** Add new workflow annotation, fire events.
     * @param annotation to add
     * @throws IllegalArgumentException If annotation already registered. */
    void addWorkflowAnnotation(IWorkflowAnnotation annotation);

    /** Remove workflow annotation with the id associated with the given workflow annotation and fire events.
     * @param annotation to remove (or more precise: the annotation with the id associated with the given annotation will be removed)
     * @throws IllegalArgumentException If annotation is not registered. */
    void removeAnnotation(IWorkflowAnnotation annotation);

    /**
     * Same as {@link #removeAnnotation(IWorkflowAnnotation)} but triggered by the annotation id directly.
     *
     * @param wfaID the id of the workflow annotation to be removed
     */
    void removeAnnotation(WorkflowAnnotationID wfaID);

    /**
     * Resorts the internal ids to move the specified annotation to the last index.
     * It also updates its associated ids (of both, the passed annotation and the with the workflow manager stored annotation, if different).
     * @param annotation to bring to front
     * @since 2.6
     */
    void bringAnnotationToFront(IWorkflowAnnotation annotation);

    /**
     * Resorts the internal ids to move the specified annotation to the first index.
     * It also updates it's associated id (of both, the passed annotation and the with the workflow manager stored annotation, if different).
     * @param annotation to bring to front
     * @since 2.6
     */
    void sendAnnotationToBack(IWorkflowAnnotation annotation);

    /** Listener to annotations, etc; sets content dirty.
     * @param evt Change event. */
    void nodeUIInformationChanged(NodeUIInformationEvent evt);

//    /**
//     * @return a list of all node annotations in the contained flow.
//     */
//    List<NodeAnnotation> getNodeAnnotations();

    /**
     * Retrieves the node with the given ID, fetches the underlying
     * {@link NodeModel} and casts it to the argument class.
     * @param id The node of interest
     * @param cl The class object the underlying NodeModel needs to implement
     * @param <T> The type the class
     * @return The casted node model.
     * @throws IllegalArgumentException If the node does not exist, is not
     *         a {@link NativeNodeContainer} or the model does not implement the
     *         requested type.
     */
    <T> T castNodeModel(NodeID id, Class<T> cl);

    /** Find all nodes in this workflow, whose underlying {@link NodeModel} is
     * of the requested type. Intended purpose is to allow certain extensions
     * (reporting, web service, ...) access to specialized nodes.
     * @param <T> Specific NodeModel derivation or another interface
     *            implemented by NodeModel instances.
     * @param nodeModelClass The class of interest
     * @param recurse Whether to recurse into contained metanodes.
     * @return A (unsorted) list of nodes matching the class criterion
     */
    <T> Map<NodeID, T> findNodes(Class<T> nodeModelClass, boolean recurse);

//    /** Calls {@link #findNodes(Class, NodeModelFilter, boolean, boolean)} with last argument <code>false</code>
//     * (no recursion into wrapped metanodes).
//     * @param <T> see delegated method
//     * @param nodeModelClass see delegated method
//     * @param filter see delegated method
//     * @param recurseIntoMetaNodes see delegated method
//     * @return see delegated method
//     * @since 2.7
//     */
//    <T> Map<NodeID, T> findNodes(Class<T> nodeModelClass, NodeModelFilter<T> filter, boolean recurseIntoMetaNodes);

//    /**
//     * Find all nodes in this workflow, whose underlying {@link NodeModel} is of the requested type. Intended purpose is
//     * to allow certain extensions (reporting, web service, ...) access to specialized nodes.
//     *
//     * @param <T> Specific NodeModel derivation or another interface implemented by NodeModel instances.
//     * @param nodeModelClass The class of interest
//     * @param filter A non-null filter to apply.
//     * @param recurseIntoMetaNodes Whether to recurse into contained metanodes.
//     * @param recurseIntoSubnodes Whether to recurse into contained wrapped metanodes.
//     * @return A (unsorted) list of nodes matching the class criterion
//     * @since 3.2
//     */
//    <T> Map<NodeID, T> findNodes(Class<T> nodeModelClass, NodeModelFilter<T> filter, boolean recurseIntoMetaNodes,
//        boolean recurseIntoSubnodes);

    /** Get the node container associated with the argument id. Recurses into
     * contained metanodes to find the node if it's not directly contained
     * in this workflow level.
     *
     * <p>Clients should generally use {@link #getNodeContainer(NodeID)} to
     * access directly contained nodes.
     *
     * @param id the id of the node in question
     * @return The node container to the node id (never null)
     * @throws IllegalArgumentException If the node is not contained in
     * this workflow.
     * @since 2.6 */
    INodeContainer findNodeContainer(NodeID id);

//    /**
//     * Find all nodes of a certain type that are currently ready to be executed (= node is configured, all predecessors
//     * are executed). See {@link #findNodes(Class, boolean)}
//     *
//     * @param <T> ...
//     * @param nodeModelClass ...
//     * @param filter non null refinement filter
//     * @return ...
//     * @since 2.7
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    <T> Map<NodeID, T> findWaitingNodes(Class<T> nodeModelClass, NodeModelFilter<T> filter);

//    /** Find all nodes of a certain type that are already executed.
//     * See {@link #findNodes(Class, boolean)}
//     *
//     * @param <T> ...
//     * @param nodeModelClass ...
//     * @param filter non null refinement filter
//     * @return ...
//     * @since 2.7
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    <T> Map<NodeID, T> findExecutedNodes(Class<T> nodeModelClass, NodeModelFilter<T> filter);

//    /** Find "next" workflowmanager which contains nodes of a certain type
//     * that are currently ready to be executed.
//     * See {@link #findWaitingNodes(Class, NodeModelFilter)}
//     *
//     * @param <T> ...
//     * @param nodeModelClass ...
//     * @param filter A non-null filter.
//     * @return Workflowmanager with waiting nodes or null if none exists.
//     * @since 2.7
//     */
//    <T> WorkflowManager findNextWaitingWorkflowManager(Class<T> nodeModelClass, NodeModelFilter<T> filter);

    /** Get quickform nodes on the root level along with their currently set value. These are all
     * {@link org.knime.core.node.dialog.DialogNode} including special nodes like "JSON Input".
     *
     * <p>Method is used to allow clients to retrieve an example input.
     * @return A map from {@link DialogNode#getParameterName() node's parameter name} to its (JSON object value)
     * @since 2.12
     */
    Map<String, ExternalNodeData> getInputNodes();

    /**
     * Counterpart to {@link #getInputNodes()} - it sets new values into quickform nodes on the root level. All nodes as
     * per map argument will be reset as part of this call.
     *
     * @param input a map from {@link org.knime.core.node.dialog.DialogNode#getParameterName() node's parameter name} to
     *            its (JSON or string object value). Invalid entries cause an exception.
     * @throws InvalidSettingsException If parameter name is not valid or a not uniquely defined in the workflow.
     * @since 2.12
     */
    void setInputNodes(Map<String, ExternalNodeData> input) throws InvalidSettingsException;

    /**
     * Receive output from workflow by means of {@link org.knime.core.node.dialog.OutputNode}. If the workflow is not
     * fully executed, the map contains only the keys of the outputs. The values are all <code>null</code>
     * in this case.
     *
     * @return A map from node's parameter name to its node data
     * @since 2.12
     */
    Map<String, ExternalNodeData> getExternalOutputs();

    /** Remove workflow variable of given name.
     * The method may change in future versions or removed entirely (bug 1937).
     *
     * @param name of variable to be removed.
     */
    void removeWorkflowVariable(String name);

//    /**
//     * @param id of node
//     * @return set of NodeGraphAnnotations for this node (can be more than one for Metanodes).
//     * @since 2.8
//     */
//    Set<NodeGraphAnnotation> getNodeGraphAnnotation(NodeID id);

    /**
     * Returns the current workflow context or <code>null</code> if no context is available.
     *
     * @return a workflow context or <code>null</code>
     * @since 2.8
     */
    WorkflowContext getContext();

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    void notifyTemplateConnectionChangedListener();

}
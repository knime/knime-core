/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Sep 23, 2016 (hornm): created
 */
package org.knime.core.ui.node.workflow;

import java.io.InputStream;
import java.net.URL;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.rpc.AbstractRpcClient;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.NodeLock;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeContainerStateObservable;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.OutPortView;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.UI;

/**
 * UI-interface that mirrors the {@link NodeContainer}.
 *
 * @author Martin Horn, KNIME.com
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface NodeContainerUI extends NodeProgressListener, NodeContainerStateObservable, UI {

    /**
     * @return parent workflowmanager holding this node (or null if root).
     */
    WorkflowManagerUI getParent();

//    /** Returns the {@linkplain #getParent() parent workflow manager}. A {@link WorkflowManager} instance contained
//     * in a {@link SubNodeContainer} overrides it to return the subnode (which then is responsible for all the actions).
//     * @return the direct node container parent.
//     */
//    NodeContainerParent getDirectNCParent();

    /**
     * @return The job manager associated with this node or null if this
     * node will use the job manager of the parent (or the parent of ...)
     * @see #findJobManager()
     */
    NodeExecutionJobManager getJobManager();

    /** @return NodeExecutionJobManager responsible for this node and all its children. */
    NodeExecutionJobManager findJobManager();

    boolean addNodePropertyChangedListener(NodePropertyChangedListener l);

    boolean removeNodePropertyChangedListener(NodePropertyChangedListener l);

//    /** add a loop to the list of waiting loops.
//     *
//     * @param slc FlowLoopContext object of the loop.
//     */
//    void addWaitingLoop(FlowLoopContext slc);
//
//    /**
//     * @return a list of waiting loops (well: their FlowLoopContext objects)
//     */
//    List<FlowLoopContext> getWaitingLoops();

    /** clears the list of waiting loops.
     */
    void clearWaitingLoopList();

//    /** Remove element from list of waiting loops.
//     *
//     * @param so loop to be removed.
//     */
//    void removeWaitingLoopHeadNode(FlowObject so);
//

    /**
     *
     * @param listener listener to the node progress
     * @return true if the listener was not already registered before, false otherwise
     */
    boolean addProgressListener(NodeProgressListener listener);

    /**
     *
     * @param listener existing listener to the node progress
     * @return true if the listener was successfully removed, false if it was not registered
     */
    boolean removeNodeProgressListener(NodeProgressListener listener);

    /**
     *
     * @param listener listener to the node messages (warnings and errors)
     * @return true if the listener was not already registered, false otherwise
     */
    boolean addNodeMessageListener(NodeMessageListener listener);

    /**
     *
     * @param listener listener to the node messages
     * @return true if the listener was successfully removed, false if it was not registered
     */
    boolean removeNodeMessageListener(NodeMessageListener listener);

    /**
     * Get the message to be displayed to the user.
     *
     * @return the node message consisting of type and message, never null.
     */
    NodeMessage getNodeMessage();

    /**
     * @param newMessage the nodeMessage to set
     */
    void setNodeMessage(NodeMessage newMessage);

    void addUIInformationListener(NodeUIInformationListener l);

    void removeUIInformationListener(NodeUIInformationListener l);

    /**
     * Returns the UI information.
     *
     * @return a the node information
     */
    NodeUIInformation getUIInformation();

    /**
     * Used, e.g., when nodes are moved.
     *
     * @param uiInformation new user interface information of the node such as coordinates on workbench.
     */
    void setUIInformation(NodeUIInformation uiInformation);

    /**
     * Updates the node's UI information only in order to correct it's bounding box and position. In that case there is
     * no need, e.g., to send the info to the server.
     *
     * @param uiInfo
     */
    void setUIInformationForCorrection(NodeUIInformation uiInfo);

    /** {@inheritDoc} */
    @Override
    boolean addNodeStateChangeListener(NodeStateChangeListener listener);

    /** {@inheritDoc} */
    @Override
    boolean removeNodeStateChangeListener(NodeStateChangeListener listener);

    /** {@inheritDoc}
     * @since 2.8 */
    @Override
    NodeContainerState getNodeContainerState();
//
//    /**
//     * @return the status of this node
//     */
//    State getState();

    /** Whether the dialog is a {@link org.knime.core.node.DataAwareNodeDialogPane}. If so,
     * the predecessor nodes need to be executed before the dialog is opened.
     * @return that property
     * @since 2.6
     */
    boolean hasDataAwareDialogPane();

    /** Return a NodeDialogPane for a node which can be embedded into
     * a JFrame oder another GUI element.
     *
     * @return A dialog pane for the corresponding node.
     * @throws NotConfigurableException if node cannot be configured
     */
    NodeDialogPane getDialogPaneWithSettings() throws NotConfigurableException;

    /** Called for nodes having {@linkplain org.knime.core.node.DataAwareNodeDialogPane data aware dialogs} in order
     * to check whether to prompt for execution or not.
     * @return true if correctly connected and all inputs have data.
     * @since 2.8
     * @see WorkflowManager#isAllInputDataAvailableToNode(NodeID)
     * @noreference This method is not intended to be referenced by clients.
     */
    boolean isAllInputDataAvailable();

    /** Currently called by nodes having {@linkplain org.knime.core.node.DataAwareNodeDialogPane data aware dialogs}
     * in order to test whether upstream nodes are correctly wired and can be executed. It only tests if the direct
     * predecessors are connected -- in the longer term it will check if all predecessors are correctly set up and
     * at least one is executable.
     * @return true if all non-optional ports are connected.
     * @since 2.8
     * @noreference This method is not intended to be referenced by clients.
     */
    boolean canExecuteUpToHere();

    //TODO can probably removed entirely
//    /** Launch a node dialog in its own JFrame (a JDialog).
//     *
//     * @throws NotConfigurableException if node cannot be configured
//     */
//    void openDialogInJFrame() throws NotConfigurableException;

    /** Take settings from the node's dialog and apply them to the model. Throws
     * an exception if the apply fails.
     *
     * @throws InvalidSettingsException if settings are not applicable.
     */
    void applySettingsFromDialog() throws InvalidSettingsException;

    boolean areDialogSettingsValid();

    boolean hasDialog();

    boolean areDialogAndNodeSettingsEqual();

    int getNrInPorts();

    NodeInPortUI getInPort(int index);

    NodeOutPortUI getOutPort(int index);

    int getNrOutPorts();

    int getNrViews();

    /** @return user settings for this node, possibly empty but not <code>null</code>. */
    ConfigBaseRO getNodeSettings();

    /**
     * Returns the number of views provided by the node implementation.
     * @return the number of views provided by the node implementation
     */
    int getNrNodeViews();

    String getViewName(int i);

    String getNodeViewName(int i);

//    AbstractNodeView<NodeModel> getView(int i);

//    /**
//     * Return the view with the specified index provided by the node.
//     *
//     * @param i the view to create
//     * @return a new view instance with index i provided by the node
//     */
//    AbstractNodeView<NodeModel> getNodeView(int i);

    /**
     * @return true if node provides an interactive view.
     * @since 2.8
     */
    boolean hasInteractiveView();

    /**
     * Returns the name of the interactive view if such a view exists. Otherwise <code>null</code> is returned.
     *
     * @return name of the interactive view or <code>null</code>
     * @since 2.8
     */
    String getInteractiveViewName();

    /** Get the 'interactive web views' provided by this node. That is, views providing a {@link WebTemplate} for an interactive
     * web view. Native nodes can have at most one view, components may have many (one for
     * each contained view node), metanodes have none.
     *
     * <p>The name associated with the web view (e.g. JS scatter plot) comes from a node's description (xml).
     * @return An new {@link InteractiveWebViewsResultUI} with possibly 0 or more views.
     */
    @SuppressWarnings("rawtypes")
    InteractiveWebViewsResultUI getInteractiveWebViews();


//    /** The input stack associated with this node - for ordinary nodes this is the the merged stack of the input
//     * (ignoring any variables pushed by the node itself), for workflows this is the workflow variable "stack".
//     * @return The stack, usually not null when used in "normal operation" (possible TODO: unset the stack when node
//     * is reset).
//     * @since 3.1 */
//    FlowObjectStack getFlowObjectStack();

    URL getIcon();

    default InputStream getIconAsStream() {
        return null;
    }

    NodeType getType();

    NodeID getID();

    String getName();

    String getNameWithID();

    /** @return Node name with status information.  */
    @Override
    String toString();

    /**
     * @return the display label for {@link NodeView}, {@link OutPortView} and
     * {@link NodeDialog}
     */
    String getDisplayLabel();

    /**
     * For reporting backward compatibility. If no custom name is set the
     * reporting creates new names (depending on the node id). (The preference
     * page prefix is ignored then.)
     *
     * @return the first line of the annotation (which contains in old (pre 2.5)
     *         flows the custom name) or null, if no annotation or no old custom
     *         name is set.
     */
    String getCustomName();

    /**
     * @return the annotation associated with the node, never null.
     */
    NodeAnnotation getNodeAnnotation();

    String getCustomDescription();

    void setCustomDescription(String customDescription);

//    /**
//     * @return an object holding information about execution frequency and times.
//     * @since 2.12
//     */
//    NodeTimer getNodeTimer();

    /** @param value the isDeletable to set */
    void setDeletable(boolean value);

    /** @return the isDeletable */
    boolean isDeletable();

    /**
     * @return the isDirty
     */
    boolean isDirty();

    /**
     * Mark this node container to be changed, that is, it needs to be saved.
     */
    void setDirty();

//    /**
//     * @param autoSaveDirectory the autoSaveDirectory to set
//     * @noreference This method is not intended to be referenced by clients.
//     */
//    void setAutoSaveDirectory(ReferencedFile autoSaveDirectory);
//
//    /**
//     * Returns the directory for this node container. If the node has not been persisted yet, <code>null</code> is
//     * returned.
//     *
//     * @return a directory or <code>null</code>
//     * @noreference this is not part of the public API
//     */
//    ReferencedFile getNodeContainerDirectory();
//
//    /** @return The directory for auto-saving (or null if not auto-saved yet).
//     * @noreference This method is not intended to be referenced by clients. */
//    ReferencedFile getAutoSaveDirectory();

//    /** Load information from execution result. Subclasses will override this
//     * method and will call this implementation as <code>super.loadEx...</code>.
//     * @param result The execution result (contains port objects, messages, etc)
//     * @param exec For progress information (no cancelation supported)
//     * @param loadResult A load result that contains, e.g. error messages.
//     */
//    void loadExecutionResult(NodeContainerExecutionResult result, ExecutionMonitor exec, LoadResult loadResult);
//
//    /** Saves all internals that are necessary to mimic the computed result
//     * into a new execution result object. This method is called on node
//     * instances, which are, e.g. executed on a server and later on read back
//     * into a true KNIME instance (upon which
//     * {@link #loadExecutionResult(NodeContainerExecutionResult, ExecutionMonitor, LoadResult)}
//     * is called).
//     * @param exec For progress information (this method will copy port
//     *        objects).
//     * @return A new execution result instance.
//     * @throws CanceledExecutionException If canceled.
//     */
//    NodeContainerExecutionResult createExecutionResult(ExecutionMonitor exec) throws CanceledExecutionException;

    /**
     * Changes the nodes lock status for various actions, i.e. from being deleted, reset or configured.
     *
     * @param setLock whether the locks should be set (<code>true</code>) or released (<code>false</code>)
     * @param locks the locks to be set or released, e.g. {@link NodeLock#DELETE}, {@link NodeLock#RESET},
     *            {@link NodeLock#CONFIGURE}
     * @since 3.2
     */
    void changeNodeLocks(boolean setLock, NodeLock... locks);

    /**
     * Returns the node's lock status, i.e. whether the node is locked from being deleted, reset or configured.
     *
     * @return the currently set {@link NodeLocks}
     * @since 3.2
     */
    NodeLocks getNodeLocks();

    /**
     * To be called from rpc clients - not needed for local, for remote, picks an endpoint and forwards ui data request
     * to be transported to remote node model and collects the response in `out`. <br/>
     * (De-)serialization is handled by {@link AbstractRpcClient}.
     *
     * @return serialized result
     */
    String doRpc(String remoteProcedureCall);

}
/*
 * ------------------------------------------------------------------------
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
 * Created on Jan 29, 2014 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.wizard.WizardViewResponse;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * A utility class received from the workflow manager that allows stepping back and forth in a wizard execution.
 * USed for the 2nd generation wizard execution based on SubNodes.
 *
 * <p>Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public final class WizardExecutionController extends WebResourceController implements ExecutionController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WizardExecutionController.class);

    /** The history of subnodes prompted, current one on top unless {@link #ALL_COMPLETED}. Each int is
     * the subnode ID suffix */
    private final Stack<Integer> m_promptedSubnodeIDSuffixes;

    /** This is the central data structure - it holds all nodes that were halted during
     * execution = nodes that were executed and none of their successors was queued. Those
     * will be active SubNodes with at least one active QF element that can be displayed.
     * After the user got a chance to interact with the Wizard page, those nodes will be
     * re-executed but this time they will not be added/halted again (which is why the status
     * is toggled if they are already in the list - see checkHaltingCriteria()). However, if
     * it is part of a loop it will be executed a second time (after the re-execute) and then
     * execution will be halted again.
     */
    private final List<NodeID> m_waitingSubnodes;

    private boolean m_hasExecutionStarted = false;

    private final Map<String, String> m_additionalProperties;

    /** Created from workflow.
     * @param manager ...
     */
    WizardExecutionController(final WorkflowManager manager) {
        super(manager);
        m_promptedSubnodeIDSuffixes = new Stack<>();
        m_waitingSubnodes = new ArrayList<>();
        m_additionalProperties = new HashMap<>();
    }

    /** Restored from settings.
     * @param manager ...
     * @param settings ...
     * @throws InvalidSettingsException ...
     */
    WizardExecutionController(final WorkflowManager manager, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        this(manager);
        int[] levelStack = settings.getIntArray("promptedSubnodeIDs");
        m_promptedSubnodeIDSuffixes.addAll(Arrays.asList(ArrayUtils.toObject(levelStack)));

        String[] waitingSubnodes = settings.getStringArray("waitingSubnodes", new String[0]);
        Stream.of(waitingSubnodes).map(s -> NodeIDSuffix.fromString(s).prependParent(manager.getID()))
            .forEach(id -> m_waitingSubnodes.add(id));
        if (settings.containsKey("additionalProperties")) {
            NodeSettingsRO additionalProps = settings.getNodeSettings("additionalProperties");
            for (String key : additionalProps) {
                m_additionalProperties.put(key, additionalProps.getString(key));
            }
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.4
     */
    @Override
    public void checkHaltingCriteria(final NodeID source) {
        assert m_manager.isLockedByCurrentThread();
        if (m_waitingSubnodes.remove(source)) {
            // trick to handle re-execution of SubNodes properly: when the node is already
            // in the list it was just re-executed and we don't add it to the list of halted
            // nodes but removed it instead. If we see it again then it is part of a loop and
            // we will add it again).
            return;
        }
        if (isSubnodeViewAvailable(source)) {
            // add to the list so we can later avoid queuing of successors!
            m_waitingSubnodes.add(source);
        }
    }

    /** {@inheritDoc}
     * @since 3.4*/
    @Override
    public boolean isHalted(final NodeID source) {
        return m_waitingSubnodes.contains(source);
    }

    /**
     * Determines whether the wizard execution is halted at a page that is represented by the very last node in that
     * branch (i.e. no further execution required).
     *
     * @return <code>true</code> if workflow is halted at a wizard page with no outgoing connections, otherwise
     *         <code>false</code>
     * @since 4.3
     */
    public boolean isHaltedAtTerminalWizardPage() {
        if (hasCurrentWizardPage()) {
            NodeID wizardPage = getCurrentWizardPageNodeID();
            return m_manager.getOutgoingConnectionsFor(wizardPage).isEmpty();
        } else {
            return false;
        }
    }

    /**
     * Determines whether the wizard execution is halted at a page that is represented by a node with successors.
     *
     * @return <code>true</code> if workflow is halted at a wizard page with outgoing connections, otherwise
     *         <code>false</code>
     * @since 4.3
     */
    public boolean isHaltedAtNonTerminalWizardPage() {
        if (hasCurrentWizardPage()) {
            NodeID wizardPage = getCurrentWizardPageNodeID();
            return !m_manager.getOutgoingConnectionsFor(wizardPage).isEmpty();
        } else {
            return false;
        }
    }

    /** Get the current wizard page. Throws exception if none is available (as per {@link #hasCurrentWizardPage()}.
     * @return The current wizard page.
     * @since 3.4
     */
    public WizardPageContent getCurrentWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                CheckUtils.checkState(hasCurrentWizardPageInternal(true), "No current wizard page");
                return getWizardPageInternal(m_waitingSubnodes.get(0));
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * @return the id of the component representing the current wizard page
     * @throws IllegalStateException if there is no current wizard page (as per {@link #hasCurrentWizardPage()})
     * @since 4.2
     */
    public NodeID getCurrentWizardPageNodeID() {
        CheckUtils.checkState(hasCurrentWizardPage(), "No current wizard page");
        return m_waitingSubnodes.get(0);
    }

    /**
     * @return the last wizard page visited or an empty optional if there is none
     * @since 4.2
     */
    public Optional<NodeID> getLastWizardPageNodeID() {
        if (m_promptedSubnodeIDSuffixes.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(toNodeID(m_promptedSubnodeIDSuffixes.peek()));
        }
    }

    /**
     * @return <code>true</code> if execution has been started, i.e. {@link #stepFirst()} has been called, otherwise
     *         <code>false</code>
     * @since 4.2
     */
    public boolean hasExecutionStarted() {
        return m_hasExecutionStarted;
    }

    /** ...
     * @return ...
     * @deprecated Use {@link #hasCurrentWizardPage()} instead.
     */
    @Deprecated
    public boolean hasNextWizardPage() {
        return hasCurrentWizardPage();
    }

    /** Returns true if the wizard was stepped forward and has a subnode awaiting input.
     * @return That property.
     * @since 2.11 */
    public boolean hasCurrentWizardPage() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return hasCurrentWizardPageInternal(true);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasCurrentWizardPageInternal(final boolean checkExecuted) {
        assert m_manager.isLockedByCurrentThread();
        if (m_waitingSubnodes.isEmpty()) {
            return false;
        } else if (checkExecuted && !areAllNodesExecuted(m_waitingSubnodes, m_manager)) {
            return false;
        } else if (!m_promptedSubnodeIDSuffixes.isEmpty()) {
            //check whether the 'waiting subnode' is the one currently re-executing
            //i.e., the one already prompted
            //if so -> no current page
            return !toNodeID(m_promptedSubnodeIDSuffixes.peek()).equals(m_waitingSubnodes.get(0));
        } else {
            return true;
        }

//        if (m_promptedSubnodeIDSuffixes.isEmpty()) {
//            // stepNext not called
//            return false;
//        } else if (m_promptedSubnodeIDSuffixes.peek() == ALL_COMPLETED) {
//            // all done - result page to be shown
//            return false;
//        }
//        return true;
    }

    private static boolean areAllNodesExecuted(final List<NodeID> nodes, final WorkflowManager wfm) {
        return nodes.stream().map(wfm::getNodeContainer).allMatch(nc -> nc.getNodeContainerState().isExecuted());
    }

    /** Continues the execution and executes up to, incl., the next subnode awaiting input. If no such subnode exists
     * it fully executes the workflow. */
    public void stepFirst() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepFirstInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepFirstInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        m_hasExecutionStarted = true;
        manager.executeAll();
    }

    /**
     * @param viewContentMap
     * @return
     */
    public Map<String, ValidationError> loadValuesIntoCurrentPage(final Map<String, String> viewContentMap) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                CheckUtils.checkState(hasCurrentWizardPageInternal(true), "No current wizard page");
                return loadValuesIntoPageInternal(viewContentMap, m_waitingSubnodes.get(0), true, false);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Processes a request issued by a view by calling the appropriate methods on the corresponding node
     * model and returns a future which can resolve a response object.
     *
     * @param nodeID the node id to which the request belongs to
     * @param viewRequest The JSON serialized view request
     * @param exec the execution monitor to set progress and check possible cancellation
     * @return a {@link CompletableFuture} object, which can resolve a {@link WizardViewResponse}.
     * @throws ViewRequestHandlingException If the request handling or response generation fails for any
     * reason.
     * @throws InterruptedException If the thread handling the request is interrupted.
     * @throws CanceledExecutionException If the handling of the request was canceled e.g. by user
     * intervention.
     * @since 3.7
     */
    public WizardViewResponse processViewRequestOnCurrentPage(final String nodeID, final String viewRequest,
        final ExecutionMonitor exec)
        throws ViewRequestHandlingException, InterruptedException, CanceledExecutionException {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                CheckUtils.checkState(hasCurrentWizardPageInternal(true), "No current wizard page");
                return processViewRequestInternal(m_waitingSubnodes.get(0), nodeID, viewRequest, exec);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    public void stepNext() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepNextInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private void stepNextInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasCurrentWizardPageInternal(false), "No current wizard page");
        NodeID currentID = m_waitingSubnodes.get(0);
        SubNodeContainer currentNC = manager.getNodeContainer(currentID, SubNodeContainer.class, true);
        if (currentNC.getFlowObjectStack().peek(FlowLoopContext.class) == null) {
            m_promptedSubnodeIDSuffixes.push(currentID.getIndex());
        }
        reexecuteNode(currentID);
    }

    private void reexecuteNode(final NodeID id) {
        if (m_manager.getNodeContainer(id).getInternalState().isExecuted()) {
            m_waitingSubnodes.remove(id);
            m_manager.configureNodeAndSuccessors(id, false);
        } else {
            m_manager.executeUpToHere(id);
        }
        // in case of back-stepping we need to mark all nodes again (for execution)
        m_hasExecutionStarted = true;
        m_manager.executeAll();
    }

    public boolean hasPreviousWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return hasPreviousWizardPageInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    private boolean hasPreviousWizardPageInternal() {
        assert m_manager.isLockedByCurrentThread();
        return !m_promptedSubnodeIDSuffixes.isEmpty();
    }


    public void stepBack() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                stepBackInternal();
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Additional properties to be stored with the wizard execution controller.
     *
     * @param key the property key
     * @param value  the property value
     * @since 4.2
     */
    public void setProperty(final String key, final String value) {
        m_additionalProperties.put(key, value);
    }

    /**
     * Retrieves a property stored with the wizard execution controller.
     *
     * @param key the key of the property
     * @return the value or an empty optional if there is no value for the given key
     * @since 4.2
     */
    public Optional<String> getProperty(final String key) {
        return Optional.ofNullable(m_additionalProperties.get(key));
    }

    /**
     * Removes a property.
     *
     * @param key the property key to remove
     * @since 4.2
     */
    public void removeProperty(final String key) {
        m_additionalProperties.remove(key);
    }

    private void stepBackInternal() {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        CheckUtils.checkState(hasPreviousWizardPageInternal(), "No more previous pages");
        int currentPage = m_promptedSubnodeIDSuffixes.pop();
        NodeID currentSNID = toNodeID(currentPage);
        m_waitingSubnodes.clear();
        m_waitingSubnodes.add(currentSNID);
        SubNodeContainer currentSN = manager.getNodeContainer(currentSNID, SubNodeContainer.class, true);
        final Integer previousSNIDSuffix = m_promptedSubnodeIDSuffixes.isEmpty()
                ? null : m_promptedSubnodeIDSuffixes.peek();
        SubNodeContainer previousSN = previousSNIDSuffix == null ? null
            : manager.getNodeContainer(toNodeID(previousSNIDSuffix), SubNodeContainer.class, true);
        LOGGER.debugWithFormat("Stepping back wizard execution - resetting Component \"%s\" (%s)",
            currentSN.getNameWithID(), previousSN == null ? "no more Components to reset"
                : "new current one is \"" + previousSN.getNameWithID() + "\"");
        manager.cancelExecution(currentSN);
        manager.resetAndConfigureNodeAndSuccessors(currentSNID, false);
    }

    /** Sets manager to null. Called when new wizard is created on top of workflow. */
    @Override
    void discard() {
    }

    void save(final NodeSettingsWO settings) {
        int[] promptedSubnodeIDs = ArrayUtils.toPrimitive(
            m_promptedSubnodeIDSuffixes.toArray(new Integer[m_promptedSubnodeIDSuffixes.size()]));
        settings.addIntArray("promptedSubnodeIDs", promptedSubnodeIDs);

        NodeID parentId = m_manager.getID();
        settings.addStringArray("waitingSubnodes",
            m_waitingSubnodes.stream().map(id -> NodeIDSuffix.create(parentId, id).toString()).toArray(String[]::new));
        if (!m_additionalProperties.isEmpty()) {
            NodeSettingsWO additionalProps = settings.addNodeSettings("additionalProperties");
            m_additionalProperties.entrySet().stream()
                .forEach(e -> additionalProps.addString(e.getKey(), e.getValue()));
        }
    }

    @Override
    boolean isResetDownstreamNodesWhenApplyingViewValue() {
        return false;
    }

    @Override
    void stateCheckWhenApplyingViewValues(final SubNodeContainer snc) {
        // the node should be executed but possibly one of the view nodes fails so it's not. We accept any.
    }

    @Override
    void stateCheckDownstreamNodesWhenApplyingViewValues(final SubNodeContainer snc, final NodeContainer downstreamNC) {
        final InternalNodeContainerState destNCState = downstreamNC.getInternalState();
        CheckUtils.checkState(destNCState.isHalted() && !destNCState.isExecuted(), "Downstream nodes of "
                + "Component %s must not be in execution/executed (node %s)", snc.getNameWithID(), downstreamNC);
    }

}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardViewRequest;
import org.knime.core.node.wizard.WizardViewRequestHandler;
import org.knime.core.node.wizard.WizardViewResponse;
import org.knime.core.node.wizard.page.WizardPageContribution;
import org.knime.core.node.wizard.page.WizardPageUtil;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * An abstract utility class received from the workflow manager that allows defining wizard execution or generating
 * combined views on subnodes. Used for example for the 2nd generation wizard execution based on SubNodes.
 *
 * <p>
 * Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @since 3.4
 */
public abstract class WebResourceController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WebResourceController.class);

    /** Host WFM. */
    protected final WorkflowManager m_manager;

    /**
     * Temporary workaround to check if the argument workflow contains sub nodes and hence can be used with the
     * {@link WizardNode} execution.
     *
     * @param manager To check, not null.
     * @return That property.
     */
    public static boolean hasWizardExecution(final WorkflowManager manager) {
        try (WorkflowLock lock = manager.lock()) {
            Collection<NodeContainer> nodes = manager.getWorkflow().getNodeValues();
            for (NodeContainer nc : nodes) {
                if (nc instanceof SubNodeContainer) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Created from workflow.
     *
     * @param manager ...
     */
    WebResourceController(final WorkflowManager manager) {
        m_manager = CheckUtils.checkArgumentNotNull(manager);
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given subnode.
     *
     * @param subnodeId the {@link NodeID} of the subnode to check
     * @return true, if a view on the subnode is available, false otherwise
     * @since 3.4
     */
    protected boolean isSubnodeViewAvailable(final NodeID subnodeId) {
        return WizardPageUtil.isWizardPage(m_manager, subnodeId);
    }

    protected Map<NodeIDSuffix, Object> getWizardPageViewValueMapInternal(final NodeID subnodeID) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for creating wizard page");
            return null;
        }
        final WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        WorkflowManager subWFM = subNC.getWorkflowManager();
        return WizardPageUtil.getWizardPageNodes(subWFM).stream()//
            .filter(n -> n.getNodeContainerState().isExecuted())//
            .filter(n -> !n.isInactive())//
            .collect(Collectors.toMap(n -> toNodeIDSuffix(n.getID()), WebResourceController::getViewValue));
    }

    @SuppressWarnings("rawtypes")
    private static Object getViewValue(final NativeNodeContainer viewNode) {
        if (viewNode.getNodeModel() instanceof WizardNode) {
            return ((WizardNode)viewNode.getNodeModel()).getViewValue();
        } else if (viewNode.getNode().getFactory() instanceof WizardPageContribution) {
            return ((WizardPageContribution)viewNode.getNode().getFactory()).getInitialViewValue(viewNode).orElse("");
        } else {
            throw new IllegalStateException("Not a view node: " + viewNode.getNameWithID());
        }
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in a given subnode.
     *
     * @param viewContentMap the values to load
     * @param subnodeID the id for the subnode (i.e. the page) containing the appropriate view nodes
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node
     *            settings), false otherwise (apply temporarily)
     * @param nodeIDsToReset list of nodes in page (i.e., the subnode denoted by subnodeID) to be reset (including all
     *            their successors within the same page or nested subnode); if <code>null</code> all nodes within the
     *            subnode (i.e. the entire page) are being reset
     * @return empty map if validation succeeds, map of errors otherwise
     * @since 5.5
     */
    protected Map<String, ValidationError> loadValuesIntoPageInternal(final Map<String, String> viewContentMap,
        final NodeID subnodeID, final boolean validate, final boolean useAsDefault,
        final Collection<NodeID> nodeIDsToReset) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied for loading values into wizard page");
            return Collections.emptyMap();
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();

        Map<String, String> filteredViewContentMap;

        if (nodeIDsToReset == null) {
            filteredViewContentMap = viewContentMap;
        } else {
			var nodesToReset = WizardPageUtil
					.getSuccessorWizardPageNodesWithinComponent(manager, subnodeID, nodeIDsToReset)
					.map(p -> p.getFirst().toString()).collect(Collectors.toCollection(HashSet::new));
            filteredViewContentMap = filterViewValues(nodesToReset, viewContentMap);
        }
        LOGGER.debugWithFormat("Loading view content into wizard nodes (%d)", filteredViewContentMap.size());

        Map<NodeID, NativeNodeContainer> wizardNodeSet = getWizardNodeSetForVerifiedID(subnodeID);
        if (validate) {
            Map<String, ValidationError> validationResult =
                validateViewValuesInternal(filteredViewContentMap, subnodeID, wizardNodeSet);
            if (!validationResult.isEmpty()) {
                return validationResult;
            }
        }

        // validation succeeded, reset subnode and apply
        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        if (!subNodeNC.getInternalState().isExecuted()) { // this used to be an error but see SRV-745
            LOGGER.warnWithFormat(
                "Component (%s) not fully executed on appyling new values -- "
                    + "consider to change component layout to have self-contained executable units",
                subNodeNC.getNameWithID());
        }

        if (nodeIDsToReset == null) {
            manager.resetSubnodeForViewUpdate(subnodeID, this);
        } else {
            for (NodeID nodeID : nodeIDsToReset) {
                manager.resetSubnodeForViewUpdate(subnodeID, this, nodeID);
            }
        }

        loadViewValues(filteredViewContentMap, wizardNodeSet, manager, useAsDefault);

        manager.configureNodeAndSuccessors(subnodeID, true);
        return Collections.emptyMap();
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in a given subnode. This is a convenience
     * method to call {@link #loadValuesIntoPageInternal(Map, NodeID, boolean, boolean, Collection)}
     *
     * @param viewContentMap the values to load
     * @param subnodeID the id for the subnode (i.e. the page) containing the appropriate view nodes
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node
     *            settings), false otherwise (apply temporarily)
     * @param nodeIDToReset a node in page (i.e., the subnode denoted by subnodeID) to be reset (including all its
     *            successors within the same page or nested subnode); if <code>null</code> all nodes within the subnode
     *            (i.e. the entire page) are being reset
     * @return empty map if validation succeeds, map of errors otherwise
     */
    protected Map<String, ValidationError> loadValuesIntoPageInternal(final Map<String, String> viewContentMap,
        final NodeID subnodeID, final boolean validate, final boolean useAsDefault, final NodeID nodeIDToReset) {

        var nodeIDsToReset = nodeIDToReset == null ? null
            : List.of(nodeIDToReset);

        return loadValuesIntoPageInternal(viewContentMap, subnodeID, validate, useAsDefault, nodeIDsToReset);
    }

    @SuppressWarnings("rawtypes")
    private static void loadViewValues(final Map<String, String> filteredViewContentMap,
        final Map<NodeID, NativeNodeContainer> wizardNodeSet, final WorkflowManager manager,
        final boolean useAsDefault) {
        for (Map.Entry<String, String> entry : filteredViewContentMap.entrySet()) {
            NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(manager.getProjectWFM().getID());
            NativeNodeContainer wizardNode = wizardNodeSet.get(id);
            try {
                if (wizardNode.getNodeModel() instanceof WizardNode) {
                    loadViewValueForWizardNode(wizardNode, (WizardNode)wizardNode.getNodeModel(), entry.getValue(),
                        useAsDefault);
                } else if (wizardNode.getNode().getFactory() instanceof WizardPageContribution) {
                    ((WizardPageContribution)wizardNode.getNode().getFactory()).loadViewValue(wizardNode,
                        entry.getValue());
                } else {
                    throw new IllegalStateException("The node '" + wizardNode.getNameWithID()
                        + "' doesn't contribute to a wizard page. View values can't be loaded.");
                }
            } catch (Exception e) {
                LOGGER.error(
                    "Failed to load view value into node \"" + wizardNode.getID() + "\" although validation succeeded",
                    e);
            }
        }
    }

    private static void loadViewValueForWizardNode(final NativeNodeContainer nnc, final WizardNode wizardNode,
        final String viewValue, final boolean useAsDefault) throws IOException {
        WebViewContent newViewValue = wizardNode.createEmptyViewValue();
        if (newViewValue == null) {
            // node has no view value
            return;
        }
        newViewValue.loadFromStream(new ByteArrayInputStream(viewValue.getBytes(StandardCharsets.UTF_8)));
        wizardNode.loadViewValue(newViewValue, useAsDefault);
        if (useAsDefault) {
            nnc.saveModelSettingsToDefault();
        }
    }

    /** Before applying view values the subnode needs to possibly reset downstream nodes. For the wizard execution
     * downstream nodes should be executing (state = executing), whereas for single page mode/component view
     * none of the downstream nodes should be executing.
     * @param subNCId The id of the subnode container.
     * @throws IllegalStateException If state isn't correct.
     */
    abstract void stateCheckWhenApplyingViewValues(final SubNodeContainer snc);

    /** Similiar to {@link #stateCheckWhenApplyingViewValues(SubNodeContainer)} this will test the state of the
     * downstream nodes when applying new view values. For wizard execution downstream nodes must be in the
     * {@link NodeContainerState#isHalted() halted} state.
     * @throws IllegalStateException If state isn't correct.
     */
    abstract void stateCheckDownstreamNodesWhenApplyingViewValues(SubNodeContainer snc, NodeContainer downstreamNC);

    /** @return for wizard execution the downstream nodes are not reset when applying the view value on a subnode. */
    abstract boolean isResetDownstreamNodesWhenApplyingViewValue();

    /**
     * Validates a given set of serialized view values for a given subnode.
     *
     * @param viewValues the values to validate
     * @param subnodeID the id of the subnode containing the appropriate view nodes
     * @param wizardNodeSet the set of view nodes that the view values correspond to.
     * @return an empty map if validation succeeds, map of errors otherwise
     * @throws IllegalArgumentException if the provided subnode id is <code>null</code>
     * @throws IllegalStateException if there are no nodes with the provided id prefixes in the page or the provided
     *             wizard-node-set doesn't contain the required wizard nodes
     */
    @SuppressWarnings({"rawtypes"})
    protected Map<String, ValidationError> validateViewValuesInternal(final Map<String, String> viewValues,
        final NodeID subnodeID, final Map<NodeID, NativeNodeContainer> wizardNodeSet) {
        if (subnodeID == null) {
            throw new IllegalArgumentException("No node ID supplied for validating view values of wizard page");
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        Map<String, ValidationError> resultMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : viewValues.entrySet()) {
            NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(entry.getKey());
            NodeID id = suffix.prependParent(manager.getProjectWFM().getID());
            CheckUtils.checkState(id.hasPrefix(subnodeID),
                "The wizard page content for ID %s (suffix %s) does not belong to the current Component (ID %s)",
                id, entry.getKey(), subnodeID);
            NativeNodeContainer wizardNode = wizardNodeSet.get(id);
            CheckUtils.checkState(wizardNode != null,
                "No wizard node with ID %s in Component, valid IDs are: " + "%s", id,
                ConvenienceMethods.getShortStringFrom(wizardNodeSet.entrySet(), 10));
            ValidationError validationError = null;
            try {
                if (wizardNode.getNodeModel() instanceof WizardNode) {
                    validationError =
                        validateViewValueForWizardNode((WizardNode)wizardNode.getNodeModel(), entry.getValue());
                } else if (wizardNode.getNode().getFactory() instanceof WizardPageContribution) {
                    validationError = ((WizardPageContribution)wizardNode.getNode().getFactory())
                        .validateViewValue(wizardNode, entry.getValue()).map(ValidationError::new).orElse(null);
                }
            } catch (Exception e) { // NOSONAR
                validationError = new ValidationError("An unexpected error occurred while validating the view value: "
                    + entry.getValue() + ": \n" + e.getMessage());
            }

            if (validationError != null) {
                resultMap.put(entry.getKey(), validationError);
            }
        }
        if (!resultMap.isEmpty()) {
            return resultMap;
        }
        return Collections.emptyMap();
    }

    private static ValidationError validateViewValueForWizardNode(final WizardNode wizardNode, final String viewValue)
        throws IOException {
        WebViewContent newViewValue = wizardNode.createEmptyViewValue();
        if (newViewValue == null) {
            // node has no view value
            return null;
        }
        ValidationError validationError = null;
        newViewValue.loadFromStream(new ByteArrayInputStream(viewValue.getBytes(StandardCharsets.UTF_8)));
        validationError = wizardNode.validateViewValue(newViewValue);
        return validationError;
    }

    /**
     * Queries a subnode and returns all appropriate view nodes contained within.
     *
     * @param subnodeID the subnode id, not null
     * @return a map of view nodes
     */
    protected Map<NodeID, NativeNodeContainer> getWizardNodeSetForVerifiedID(final NodeID subnodeID) {
        if (subnodeID == null) {
            LOGGER.error("No node ID supplied while trying to retrieve node set for wizard page");
            return null;
        }
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        WorkflowManager subNodeWFM = subNodeNC.getWorkflowManager();
        return WizardPageUtil.getWizardPageNodes(subNodeWFM, true).stream()
            .collect(Collectors.toMap(NativeNodeContainer::getID, n -> n));
    }

    /**
     * Returns a wizard node to a given subnode and node id
     * @param subnodeID the subnode id, which is the container for the wizard node
     * @param wizardNodeID the node id of the wizard node
     * @return the resolved wizard node or null, if node id does not denote a wizard node
     * @since 3.7
     */
    @SuppressWarnings("rawtypes")
    protected WizardNode getWizardNodeForVerifiedID(final NodeID subnodeID, final NodeID wizardNodeID) {
        CheckUtils.checkNotNull(subnodeID);
        CheckUtils.checkNotNull(wizardNodeID);
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        SubNodeContainer subNodeNC = manager.getNodeContainer(subnodeID, SubNodeContainer.class, true);
        NodeContainer cont = subNodeNC.getWorkflowManager().findNodeContainer(wizardNodeID);
        if (cont instanceof NativeNodeContainer) {
            NodeModel model = ((NativeNodeContainer)cont).getNodeModel();
            if (model instanceof WizardNode) {
                return (WizardNode)model;
            } else {
                LOGGER.error("Node model is not of type WizardNode");
            }
            LOGGER.error("Node container is not of type NativeNodeContainer");
        }
        return null;
    }

    /**
     * Retrieves the response for a view request, which is from a node within a subnode
     *
     * @param subnodeID the node id of the subnode container
     * @param nodeID the node id of the wizard node, as fetched from the combined view
     * @param viewRequest the JSON serialized view request string
     * @param exec the execution monitor to set progress and check possible cancellation
     * @return a {@link WizardViewResponse} which is generated by the concrete node
     * @throws ViewRequestHandlingException If the request handling or response generation fails for any
     * reason.
     * @throws InterruptedException If the thread handling the request is interrupted.
     * @throws CanceledExecutionException If the handling of the request was canceled e.g. by user
     * intervention.
     * @since 3.7
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected WizardViewResponse processViewRequestInternal(final NodeID subnodeID,
        final String nodeID, final String viewRequest, final ExecutionMonitor exec)
                throws ViewRequestHandlingException, InterruptedException, CanceledExecutionException {
        WorkflowManager manager = m_manager;
        assert manager.isLockedByCurrentThread();
        NodeID.NodeIDSuffix suffix = NodeID.NodeIDSuffix.fromString(nodeID);
        NodeID id = suffix.prependParent(manager.getID());
        WizardNode model = getWizardNodeForVerifiedID(subnodeID, id);
        if (model == null || !(model instanceof WizardViewRequestHandler)) {
            throw new ViewRequestHandlingException(
                "Node model can not process view requests. Possible implementation error.");
        }
        WizardViewRequest req = ((WizardViewRequestHandler)model).createEmptyViewRequest();
        try {
            req.loadFromStream(new ByteArrayInputStream(viewRequest.getBytes(Charset.forName("UTF-8"))));
        } catch (IOException ex) {
            throw new ViewRequestHandlingException("Error deserializing request: " + ex.getMessage(), ex);
        }
        return (WizardViewResponse)((WizardViewRequestHandler)model).handleRequest(req, exec);
    }

    /**
     * Utility method to filter a view value map based on the provided inclusive set of {@link NodeID} strings.
     *
     * @param resetNodeIds the {@link NodeID} set to include in the value map.
     * @param viewValues the superset of view values to filter.
     * @return the subset of matching view values.
     *
     * @since 4.4
     */
    private static Map<String, String> filterViewValues(final Set<String> resetNodeIds,
        final Map<String, String> viewValues) {
        return viewValues.entrySet().stream().filter(e -> resetNodeIds.contains(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Re-executes a subset of nodes of the current page. I.e. resets all nodes downstream of the given list of nodes
     * (within the page), loads the provided values into the reset nodes and triggers workflow execution of the entire
     * page. Notes: Provided values that refer to a node that hasn't been reset will be ignored. If the validation of
     * the input values fails, the page won't be re-executed and remains reset.
     *
     * @param nodeIDsToReset list of absolute {@link NodeID}s of the upstream-most nodes in the page that shall be
     *            reset.
     * @param valueMap the values to load before re-execution, a map from {@link NodeIDSuffix} strings to parsed view
     *            values.
     * @return empty map if validation succeeds, map of errors otherwise
     *
     * @since 5.5
     */
    public abstract Map<String, ValidationError> reexecuteSinglePage(final Collection<NodeID> nodeIDsToReset,
        final Map<String, String> valueMap);

    /**
     * Re-executes a subset of nodes of the current page. I.e. resets all nodes downstream of the given node (within the
     * page), Convenience method to call {@link #reexecuteSinglePage(Collection, Map)} with a single node.
     *
     * @param nodeIDToReset the absolute {@link NodeID} of the upstream-most node in the page that shall be reset.
     * @param valueMap the values to load before re-execution, a map from {@link NodeIDSuffix} strings to parsed view
     * @return empty map if validation succeeds, map of errors otherwise
     */
    public Map<String, ValidationError> reexecuteSinglePage(final NodeID nodeIDToReset,
        final Map<String, String> valueMap) {

        var nodeIDsToReset = nodeIDToReset == null ? null
                : List.of(nodeIDToReset);

        return reexecuteSinglePage(nodeIDsToReset, valueMap);
    }

    /**
     * Composes the NodeID of a subnode.
     *
     * @param subnodeIDSuffix ...
     * @return new NodeID(m_manager.getID(), subnodeIDSuffix);
     */
    protected NodeID toNodeID(final int subnodeIDSuffix) {
        return new NodeID(m_manager.getID(), subnodeIDSuffix);
    }

    /**
     * Composes a NodeIDSuffix from a subnode ID by removing the manager prefix.
     *
     * @param subnodeID the ID of a subnode of the manager associated with this controller.
     * @return NodeIDSuffix the component relative suffix for the subnode.
     */
    private NodeIDSuffix toNodeIDSuffix(final NodeID subnodeID) {
        return NodeIDSuffix.create(m_manager.getID(), subnodeID);
    }

    /**
     * Checks if the associated workflow manager has been discarded.
     *
     * @throws IllegalArgumentException if workflow manager is discarded
     */
    protected void checkDiscard() {
        CheckUtils.checkArgument(m_manager != null, "%s has been disconnected from workflow",
            WebResourceController.class.getSimpleName());
    }

}

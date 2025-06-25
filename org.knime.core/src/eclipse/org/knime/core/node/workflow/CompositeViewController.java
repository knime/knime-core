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
 *   24 Mar 2017 (albrecht): created
 */
package org.knime.core.node.workflow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.wizard.WizardViewResponse;
import org.knime.core.node.wizard.page.WizardPage;
import org.knime.core.node.wizard.page.WizardPageUtil;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * A utility class received from the workflow manager that allows the combined view creation for a single subnode (aka
 * component).
 *
 * This class is really only dedicated to 'isolated' single pages of components. Single page (re-)execution of page that
 * are part of a wizard is done via {@link WizardExecutionController#reexecuteSinglePage(Collection<NodeID>, Map)}.
 *
 * <p>
 * Do not use, no public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CompositeViewController extends WebResourceController {

    private final NodeID m_nodeID;

    /**
     * Creates a new controller.
     *
     * @param manager the workflow this controller is created for
     * @param nodeID the id of the component representing the page this controller is created for
     */
    public CompositeViewController(final WorkflowManager manager, final NodeID nodeID) {
        super(manager);
        m_nodeID = nodeID;
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given subnode.
     * @return true, if a view on the subnode is available, false otherwise
     */
    public boolean isSubnodeViewAvailable() {
        return super.isSubnodeViewAvailable(m_nodeID);
    }

    /**
     * Gets the wizard page for a given node id. Throws exception if no wizard page available.
     * @return The wizard page for the given node id
     *
     * @since 4.5
     */
    public WizardPage getWizardPage() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                return WizardPageUtil.createWizardPage(manager, m_nodeID);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Retrieves all available view values from the available wizard nodes for the given node id.
     * @return a map from NodeID to view value for all appropriate wizard nodes.
     */
    public Map<NodeIDSuffix, Object> getWizardPageViewValueMap() {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            NodeContext.pushContext(manager);
            try {
                return getWizardPageViewValueMapInternal(m_nodeID);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in the given subnode.
     *
     * @param viewContentMap the values to validate
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node
     *            settings), false otherwise (apply temporarily)
     * @param nodeIDsToReset list of nodes in current page (i.e. component/composite view) to be reset (including all
     *            their successors within the same page) before loading the values; if <code>null</code> all nodes
     *            within the page are being reset
     *
     * @throws IllegalStateException if the page is executing or if the associated page is not the 'current' page
     *             anymore (only if part of a workflow in wizard execution)
     *
     * @return an empty map if validation succeeds, map of errors otherwise
     * @since 5.5
     */
    public Map<String, ValidationError> loadValuesIntoPage(final Map<String, String> viewContentMap,
        final boolean validate, final boolean useAsDefault, final Collection<NodeID> nodeIDsToReset) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return loadValuesIntoPageInternal(viewContentMap, m_nodeID, validate, useAsDefault, nodeIDsToReset);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Tries to load a map of view values to all appropriate views contained in the given subnode.
     *
     * @param viewContentMap the values to validate
     * @param validate true, if validation is supposed to be done before applying the values, false otherwise
     * @param useAsDefault true, if the given value map is supposed to be applied as new node defaults (overwrite node
     *            settings), false otherwise (apply temporarily)
     * @param nodeToReset a node in current page (i.e. component/composite view) to be reset (including all it's
     *            successors within the same page) before loading the values; if <code>null</code> all nodes within the
     *            page are being reset
     *
     * @throws IllegalStateException if the page is executing or if the associated page is not the 'current' page
     *             anymore (only if part of a workflow in wizard execution)
     *
     * @return an empty map if validation succeeds, map of errors otherwise
     */
    public Map<String, ValidationError> loadValuesIntoPage(final Map<String, String> viewContentMap,
        final boolean validate, final boolean useAsDefault, final NodeID nodeToReset) {

    	var nodeIDsToReset = nodeToReset == null ? null
                : List.of(nodeToReset);

        return loadValuesIntoPage(viewContentMap, validate, useAsDefault, nodeIDsToReset);
    }

    /**
     * Processes a request issued by a view by calling the appropriate methods on the corresponding node model and
     * returns the rendered response. This request handling is *only* for node-specific requests. For component-level
     * requests (such as for re-execution), the {@link ViewableModel} impl. should handle the bulk of the logic
     * regarding composition of the response.
     *
     * @param nodeID The node id of the node that the request belongs to.
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
    public WizardViewResponse processViewRequest(final String nodeID, final String viewRequest,
        final ExecutionMonitor exec)
        throws ViewRequestHandlingException, InterruptedException, CanceledExecutionException {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return processViewRequestInternal(m_nodeID, nodeID, viewRequest, exec);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @Override
    boolean isResetDownstreamNodesWhenApplyingViewValue() {
        return true;
    }

    @Override
    void stateCheckDownstreamNodesWhenApplyingViewValues(final SubNodeContainer snc, final NodeContainer downstreamNC) {
        // no check needed, done in #stateCheckWhenApplyingViewValues
    }

    @Override
    void stateCheckWhenApplyingViewValues(final SubNodeContainer snc) {
        NodeID id = snc.getID();
        WorkflowManager parent = snc.getParent();
        CheckUtils.checkState(parent.canResetNode(id), "Can't reset component%s",
            parent.hasSuccessorInProgress(id) ? " - some downstream nodes are still executing" : "");
    }

    /**
     * Validates a given set of serialized view values for the given subnode.
     * @param viewContentMap the values to validate
     *
     * @throws IllegalStateException if the page is executing or if the associated page is not the 'current' page
     *             anymore (only if part of a workflow in wizard execution)
     *
     * @return an empty map if validation succeeds, map of errors otherwise
     */
    public Map<String, ValidationError> validateViewValuesInPage(final Map<String, String> viewContentMap) {
        WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            NodeContext.pushContext(manager);
            try {
                return validateViewValuesInternal(viewContentMap, m_nodeID, getWizardNodeSetForVerifiedID(m_nodeID));
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Triggers workflow execution up until the given subnode.
     */
    public void executeSinglePage() {
        final WorkflowManager manager = m_manager;
        try (WorkflowLock lock = manager.lock()) {
            checkDiscard();
            m_manager.executeUpToHere(m_nodeID);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Map<String, ValidationError> reexecuteSinglePage(final Collection<NodeID> nodeIDsToReset,
        final Map<String, String> valueMap) {
        try (WorkflowLock lock = m_manager.lock()) {
            NodeID pageID = m_nodeID;
            SubNodeContainer snc = (SubNodeContainer)m_manager.getNodeContainer(pageID);
            try (WorkflowLock sncLock = snc.lock()) {
                Map<String, ValidationError> validationResult =
                    loadValuesIntoPage(valueMap, true, false, nodeIDsToReset);
                if (validationResult == null || validationResult.isEmpty()) {
                    m_manager.executeUpToHere(pageID);
                }
                return validationResult;
            }
        }
    }
}

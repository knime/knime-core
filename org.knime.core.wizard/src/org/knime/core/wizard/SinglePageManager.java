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
 *   3 Apr 2017 (albrecht): created
 */
package org.knime.core.wizard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SinglePageWebResourceController;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.JSONWebNodePage;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class SinglePageManager extends AbstractPageManager {

    /**
     * Returns a {@link SinglePageManager} instance for the given {@link SinglePageManager}
     * @param workflowManager the {@link WorkflowManager} to get the {@link SinglePageManager} instance for
     * @return a {@link SinglePageManager} of the given {@link WorkflowManager}
     */
    public static SinglePageManager of(final WorkflowManager workflowManager) {
        // return new instance, could also be used to invoke a caching/pooling service in the future
        return new SinglePageManager(workflowManager);
    }

    /**
     * Creates a new SinglePageManager instance.
     *
     * @param workflowManager a {@link WorkflowManager} corresponding to the current workflow
     */
    private SinglePageManager(final WorkflowManager workflowManager) {
        super(workflowManager);
    }

    private SinglePageWebResourceController getController(final NodeID containerNodeID) {
        return new SinglePageWebResourceController(getWorkflowManager(), containerNodeID);
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given metanode.
     * @param containerNodeID the {@link NodeID} of the metanode to check
     * @return true, if a view on the metanode is available, false otherwise
     */
    public boolean hasWizardPage(final NodeID containerNodeID) {
        return getController(containerNodeID).isSubnodeViewAvailable();
    }

    /**
     * Creates a wizard page object from a given node id
     *
     * @param containerNodeID the node id to create the wizard page for
     * @return a {@link JSONWebNodePage} object which can be used for serialization
     * @throws IOException if the layout of the wizard page can not be generated
     */
    public JSONWebNodePage createWizardPage(final NodeID containerNodeID) throws IOException {
        SinglePageWebResourceController sec = getController(containerNodeID);
        WizardPageContent page = sec.getWizardPage();
        return createWizardPageInternal(page);
    }

    /**
     * Creates a map of node id string to JSON view value string for all appropriate wizard nodes from a given node id.
     *
     * @param containerNodeID the node id to create the view value map for
     * @return a map containing all appropriate view values
     * @throws IOException on serialization error
     */
    public Map<String, String> createWizardPageViewValueMap(final NodeID containerNodeID) throws IOException {
        SinglePageWebResourceController sec = getController(containerNodeID);
        Map<NodeID, WebViewContent> viewMap = sec.getWizardPageViewValueMap();
        Map<String, String> resultMap = new HashMap<String, String>();
        for (Entry<NodeID, WebViewContent> entry : viewMap.entrySet()) {
            WebViewContent c = entry.getValue();
            resultMap.put(entry.getKey().toString(), new String(((ByteArrayOutputStream)c.saveToStream()).toByteArray()));
        }
        return resultMap;
    }

    /**
     * Validates a given map of view values contained in a given subnode.
     * @param viewValues a map with {@link NodeIDSuffix} string as key and parsed view value as value
     * @param containerNodeId the {@link NodeID} of the subnode
     * @return Null or empty map if validation succeeds, map of errors otherwise
     * @throws IOException on serialization error
     */
    public Map<String, ValidationError> validateViewValues(final Map<String, String> viewValues, final NodeID containerNodeId) throws IOException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            /*ObjectMapper mapper = new ObjectMapper();
            for (String key : viewValues.keySet()) {
                String content = mapper.writeValueAsString(viewValues.get(key));
                viewValues.put(key, content);
            }*/
            if (!viewValues.isEmpty()) {
                SinglePageWebResourceController sec = getController(containerNodeId);
                return sec.validateViewValuesInPage(viewValues);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    /**
     * Applies a given map of view values to a given subnode which have already been validated.
     * @param viewValues an already validated map with {@link NodeIDSuffix} string as key and parsed view value as value
     * @param containerNodeId the {@link NodeID} of the subnode
     * @param useAsDefault true, if values are supposed to be applied as new defaults, false if applied temporarily
     * @throws IOException on serialization error
     */
    public void applyValidatedViewValues(final Map<String, String> viewValues, final NodeID containerNodeId, final boolean useAsDefault) throws IOException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            /*ObjectMapper mapper = new ObjectMapper();
            for (String key : viewValues.keySet()) {
                String content = mapper.writeValueAsString(viewValues.get(key));
                viewValues.put(key, content);
            }*/
            if (!viewValues.isEmpty()) {
                SinglePageWebResourceController sec = getController(containerNodeId);
                sec.loadValuesIntoPage(viewValues, false, useAsDefault);
            }
        }
    }

    /**
     * Applies a given map of view values to a given subnode.
     *
     * @param valueMap a map with {@link NodeIDSuffix} string as key and parsed view value as value
     * @param containerNodeId the node ID to apply the values to
     * @return A JSON-serialized string containing the validation result, null if validation succeeded.
     * @throws IOException on JSON serialization errors
     */
    public String applyViewValues(final Map<String, String> valueMap, final NodeID containerNodeId) throws IOException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            Map<String, String> viewContentMap = validateValueMap(valueMap);
            Map<String, ValidationError> validationResults = null;
            if (!valueMap.isEmpty()) {
                SinglePageWebResourceController sec = getController(containerNodeId);
                validationResults = sec.loadValuesIntoPage(viewContentMap);
            }
            return serializeValidationResult(validationResults);
        }
    }

    /**
     * Triggers reexecution of the subnode, including all contained nodes
     * @param containerNodeId the {@link NodeID} of the subnode to reexecute.
     */
    private void reexecuteSubnode(final NodeID containerNodeId) {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            getController(containerNodeId).reexecuteSinglePage();
        }
    }

    /**
     * Applies a given map of view values to a given subnode which have already been validated and triggers reexecution
     * subsequently.
     *
     * @param valueMap an already validated map with {@link NodeIDSuffix} string as key and parsed view value as value
     * @param containerNodeId the {@link NodeID} of the subnode
     * @param useAsDefault true, if values are supposed to be applied as new defaults, false if applied temporarily
     * @throws IOException on serialization error
     */
    public void applyValidatedValuesAndReexecute(final Map<String, String> valueMap, final NodeID containerNodeId,
        final boolean useAsDefault) throws IOException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            applyValidatedViewValues(valueMap, containerNodeId, useAsDefault);
            reexecuteSubnode(containerNodeId);
        }
    }

}

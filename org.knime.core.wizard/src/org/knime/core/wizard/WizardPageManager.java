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
 *   20 Feb 2017 (albrecht): created
 */
package org.knime.core.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.ExternalNodeData.ExternalNodeDataBuilder;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WizardExecutionController;
import org.knime.core.node.workflow.WizardExecutionController.WizardPageContent;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.JSONViewContent;
import org.knime.js.core.JSONWebNode;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.JSONWebNodePageConfiguration;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.selections.json.JSONSelectionTranslator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public final class WizardPageManager {

    private final WorkflowManager m_wfm;

    /**
     * Returns a {@link WizardPageManager} instance for the given {@link WorkflowManager}
     * @param workflowManager the {@link WorkflowManager} to get the {@link WizardPageManager} instance for
     * @return a {@link WizardPageManager} of the given {@link WorkflowManager}
     */
    public static WizardPageManager of(final WorkflowManager workflowManager) {
        // return new instance, could also be used to invoke a caching/pooling service in the future
        return new WizardPageManager(workflowManager);
    }

    /**
     * Creates a new WizardPageManager instance.
     *
     * @param workflowManager a {@link WorkflowManager} corresponding to the current workflow
     */
    private WizardPageManager(final WorkflowManager workflowManager) {
        m_wfm = workflowManager;
    }

    /**
     * Returns the underlying {@link WorkflowManager} instance
     * @return The underlying {@link WorkflowManager} instance
     */
    public WorkflowManager getWorkflowManager() {
        return m_wfm;
    }

    /**
     * Returns the underlying {@link WizardExecutionController} instance.
     *<br><br>
     * WARNING: this method will most likely be removed, once functionality is completely encapsulated.
     *
     * @return
     * the underlying {@link WizardExecutionController} instance.
     * @noreference This method is not intended to be referenced by clients.
     */
    public WizardExecutionController getWizardExecutionController() {
        return m_wfm.getWizardExecutionController();
    }

    /**
     * Checks different criteria to determine if a combined page view is available for a given metanode.
     * @param containerNodeID the {@link NodeID} of the metanode to check
     * @return true, if a view on the metanode is available, false otherwise
     */
    public boolean hasWizardPage(final NodeID containerNodeID) {
        return m_wfm.getWizardExecutionController().isSubnodeViewAvailable(containerNodeID);
    }

    /**
     * Creates a wizard page object from a given node id
     *
     * @param containerNodeID the node id to create the wizard page for, null if the current subnode in wizard execution is supposed to be created
     * @return a {@link JSONWebNodePage} object which can be used for serialization
     * @throws IOException if the layout of the wizard page can not be generated
     */
    public JSONWebNodePage createWizardPage(final NodeID containerNodeID) throws IOException {
        WizardExecutionController wec = m_wfm.getWizardExecutionController();
        WizardPageContent page = wec.getWizardPage(containerNodeID);
        // process layout
        JSONLayoutPage layout = new JSONLayoutPage();
        try {
            String lString = page.getLayoutInfo();
            if (lString != null && !lString.isEmpty()) {
                layout = getJSONLayoutFromSubnode(page.getPageNodeID(), page.getLayoutInfo());
            }
        } catch (IOException e) {
            throw new IOException("Layout for page could not be generated: " + e.getMessage(), e);
        }

        // process selection translators
        List<JSONSelectionTranslator> selectionTranslators = new ArrayList<JSONSelectionTranslator>();
        if (page.getHiLiteTranslators() != null) {
            for (HiLiteTranslator hiLiteTranslator : page.getHiLiteTranslators()) {
                if (hiLiteTranslator != null) {
                    selectionTranslators.add(new JSONSelectionTranslator(hiLiteTranslator));
                }
            }
        }
        if (page.getHiliteManagers() != null) {
            for (HiLiteManager hiLiteManager : page.getHiliteManagers()) {
                if (hiLiteManager != null) {
                    selectionTranslators.add(new JSONSelectionTranslator(hiLiteManager));
                }
            }
        }
        if (selectionTranslators.size() < 1) {
            selectionTranslators = null;
        }
        JSONWebNodePageConfiguration pageConfig = new JSONWebNodePageConfiguration(layout, null, selectionTranslators);

        Map<String, JSONWebNode> nodes = new HashMap<String, JSONWebNode>();
        for (@SuppressWarnings("rawtypes") Map.Entry<NodeIDSuffix, WizardNode> e : page.getPageMap().entrySet()) {
            WizardNode<?, ?> node = e.getValue();
            WebTemplate template =
                WizardExecutionController.getWebTemplateFromJSObjectID(node.getJavascriptObjectID());
            List<String> jsList = new ArrayList<String>();
            List<String> cssList = new ArrayList<String>();
            for (WebResourceLocator locator : template.getWebResources()) {
                if (locator.getType() == WebResourceType.JAVASCRIPT) {
                    jsList.add(locator.getRelativePathTarget());
                } else if (locator.getType() == WebResourceType.CSS) {
                    cssList.add(locator.getRelativePathTarget());
                }
            }
            JSONWebNode jsonNode = new JSONWebNode();
            jsonNode.setJavascriptLibraries(jsList);
            jsonNode.setStylesheets(cssList);
            jsonNode.setNamespace(template.getNamespace());
            jsonNode.setInitMethodName(template.getInitMethodName());
            jsonNode.setValidateMethodName(template.getValidateMethodName());
            jsonNode.setSetValidationErrorMethodName(template.getSetValidationErrorMethodName());
            jsonNode.setGetViewValueMethodName(template.getPullViewContentMethodName());
            jsonNode.setViewRepresentation((JSONViewContent)node.getViewRepresentation());
            jsonNode.setViewValue((JSONViewContent)node.getViewValue());
            nodes.put(e.getKey().toString(), jsonNode);
        }
        return new JSONWebNodePage(pageConfig, nodes);
    }

    /**
     * Creates a JSON string containing a wizard page from a given node id
     *
     * @param containerNodeID the node id to create the wizard page string for, null if the current subnode in wizard execution is supposed to be created
     * @return a JSON string containing the wizard page
     * @throws IOException if the layout of the wizard page can not be generated
     * @throws JsonProcessingException on serialization errors
     */
    public String createWizardPageString(final NodeID containerNodeID) throws IOException, JsonProcessingException {
        JSONWebNodePage jsonPage = createWizardPage(containerNodeID);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        return mapper.writeValueAsString(jsonPage);
    }

    private JSONLayoutPage getJSONLayoutFromSubnode(final NodeIDSuffix pageID, final String layoutInfo) throws IOException {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        JSONLayoutPage page = reader.readValue(layoutInfo);
        if (page != null && page.getRows() != null) {
            for (JSONLayoutRow row : page.getRows()) {
                setNodeIDInContent(row, pageID);
            }
        }
        return page;
    }

    private void setNodeIDInContent(final JSONLayoutContent content, final NodeIDSuffix pageID) {
        if (content instanceof JSONLayoutRow) {
            for (JSONLayoutColumn col : ((JSONLayoutRow)content).getColumns()) {
                for (JSONLayoutContent subContent : col.getContent()) {
                    setNodeIDInContent(subContent, pageID);
                }
            }
        } else if (content instanceof JSONLayoutViewContent) {
            JSONLayoutViewContent view = (JSONLayoutViewContent)content;
            String nodeIDString = view.getNodeID();
            if (pageID != null) {
                NodeIDSuffix layoutNodeID = pageID.createChild(Integer.parseInt(view.getNodeID()));
                nodeIDString = layoutNodeID.toString();
            }
            view.setNodeID(nodeIDString);
        }
    }

    /**
     * Applies a given map of workflow parameters to the current workflow
     *
     * @param parameterMap a map with parameter name as key and parameter string value as value
     * @throws InvalidSettingsException If a parameter name is not valid or a not uniquely defined in the workflow or if the parameter value does not validate.
     */
    public void applyWorkflowParameters(final Map<String, String> parameterMap) throws InvalidSettingsException {
        try (WorkflowLock lock = m_wfm.lock()) {
            if (parameterMap.size() > 0) {
                Map<String, ExternalNodeData> inputData = new HashMap<String, ExternalNodeData>(parameterMap.size());
                for (String key : parameterMap.keySet()) {
                    ExternalNodeDataBuilder dataBuilder = ExternalNodeData.builder(key);
                    dataBuilder.stringValue(parameterMap.get(key));
                    inputData.put(key, dataBuilder.build());
                }
                try {
                    //FIXME: This call should happen on the WizardExecutionController, once there is no potential version issues
                    m_wfm.setInputNodes(inputData);
                } catch (Exception ex) {
                    String errorPrefix = "Could not set workflow parameters: ";
                    String errorMessage = ex.getMessage();
                    if (!errorMessage.startsWith(errorPrefix)) {
                        errorMessage = errorPrefix + ex.getMessage();
                    }
                    throw new InvalidSettingsException(errorMessage, ex);
                }
            }
        }
    }

    public Map<String, ValidationError> validateViewValues(final Map<String, String> viewValues, final NodeID containerNodeId) throws IOException {
        try (WorkflowLock lock = m_wfm.lock()) {
            ObjectMapper mapper = new ObjectMapper();
            for (String key : viewValues.keySet()) {
                String content = mapper.writeValueAsString(viewValues.get(key));
                viewValues.put(key, content);
            }
            if (!viewValues.isEmpty()) {
                WizardExecutionController wec = m_wfm.getWizardExecutionController();
                return wec.validateViewValuesInPage(viewValues, containerNodeId);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    public void applyValidatedViewValues(final Map<String, String> viewValues, final NodeID containerNodeId, final boolean useAsDefault) throws IOException {
        try (WorkflowLock lock = m_wfm.lock()) {
            ObjectMapper mapper = new ObjectMapper();
            for (String key : viewValues.keySet()) {
                String content = mapper.writeValueAsString(viewValues.get(key));
                viewValues.put(key, content);
            }
            if (!viewValues.isEmpty()) {
                WizardExecutionController wec = m_wfm.getWizardExecutionController();
                wec.loadValuesIntoPage(viewValues, containerNodeId, false, useAsDefault);
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
        try (WorkflowLock lock = m_wfm.lock()) {
            ObjectMapper mapper = new ObjectMapper();

            for (String key : valueMap.keySet()) {
                String content = mapper.writeValueAsString(valueMap.get(key));
                valueMap.put(key, content);
            }
            Map<String, ValidationError> validationResults = null;
            if (!valueMap.isEmpty()) {
                WizardExecutionController wec = m_wfm.getWizardExecutionController();
                validationResults = wec.loadValuesIntoPage(valueMap, containerNodeId);
            }
            String jsonString = null;
            if (validationResults != null && !validationResults.isEmpty()) {
                jsonString = mapper.writeValueAsString(validationResults);
            }
            return jsonString;
        }
    }
}

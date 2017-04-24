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
 *   24 Feb 2017 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.wizard.AbstractWizardNodeView;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.WizardViewCreator;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.wizard.SinglePageManager;
import org.knime.js.core.JSONViewContent;
import org.knime.js.core.JSONWebNode;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.JSONWebNodePageConfiguration;
import org.knime.js.core.JavaScriptViewCreator;
import org.knime.workbench.editor2.subnode.SubnodeViewableModel.SubnodeViewValue;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class SubnodeViewableModel implements ViewableModel, WizardNode<JSONWebNodePage, SubnodeViewValue> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SubnodeViewableModel.class);

    private JSONWebNodePage m_page;
    private SubnodeViewValue m_value;

    private final SinglePageManager m_spm;
    private final SubNodeContainer m_container;
    private final String m_viewName;
    private final JavaScriptViewCreator<JSONWebNodePage, SubnodeViewValue> m_viewCreator;
    private String m_viewPath;
    private AbstractWizardNodeView<SubnodeViewableModel, JSONWebNodePage, SubnodeViewValue> m_view;
    private AtomicBoolean m_isReexecuteInProgress = new AtomicBoolean(false);
    private NodeStateChangeListener m_nodeStateChangeListener;

    /**
     * Creates a new instance of this viewable model
     * @param nodeContainer the subnode container
     * @param viewName the name of the view
     * @throws IOException on view model creation error
     *
     */
    public SubnodeViewableModel(final SubNodeContainer nodeContainer, final String viewName) throws IOException {
        m_viewName = viewName;
        m_viewCreator = new SubnodeWizardViewCreator();
        m_spm = SinglePageManager.of(nodeContainer.getParent());
        m_container = nodeContainer;
        createPageAndValue();
        m_nodeStateChangeListener = s -> onNodeStateChange();
        nodeContainer.addNodeStateChangeListener(m_nodeStateChangeListener);
    }


    /** Called by state listener on subnode container. */
    private void onNodeStateChange() {
        NodeContainerState nodeContainerState = m_container.getNodeContainerState();

        // only react on state changes when
        //  - no re-exec is ongoing
        //  - no irrelevant pre-exec -> queue -> post-exec step is ongoing.
        //    (ideally this should be removed but those state changes on the SNC are not protected by the workflow lock)
        if (!(m_isReexecuteInProgress.get() || nodeContainerState.isExecutionInProgress())) {

            boolean isCallModelChanged = true;
            SubnodeViewValue v = getViewValue();
            if (nodeContainerState.isExecuted()) {
                if (v == null) {
                    // node was just executed, i.e. view is open and user executes via "run" button in main application
                    try {
                        createPageAndValue();
                        assert m_value != null : "value supposed to be non-null on executed node";
                    } catch (IOException e) {
                        LOGGER.error("Creating view failed: " + e.getMessage(), e);
                        reset();
                    }
                } else {
                    // node was 're-executed', i.e. user clicked 'apply' button in view and subsequent
                    // reset->configured->executing events were swallowed as part of m_isReexecutionInProgress
                    if (m_view != null && v.equals(m_view.getLastRetrievedValue())) {
                        isCallModelChanged = false;
                    }
                }
            } else if (v != null) {
                reset(); // sets #getViewValue to null
            }
            if (m_view != null && isCallModelChanged) {
                m_view.callViewableModelChanged();
            }
        }
    }


    /**
     * Registers a view with this model, so it can be notified about model changed events.
     * If a view is already registered, the method will not update it.
     * @param view the view to register
     */
    public void registerView(final AbstractWizardNodeView<SubnodeViewableModel, JSONWebNodePage, SubnodeViewValue> view) {
        if (m_view == null) {
            m_view = view;
        }
    }

    private void createPageAndValue() throws IOException {
        m_page = m_spm.createWizardPage(m_container.getID());
        Map<String, String> valueMap = new HashMap<String, String>();
        ObjectMapper mapper = new ObjectMapper();
        for (Entry<String, JSONWebNode> entry : m_page.getWebNodes().entrySet()) {
            String value = mapper.writeValueAsString(entry.getValue().getViewValue());
            valueMap.put(entry.getKey(), value);
        }
        m_value = new SubnodeViewValue();
        m_value.setViewValues(valueMap);
    }

    private SubnodeViewValue createViewValue() throws IOException {
        Map<String, String> valueMap = m_spm.createWizardPageViewValueMap(m_container.getID());
        SubnodeViewValue value = new SubnodeViewValue();
        value.setViewValues(valueMap);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationError validateViewValue(final SubnodeViewValue viewContent) {
        try {
            Map<String, ValidationError> validationResult = m_spm.validateViewValues(viewContent.getViewValues(), m_container.getID());
            if (!validationResult.isEmpty()) {
                return new CollectionValidationError(validationResult);
            }
        } catch (IOException e) {
            logErrorAndReset("Validating view values for node " + m_container.getID() + " failed: ", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadViewValue(final SubnodeViewValue value, final boolean useAsDefault) {
        try {
            CheckUtils.checkState(m_container.getNodeContainerState().isExecuted(),
                "Node needs to be in executed state to apply new view values.");
            m_isReexecuteInProgress.set(true);
            try (WorkflowLock lock = m_container.getParent().lock()) {
                m_value = value;
                m_spm.applyValidatedValuesAndReexecute(value.getViewValues(), m_container.getID(), useAsDefault);
            } finally {
                m_isReexecuteInProgress.set(false);
                if (!m_container.getNodeContainerState().isExecutionInProgress()) {
                    // this happens if after the reset the execution can't be triggered, e.g. because #configure of
                    // a node rejects the current settings -> #onNodeStateChange has been called as part of the reset
                    // but was ignored due to the m_isReexecuteInProgress = true.
                    onNodeStateChange();
                }
            }
        } catch (IOException e) {
            logErrorAndReset("Loading view values for node " + m_container.getID() + " failed: ", e);
        }
    }

    private void logErrorAndReset(final String message, final Exception ex) {
        LOGGER.error(message + ex.getMessage(), ex);
        reset();
        if (m_view != null) {
            m_view.callViewableModelChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCurrentValue(final NodeSettingsWO content) { /* not used */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWebNodePage getViewRepresentation() {
        return m_page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeViewValue getViewValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONWebNodePage createEmptyViewRepresentation() {
        Map<String, JSONWebNode> emptyNode = new HashMap<String, JSONWebNode>();
        return new JSONWebNodePage(new JSONWebNodePageConfiguration(), emptyNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeViewValue createEmptyViewValue() {
        return new SubnodeViewValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJavascriptObjectID() {
        /* no id present */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewHTMLPath() {
        boolean create = false;
        if (m_viewPath == null || m_viewPath.isEmpty()) {
            // view is not created
            create = true;
        } else {
            // check if file still exists, create otherwise
            File viewFile = new File(m_viewPath);
            if (!viewFile.exists()) {
                create = true;
            }
        }
        if (create) {
            try {
                m_viewPath = m_viewCreator.createWebResources(m_viewName, getViewRepresentation(), null);
            } catch (IOException e) {
                LOGGER.error("Unable to create temporary web resource: " + e.getMessage(), e);
            }
        }
        return m_viewPath;
    }

    private void reset() {
        m_page = null;
        m_value = null;
        if (m_viewPath != null) {
            File viewFile = new File(m_viewPath);
            if (viewFile.exists() && !FileUtils.deleteQuietly(viewFile)) {
                LOGGER.warnWithFormat("Unable to delete temporary file \"%s\"", viewFile.getAbsolutePath());
            }
        }
        m_viewPath = null;
    }

    /**
     * Performs cleanup, call on view dispose.
     */
    public void discard() {
        reset();
        m_view = null;
        if (m_nodeStateChangeListener != null) {
            m_container.removeNodeStateChangeListener(m_nodeStateChangeListener);
        }
        m_nodeStateChangeListener = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WizardViewCreator<JSONWebNodePage, SubnodeViewValue> getViewCreator() {
        return m_viewCreator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard() {
        /* TODO no implementation possible at the moment
         * this needs to be configurable for nested subnodes, etc
         */
        return false;
    }

    //TODO decide if this belongs here. Could also be sth. like PageViewValue in org.knime.js.core
    static class SubnodeViewValue extends JSONViewContent {

        private Map<String, String> m_viewValues = new HashMap<String, String>();

        /**
         * @return the viewValues
         */
        @JsonAnyGetter
        public Map<String, String> getViewValues() {
            return m_viewValues;
        }

        /**
         * @param viewValues the viewValues to set
         */
        public void setViewValues(final Map<String, String> viewValues) {
            m_viewValues = viewValues;
        }

        @JsonAnySetter
        public void addViewValue(final String key, final Object value) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                m_viewValues.put(key, mapper.writeValueAsString(value));
            } catch (JsonProcessingException e) { /* do nothing */ }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void saveToNodeSettings(final NodeSettingsWO settings) { /* nothing to save */ }

        /**
         * {@inheritDoc}
         */
        @Override
        public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException { /* nothing to load */ }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            SubnodeViewValue other = (SubnodeViewValue)obj;
            if (!m_viewValues.keySet().equals(other.m_viewValues.keySet())) {
                return false;
            }
            EqualsBuilder builder = new EqualsBuilder();
            ObjectMapper mapper = new ObjectMapper();
            for (String key : m_viewValues.keySet()) {
                try {
                    // try deserializing and comparing generic JSON objects
                    JsonNode first = mapper.readTree(m_viewValues.get(key));
                    JsonNode second = mapper.readTree(other.m_viewValues.get(key));
                    // the following would be better but concrete view classes might not be visible here
                    /*JSONViewContent first = mapper.readValue(m_viewValues.get(key), JSONViewContent.class);
                    JSONViewContent second = mapper.readValue(other.m_viewValues.get(key), JSONViewContent.class);*/
                    builder.append(first, second);
                } catch (Exception e) {
                    LOGGER.debug("Can't compare JsonNode in #equals", e);
                    //compare strings on exception
                    builder.append(m_viewValues.get(key), other.m_viewValues.get(key));
                }
            }
            return builder.isEquals();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(m_viewValues)
                    .toHashCode();
        }

    }

    private static class SubnodeWizardViewCreator extends JavaScriptViewCreator<JSONWebNodePage, SubnodeViewValue> {

        public SubnodeWizardViewCreator() {
            super(null);
            setWebTemplate(createSubnodeWebTemplate());
        }

        private WebTemplate createSubnodeWebTemplate() {
            List<WebResourceLocator> locators = new ArrayList<WebResourceLocator>();
            String pluginName = "org.knime.js.core";
            boolean isDebug = isDebug();
            String requireJS = isDebug ? "org/knime/debug/require.js" : "org/knime/core/require.js";
            String jQuery = isDebug ? "js-lib/jQuery/jquery-1.11.3.js" : "js-lib/jQuery/jquery-1.11.3.min.js";
            String bsJS = isDebug ? "js-lib/bootstrap/3_3_6/debug/js/bootstrap.js" : "js-lib/bootstrap/3_3_6/min/js/bootstrap.min.js";
            String bsCSS = isDebug ? "js-lib/bootstrap/3_3_6/debug/css/bootstrap.css" : "js-lib/bootstrap/3_3_6/min/css/bootstrap.min.css";
            String iframeResizer = isDebug ? "org/knime/debug/iframeResizer/iframeResizer.js" : "org/knime/core/iframeResizer/iframeResizer.js";
            String pageBuilder = isDebug ? "org/knime/debug/knime_pagebuilder.js" : "org/knime/core/knime_pagebuilder.js";

            locators.add(new WebResourceLocator(pluginName, requireJS, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, jQuery, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, bsJS, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, bsCSS, WebResourceType.CSS));
            locators.add(new WebResourceLocator(pluginName, iframeResizer, WebResourceType.JAVASCRIPT));
            locators.add(new WebResourceLocator(pluginName, pageBuilder, WebResourceType.JAVASCRIPT));
            String namespace = "KnimePageLoader";
            return new DefaultWebTemplate(locators.toArray(new WebResourceLocator[0]), namespace, "init", "validate",
                "getPageValues", "setValidationError");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String createInitJSViewMethodCall(final JSONWebNodePage viewRepresentation, final SubnodeViewValue viewValue) {
            String jsonViewRepresentation = getViewRepresentationJSONString(viewRepresentation);
            StringBuilder builder = new StringBuilder();
            String escapedRepresentation = jsonViewRepresentation.replace("\\", "\\\\").replace("'", "\\'");
            String repParseCall = "var parsedRepresentation = JSON.parse('" + escapedRepresentation + "');";
            builder.append(repParseCall);
            String initMethod = getWebTemplate().getInitMethodName();
            String initCall = getNamespacePrefix() + initMethod + "(parsedRepresentation, null, null, " + isDebug() + ");";
            builder.append(initCall);
            return builder.toString();
        }
    }

    private static class CollectionValidationError extends ValidationError {

        private Map<String, String> m_errorMap;

        private CollectionValidationError(final Map<String, ValidationError> errorCollection) {
            m_errorMap = new HashMap<String, String>(errorCollection.size());
            for (Entry<String, ValidationError> entry : errorCollection.entrySet()) {
                m_errorMap.put(entry.getKey(), entry.getValue().getError());
            }
        }

        /** @return the error */
        @JsonProperty("error")
        public Map<String, String> getErrorMap() {
            return m_errorMap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @JsonIgnore
        public String getError() {
            if (m_errorMap == null || m_errorMap.isEmpty()) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNodeFactory factory = JsonNodeFactory.instance;
                ObjectNode sErrorMap = factory.objectNode();
                for (Entry<String, String> entry : m_errorMap.entrySet()) {
                    ObjectNode sSingleError = factory.objectNode();
                    sSingleError.set("error", factory.textNode(entry.getValue()));
                    sErrorMap.set(entry.getKey(), sSingleError);
                }
                return mapper.writeValueAsString(sErrorMap);
            } catch (JsonProcessingException e) {
                return "Validation errors present but could not be serialized: " + e.getMessage();
            }
        }

    }



}

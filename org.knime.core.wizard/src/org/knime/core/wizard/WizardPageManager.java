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
 *   20 Feb 2017 (albrecht): created
 */
package org.knime.core.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.dialog.ExternalNodeData.ExternalNodeDataBuilder;
import org.knime.core.node.interactive.SimpleErrorViewResponse;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.interactive.ViewResponseMonitor;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.wizard.DefaultViewRequestJob;
import org.knime.core.node.wizard.ViewRequestExecutor;
import org.knime.core.node.wizard.WizardViewRequestHandler;
import org.knime.core.node.wizard.WizardViewResponse;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WebResourceController;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;
import org.knime.core.node.workflow.WizardExecutionController;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.layout.bs.JSONLayoutPage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Utility class which handles serialization/deserialization of meta node or wizard views
 * executed in wizard execution.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public final class WizardPageManager extends AbstractPageManager implements ViewRequestExecutor<String>,
    WizardViewRequestHandler<SubnodeViewRequest, SubnodeViewResponse> {

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
        super(workflowManager);
    }

    /**
     * Returns the underlying {@link WebResourceController} instance.
     *<br><br>
     * WARNING: this method will most likely be removed, once functionality is completely encapsulated.
     *
     * @return
     * the underlying {@link WebResourceController} instance.
     * @noreference This method is not intended to be referenced by clients.
     */
    public WizardExecutionController getWizardExecutionController() {
        return getWorkflowManager().getWizardExecutionController();
    }

    /**
     * Creates a wizard page for the current subnode in wizard execution
     * @return a {@link JSONWebNodePage} object which can be used for serialization
     * @throws IOException if the layout of the wizard page can not be generated
     */
    public JSONWebNodePage createCurrentWizardPage() throws IOException {
        WizardExecutionController wec = getWizardExecutionController();
        WizardPageContent page = wec.getCurrentWizardPage();
        return createWizardPageInternal(page);
    }

    /**
     * Creates a JSON string containing a wizard page for the current subnode in wizard execution
     *
     * @return a JSON string containing the wizard page
     * @throws IOException if the layout of the wizard page can not be generated
     * @throws JsonProcessingException on serialization errors
     */
    public String createCurrentWizardPageString() throws IOException, JsonProcessingException {
        JSONWebNodePage jsonPage = createCurrentWizardPage();
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        return mapper.writeValueAsString(jsonPage);
    }

    /**
     * Applies a given map of workflow parameters to the current workflow
     *
     * @param parameterMap a map with parameter name as key and parameter string value as value
     * @throws InvalidSettingsException If a parameter name is not valid or a not uniquely defined in the workflow or if the parameter value does not validate.
     */
    public void applyWorkflowParameters(final Map<String, String> parameterMap) throws InvalidSettingsException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            if (parameterMap.size() > 0) {
                Map<String, ExternalNodeData> inputData = new HashMap<String, ExternalNodeData>(parameterMap.size());
                for (String key : parameterMap.keySet()) {
                    ExternalNodeDataBuilder dataBuilder = ExternalNodeData.builder(key);
                    dataBuilder.stringValue(parameterMap.get(key));
                    inputData.put(key, dataBuilder.build());
                }
                try {
                    //FIXME: This call should happen on the WizardExecutionController, once there is no potential version issues
                    getWorkflowManager().setInputNodes(inputData);
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

    /**
     * Applies a given map of view values to the current subnode in wizard execution.
     *
     * @param valueMap a map with {@link NodeIDSuffix} string as key and parsed view value as value
     * @return A JSON-serialized string containing the validation result, null if validation succeeded.
     * @throws IOException on JSON serialization errors
     */
    public String applyViewValuesToCurrentPage(final Map<String, String> valueMap) throws IOException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            Map<String, String> viewContentMap = validateValueMap(valueMap);
            Map<String, ValidationError> validationResults = null;
            if (!valueMap.isEmpty()) {
                WizardExecutionController wec = getWizardExecutionController();
                validationResults = wec.loadValuesIntoCurrentPage(viewContentMap);
            }
            return serializeValidationResult(validationResults);
        }
    }

    /**
     * @param wrappedRequest the JSON serialized request wrapping the actual request, which will be forwarded
     * and processed on the appropriate node
     * @param exec the execution monitor to set progress and check possible cancellation
     * @return a {@link CompletableFuture} wrapping the response JSON string. String may be null in case of error
     * @throws IOException on serialization/deserialization error
     * @throws ViewRequestHandlingException on processing error
     * @since 3.6
     */
    private CompletableFuture<SubnodeViewResponse> processViewRequestOnCurrentPage(final SubnodeViewRequest request,
        final ExecutionMonitor exec) throws IOException, ViewRequestHandlingException {
        try (WorkflowLock lock = getWorkflowManager().lock()) {
            WizardExecutionController wec = getWizardExecutionController();
            CompletableFuture<WizardViewResponse> future =
                wec.processViewRequestOnCurrentPage(request.getNodeID(), request.getJsonRequest(), exec);
            return future.thenApply(response -> serializeViewResponse(response))
                .thenApply(response -> buildSubnodeViewResponse(request, response));
        }
    }

    private SubnodeViewResponse buildSubnodeViewResponse(final SubnodeViewRequest request, final String jsonResponse) {
        return new SubnodeViewResponse(request, request.getNodeID(), jsonResponse);
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public String handleViewRequest(final String request) {
        ViewRequestRegistry registry = ViewRequestRegistry.getInstance();
        ExecutionMonitor exec = new ExecutionMonitor();
        DefaultViewRequestJob<SubnodeViewResponse> requestJob = null;
        int requestSequence = -1;
        try (WorkflowLock lock = getWorkflowManager().lock()){
            SubnodeViewRequest wrapperRequest = new SubnodeViewRequest();
            wrapperRequest.loadFromStream(new ByteArrayInputStream(request.getBytes("UTF-8")));
            requestSequence = wrapperRequest.getSequence();
            requestJob = new DefaultViewRequestJob<SubnodeViewResponse>(requestSequence, exec);
            requestJob.start(this, wrapperRequest);
            registry.addOrUpdateJob(requestJob);

            return serializeResponseMonitor(requestJob);
        } catch (Exception ex) {
            if (requestJob != null) {
                registry.removeJob(requestJob.getId());
            }
            if (requestSequence == -1) {
                try {
                    requestSequence = tryGetSequenceFromRequest(request);
                } catch (Exception ex2) { /* nothing can be done in this case */ }
            }
            return serializeResponseMonitor(new SimpleErrorViewResponse(requestSequence, ex.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public SubnodeViewResponse handleRequest(final SubnodeViewRequest request, final ExecutionMonitor exec)
        throws ViewRequestHandlingException, InterruptedException, CanceledExecutionException {
        try {
            CompletableFuture<SubnodeViewResponse> future = processViewRequestOnCurrentPage(request, exec);
            future.thenApply(response -> serializeViewResponse(response));
            return future.get();
        } catch (CompletionException ex1) {
            Throwable cause = ex1.getCause();
            if (cause != null) {
                if (cause instanceof InterruptedException) {
                    throw (InterruptedException)cause;
                } else if (cause instanceof ViewRequestHandlingException) {
                    throw (ViewRequestHandlingException)cause;
                }
            }
            throw new ViewRequestHandlingException(ex1.getMessage(), ex1);
        } catch (CancellationException ex2) {
            throw new CanceledExecutionException(ex2.getMessage());
        } catch (ExecutionException | IOException ex3) {
            throw new ViewRequestHandlingException(ex3.getMessage(), ex3);
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public SubnodeViewRequest createEmptyViewRequest() {
        return new SubnodeViewRequest();
    }

    private static String serializeResponseMonitor(
        final ViewResponseMonitor<? extends WizardViewResponse> monitor) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
            return mapper.writeValueAsString(monitor);
        } catch (JsonProcessingException ex) {
            //log error?
            return null;
        }
    }

    private static int tryGetSequenceFromRequest(final String jsonRequest) throws JsonProcessingException,
        IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(jsonRequest);
    JsonNode sequenceNode = node.get("sequence");
    if (sequenceNode != null) {
        int sequence = sequenceNode.asInt(-1);
        if (sequence >= 0) {
            return sequence;
        }
    }
    throw new IllegalArgumentException("JSON request string did not contain a 'sequence' field.");
}

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public String updateRequestStatus(final String monitorID) {
        ViewRequestRegistry registry = ViewRequestRegistry.getInstance();
        if (registry.isJobRegistered(monitorID)) {
            ViewResponseMonitor<? extends WizardViewResponse> monitor = registry.getJob(monitorID);
            if (monitor != null) {
                if (monitor.isCancelled() || monitor.isExecutionFailed()
                    || (monitor.isExecutionFinished() && monitor.isResponseAvailable())) {
                    registry.removeJob(monitor.getId());
                }
                return serializeResponseMonitor(monitor);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public void cancelRequest(final String monitorID) {
        ViewRequestRegistry registry = ViewRequestRegistry.getInstance();
        if (registry.isJobRegistered(monitorID)) {
            DefaultViewRequestJob<? extends WizardViewResponse> job = registry.getJob(monitorID);
            job.cancel();
            registry.removeJob(monitorID);
        }
    }
}

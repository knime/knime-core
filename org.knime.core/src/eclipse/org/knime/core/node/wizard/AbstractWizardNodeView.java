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
 *   23.09.2014 (Christian Albrecht, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.core.node.wizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.interactive.DefaultReexecutionCallback;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.InteractiveViewDelegate;
import org.knime.core.node.interactive.ReexecutionCallback;
import org.knime.core.node.interactive.SimpleErrorViewResponse;
import org.knime.core.node.interactive.ViewResponseMonitor;
import org.knime.core.node.interactive.ViewResponseMonitorUpdateEvent;
import org.knime.core.node.interactive.ViewResponseMonitorUpdateEvent.ViewResponseMonitorUpdateEventType;
import org.knime.core.node.interactive.ViewResponseMonitorUpdateEvent.ViewResponseMonitorUpdateListener;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @param <T> requires a {@link NodeModel} implementing {@link WizardNode} as well
 * @param <REP> the {@link WebViewContent} implementation used as view representation
 * @param <VAL> the {@link WebViewContent} implementation used as view value
 * @since 2.11
 */
public abstract class AbstractWizardNodeView<T extends ViewableModel & WizardNode<REP, VAL>,
    REP extends WebViewContent, VAL extends WebViewContent> extends AbstractNodeView<T>
    implements InteractiveView<T, REP, VAL>, ViewRequestExecutorPushEnabled<String> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractWizardNodeView.class);

    private static final String EXT_POINT_ID = "org.knime.core.WizardNodeView";

    private final InteractiveViewDelegate<VAL> m_delegate;

    private AtomicReference<VAL> m_lastRetrievedValue = new AtomicReference<VAL>();

    private Map<String, ViewResponseMonitor<? extends WizardViewResponse>> m_viewRequestMap;

    /**
     * Label for discard option.
     *
     * @since 3.4
     */
    protected final static String DISCARD_LABEL = "Discard Changes";

    /**
     * Description text for discard option.
     *
     * @since 3.4
     */
    protected final static String DISCARD_DESCRIPTION = "Discards any changes made and closes the view.";

    /**
     * Label for apply temporarily option.
     *
     * @since 3.4
     */
    protected final static String APPLY_LABEL = "Apply settings temporarily";

    /**
     * Description text template for apply temporarily option.
     *
     * @since 3.4
     */
    protected final static String APPLY_DESCRIPTION_FORMAT = "Applies the current view settings to the node%s"
        + " and triggers a re-execute of the node. This option will not override the default node settings "
        + "set in the dialog. Changes will be lost when the node is reset.";

    /**
     * Label for apply as new default option.
     *
     * @since 3.4
     */
    protected final static String APPLY_DEFAULT_LABEL = "Apply settings as new default";

    /**
     * Description text template for apply as new default option.
     *
     * @since 3.4
     */
    protected final static String APPLY_DEFAULT_DESCRIPTION_FORMAT =
        "Applies the current view settings as the new default node settings%s"
            + " and triggers a re-execute of the node. This option will override the settings set in the node dialog "
            + "and changes made will remain applied after a node reset.";

    /**
     * @param nodeModel
     * @since 3.4
     */
    protected AbstractWizardNodeView(final T nodeModel) {
        super(nodeModel, true);
        m_delegate = new InteractiveViewDelegate<VAL>();
        m_viewRequestMap = new HashMap<String, ViewResponseMonitor<? extends WizardViewResponse>>();
    }

    @Override
    public void setWorkflowManagerAndNodeID(final WorkflowManager wfm, final NodeID id) {
        m_delegate.setWorkflowManagerAndNodeID(wfm, id);
    }

    @Override
    public boolean canReExecute() {
        return m_delegate.canReExecute();
    }

    /**
     * @since 2.10
     */
    @Override
    public void triggerReExecution(final VAL value, final boolean useAsNewDefault, final ReexecutionCallback callback) {
        m_delegate.triggerReExecution(value, useAsNewDefault, callback);
    }

    /**
     * @since 3.4
     */
    public void callViewableModelChanged() {
        try {
            // call model changed on concrete implementation
            modelChanged();
        } catch (NullPointerException npe) {
            LOGGER.coding("AbstractWizardNodeView.modelChanged() causes "
                + "NullPointerException during notification of a " + "changed model, reason: " + npe.getMessage(), npe);
        } catch (Throwable t) {
            LOGGER.error("AbstractWizardNodeView.modelChanged() causes an error "
                + "during notification of a changed model, reason: " + t.getMessage(), t);
        }
    }

    /**
     * @return the model
     * @since 3.4
     */
    protected final WizardNode<REP, VAL> getModel() {
        return super.getViewableModel();
    }

    /**
     * @return The current html file object.
     */
    protected File getViewSource() {
        String viewPath = getModel().getViewHTMLPath();
        if (viewPath != null && !viewPath.isEmpty()) {
            return new File(viewPath);
        }
        return null;
    }

    /**
     * @return a map of all currently active view requests, never null
     * @since 3.7
     */
    protected Map<String, ViewResponseMonitor<? extends WizardViewResponse>> getViewRequestMap() {
        return m_viewRequestMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void callCloseView() {
        cancelOutstandingViewRequests();
        closeView();
    }

    /**
     * Cancel all view requests that have beein initiated by this view and are still running
     * @since 4.0
     */
    protected synchronized void cancelOutstandingViewRequests() {
        m_viewRequestMap.values().forEach(job -> job.cancel());
        m_viewRequestMap.clear();
    }

    /**
     * @return The node views creator instance.
     */
    protected WizardViewCreator<REP, VAL> getViewCreator() {
        return getModel().getViewCreator();
    }

    /**
     * Called on view close.
     */
    protected abstract void closeView();

    /**
     * @return the lastRetrievedValue
     * @since 3.4
     */
    public VAL getLastRetrievedValue() {
        return m_lastRetrievedValue.get();
    }

    /**
     * @param lastRetrievedValue the lastRetrievedValue to set
     * @since 3.4
     */
    protected void setLastRetrievedValue(final VAL lastRetrievedValue) {
        m_lastRetrievedValue.set(lastRetrievedValue);
    }

    /**
     * @param useAsDefault true if changed values are supposed to be applied as new node default, false otherwise
     * @return true if apply was successful, false otherwise
     * @since 3.4
     */
    protected boolean applyTriggered(final boolean useAsDefault) {
        if (!viewInteractionPossible() || !checkSettingsChanged()) {
            return true;
        }
        boolean valid = validateCurrentValueInView();
        if (valid) {
            String jsonString = retrieveCurrentValueFromView();
            if (jsonString == null || jsonString.equals("{}")) {
                return false;
            }
            try {
                VAL viewValue = getModel().createEmptyViewValue();
                viewValue.loadFromStream(new ByteArrayInputStream(jsonString.getBytes(Charset.forName("UTF-8"))));
                setLastRetrievedValue(viewValue);
                ValidationError error = getModel().validateViewValue(viewValue);
                if (error != null) {
                    showValidationErrorInView(error.getError());
                    return false;
                }
                if (getModel() instanceof NodeModel) {
                    triggerReExecution(viewValue, useAsDefault, new DefaultReexecutionCallback());
                } else {
                    getModel().loadViewValue(viewValue, useAsDefault);
                }

                return true;
            } catch (Exception e) {
                LOGGER.error("Could not set error message or trigger re-execution: " + e.getMessage(), e);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Check if the view value represented in the currently open view has changed from the view value represented in the
     * node model.
     *
     * @return true if the settings have changed, false otherwise or if status cannot be determined
     * @since 3.4
     */
    protected boolean checkSettingsChanged() {
        if (!viewInteractionPossible()) {
            return false;
        }
        String jsonString = retrieveCurrentValueFromView();
        if (jsonString == null) {
            // no view value present in view
            return false;
        }
        try {
            VAL viewValue = getModel().createEmptyViewValue();
            viewValue.loadFromStream(new ByteArrayInputStream(jsonString.getBytes(Charset.forName("UTF-8"))));
            VAL currentViewValue = getModel().getViewValue();
            if (currentViewValue != null) {
                return !currentViewValue.equals(viewValue);
            }
        } catch (Exception e) {
            LOGGER.error("Could not create view value for comparison: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Query if an interaction with the concrete view is possible.
     *
     * @return true, if interaction is possible, false otherwise
     * @since 3.4
     */
    protected abstract boolean viewInteractionPossible();

    /**
     * Execute JavaScript code in view to determine if the current settings validate.
     *
     * @return true, if validation succeeds, false otherwise
     * @since 3.4
     */
    protected abstract boolean validateCurrentValueInView();

    /**
     * Execute JavaScript code in view to retrieve the current view settings.
     *
     * @return the JSON serialized view value string
     * @since 3.4
     */
    protected abstract String retrieveCurrentValueFromView();

    /**
     * Execute JavaScript code in view to display a validation error.
     *
     * @param error the error to display
     * @since 3.4
     */
    protected abstract void showValidationErrorInView(String error);

    /**
     * Called when a close dialog is supposed to be shown with options on how to deal with changed settings in the view.
     *
     * @return true, if discard is chosen or a subsequent apply was successful, false otherwise
     * @since 3.4
     */
    protected boolean showCloseDialog() {
        String title = "View settings changed";
        String message = "View settings have changed. Please choose one of the following options:";
        return showApplyOptionsDialog(true, title, message);
    }

    /**
     * Called when an apply dialog (temporary or default apply) is supposed to be shown.
     *
     * @return true, if a subsequent apply was successful, false otherwise
     * @since 3.4
     */
    protected boolean showApplyDialog() {
        String title = "Apply view settings";
        String message = "Please choose one of the following options:";
        return showApplyOptionsDialog(false, title, message);
    }

    /**
     * Displays a dialog to ask user how to handle settings changed in view.
     *
     * @param showDiscardOption true, if discard option is to be displayed, false otherwise
     * @param title the title of the dialog
     * @param message the message of the dialog
     * @return true, if discard is chosen or a subsequent apply was successful, false otherwise
     * @since 3.4
     */
    protected abstract boolean showApplyOptionsDialog(final boolean showDiscardOption, final String title,
        final String message);

    /**
     *
     * @param jsonRequest the json serialized request object string
     * @return true, if the request could be processed correctly, false otherwise
     * @since 3.7
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final String handleViewRequest(final String jsonRequest) {
        ExecutionMonitor overallExec = new ExecutionMonitor();
        WizardNode<REP, VAL> model = getModel();
        int requestSequence = -1;
        if (!(model instanceof WizardViewRequestHandler)) {
            try {
                requestSequence = tryGetSequenceFromRequest(jsonRequest);
            } catch (Exception ex) { /* nothing can be done in this case */ }
            String errorMessage = "Node model can not handle view requests. Possible implementation error.";
            return serializeResponseMonitor(new SimpleErrorViewResponse(requestSequence, errorMessage));
        }
        LOGGER.debug("Received request from view: " + jsonRequest);
        WizardViewRequest req = ((WizardViewRequestHandler)model).createEmptyViewRequest();
        final String errorString = "View request failed: ";
        ViewResponseMonitor<? extends WizardViewResponse> responseMonitor = null;
        try {
            req.loadFromStream(new ByteArrayInputStream(jsonRequest.getBytes(Charset.forName("UTF-8"))));
            requestSequence = req.getSequence();

            responseMonitor = WizardViewRequestRunner.run((WizardViewRequestHandler)model, req, overallExec);
            final String requestID = responseMonitor.getId();
            m_viewRequestMap.put(requestID, responseMonitor);
            final ViewResponseMonitor<? extends WizardViewResponse> monitor = responseMonitor;
            responseMonitor.addUpdateListener(new ViewResponseMonitorUpdateListener() {

                @Override
                public void monitorUpdate(final ViewResponseMonitorUpdateEvent event) {
                    ViewResponseMonitorUpdateEventType type = event.getType();
                    pushRequestUpdate(serializeResponseMonitor(monitor));
                    if (ViewResponseMonitorUpdateEventType.STATUS_UPDATE == type) {
                        if (monitor.isExecutionFinished() && monitor.isResponseAvailable()) {
                            overallExec.setProgress(1);
                            overallExec.setMessage((String)null);
                            if (m_viewRequestMap.containsKey(requestID)) {
                                respondToViewRequest(monitor.getResponse().get());
                                m_viewRequestMap.remove(requestID);
                            }
                        }
                        else if (monitor.isExecutionFailed() || monitor.isCancelled()) {
                            m_viewRequestMap.remove(requestID);
                        }
                    }
                }
            });
            return serializeResponseMonitor(responseMonitor);
        } catch (Exception ex) {
            LOGGER.error(errorString + ex, ex);
            if (responseMonitor != null) {
                m_viewRequestMap.remove(responseMonitor.getId());
            }
            if (requestSequence == -1) {
                try {
                    requestSequence = tryGetSequenceFromRequest(jsonRequest);
                } catch (Exception ex2) { /* nothing can be done in this case */ }
            }
            return serializeResponseMonitor(new SimpleErrorViewResponse(requestSequence, ex.getMessage()));
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
     * @since 3.7
     */
    @Override
    public String updateRequestStatus(final String monitorID) {
        ViewResponseMonitor<? extends WizardViewResponse> monitor = m_viewRequestMap.get(monitorID);
        if (monitor != null) {
            if (monitor.isCancelled() || monitor.isExecutionFailed()
                || (monitor.isExecutionFinished() && monitor.isResponseAvailable())) {
                m_viewRequestMap.remove(monitor.getId());
            }
            return serializeResponseMonitor(monitor);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public void cancelRequest(final String monitorID) {
        //try to get by id
        ViewResponseMonitor<? extends WizardViewResponse> monitor = m_viewRequestMap.get(monitorID);
        if (monitor == null) {
            //try to get by request sequence, this might happen when the view can only push but not return
            //the id on an initial polling request
            List<ViewResponseMonitor<? extends WizardViewResponse>> jobList = m_viewRequestMap.entrySet()
                .stream()
                .filter(e -> monitorID.equals(Integer.toString(e.getValue().getRequestSequence())))
                .map(e -> e.getValue())
                .collect(Collectors.toList());
            if (jobList.size() > 0) {
                monitor = jobList.get(0);
            }
        }
        if (monitor != null) {
            LOGGER.debug("Cancelling view request " + monitor.getId());
            monitor.cancel();
            m_viewRequestMap.remove(monitorID);
        }
    }


    private static String serializeResponseMonitor(
        final ViewResponseMonitor<? extends WizardViewResponse> monitor) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
            return mapper.writeValueAsString(monitor);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Error serializing response monitor object: " + ex.getMessage(), ex);
            return null;
        }
    }

    private final void respondToViewRequest(final WizardViewResponse response) {
        try (OutputStream stream = response.saveToStream()) {
            if (stream instanceof ByteArrayOutputStream) {
                String responseString = ((ByteArrayOutputStream)stream).toString("UTF-8");
                respondToViewRequest(responseString);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not update view: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public abstract void respondToViewRequest(final String response);

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public abstract void pushRequestUpdate(final String monitor);

    /**
     * Queries extension point for additional {@link AbstractWizardNodeView} implementations.
     *
     * @return A list with all registered view implementations.
     */
    @SuppressWarnings("unchecked")
    public static List<WizardNodeViewExtension> getAllWizardNodeViews() {
        List<WizardNodeViewExtension> viewExtensionList = new ArrayList<WizardNodeViewExtension>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        assert point != null : "Invalid extension point id: " + EXT_POINT_ID;

        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement viewElement : elements) {
                String viewClassName = viewElement.getAttribute("viewClass");
                String viewName = viewElement.getAttribute("name");
                String viewDesc = viewElement.getAttribute("description");
                Class<AbstractWizardNodeView<?, ?, ?>> viewClass;
                try {
                    viewClass = (Class<AbstractWizardNodeView<?, ?, ?>>)Platform
                            .getBundle(viewElement.getDeclaringExtension().getContributor().getName())
                            .loadClass(viewClassName);
                    viewExtensionList.add(new WizardNodeViewExtension(viewClass, viewName, viewDesc));
                } catch (ClassNotFoundException ex) {
                    NodeLogger.getLogger(AbstractWizardNodeView.class).error(
                        "Could not find implementation for " + viewClassName, ex);
                }
            }
        }
        return viewExtensionList;
    }

    /**
     * Implementation of a WizardNodeView from extension point.
     *
     * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
     */
    public static class WizardNodeViewExtension {

        private Class<AbstractWizardNodeView<?, ?, ?>> m_viewClass;

        private String m_viewName;

        private String m_viewDescription;

        /**
         * Creates a new WizardNodeViewExtension.
         *
         * @param viewClass the class holding the view implementation
         * @param viewName the name of the view
         * @param viewDescription the optional description of the view
         */
        public WizardNodeViewExtension(final Class<AbstractWizardNodeView<?, ?, ?>> viewClass, final String viewName,
            final String viewDescription) {
            m_viewClass = viewClass;
            m_viewName = viewName;
            m_viewDescription = viewDescription;
        }

        /**
         * @return the viewClass
         */
        public
            Class<AbstractWizardNodeView<? extends ViewableModel, ? extends WebViewContent, ? extends WebViewContent>>
            getViewClass() {
            return m_viewClass;
        }

        /**
         * @return the viewName
         */
        public String getViewName() {
            return m_viewName;
        }

        /**
         * @return the viewDescription
         */
        public String getViewDescription() {
            return m_viewDescription;
        }
    }

}

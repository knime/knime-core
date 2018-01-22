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
 *   18 Apr 2018 (albrecht): created
 */
package org.knime.core.node.wizard;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.interactive.ViewRequestHandlingException;
import org.knime.core.node.interactive.ViewRequestJob;
import org.knime.core.node.interactive.ViewResponse;
import org.knime.core.node.interactive.ViewResponseMonitor;
import org.knime.core.node.workflow.NodeContext;

/**
 * Default implementation of a job which represents the processing of a view request in an asynchronous fashion.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @param <RES> the actual class of the response implementation to be generated
 * @since 3.6
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DefaultViewRequestJob<RES extends WizardViewResponse> implements ViewResponseMonitor<RES>,
    ViewRequestJob<RES> {

    private String m_id;
    private int m_requestSequence;
    private CompletableFuture<RES> m_future;
    private RES m_response;
    private boolean m_executionStarted;
    private boolean m_executionFinished;
    private boolean m_executionFailed;
    private String m_errorMessage;

    private final ExecutionMonitor m_monitor;
    private final List<ViewResponseMonitorUpdateListener> m_listeners;
    private final DefaultViewRequestJob<RES> m_job;
    private final NodeContext m_context;
    private final Object m_block = new Object();

    /**
     * Creates a new job instance.
     *
     * @param sequence the sequence of the corresponding request, used for identification in the view implementation and
     *            ordering
     * @param exec an execution monitor used to query progress and possible cancellation
     */
    public DefaultViewRequestJob(final int sequence, final ExecutionMonitor exec) {
        m_id = UUID.randomUUID().toString();
        m_requestSequence = sequence;
        m_monitor = exec;
        m_listeners = new ArrayList<ViewResponseMonitorUpdateListener>(1);
        m_job = this;
        m_context = NodeContext.getContext();
    }

    /**
     * Adds a listener to be notified about changes to the processing status of this job.
     * @param listener the listener to add
     */
    public void addUpdateListener(final ViewResponseMonitorUpdateListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Removes a listener so that it receives no more update events.
     * @param listener the listener to remove
     */
    public void removeUpdateListener(final ViewResponseMonitorUpdateListener listener) {
        m_listeners.remove(listener);
    }

    /**
     * Removes all registered listeners so that they do not receive any more update events.
     */
    public void removeAllUpdateListeners() {
        m_listeners.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <REQ extends WizardViewRequest> void start(final WizardViewRequestHandler<REQ, RES> handler,
        final REQ request) {
        m_requestSequence = request.getSequence();
        ViewResponseMonitorUpdateEvent pEvent =
                new ViewResponseMonitorUpdateEvent(m_job, ViewResponseMonitorUpdateEventType.PROGRESS_UPDATE);
        m_monitor.getProgressMonitor().addProgressListener((event) -> {
            m_listeners.forEach((listener) -> listener.monitorUpdate(pEvent));
        });
        m_future = CompletableFuture.supplyAsync(() -> {
            try {
                NodeContext.pushContext(m_context);
                m_executionStarted = true;
                return handler.handleRequest(request, m_monitor);
            } catch (ViewRequestHandlingException | InterruptedException | CanceledExecutionException ex) {
                synchronized (m_block) {
                    m_executionFailed = true;
                    m_errorMessage = ex.getMessage();
                    return null;
                }
            } finally {
                NodeContext.removeLastContext();
            }
        });
        m_future.thenAccept((response) -> {
            synchronized (m_block) {
                boolean success = response != null;
                m_response = response;
                m_executionFinished = success;
                m_executionFailed = !success;
            }
            ViewResponseMonitorUpdateEvent sEvent =
                new ViewResponseMonitorUpdateEvent(m_job, ViewResponseMonitorUpdateEventType.STATUS_UPDATE);
            m_listeners.forEach((listener) -> listener.monitorUpdate(sEvent));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        if (m_future != null && !m_future.isDone()) {
            m_future.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return m_id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRequestSequence() {
        return m_requestSequence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Double> getProgress() {
        return Optional.ofNullable(m_monitor.getProgressMonitor().getProgress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getProgressMessage() {
        return Optional.ofNullable(m_monitor.getProgressMonitor().getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return m_future.isCancelled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RES> getResponse() {
        synchronized(m_block) {
            return Optional.ofNullable(m_response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResponseAvailable() {
        synchronized(m_block) {
            return m_response != null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionStarted() {
        synchronized(m_block) {
            return m_executionStarted;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionFinished() {
        synchronized(m_block) {
            return m_executionFinished;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecutionFailed() {
        synchronized(m_block) {
            return m_executionFailed;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(m_errorMessage);
    }

    /**
     * Interface for listeners wanting to be informed about changes to the execution status of view
     * request jobs.
     *
     * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
     */
    public static interface ViewResponseMonitorUpdateListener extends EventListener {

        /**
         * Triggered when a status change is registered in the request job.
         * @param event an event object yielding additional information
         */
        public void monitorUpdate(ViewResponseMonitorUpdateEvent event);

    }

    /**
     * Enum listing the types of view response monitor update events.
     *
     * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
     */
    public enum ViewResponseMonitorUpdateEventType {

            /**
             * Update events of this type indicate that only the progress or progress message of a running request job
             * has changed. Query the source job object for the actual changes.
             */
            PROGRESS_UPDATE,

            /**
             * Update events of this type indicate that the status of the request job has changed. Query the source
             * job object for the actual changes.
             */
            STATUS_UPDATE

    }

    /**
     *
     * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("serial")
    public static class ViewResponseMonitorUpdateEvent extends EventObject {

        private ViewResponseMonitorUpdateEventType m_type;

        /**
         * @param sourceMonitor the source of this update event
         * @param type the type of update event
         *
         */
        public ViewResponseMonitorUpdateEvent(final ViewResponseMonitor<? extends ViewResponse> sourceMonitor,
            final ViewResponseMonitorUpdateEventType type) {
            super(sourceMonitor);
            m_type = type;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public ViewResponseMonitor<? extends ViewResponse> getSource() {
            return (ViewResponseMonitor<? extends ViewResponse>)super.getSource();
        }

        /**
         * @return the type of update event
         */
        public ViewResponseMonitorUpdateEventType getType() {
            return m_type;
        }


    }
}

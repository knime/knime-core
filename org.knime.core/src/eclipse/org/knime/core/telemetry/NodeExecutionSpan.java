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
 *   21 Jun 2022 (carlwitt): created
 */
package org.knime.core.telemetry;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * This span covers a node execution within a {@link WorkflowSessionSpan}.
 *
 * It contains subspans for the execution of the node model and the postprocessing of the node model output.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class NodeExecutionSpan extends OpenTelemetrySpanHolder {

    public static final UnaryOperator<String> OPERATION_NAME = nodeName -> String.format("node/%s", nodeName);

    public static final UnaryOperator<String> SUB_MODEL_EXECUTE_OPERATION_NAME = nodeName -> String.format("node/%s/model/execute", nodeName);

    public static final UnaryOperator<String> SUB_MODEL_POSTPROCESS_OPERATION_NAME = nodeName -> String.format("node/%s/model/postprocess", nodeName);


    /**
     * @see #setNodeId(String)
     */
    public static final String SPAN_ATTRIBUTE_NODE_ID = "node.id";

    /**
     * @see #setNodeContext(String)
     */
    public static final String SPAN_ATTRIBUTE_NODE_CONTEXT = "node.context";

    /**
     * @see #setNodeFactory(String)
     */
    public static final String SPAN_ATTRIBUTE_NODE_FACTORY = "node.factory";

    /**
     * @see #startExecution(PortObject[])
     */
    public static final String SPAN_ATTRIBUTE_DATA_IN_ROWS = "data.inport-%s.rows";

    /**
     * @see #submittedToJobManager(String)
     */
    public static final String SPAN_EVENT_SUBMITTED_TO_JOB_MANAGER = "submittedToJobManager";

    /**
     * @see #submittedToJobManager(String)
     */
    public static final String SPAN_EVENT_SUBMITTED_TO_JOB_MANAGER_ATTRIBUTE_CLASS = "jobManagerClass";

    /**
     * Used to build the operation names, see {@link #OPERATION_NAME}, {@link #SUB_MODEL_EXECUTE_OPERATION_NAME}, etc.
     */
    private final String m_nodeName;

    /**
     * This is a subspan of this span and covers the period from the start of the node model execution until completion
     * or abnormal termination. The span is created when calling {@link #startExecution(PortObject[])}. This is expected
     * to be always present, but in case of exception prior to node model execution it might not.
     */
    private Optional<Span> m_executionSpan = Optional.empty();

    /**
     * This is a subspan of this span and it follows the {@link #m_executionSpan}. It covers the post-processing of the
     * node models's output data.
     */
    private Optional<Span> m_postProcessingSpan = Optional.empty();

    public NodeExecutionSpan(final String nodeName, final Span parentSpan) {
        super(OPERATION_NAME.apply(nodeName), parentSpan);
        m_nodeName = nodeName;
    }

    /**
     * @param nodeId the identifier of the node in the containing workflow
     */
    public void setNodeId(final String nodeId) {
        getSpan().setAttribute(NodeExecutionSpan.SPAN_ATTRIBUTE_NODE_ID, nodeId);
    }

    /**
     * @param nodeContext identifies the node execution context. This is typically derived from the NodeContainer that
     *            is used to execute the node but can be also derived from something else, e.g. in case of the remote
     *            workflow editor.
     * @return this instance
     */
    public NodeExecutionSpan setNodeContext(final String nodeContext) {
        getSpan().setAttribute(NodeExecutionSpan.SPAN_ATTRIBUTE_NODE_CONTEXT, nodeContext);
        return this;
    }

    /**
     * @param factoryName
     * @return this instance
     */
    public NodeExecutionSpan setNodeFactory(final String factoryName) {
        getSpan().setAttribute(NodeExecutionSpan.SPAN_ATTRIBUTE_NODE_FACTORY, factoryName);
        return this;
    }

    /**
     * Sets the status of the span to error if not successfully executed.
     *
     * @param success true if the node execution finished successfully
     */
    public void setSuccess(final boolean success) {
        if (!success) {
            getSpan().setStatus(StatusCode.ERROR);
        }
    }

    /**
     * What happens with the span depends on the configured {@link SpanProcessor}(s). It may be sent immediately or
     * collected to be sent as a batch later.
     */
    @Override
    public void end() {
        if (m_executionSpan.map(Span::isRecording).orElse(false)) {
            m_executionSpan.get().end();
        }
        if (m_postProcessingSpan.map(Span::isRecording).orElse(false)) {
            m_executionSpan.get().end();
        }
        getSpan().end();
    }

    /**
     * Creates an event in the span that marks the beginning of the {@link NodeModel}'s execution.
     *
     * @param data the input data for the node model
     */
    public void startExecution(final PortObject[] data) {
        var attributesBuilder = NodeContextTracer.rowCountsToAttributes(data,
            portIndex -> String.format(SPAN_ATTRIBUTE_DATA_IN_ROWS, portIndex));

        m_executionSpan = Optional.of(subSpanBuilder(SUB_MODEL_EXECUTE_OPERATION_NAME.apply(m_nodeName))//
            .setAllAttributes(attributesBuilder.build())//
            .startSpan());
    }

    /**
     * Creates an event in the span that marks the end of the {@link NodeModel}'s execution
     *
     * @param outData the data computed by the node model
     */
    public void finishExecution(final PortObject[] outData) {
        m_executionSpan
            .orElseThrow(
                () -> new IllegalStateException("Cannot record execution finish: Execution start was never recorded."))
            .end();
    }

    /**
     * @param outData
     */
    public void startPostProcessing(final PortObject[] outData) {
        m_postProcessingSpan = Optional.of(subSpanBuilder(SUB_MODEL_POSTPROCESS_OPERATION_NAME.apply(m_nodeName))//
            .startSpan());
    }

    /**
     * @param outData
     */
    public void finishPostProcessing(final PortObject[] outData) {
        m_postProcessingSpan.orElseThrow(() -> new IllegalStateException(
            "Cannot record postprocessing finish: Postprocessing start was never recorded.")).end();
    }

    /**
     * Signals that the execution of the node was cancelled by the user.
     */
    public void executionCanceled() {
        m_executionSpan.orElseThrow(IllegalStateException::new).setStatus(StatusCode.OK, "Canceled");
    }

    /**
     * @param e
     */
    public void setExecutionException(final Exception e) {
        m_executionSpan.orElseThrow(IllegalStateException::new).setStatus(StatusCode.ERROR).setAttribute("Exception",
            e.toString());
    }

    /**
     * @param jobManagerClassName
     */
    public void submittedToJobManager(final String jobManagerClassName) {
        getSpan().addEvent(SPAN_EVENT_SUBMITTED_TO_JOB_MANAGER, Attributes
            .of(AttributeKey.stringKey(SPAN_EVENT_SUBMITTED_TO_JOB_MANAGER_ATTRIBUTE_CLASS), jobManagerClassName));

    }

}
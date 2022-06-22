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

import java.util.UUID;
import java.util.function.UnaryOperator;

import org.knime.core.node.workflow.WorkflowContext;

import io.opentelemetry.api.trace.Span;

/**
 * This span covers the time in which a workflow is open. It is part of a {@link KnimeSessionSpan}.
 *
 * It begins with a span that covers the workflow loading process and then contains an arbitrary number of
 * {@link NodeExecutionSpan}s.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class WorkflowSessionSpan extends OpenTelemetrySpanHolder {

    /**
     * Generates the name of the span from the workflow name. The span name marks this as a {@link WorkflowSessionSpan}
     * but also states the workflow name in order to be able to aggregate information over multiple executions of the
     * same workflow.
     */
    public static final UnaryOperator<String> OPERATION_NAME =
        workflowName -> String.format("workflow/%s", workflowName);

    /**
     * Optional attribute on the span.
     *
     * @see #setWorkflowName(String)
     */
    public static final String SPAN_ATTRIBUTE_WORKFLOW_NAME = "workflow.name";

    /**
     * The subspan for loading the workflow class/group/type is identified by this string. This should be general enough
     * to generate insights when aggregating over all spans with this operation identifier.
     */
    public static final UnaryOperator<String> WORKFLOW_LOAD_OPERATION_NAME = workflowName -> String.format("workflow/%s/load", workflowName);


    /**
     * This is a subspan of this span that covers the time it takes to load the workflow. It is created and started upon
     * construction of this {@link WorkflowSessionSpan} and ended upon {@link #finishedLoading()}.
     */
    private final Span m_loadSpan;

    private final String m_workflowName;

    /**
     * @param workflowName used to create the span name, see {@link #OPERATION_NAME}
     */
    public WorkflowSessionSpan(final String workflowName) {
        super(OPERATION_NAME.apply(workflowName), OpenTelemetryUtil.getKnimeSessionSpan().getOtelSpan());
        m_workflowName = workflowName;

        // assume that workflow loading begins immediately on creating the workflow session span
        m_loadSpan = subSpanBuilder(WORKFLOW_LOAD_OPERATION_NAME.apply(m_workflowName)).startSpan();
    }

    /**
     * Adds an attribute to this workflow session span. This might not be called if no appropriate workflow name is
     * available.
     *
     * @param name the name of the workflow
     * @return this for fluent API
     */
    public WorkflowSessionSpan setWorkflowName(final String name) {
        getSpan().setAttribute(SPAN_ATTRIBUTE_WORKFLOW_NAME, name);
        return this;
    }

    /**
     * Marks the end of the workflow loading process.
     */
    public void finishedLoading() {
        m_loadSpan.end();
    }


    @Override
    public void end() {
        if (m_loadSpan.isRecording()) {
            m_loadSpan.end();
            // TODO log warning
        }
        super.end();
    }

    /**
     * @param workflowContext
     */
    public void setWorkflowContext(final WorkflowContext workflowContext) {
        workflowContext.getJobId().map(UUID::toString)
            .ifPresent(uuidString -> getSpan().setAttribute("workflow.jobid", uuidString));
        getSpan().setAttribute("user.id", workflowContext.getUserid());
    }

}
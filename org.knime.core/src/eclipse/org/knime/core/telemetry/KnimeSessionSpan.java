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

import org.knime.core.util.IEarlyStartup;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * This span covers the time in which an analytics platform or executor is running.
 *
 * It may contain multiple, possibly parallel, {@link WorkflowSessionSpan}s.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class KnimeSessionSpan {

    /**
     * The span class/group/type is identified by this string. This should be general enough to generate insights when
     * aggregating over all spans with this operation identifier.
     */
    public static final String OPERATION = "KNIME Analytics Platform Session";

    /**
     *
     */
    public static final UnaryOperator<String> EARLY_STARTUP_FINISHED =
        extensionName -> String.format("EarlyStartup/%s", extensionName);

    /**
     * This is the span that describes the life time of a KNIME analytics platform or executor instance.
     */
    private final Span m_span;

    /**
     * @param parentSpan TODO could become the executor lifetime span
     */
    public KnimeSessionSpan(final KnimeSessionSpan parentSpan) {
        var builder = OpenTelemetryUtil.tracer()//
            // TODO span scope
            .spanBuilder(OPERATION)//
            .setSpanKind(SpanKind.CLIENT);
        Optional.ofNullable(parentSpan).ifPresent(p -> builder.setParent(Context.current().with(parentSpan.m_span)));
        m_span = builder.startSpan();
    }

    /**
     * What happens with the span depends on the configured {@link SpanProcessor}(s). It may be sent immediately or
     * collected to be sent as a batch later.
     */
    public void end() {
        m_span.end();
    }

    /**
     * Usage example:
     *
     * <code>
     * OpenTelemetryUtil.tracer()//
            .spanBuilder(OPERATION)//
            .setParent(Context.current().with(OpenTelemetryUtil.getKnimeSessionSpan().getOtelSpan()))//
            .startSpan();
     * </code>
     *
     * @return for creating subspans contexts.
     */
    public Span getOtelSpan() {
        return m_span;
    }

    /**
     * @param name of the extension hooking into {@link IEarlyStartup}
     */
    public void earlyStartupFinished(final String name) {
        m_span.addEvent(EARLY_STARTUP_FINISHED.apply(name));
    }

}
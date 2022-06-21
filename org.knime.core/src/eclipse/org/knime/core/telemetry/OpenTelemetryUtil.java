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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class OpenTelemetryUtil {

    /**
     * This is passed to {@link OpenTelemetry#getTracer(String)} to identify the library that is responsible for
     * collecting telemetry signals (the instrumenter). The instrumentee is in this case org.knime.core.
     */
    public final static String INSTRUMENTATION_LIBRARY = "org.knime.core.telemetry";

    public final static String INSTRUMENTATION_LIBRARY_VERSION = "1.0.0.";

    private static final OpenTelemetry INSTANCE = OpenTelemetryUtil.init();

    // Share context via text headers
    private final TextMapPropagator textFormat = INSTANCE.getPropagators().getTextMapPropagator();

    /**
     * Initializes the SDK that provides a concrete implementation for the OpenTelemetry API.
     *
     * @return
     */
    private static OpenTelemetry init() {

        // Tracer

        // Set to process the spans with the LoggingSpanExporter
        LoggingSpanExporter exporter = LoggingSpanExporter.create();
        final SpanProcessor spanProcessor = SimpleSpanProcessor.create(exporter);
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build();

        // a real SdkTracerProvider would probably look more like this
//        SdkTracerProvider.builder()
//                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
            // install the W3C Trace Context propagator
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();

        // it's always a good idea to shutdown the SDK when your process exits.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** forcing the Span Exporter to shutdown and process the remaining spans");
            sdkTracerProvider.shutdown();
            System.err.println("*** Trace Exporter shut down");
            exporter.close();
            spanProcessor.close();
        }));

        return openTelemetrySdk;
    }

    /**
     * @return the singleton instance for custom instrumentation
     */
    public static OpenTelemetry instance() {
        return INSTANCE;
    }

    /**
     * @return the singleton instance
     */
    public static Tracer tracer() {
        return INSTANCE.getTracer(OpenTelemetryUtil.INSTRUMENTATION_LIBRARY);
    }

}

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
 *   Sep 29, 2025 (manuelhotz): created
 */
package org.knime.core.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.knime.core.internal.diagnostics.ApplicationHealthCollector;
import org.knime.core.internal.diagnostics.Collector;
import org.knime.core.internal.diagnostics.DiagnosticInstructions;
import org.knime.core.internal.diagnostics.GcInfoCollector;
import org.knime.core.internal.diagnostics.HeapDumpCollector;
import org.knime.core.internal.diagnostics.JvmInfoCollector;
import org.knime.core.internal.diagnostics.KNIMEInfoCollector;
import org.knime.core.internal.diagnostics.SystemInfoCollector;
import org.knime.core.internal.diagnostics.ThreadDumpCollector;
import org.knime.core.internal.diagnostics.WorkflowManagersCollector;
import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Utility class for exporting diagnostic information using the collector pattern. This class handles the coordination
 * of all diagnostic collectors and manages the JSON object structure and enablement checks.
 *
 * @since 5.8
 */
public final class DiagnosticsExport {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DiagnosticsExport.class);

    /** All available diagnostic collectors. */
    private static final List<Collector> COLLECTORS = Arrays.asList( //
        SystemInfoCollector.INSTANCE, //
        JvmInfoCollector.INSTANCE, //
        GcInfoCollector.INSTANCE, //
        ApplicationHealthCollector.INSTANCE, //
        KNIMEInfoCollector.INSTANCE, //
        WorkflowManagersCollector.INSTANCE, //
        ThreadDumpCollector.INSTANCE, //
        HeapDumpCollector.INSTANCE //
    );

    private DiagnosticsExport() {
        // Utility class
    }

    /**
     * Exports diagnostic information according to the given instructions.
     *
     * @param timestamp timestamp of the export
     * @param instructions the diagnostic instructions
     * @param generator the JSON generator to write to
     * @param outputDir the output directory for any files
     * @throws IOException if an I/O error occurs
     */
    public static void exportDiagnostics(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {

        System.gc(); // Suggest a GC before collecting diagnostics

        for (final var collector : COLLECTORS) {
            try {
                writeCollectorData(timestamp, collector, instructions, generator, outputDir);
            } catch (final Exception e) { // NOSONAR: Catch all to avoid one collector breaking the whole export
                LOGGER.error(() -> "Collector \"%s\" had a problem, will be missing from output"
                    .formatted(collector.getClass().getSimpleName()), e);
            }
        }
    }

    /**
     * Writes data from a collector, handling the JSON object structure. All collectors now write complex objects
     * uniformly.
     *
     * @param timestamp the timestamp of the export
     * @param collector the collector to write data from
     * @param instructions the diagnostic instructions
     * @param generator the JSON generator to write to
     * @param outputDir the output directory for any files
     * @throws IOException if an I/O error occurs
     */
    public static void writeCollectorData(final Instant timestamp, final Collector collector,
        final DiagnosticInstructions instructions, final JsonGenerator generator, final Path outputDir)
        throws IOException {
        final var jsonKey = collector.getJsonKey();
        generator.writeObjectFieldStart(jsonKey);
        if (collector.isEnabled(instructions)) {
            collector.collect(timestamp, instructions, generator, outputDir);
        }
        generator.writeEndObject();
    }
}

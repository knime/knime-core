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
package org.knime.core.internal.diagnostics;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Instructions for diagnostic dumps, specifying what to collect and where to store the output.
 *
 * @param outputDirectory the output directory path (relative to {@code diagnostics} dir or absolute)
 * @param heapDumpPath optional path for heap dump file (null to skip, path to generate, relative to
 *            {@code outputDirectory} or absolute)
 * @param systemInfo whether to collect system information (OS, CPU, memory, etc.)
 * @param jvmInfo whether to collect JVM information (uptime, version, etc.)
 * @param gcInfo whether to collect garbage collection statistics
 * @param applicationHealth whether to collect application health information
 * @param knimeInfo whether to collect KNIME-specific information (version, etc.)
 * @param workflowManagers whether to collect workflow manager state information
 * @param threadDump whether to collect thread dump information
 *
 * @since 5.10
 */
public record DiagnosticInstructions(//
    @JsonSerialize(using = PathAsStringSerializer.class) @JsonProperty("outputDirectory") Path outputDirectory, //
    @JsonSerialize(using = PathAsStringSerializer.class) @JsonProperty("heapDump") Path heapDumpPath, //
    @JsonProperty("systemInfo") boolean systemInfo, // SystemInfoCollector
    @JsonProperty("jvmInfo") boolean jvmInfo, // JvmInfoCollector
    @JsonProperty("gcInfo") boolean gcInfo, // GcInfoCollector
    @JsonProperty("applicationHealth") boolean applicationHealth, // ApplicationHealthCollector
    @JsonProperty("knimeInfo") boolean knimeInfo, // KNIMEInfoCollector
    @JsonProperty("workflowManagers") boolean workflowManagers, // WorkflowManagersCollector
    @JsonProperty("threadDump") boolean threadDump // ThreadDumpCollector
) {

    /**
     * Creates default instructions with all diagnostics enabled except heap dump.
     *
     * @param outputDirectory the output directory
     * @return default instructions
     */
    public static DiagnosticInstructions createDefaults(final Path outputDirectory) {
        return new DiagnosticInstructions(outputDirectory, //
            null, // No heap dump by default
            true, // System info
            true, // JVM info
            true, // GC info
            true, // Application health
            true, // KNIME info
            true, // Workflow managers
            true // Thread dump
        );
    }

    /**
     * Creates instructions with heap dump enabled if heap size is reasonable.
     *
     * @param outputDirectory the output directory
     * @param heapDumpPath the file path to write the heap dump to
     * @return instructions with conditional heap dump
     */
    public static DiagnosticInstructions createWithHeapDump(final Path outputDirectory, final Path heapDumpPath) {
        return new DiagnosticInstructions(outputDirectory, //
            heapDumpPath, //
            true, // System info
            true, // JVM info
            true, // GC info
            true, // Application health
            true, // KNIME info
            true, // Workflow managers
            true // Thread dump
        );
    }

    /**
     * Custom serializer for {@link Path} objects that outputs simple path strings instead of file:// URLs. Without
     * this, Jackson's default Path serialization produces URL format (e.g., "file:///path") instead of plain path
     * syntax.
     */
    static final class PathAsStringSerializer extends JsonSerializer<Path> {
        @Override
        public void serialize(final Path path, final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {
            if (path != null) {
                jsonGenerator.writeString(path.toString());
            } else {
                jsonGenerator.writeNull();
            }
        }
    }
}


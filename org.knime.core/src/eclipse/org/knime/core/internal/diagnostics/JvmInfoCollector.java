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
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * JVM information diagnostic collector including memory information.
 *
 * @since 5.8
 */
public final class JvmInfoCollector implements Collector {

    /** The singleton instance. */
    public static final JvmInfoCollector INSTANCE = new JvmInfoCollector();

    private JvmInfoCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.jvmInfo();
    }

    @Override
    public String getJsonKey() {
        return "jvmInfo";
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {

        final var runtimeBean = ManagementFactory.getRuntimeMXBean();
        final var memoryBean = ManagementFactory.getMemoryMXBean();
        final var runtime = Runtime.getRuntime();

        generator.writeStringField("jvmVersion", System.getProperty("java.version"));
        generator.writeStringField("jvmVendor", System.getProperty("java.vendor"));
        generator.writeStringField("jvmName", runtimeBean.getVmName());
        generator.writeNumberField("uptimeMS", runtimeBean.getUptime());
        generator.writeStringField("startTime", Instant.ofEpochMilli(runtimeBean.getStartTime()).toString());
        generator.writeNumberField("availableProcessors", Runtime.getRuntime().availableProcessors());

        generator.writeArrayFieldStart("jvmArguments");
        for (String arg : runtimeBean.getInputArguments()) {
            generator.writeString(arg);
        }
        generator.writeEndArray();

        generator.writeObjectFieldStart("memory");

        // Heap memory
        generator.writeObjectFieldStart("heap");
        generator.writeNumberField("used", memoryBean.getHeapMemoryUsage().getUsed());
        generator.writeNumberField("committed", memoryBean.getHeapMemoryUsage().getCommitted());
        generator.writeNumberField("max", memoryBean.getHeapMemoryUsage().getMax());
        generator.writeEndObject();

        // Non-heap memory
        generator.writeObjectFieldStart("nonHeap");
        generator.writeNumberField("used", memoryBean.getNonHeapMemoryUsage().getUsed());
        generator.writeNumberField("committed", memoryBean.getNonHeapMemoryUsage().getCommitted());
        generator.writeNumberField("max", memoryBean.getNonHeapMemoryUsage().getMax());
        generator.writeEndObject();

        // Runtime memory
        generator.writeObjectFieldStart("runtime");
        generator.writeNumberField("totalMemory", runtime.totalMemory());
        generator.writeNumberField("freeMemory", runtime.freeMemory());
        generator.writeNumberField("maxMemory", runtime.maxMemory());
        generator.writeEndObject();

        generator.writeEndObject();
    }
}

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
package org.knime.core.internal.diagnostics.collectors;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;

import org.knime.core.internal.diagnostics.Collector;
import org.knime.core.internal.diagnostics.DiagnosticInstructions;
import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Heap dump diagnostic collector.
 *
 * @since 5.8
 */
public final class HeapDumpCollector implements Collector {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(HeapDumpCollector.class);

    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    /** The singleton instance. */
    public static final HeapDumpCollector INSTANCE = new HeapDumpCollector();

    private HeapDumpCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.heapDumpPath() != null;
    }

    @Override
    public String getJsonKey() {
        return "heapDump";
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        try {
            var heapDumpPath = instructions.heapDumpPath();
            if (heapDumpPath == null || heapDumpPath.toString().isBlank()) {
                generator.writeStringField("status", "skipped");
                generator.writeStringField("error", "No heap dump path specified");
                generator.writeStringField("filePath", null);
                generator.writeNumberField("fileSize", 0);
                LOGGER.info("Skipping heap dump creation -- no path specified");
                return;
            }

            if (!heapDumpPath.isAbsolute()) {
                heapDumpPath = outputDir.resolve(heapDumpPath).toAbsolutePath();
            }
            // pass absolute path otherwise will be relative to JVM working dir
            LOGGER.infoWithFormat("Creating heap dump at \"%s\" ...", heapDumpPath);
            ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), HOTSPOT_BEAN_NAME,
                HotSpotDiagnosticMXBean.class).dumpHeap(heapDumpPath.toString(), true);

            generator.writeStringField("status", "success");
            generator.writeStringField("filePath", heapDumpPath.toString());
            generator.writeNumberField("fileSize", heapDumpPath.toFile().length());

            LOGGER.infoWithFormat("Created heap dump at \"%s\"", heapDumpPath);

        } catch (final Exception e) {
            generator.writeStringField("status", "failed");
            generator.writeStringField("error", e.getMessage());
            generator.writeStringField("filePath", null);
            generator.writeNumberField("fileSize", 0);

            LOGGER.error("Failed to create heap dump", e);
        }
    }
}

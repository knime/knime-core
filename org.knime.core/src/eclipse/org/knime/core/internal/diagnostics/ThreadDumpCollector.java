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
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Thread dump diagnostic collector that writes some metrics and a standard Java thread dump output to JSON.
 *
 * The dump entry can be extracted via {@code jq} as follows:
 * <pre><code>
 * $ jq -r '.threadDump.dump' output-diagnostics.json > threaddump.txt
 * </code></pre>
 *
 * @since 5.8
 */
public final class ThreadDumpCollector implements Collector {

    /** The singleton instance. */
    public static final ThreadDumpCollector INSTANCE = new ThreadDumpCollector();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ThreadDumpCollector.class);

    private ThreadDumpCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.threadDump();
    }

    @Override
    public String getJsonKey() {
        return "threadDump";
    }

    private record ThreadMetrics(long runnable, long blocked, long waiting, long timedWaiting) {
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        LOGGER.debug("Collecting thread dump...");

        final var threadBean = ManagementFactory.getThreadMXBean();
        final var threadInfos = threadBean.dumpAllThreads(true, true);
        final var metrics = getMetrics(threadInfos);

        generator.writeNumberField("threadCount", threadInfos.length);
        generator.writeObjectFieldStart("threadStates");
        generator.writeNumberField("runnable", metrics.runnable);
        generator.writeNumberField("blocked", metrics.blocked);
        generator.writeNumberField("waiting", metrics.waiting);
        generator.writeNumberField("timedWaiting", metrics.timedWaiting);
        generator.writeEndObject();

        var threadDumpOutput = captureViaHotSpotDiagnostic();
        if (threadDumpOutput == null || threadDumpOutput.isEmpty()) {
            // fall back to manual format (that does not 1:1 match the jstack output)
            final var sb = new StringBuilder();
            Arrays.stream(threadInfos).forEach(info -> sb.append(info.toString()).append("\n"));
            threadDumpOutput = sb.toString();
        }
        generator.writeStringField("dump", threadDumpOutput);
    }

    private static String captureViaHotSpotDiagnostic() {
        try {
            LOGGER.debug("Thread dump via HotSpot diagnostic command");

            final var server = ManagementFactory.getPlatformMBeanServer();
            final var hotspotDiag = new javax.management.ObjectName("com.sun.management:type=DiagnosticCommand");
            if (!server.isRegistered(hotspotDiag)) {
                return null;
            }

            final var result =
                server.invoke(hotspotDiag, "threadPrint", new Object[]{null}, new String[]{"[Ljava.lang.String;"});

            if (result != null) {
                final var dump = result.toString();
                if (dump.contains("Full thread dump")) {
                    LOGGER.debug("Successfully captured thread dump using HotSpot diagnostic");
                    return dump;
                } else {
                    LOGGER.warn("HotSpot diagnostic output did not contain expected thread dump header");
                }
            }
        } catch (ReflectionException | MalformedObjectNameException | InstanceNotFoundException | MBeanException e) {
            LOGGER.warn("HotSpot diagnostic failed: " + e.getMessage());
        }
        return null;
    }

    private static ThreadMetrics getMetrics(final ThreadInfo[] threadInfos) {
        return Arrays.stream(threadInfos).map(ThreadInfo::getThreadState).map(state -> switch (state) {
            case RUNNABLE -> new ThreadMetrics(1, 0, 0, 0);
            case BLOCKED -> new ThreadMetrics(0, 1, 0, 0);
            case WAITING -> new ThreadMetrics(0, 0, 1, 0);
            case TIMED_WAITING -> new ThreadMetrics(0, 0, 0, 1);
            case NEW, TERMINATED -> new ThreadMetrics(0, 0, 0, 0);
        }).reduce((l, r) -> new ThreadMetrics(l.runnable + r.runnable, l.blocked + r.blocked, l.waiting + r.waiting,
            l.timedWaiting + r.timedWaiting)).orElse(new ThreadMetrics(0, 0, 0, 0));
    }
}

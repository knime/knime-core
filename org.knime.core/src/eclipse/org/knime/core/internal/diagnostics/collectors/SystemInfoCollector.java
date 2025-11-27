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
 *   29 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.internal.diagnostics.collectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.knime.core.internal.diagnostics.Collector;
import org.knime.core.internal.diagnostics.DiagnosticInstructions;
import org.knime.core.node.NodeLogger;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Collector for system information.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class SystemInfoCollector implements Collector {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SystemInfoCollector.class);

    /** The singleton instance. */
    public static final SystemInfoCollector INSTANCE = new SystemInfoCollector();

    private SystemInfoCollector() {
        // Singleton
    }

    @Override
    public boolean isEnabled(final DiagnosticInstructions instructions) {
        return instructions.systemInfo();
    }

    @Override
    public String getJsonKey() {
        return "systemInfo";
    }

    @Override
    public void collect(final Instant timestamp, final DiagnosticInstructions instructions,
        final JsonGenerator generator, final Path outputDir) throws IOException {
        LOGGER.info("Collecting system information...");

        final var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        writeBasicSystemInfo(generator);
        writeCpuInfo(os, generator);
        writeMemoryInfo(generator);
        writeProcessInfo(os, generator);
    }

    /**
     * Writes basic system information like OS, architecture, etc.
     */
    private static void writeBasicSystemInfo(final JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("basicInfo");
        generator.writeStringField("osName", System.getProperty("os.name"));
        generator.writeStringField("osVersion", System.getProperty("os.version"));
        generator.writeStringField("osArch", System.getProperty("os.arch"));
        generator.writeStringField("userDir", System.getProperty("user.dir"));
        generator.writeStringField("userHome", System.getProperty("user.home"));

        // System temp directory with disk space information
        String tempDir = System.getProperty("java.io.tmpdir");
        generator.writeStringField("tempDir", tempDir);

        final var tempFile = new File(tempDir);
        long freeSpace = tempFile.getFreeSpace();
        long totalSpace = tempFile.getTotalSpace();
        long usableSpace = tempFile.getUsableSpace();

        generator.writeObjectFieldStart("tempDirDiskSpace");
        generator.writeNumberField("freeBytes", freeSpace);
        generator.writeNumberField("totalBytes", totalSpace);
        generator.writeNumberField("usableBytes", usableSpace);
        generator.writeEndObject();

        generator.writeEndObject();
    }

    private static void writeCpuInfo(final String os, final JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("cpuInfo");

        final var runtime = Runtime.getRuntime();
        final var osBean = ManagementFactory.getOperatingSystemMXBean();

        generator.writeNumberField("availableProcessors", runtime.availableProcessors());
        generator.writeNumberField("logicalCpuCount", runtime.availableProcessors());

        // System load average
        double loadAverage = osBean.getSystemLoadAverage();
        if (loadAverage >= 0) {
            generator.writeNumberField("systemLoadAverage", loadAverage);
        }

        // Try to get more detailed load averages (1, 5, 15 minutes)
        double[] loadAverages = getLoadAverages(os);
        if (loadAverages != null && loadAverages.length == 3) {
            generator.writeArrayFieldStart("loadAverages");
            generator.writeNumber(loadAverages[0]); // 1 minute
            generator.writeNumber(loadAverages[1]); // 5 minutes
            generator.writeNumber(loadAverages[2]); // 15 minutes
            generator.writeEndArray();
        }

        // CPU usage if available
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            generator.writeNumberField("processCpuLoad", sunOsBean.getProcessCpuLoad());
            generator.writeNumberField("systemCpuLoad", sunOsBean.getCpuLoad());
        }

        generator.writeEndObject();
    }

    private static void writeMemoryInfo(final JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("memoryInfo");

        final var osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            generator.writeObjectFieldStart("systemMemory");
            generator.writeNumberField("totalPhysicalMemory", sunOsBean.getTotalMemorySize());
            generator.writeNumberField("freePhysicalMemory", sunOsBean.getFreeMemorySize());
            generator.writeNumberField("usedPhysicalMemory",
                sunOsBean.getTotalMemorySize() - sunOsBean.getFreeMemorySize());
            generator.writeNumberField("totalSwapSpace", sunOsBean.getTotalSwapSpaceSize());
            generator.writeNumberField("freeSwapSpace", sunOsBean.getFreeSwapSpaceSize());
            generator.writeNumberField("committedVirtualMemory", sunOsBean.getCommittedVirtualMemorySize());
            generator.writeEndObject();
        }

        generator.writeEndObject();
    }

    private static double[] getLoadAverages(final String os) {
        try {
            // Try reading from /proc/loadavg on Linux
            if (os.contains("linux")) {
                final var loadavgPath = Paths.get("/proc/loadavg");
                if (Files.exists(loadavgPath)) {
                    String content = Files.readString(loadavgPath, StandardCharsets.UTF_8).trim();
                    String[] parts = content.split("\\s+"); // NOSONAR -- ascii
                    if (parts.length >= 3) {
                        return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2])};
                    }
                }
            }
        } catch (final IOException | NumberFormatException e) {
            LOGGER.warn(String.format("Failed to get load averages: %s", e.getMessage()), e);
        }

        return new double[0];
    }

    private static void writeProcessInfo(final String os, final JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("processInfo");

        final var processName = ManagementFactory.getRuntimeMXBean().getName();
        generator.writeStringField("currentProcess", processName);
        generator.writeStringField("currentPid", processName.split("@")[0]);

        final String[][] topProcesses;
        try {
            topProcesses = getTopProcesses(os, 5);
            if (topProcesses != null && topProcesses.length > 0) {
                generator.writeArrayFieldStart("topProcesses");
                for (String[] process : topProcesses) {
                    if (process.length >= 4) {
                        generator.writeStartObject();
                        generator.writeStringField("pid", process[0]);
                        generator.writeStringField("cpu", process[1]);
                        generator.writeStringField("memory", process[2]);
                        generator.writeStringField("command", process[3]);
                        generator.writeEndObject();
                    }
                }
                generator.writeEndArray();
            }
        } catch (final Exception e) { // NOSONAR reading system commands output
            LOGGER.warn(String.format("Failed to get top processes: %s", e.getMessage()), e);
        }

        generator.writeEndObject();
    }

    private static String[][] getTopProcesses(final String os, final int count) {
        final String output;
        if (os.contains("linux")) {
            // Linux: use ps with specific format
            output = executeCommand("ps", "axo", "pid,pcpu,pmem,comm", "--sort=-pcpu", "--no-headers");
        } else if (os.contains("mac")) {
            // macOS: use ps with BSD format
            output = executeCommand("ps", "axo", "pid,pcpu,pmem,comm", "-r");
        } else {
            return new String[0][]; // Unsupported OS
        }

        if (output != null) {
            final var lines = output.split("\n");
            final var result = new String[Math.min(count, lines.length)][];

            var added = 0;
            for (int i = 0; i < lines.length && added < count; i++) {
                final var line = lines[i].trim();
                if (!line.isEmpty() && !line.startsWith("PID")) {
                    String[] parts = line.split("\\s+", 4);
                    if (parts.length >= 4) {
                        result[added++] = parts;
                    }
                }
            }

            return Arrays.copyOf(result, added);
        }

        return new String[0][];
    }

    private static String executeCommand(final String... command) {
        try {
            final var pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            final var process = pb.start();

            final var output = new StringBuilder();
            try (final var reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            final var finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return output.toString();
            }
        } catch (final IOException e) {
            LOGGER.debugWithFormat("Command execution failed: %s - %s", Arrays.toString(command), e.getMessage());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debugWithFormat("Command execution interrupted: %s - %s", Arrays.toString(command), e.getMessage());
        }
        return null;
    }
}

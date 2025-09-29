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
 *   Jun 6, 2025 (manuelhotz): created
 */
package org.knime.core.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.knime.core.internal.diagnostics.DiagnosticInstructions;
import org.knime.core.monitor.ProcessWatchdog;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * {@link IEarlyStartup} implementation that starts the watchdog as part of the application (via extension point).
 *
 * @since 5.8
 */
public final class ApplicationHealthEarlyStartup implements IEarlyStartup {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ApplicationHealthEarlyStartup.class);

    private static final String FEATURE_FLAG_DIAGNOSTICS = "org.knime.core.diagnostics.enabled";

    private static final boolean IS_DIAGNOSTICS_ENABLED =
        Boolean.parseBoolean(System.getProperty(FEATURE_FLAG_DIAGNOSTICS, "false"));

    @Override
    public void run() {
        ProcessWatchdog.getInstance();
        if (IS_DIAGNOSTICS_ENABLED) {
            DiagnosticsFileDump.getInstance();
        } else {
            LOGGER.debug(() -> "KNIME diagnostics system is disabled, to enable it set the system property %s=true"
                .formatted(FEATURE_FLAG_DIAGNOSTICS));
        }
    }

    /**
     * Creates a diagnostic dump with default settings for testing purposes.
     *
     * @return the path to the created dump directory
     * @throws IOException if an I/O error occurs
     */
    public static Path createDefaultDiagnostics() throws IOException {
        return DiagnosticsFileDump.getInstance().exportDefault();
    }

    /**
     * Creates a diagnostic dump with default settings for testing purposes.
     *
     * @param instructions custom instructions to include/exclude specific information
     * @return the path to the created dump directory
     * @throws IOException if an I/O error occurs
     */
    public static Path createDiagnostics(final DiagnosticInstructions instructions) throws IOException {
        return DiagnosticsFileDump.getInstance().export(instructions);
    }

    static final class DiagnosticsFileDump {

        private static final NodeLogger DUMP_LOGGER = NodeLogger.getLogger(DiagnosticsFileDump.class);

        private static final DiagnosticsFileDump INSTANCE = new DiagnosticsFileDump();

        private final WatchService m_watchService;

        private final Path m_diagnosticsDir;

        private final ExecutorService m_watchServiceExec;

        private final ExecutorService m_dumpServiceExec;

        private final ObjectMapper m_objectMapper;

        public static DiagnosticsFileDump getInstance() {
            return INSTANCE;
        }

        @SuppressWarnings("resource") // we don't own the filesystem
        private DiagnosticsFileDump() {
            m_objectMapper = new ObjectMapper();
            m_objectMapper.registerModule(new Jdk8Module());
            m_objectMapper.registerModule(new JavaTimeModule());
            m_objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            m_watchServiceExec = Executors.newSingleThreadExecutor(r -> {
                final var t = new Thread(r, "DiagnosticsFileDump-Watcher");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler((thread, e) -> DUMP_LOGGER
                    .error(() -> "Thread \"%s\" had uncaught exception during diagnostics file watching, "
                        .formatted(thread.getName()) + "scheduling new thread...", e));
                return t;
            });
            m_dumpServiceExec = Executors.newSingleThreadExecutor(r -> {
                final var t = new Thread(r, "DiagnosticsFileDump-Processor");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler((thread, e) -> DUMP_LOGGER
                    .error(() -> "Thread \"%s\" had uncaught exception during diagnostics file processing"
                        .formatted(thread.getName()), e));
                return t;
            });

            try {
                m_diagnosticsDir = initializeDiagnosticsDirectory();
                m_watchService = m_diagnosticsDir.getFileSystem().newWatchService();
                startWatching();
                DUMP_LOGGER.debugWithFormat("Diagnostics file dump watcher started, monitoring: %s", m_diagnosticsDir);
            } catch (final IOException e) {
                throw new RuntimeException("Failed to initialize diagnostics watcher", e);
            }
        }

        private static Path initializeDiagnosticsDirectory() throws IOException {
            final var knimeHome = KNIMEConstants.getKNIMEHomeDir();
            final var diagDir = Path.of(knimeHome).resolve("diagnostics");
            Files.createDirectories(diagDir);
            Files.writeString(diagDir.resolve("created-by-knime"),
                "This directory was created by the KNIME diagnostics system", StandardOpenOption.CREATE);
            return diagDir;
        }

        private void startWatching() {
            m_watchServiceExec.submit(() -> {
                try {
                    // Register the diagnostics directory for watching
                    m_diagnosticsDir.register(m_watchService, StandardWatchEventKinds.ENTRY_CREATE);

                    DUMP_LOGGER.debugWithFormat("File watcher registered for directory: %s", m_diagnosticsDir);

                    while (!Thread.currentThread().isInterrupted()) {
                        WatchKey key;
                        try {
                            key = m_watchService.take();
                            if (key == null) {
                                continue;
                            }
                        } catch (final ClosedWatchServiceException e) {
                            DUMP_LOGGER.info("Watch service closed, stopping diagnostics watcher");
                            break;
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            DUMP_LOGGER.info("Diagnostics watcher interrupted");
                            break;
                        }

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }

                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                            final var fileName = pathEvent.context();

                            if (isInstructionsFile(fileName)) {
                                final var fullPath = m_diagnosticsDir.resolve(fileName);
                                DUMP_LOGGER.debugWithFormat("Detected instructions file: %s", fullPath);
                                processInstructionsFile(fullPath);
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            DUMP_LOGGER.warn("Watch key no longer valid, stopping watcher");
                            break;
                        }
                    }
                } catch (final IOException e) {
                    DUMP_LOGGER.error("Error in diagnostics file watcher", e);
                }
            });
        }

        private static boolean isInstructionsFile(final Path fileName) {
            String name = fileName.toString().toLowerCase(Locale.US);
            return name.startsWith("diagnostics-") && name.endsWith(".json");
        }

        private void processInstructionsFile(final Path instructionsFile) {
            m_dumpServiceExec.submit(() -> {
                try {
                    // Wait a bit to ensure file is fully written
                    Thread.sleep(500);

                    if (!Files.exists(instructionsFile)) {
                        DUMP_LOGGER.warnWithFormat("Instructions file no longer exists: %s", instructionsFile);
                        return;
                    }

                    DUMP_LOGGER.debugWithFormat("Processing instructions file: %s", instructionsFile);

                    // Read and parse instructions using new schema
                    final var instructions =
                        m_objectMapper.readValue(instructionsFile.toFile(), DiagnosticInstructions.class);

                    final var instructionsFileName = instructionsFile.getFileName().toString();
                    final var outputDirectory = getDumpDir(m_diagnosticsDir, instructions.outputDirectory());
                    Files.createDirectories(outputDirectory);
                    // rename file to indicate it has been processed (and not matching the condition anymore)
                    final var outputFileName = instructionsFileName.replace("diagnostics-", "output-diagnostics-");
                    final var outputFile = outputDirectory.resolve(outputFileName);

                    exportInto(instructions, outputDirectory, outputFile);

                    Files.move(instructionsFile, outputDirectory.resolve(instructionsFileName));
                } catch (final IOException e) {
                    DUMP_LOGGER.error(
                        "An error occurred while processing instructions file \"%s\"".formatted(instructionsFile), e);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    DUMP_LOGGER.warn("Processing of instructions file interrupted", ex);
                }
            });
        }

        private static Path getDumpDir(final Path diagnosticsDir, final Path outputDirectory) {
            if (outputDirectory.isAbsolute()) {
                return outputDirectory;
            }
            return diagnosticsDir.resolve(outputDirectory);
        }

        /**
         * Creates a diagnostic dump with default settings.
         *
         * @return the path to the created dump directory
         * @throws IOException if an I/O error occurs
         */
        public Path exportDefault() throws IOException {
            return export(null);
        }

        public Path export(final DiagnosticInstructions customInstructions) throws IOException {
            final var timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            final var outputDir = Path.of("diagnostic-" + timestamp);
            final var instructions =
                customInstructions != null ? customInstructions : DiagnosticInstructions.createDefaults(outputDir);
            final var dumpDir = getDumpDir(m_diagnosticsDir, outputDir);
            Files.createDirectories(dumpDir);
            exportInto(instructions, dumpDir, null);
            return dumpDir;
        }

        private void exportInto(final DiagnosticInstructions instructions, final Path outputDirectory,
            final Path outputFile) throws IOException {
            final var diagnosticsOutput =
                outputFile == null ? outputDirectory.resolve("diagnostics-output.json") : outputFile;
            DUMP_LOGGER.debugWithFormat("Creating diagnostic dump at: %s", diagnosticsOutput.toAbsolutePath());

            // Create diagnostic dump with JSON file generator
            try (final var fileWriter = new FileWriter(diagnosticsOutput.toFile(), StandardCharsets.UTF_8);
                    final var gen = m_objectMapper.getFactory().createGenerator(fileWriter);
                    final var pretty = gen.useDefaultPrettyPrinter()) {

                pretty.writeStartObject();
                final var now = Instant.now();
                pretty.writeStringField("createdAt", now.toString());
                DiagnosticsExport.exportDiagnostics(now, instructions, pretty, outputDirectory);
                pretty.writeEndObject();
                pretty.flush();
            }

            DUMP_LOGGER.debugWithFormat("Created diagnostic dump at: %s", diagnosticsOutput.toAbsolutePath());
        }
    }
}

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
 *   21 May 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.node.logging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.ThreadPool;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RichTextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.testing.node.logging.LoggerNodeFactory.LoggerNodeModel;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui is not API yet
public final class LoggerNodeFactory extends WebUINodeFactory<LoggerNodeModel> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoggerNodeFactory.class);

    private static final String DESC = """
            Testing node for NodeLogger. This node implementation can change at any time.
            Do not use in production workflows!
            """;

    static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder() //
        .name("Testing Logger") //
        .icon(null) //
        .shortDescription(DESC) //
        .fullDescription(DESC) //
        .modelSettingsClass(LoggerNodeSettings.class) //
        .sinceVersion(5, 5, 0) //
        .build();

    public LoggerNodeFactory() {
        super(CONFIG);
    }

    @Override
    public LoggerNodeModel createNodeModel() {
        return new LoggerNodeModel(CONFIG);
    }

    static final class LoggerNodeSettings implements DefaultNodeSettings {

        @Widget(title = "Log level",
            description = "Level to log at. Supported levels: DEBUG, INFO, WARN, ERROR, FATAL.")
        LEVEL m_level = LEVEL.INFO;

        @Widget(title = "Log message", description = "Message to log")
        @RichTextInputWidget
        String m_message = "Hello World!";

        @Widget(title = "Logger name",
            description = "Logger name to log under. If empty, the default logger will be used.")
        String m_loggerName;

        @Widget(title = "Run infinitely (dangerous!)", description = "If checked, the node will run infinitely. ")
        boolean m_runInfinitely;

        @Widget(title = "Into Nirvana", description = "Run thread into Nirvana such that node can finish executing."
            + "Will be interrupted by repeated node execution")
        boolean m_runIntoNirvana;

    }

    static final class LoggerNodeModel extends WebUINodeModel<LoggerNodeSettings> {

        private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

        private static final AtomicReference<Future<Void>> TASK = new AtomicReference<>();

        LoggerNodeModel(final WebUINodeConfiguration config) {
            super(config, LoggerNodeSettings.class);
        }

        @Override
        protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final LoggerNodeSettings settings)
            throws InvalidSettingsException {
            // no-op
            return new PortObjectSpec[]{};
        }

        @Override
        protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
            final LoggerNodeSettings settings) throws Exception {
            final var existing = TASK.get();
            if (existing != null) {
                existing.cancel(true);
                if (existing.isDone() || existing.isCancelled()) {
                    LOGGER.debug("Existing task is no longer running");
                }
            }

            final var pool = ThreadPool.currentPool();
            if (settings.m_runInfinitely && pool != null) {
                final Future<Void> task = pool.submit(() -> {
                    while(true) {
                        log(settings.m_level, settings.m_loggerName, settings.m_message);
                        if (!settings.m_runIntoNirvana) {
                            exec.checkCanceled();
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }
                        // TODO configurable
                        pool.runInvisible(() -> { Thread.sleep(1000); return null; });
                    }
                });
                // store it so a later execution can cancel it
                if (settings.m_runIntoNirvana) {
                    TASK.set(task);
                } else {
                    LOGGER.debug("Waiting for task to finish");
                    // don't block worker by this node
                    pool.runInvisible(task::get);
                    LOGGER.debug("Task finished");
                }
                LOGGER.debug("Submitted infinitely running logging task");
            } else {
                log(settings.m_level, settings.m_loggerName, settings.m_message);
            }

            return new PortObject[]{};
        }

        private static void log(final LEVEL level, final String loggerName, final String message) {
            final var logger = StringUtils.isBlank(loggerName) ? LOGGER : NodeLogger.getLogger(loggerName);
            switch (level) {
                case DEBUG -> logger.debug(message);
                case INFO -> logger.info(message);
                case WARN -> logger.warn(message);
                case ERROR -> logger.error(message);
                case FATAL -> logger.fatal(message);
                case ALL -> throw new UnsupportedOperationException("Unimplemented case: " + level);
                case OFF -> throw new UnsupportedOperationException("Unimplemented case: " + level);
            }
        }

    }
}

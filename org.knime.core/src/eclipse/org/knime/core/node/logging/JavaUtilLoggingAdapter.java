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
 *   19 Aug 2024 (leonard.woerteler): created
 */
package org.knime.core.node.logging;

import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;

/**
 * An adapter from {@link java.util.logging.Logger} to AP's {@link NodeLogger}. Loggers have to be added to the
 * {@code log4j3.xml} file and separately to a properties file registered via
 * {@code -Djava.util.logging.config.file=[...]} in order to reach the {@link NodeLogger}.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public final class JavaUtilLoggingAdapter extends Handler implements IEarlyStartup {

    @Override
    public void run() {
        // run at the `EARLIEST` startup stage
        LogManager.getLogManager().getLogger("").addHandler(this);
    }

    @Override
    public void publish(final LogRecord logRecord) {
        final var nodeLogger = getNodeLogger(logRecord);
        if (nodeLogger == null) {
            return;
        }

        final var message = Objects.toString(logRecord.getMessage(), "");
        final var cause = logRecord.getThrown();
        final var julLevel = logRecord.getLevel().intValue();
        if (julLevel <= Level.FINEST.intValue()) {
            nodeLogger.debug(() -> "(FINEST) " + message, cause);
        } else if (julLevel <= Level.FINE.intValue()) {
            nodeLogger.debug(message, cause);
        } else if (julLevel <= Level.INFO.intValue()) {
            nodeLogger.info(message, cause);
        } else if (julLevel <= Level.WARNING.intValue()) {
            nodeLogger.warn(message, cause);
        } else {
            nodeLogger.error(message, cause);
        }
    }

    private static NodeLogger getNodeLogger(final LogRecord logRecord) {
        final var loggerName = logRecord == null ? null : logRecord.getLoggerName();
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }

        // for a name such as "foo.bar.SomeName" check if Log4J contains any configured loggers
        // (check "foo.bar.SomeName", then "foo.bar", then "foo")

        // known loggers (org.knime) are passed through, unknown loggers (e.g. org.apache.arrow.xyz) are noop'ed
        // see log4j3.xml -- it contains separate loggers for {com|org}.knime
        String currentNamePrefix = loggerName;
        while (org.apache.log4j.LogManager.exists(currentNamePrefix) == null) {
            final var lastDot = currentNamePrefix.lastIndexOf('.');
            if (lastDot < 0) {
                // reached the root, no logger found
                return null;
            }
            currentNamePrefix = currentNamePrefix.substring(0, lastDot);
        }
        return NodeLogger.getLogger(loggerName);
    }

    @Override
    public void flush() {
        // empty
    }

    @Override
    public void close() {
        // empty
    }
}

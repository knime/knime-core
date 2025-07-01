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
 *   1 Jul 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import java.util.Optional;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.IEarlyStartup;

/**
 * NodeLogger adapter for {@link ILog}s obtained from the Eclipse runtime.
 * Its messages are to be logged if they are INFO, WARNING, or ERROR.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @since 5.9
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class ILogAdapter implements ILogListener, IEarlyStartup {

    @Override
    public void run() {
        // run at the `EARLIEST` startup stage
        Platform.addLogListener(this);
    }

    @Override
    public void logging(final IStatus status, final String plugin) {
        final var logger = NodeLogger.getLogger(plugin);
        final var level = mapToLogLEVEL(status.getSeverity());
        level.ifPresent(l -> {
            switch (l) {
                case DEBUG -> logger.debug(status.getMessage(), status.getException());
                case INFO -> logger.info(status.getMessage(), status.getException());
                case WARN -> logger.warn(status.getMessage(), status.getException());
                case ERROR -> logger.error(status.getMessage(), status.getException());
                case FATAL -> logger.fatal(status.getMessage(), status.getException());

                // ALL, OFF are only used for configuration, not for actual log message levels
                case ALL, OFF -> throw new IllegalStateException("Unexpected log level %s".formatted(l));
            }
        });
    }

    private static Optional<LEVEL> mapToLogLEVEL(final int severity) {
        return Optional.ofNullable(switch (severity) {
            case IStatus.INFO -> LEVEL.INFO;
            case IStatus.WARNING -> LEVEL.WARN;
            case IStatus.ERROR -> LEVEL.ERROR;
            case IStatus.CANCEL, IStatus.OK -> null;

            // there are currently no other `IStatus`s, above list is exhaustive
            default -> throw new IllegalArgumentException("Unexpected severity value %d".formatted(severity));
        });
    }
}

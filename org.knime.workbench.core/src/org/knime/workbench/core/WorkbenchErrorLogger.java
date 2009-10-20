/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utility class for logging messages to the Eclipse Workbench log.
 * 
 * Note: This introduces dependency to JFace.
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class WorkbenchErrorLogger {

    private WorkbenchErrorLogger() {
        // hidden utility constructor
    }

    /**
     * Logs a message with the plugin ID of the KNIMECorePlugin.
     * 
     * @param severity Severity code e.g. <code>IStatus.WARN</code>
     * @param code Status code e.g. <code>IStatus.OK</code>
     * @param message The message
     * @param throwable The throwable to log
     */
    public static void log(final int severity, final int code,
            final String message, final Throwable throwable) {
        IStatus stat = createStatus(severity, KNIMECorePlugin.PLUGIN_ID, code,
                message, throwable);

        KNIMECorePlugin.getDefault().getLog().log(stat);

    }

    /**
     * Logs a simple information message.
     * 
     * @param message The message to log
     */
    public static void info(final String message) {
        log(IStatus.INFO, IStatus.OK, message, null);
    }

    /**
     * Logs an error with a throwable.
     * 
     * @param message The message to log
     * @param throwable The throwable
     */
    public static void error(final String message, final Throwable throwable) {
        log(IStatus.ERROR, IStatus.OK, message, throwable);
        KNIMEErrorDialog.openError(message, createStatus(IStatus.ERROR,
                KNIMECorePlugin.PLUGIN_ID, IStatus.OK, throwable.getMessage(), 
                throwable));
    }

    /**
     * Logs an error.
     * 
     * @param message The message to log
     */
    public static void error(final String message) {
        error(message, null);
    }

    /**
     * Logs a warning.
     * 
     * @param message The message to log
     */
    public static void warning(final String message) {
        log(IStatus.WARNING, IStatus.OK, message, null);
    }

    /**
     * Creates a Status-Object.
     * 
     * @param severity The severity
     * @param pluginID the plugin ID
     * @param code The code
     * @param message The message
     * @param throwable The throwable, or <code>null</code>
     * @return The status object
     */
    private static IStatus createStatus(final int severity,
            final String pluginID, final int code, final String message,
            final Throwable throwable) {
        return new Status(severity, pluginID, code, message + ".", throwable);
    }
}

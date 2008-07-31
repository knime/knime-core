/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

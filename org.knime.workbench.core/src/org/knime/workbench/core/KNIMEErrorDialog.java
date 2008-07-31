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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * JFace dialog presenting an (internal) error to the user.
 * 
 * TODO add a "details" section
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEErrorDialog extends ErrorDialog {

    /**
     * Main constructor, takes all possible arguments.
     * 
     * @param parentShell The parent shell
     * @param dialogTitle Title to display
     * @param text The message text
     * @param status Statuscode, like <code>IStatus.ERROR</code>
     * @param displayMask mask for filtering children
     */
    public KNIMEErrorDialog(final Shell parentShell, final String dialogTitle,
            final String text, final IStatus status, final int displayMask) {
        super(parentShell, dialogTitle, text, status, displayMask);
    }

    /**
     * Opens an error dialog.
     * 
     * @param parentShell The Shell
     * @param title The title
     * @param message the message
     * @param status the status code
     * @param displayMask display mask for filtering
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final Shell parentShell, final String title,
            final String message, final IStatus status, final int displayMask) {
        KNIMEErrorDialog dialog = new KNIMEErrorDialog(parentShell, title,
                message, status, displayMask);
        return dialog.open();
    }

    /**
     * Opens an error dialog with standard filtering (displayMask).
     * 
     * @param parentShell The Shell
     * @param title The title
     * @param message the message
     * @param status the status code
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final Shell parentShell, final String title,
            final String message, final IStatus status) {
        return KNIMEErrorDialog.openError(parentShell, title, message, status,
                IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
    }

    /**
     * Opens an error dialog with standard filtering (displayMask) and standard
     * title on the active shell on the current display.
     * 
     * @param message the message
     * @param status the status code
     * 
     * @return <code>Window.OK</code> or <code>Window.CANCEL</code>
     */
    public static int openError(final String message, final IStatus status) {
        return KNIMEErrorDialog.openError(
                Display.getCurrent().getActiveShell(),
                "KNIME Workbench: Error occured", message, status);
    }
}

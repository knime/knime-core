/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
public final class KNIMEErrorDialog {
    private KNIMEErrorDialog() {}

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
        ErrorDialog dialog = new ErrorDialog(parentShell, title,
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
                "KNIME Workbench: Error occurred", message, status);
    }
}

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
 *   Feb 27, 2018 (loki): created
 */
package org.knime.workbench.ui.wrapper;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 * There was commonality between WrappedNodeDialog and WrappedMultipleNodeDialog, so i consolidated it here while i
 *  was making changes to both classes as part of AP-5670 work.
 */
public abstract class AbstractWrappedDialog extends Dialog {

    /**
     * This is the SWT-AWT bridge Composite instance.
     */
    protected Panel2CompositeWrapper m_wrapper;

    /**
     * The action taken should any AWT component contained in this dialog, should KNIME be running on a Mac, for
     *  a key events.
     */
    protected java.awt.event.KeyListener awtKeyListener;
    /**
     * The action taken should any SWT component contained in this dialog, should KNIME be running on a Mac, for
     *  a key events.
     */
    protected org.eclipse.swt.events.KeyListener swtKeyListener;

    /**
     * This constructs our superclass using parentShell, and sets the shell style to be SWT.PRIMARY_MODAL with
     *      SWT.SHELL_TRIM
     *
     * @param parentShell
     */
    protected AbstractWrappedDialog (final Shell parentShell) {
        super(parentShell);

        this.setShellStyle(SWT.PRIMARY_MODAL | SWT.SHELL_TRIM);
    }

    /**
     * Sets the common KNIME image for the shell.
     *
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);

        Image img = ImageRepository.getIconImage(SharedImages.KNIME);
        newShell.setImage(img);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleShellCloseEvent() {
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            MacKeyTracker keyTracker = MacKeyTracker.getInstance();

            keyTracker.removeListeners(this.m_wrapper.getRootFrame(), this.getShell());
        }

        super.handleShellCloseEvent();
    }

    /**
     * Subclasses should call this function before exiting their implementation of create().
     *
     * @throws IllegalStateException this is invoked, and the platform is Mac, and the concrete subclass did not
     *                  construct this instance variables awtKeyListener and swtKeyListener defined in this class.
     */
    protected void finishDialogCreation () {
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            if (this.awtKeyListener == null) {
                throw new IllegalStateException("AWT KeyListener was never constructed.");
            }

            if (this.swtKeyListener == null) {
                throw new IllegalStateException("SWT KeyListener was never constructed.");
            }

            MacKeyTracker keyTracker = MacKeyTracker.getInstance();

            keyTracker.instrumentTree(this.m_wrapper.getRootFrame(), this.awtKeyListener,
                                      this.getShell(), this.swtKeyListener);
        }
    }

    /**
     * Shows the latest error message of the dialog in a MessageBox.
     *
     * @param message The error string.
     */
    protected void showErrorMessage(final String message) {
        MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR);
        mb.setText("Error");
        mb.setMessage(message != null ? message : "(no message)");
        mb.open();
    }

    /**
     * Shows the latest error message of the dialog in a MessageBox.
     *
     * @param message The error string.
     */
    protected void showWarningMessage(final String message) {
        MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING);
        mb.setText("Warning");
        mb.setMessage(message != null ? message : "(no message)");
        mb.open();
    }

    /**
     * Show an information dialog that the settings were not changed and therefore the settings are not reset (node
     * stays executed).
     */
    protected void informNothingChanged() {
        MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("Settings were not changed.");
        mb.setMessage("The settings were not changed. The node(s) will not be reset.");
        mb.open();
    }

}

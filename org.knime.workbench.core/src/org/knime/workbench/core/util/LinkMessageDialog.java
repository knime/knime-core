/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 3, 2015 (albrecht): created
 */
package org.knime.workbench.core.util;

import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

/**
 *
 * @author albrecht
 */
public class LinkMessageDialog extends MessageDialog {

    protected Link m_link;

    /**
     * Create a message dialog. Note that the dialog will have no visual
     * representation (no widgets) until it is told to open.
     * <p>
     * Optionally create a link element underneath the message by calling setLink().
     * </p>
     * <p>
     * The labels of the buttons to appear in the button bar are supplied in
     * this constructor as an array. The <code>open</code> method will return
     * the index of the label in this array corresponding to the button that was
     * pressed to close the dialog.
     * </p>
     * <p>
     * <strong>Note:</strong> If the dialog was dismissed without pressing
     * a button (ESC key, close box, etc.) then {@link SWT#DEFAULT} is returned.
     * Note that the <code>open</code> method blocks.
     * </p>
     *
     * @param parentShell
     *            the parent shell
     * @param dialogTitle
     *            the dialog title, or <code>null</code> if none
     * @param dialogTitleImage
     *            the dialog title image, or <code>null</code> if none
     * @param dialogMessage
     *            the dialog message. Can include <a> tags for links.
     * @param dialogImageType
     *            one of the following values:
     *            <ul>
     *            <li><code>MessageDialog.NONE</code> for a dialog with no
     *            image</li>
     *            <li><code>MessageDialog.ERROR</code> for a dialog with an
     *            error image</li>
     *            <li><code>MessageDialog.INFORMATION</code> for a dialog
     *            with an information image</li>
     *            <li><code>MessageDialog.QUESTION </code> for a dialog with a
     *            question image</li>
     *            <li><code>MessageDialog.WARNING</code> for a dialog with a
     *            warning image</li>
     *            </ul>
     * @param dialogButtonLabels
     *            an array of labels for the buttons in the button bar
     * @param defaultIndex
     *            the index in the button label array of the default button
     */
    public LinkMessageDialog(final Shell parentShell, final String dialogTitle, final Image dialogTitleImage, final String dialogMessage,
        final int dialogImageType, final String[] dialogButtonLabels, final int defaultIndex) {
        super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createMessageArea(final Composite composite) {
     // create composite
        // create image
        Image image = getImage();
        if (image != null) {
            imageLabel = new Label(composite, SWT.NULL);
            image.setBackground(imageLabel.getBackground());
            imageLabel.setImage(image);
            //addAccessibleListeners(imageLabel, image);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING)
                    .applyTo(imageLabel);
        }
        // create message
        if (message != null) {
            m_link = new Link(composite, getMessageLabelStyle());
            m_link.setText(message);
            m_link.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    try {
                        //Open external browser
                        IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                        browser.openURL(new URL(e.text));
                      } catch (Exception ex) {

                      }
                }
            });
            GridDataFactory
                    .fillDefaults()
                    .align(SWT.FILL, SWT.BEGINNING)
                    .grab(true, false)
                    .hint(
                            convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH),
                            SWT.DEFAULT).applyTo(m_link);
        }
        return composite;
    }

    /**
     * Convenience method to open a simple dialog as specified by the
     * <code>kind</code> flag.
     *
     * @param kind
     *            the kind of dialog to open, one of {@link #ERROR},
     *            {@link #INFORMATION}, {@link #QUESTION}, {@link #WARNING},
     *            {@link #CONFIRM}, or {@link #QUESTION_WITH_CANCEL}.
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     * @param style
     *            {@link SWT#NONE} for a default dialog, or {@link SWT#SHEET} for
     *            a dialog with sheet behavior
     * @return <code>true</code> if the user presses the OK or Yes button,
     *         <code>false</code> otherwise
     * @since 3.5
     */
    public static boolean open(final int kind, final Shell parent, final String title,
            final String message, int style) {
        LinkMessageDialog dialog = new LinkMessageDialog(parent, title, null, message,
                kind, getButtonLabels(kind), 0);
        style &= SWT.SHEET;
        dialog.setShellStyle(dialog.getShellStyle() | style);
        return dialog.open() == 0;
    }

    /**
     * @param kind
     * @return
     */
    static String[] getButtonLabels(final int kind) {
        String[] dialogButtonLabels;
        switch (kind) {
        case ERROR:
        case INFORMATION:
        case WARNING: {
            dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL };
            break;
        }
        case CONFIRM: {
            dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL };
            break;
        }
        case QUESTION: {
            dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL,
                    IDialogConstants.NO_LABEL };
            break;
        }
        case QUESTION_WITH_CANCEL: {
            dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL,
                    IDialogConstants.NO_LABEL,
                    IDialogConstants.CANCEL_LABEL };
            break;
        }
        default: {
            throw new IllegalArgumentException(
                    "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        }
        }
        return dialogButtonLabels;
    }

    /**
     * Convenience method to open a simple confirm (OK/Cancel) dialog.
     *
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     * @return <code>true</code> if the user presses the OK button,
     *         <code>false</code> otherwise
     */
    public static boolean openConfirm(final Shell parent, final String title, final String message) {
        return open(CONFIRM, parent, title, message, SWT.NONE);
    }

    /**
     * Convenience method to open a standard error dialog.
     *
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     */
    public static void openError(final Shell parent, final String title, final String message) {
        open(ERROR, parent, title, message, SWT.NONE);
    }

    /**
     * Convenience method to open a standard information dialog.
     *
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     */
    public static void openInformation(final Shell parent, final String title,
            final String message) {
        open(INFORMATION, parent, title, message, SWT.NONE);
    }

    /**
     * Convenience method to open a simple Yes/No question dialog.
     *
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     * @return <code>true</code> if the user presses the Yes button,
     *         <code>false</code> otherwise
     */
    public static boolean openQuestion(final Shell parent, final String title,
            final String message) {
        return open(QUESTION, parent, title, message, SWT.NONE);
    }

    /**
     * Convenience method to open a standard warning dialog.
     *
     * @param parent
     *            the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *            the dialog's title, or <code>null</code> if none
     * @param message
     *            the message
     */
    public static void openWarning(final Shell parent, final String title, final String message) {
        open(WARNING, parent, title, message, SWT.NONE);
    }

}

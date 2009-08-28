/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   26.08.2009 (ohl): created
 */
package org.knime.workbench.ui.preferences;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExportPreferencesDialog extends Dialog {

    private String m_filename;

    private boolean m_overwrite;

    private Label m_errMsg;

    public ExportPreferencesDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Save Preferences To File");
    }

    public String fileName() {
        return m_filename;
    }

    public boolean overwrite() {
        return m_overwrite;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));
        createMessage(overall);
        createFileSelection(overall);
        createOverwrite(overall);
        createError(overall);
        return overall;
    }

    protected void createMessage(final Composite parent) {
        Label msg = new Label(parent, SWT.LEFT);
        msg.setLayoutData(new GridData(GridData.FILL_BOTH));
        msg.setText("Please enter the location where the "
                + "preferences should be stored:");
    }

    protected void createFileSelection(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(fillBoth);
        panel.setLayout(new GridLayout(3, false));

        Label msg = new Label(panel, SWT.RIGHT);
        msg.setLayoutData(new GridData(GridData.FILL_BOTH));
        msg.setText("File:");

        final Text filenameUI = new Text(panel, SWT.NONE);
        filenameUI.setLayoutData(fillBoth);
        filenameUI.addListener(SWT.Modify, new Listener() {
            public void handleEvent(final Event event) {
                m_filename = filenameUI.getText().trim();
                validate();
            }
        });
        final Button browse = new Button(panel, SWT.PUSH);
        browse.setText("Select...");
        browse.setToolTipText("Opens a file selection dialog.");
        browse.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
                fileDialog.setFilterExtensions(new String[]{"*.epf", "*.*"});
                fileDialog.setText("Specify the preferences export file.");
                if (m_filename != null) {
                    fileDialog.setFileName(m_filename);
                }
                String filePath = fileDialog.open();
                if (filePath != null && filePath.trim().length() > 0) {
                    if (filePath.length() < 5
                            || filePath.lastIndexOf('.') < filePath.length() - 4) {
                        // they have no extension - add .epf
                        filePath += ".epf";
                    }

                    m_filename = filePath;
                    filenameUI.setText(filePath);
                    validate();
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent se) {
                widgetSelected(se);
            }
        });
    }

    protected void createError(final Composite parent) {
        m_errMsg = new Label(parent, SWT.LEFT);
        m_errMsg.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_errMsg.setText("");
        m_errMsg.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
    }

    private void createOverwrite(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(fillBoth);
        panel.setLayout(new GridLayout(1, false));

        final Button overwriteUI = new Button(panel, SWT.CHECK);
        overwriteUI.setText("Overwrite existing file");
        overwriteUI.setToolTipText("Check this to overwrite an existing file");
        overwriteUI.setSelection(false);
        overwriteUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                m_overwrite = overwriteUI.getSelection();
                validate();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent se) {
                widgetSelected(se);
            }
        });
    }

    private void validate() {
        String errMsg = "unknown error";

        try {
            if (m_filename == null || m_filename.isEmpty()) {
                errMsg = "Please enter a file location.";
                return;
            }
            File sel = new File(m_filename);
            if (sel.exists()) {
                if (sel.isFile() && sel.canWrite()) {
                    if (!m_overwrite) {
                        errMsg =
                                "File exists. Please check 'overwrite "
                                        + "existing file' to overwrite.";
                    } else {
                        errMsg = null;
                    }
                    return;
                }
                if (sel.isDirectory()) {
                    errMsg = "Please select a file location. Not a directory.";
                    return;
                }
                errMsg = "Can't write to selected location.";
                return;
            }
            // lets hope we can create it then...
            errMsg = null;
            return;
        } catch (Throwable t) {
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                errMsg = t.getMessage();
            }
            return;
        } finally {
            if (errMsg == null || errMsg.isEmpty()) {
                m_errMsg.setText("");
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            } else {
                m_errMsg.setText(errMsg);
                getButton(IDialogConstants.OK_ID).setEnabled(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }
}

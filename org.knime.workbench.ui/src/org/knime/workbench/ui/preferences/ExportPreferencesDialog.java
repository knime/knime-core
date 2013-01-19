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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));
        createHeader(overall);
        createFileSelection(overall);
        createOverwrite(overall);
        createError(overall);
        return overall;
    }

    private void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(2, false));
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText("Export KNIME Preferences");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label mkdirIcon = new Label(header, SWT.NONE);
        mkdirIcon.setBackground(white);
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        txt.setText("Please enter the name of the file the preferences should "
                + "be saved to. Or select a destination.");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    protected void createFileSelection(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(fillBoth);
        panel.setLayout(new GridLayout(1, true));

        Group border = new Group(panel, SWT.SHADOW_IN);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        border.setLayout(new GridLayout(2, false));
        border.setText("File to store preferences:");

        final Text filenameUI =
                new Text(border, SWT.FILL | SWT.SINGLE | SWT.BORDER);
        filenameUI.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        filenameUI.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                m_filename = filenameUI.getText().trim();
                validate();
            }
        });
        final Button browse = new Button(border, SWT.PUSH);
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

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   17.10.2011 (wiswedel): created
 */
package org.knime.workbench.editor2.actions;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.crypto.HexUtils;
import org.knime.core.util.crypto.KNIMEDecryptionStream;

/**
 * Allows changing the meta node lock (set password).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich
 */
public class LockMetaNodeDialog extends IconAndMessageDialog {

    private final WorkflowManager m_workflow;
    private String m_newPassword;
    private String m_passwordHint;

    private Button m_enableChecker;
    private Button m_showPlainChecker;
    private Text m_passwordTextField;
    private Label m_passwordKnimeCOMLabel;
    private Text m_passwordKnimeCOMTextField;
    private Label m_passwordHintLabel;
    private Text m_passwordHintTextField;


    /**
     * Dialog with enable checker and two text fields in which the user enters
     * the password and the password hint.
     *
     * @param parent the parent shell for this dialog
     */
    public LockMetaNodeDialog(final Shell parent, final WorkflowManager mgr) {
        super(parent);
        super.message = "Metanode Locking";
        m_workflow = mgr;
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    /** {@inheritDoc} */
    @Override
    protected Control createDialogArea(final Composite parent) {
        createMessageArea(parent);
        m_enableChecker = new Button(parent, SWT.CHECK);
        m_enableChecker.setText("Enable password protection");
        m_passwordTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        m_showPlainChecker = new Button(parent, SWT.CHECK);
        m_passwordKnimeCOMLabel = new Label(parent, 0);
        m_passwordKnimeCOMLabel.setText("KNIME.com encrypted password (HEX)");
        m_passwordKnimeCOMTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        m_passwordHintLabel = new Label(parent, 0);
        m_passwordHintLabel.setText("Password Hint (or copyright message)");
        m_passwordHintTextField = new Text(parent, SWT.MULTI | SWT.WRAP
                | SWT.V_SCROLL | SWT.BORDER);
        String hint = m_workflow.getPasswordHint();
        m_passwordHintTextField.setText(hint == null ? "" : hint);
        m_enableChecker.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                updateEnableStatus();
            }
        });
        m_showPlainChecker.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                updateEnableStatus();
            }
        });
        m_showPlainChecker.setText("Show plaintext");
        boolean isEncrypted = m_workflow.isEncrypted();
        m_enableChecker.setSelection(isEncrypted);
        m_showPlainChecker.setSelection(false);
        final int indent = 20;
        GridData gd = new GridData();
//        gd.heightHint = convertVerticalDLUsToPixels(BAR_DLUS);
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_enableChecker.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_passwordTextField.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_showPlainChecker.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_passwordKnimeCOMLabel.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_passwordKnimeCOMTextField.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        m_passwordHintLabel.setLayoutData(gd);

        gd = new GridData();
        gd.horizontalIndent = indent;
        gd.horizontalAlignment = GridData.FILL;
        gd.verticalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 100;
        gd.horizontalSpan = 2;
        m_passwordHintTextField.setLayoutData(gd);
        updateEnableStatus();

        return parent;
    }

    private void updateEnableStatus() {
        boolean isEnabled = m_enableChecker.getSelection();
        boolean isPlainText = m_showPlainChecker.getSelection();
        m_showPlainChecker.setEnabled(isEnabled);
        m_passwordTextField.setEnabled(isEnabled);
        if (!isEnabled) {
            m_passwordTextField.setText("");
        } else {
            m_passwordTextField.setFocus();
        }
        m_passwordTextField.setEchoChar(isPlainText ? '\0' : '*');
        m_passwordHintLabel.setEnabled(isEnabled);
        m_passwordHintTextField.setEnabled(isEnabled);
    }

    /** {@inheritDoc} */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Metanode Locking");
    }

    /**
     * Linux (GTK) hack: must explicitly invoke <code>getInitialSize()</code>.
     *
     * @see org.eclipse.jface.window.Window#create()
     */
    @Override
    public void create() {
        super.create();
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("linux")) {
            getShell().setSize(getInitialSize());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Image getImage() {
        return getInfoImage();
    }

    /**
     * Invoked by the super class if ok is pressed. Copies the text field
     * content to member variables, so that they can be accessed afterwards.
     * (Neccessary as the widgets will be disposed)
     *
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        String pass = m_passwordTextField.getText();
        boolean enabled = m_enableChecker.getSelection();
        if (enabled && (pass == null || pass.isEmpty())) {
            MessageDialog.openError(getParentShell(), "Empty password",
                    "The password you entered is empty");
        } else {
            if (enabled) {
                try {
                    verifyPassword(pass, m_passwordKnimeCOMTextField.getText());
                } catch (Exception e) {
                    MessageDialog.openError(getParentShell(),
                            "Invalid password", "The password you entered does "
                            + "not match the signature (write EMail to "
                            + "contact@knime.com for details)");
                    return;
                }
                m_newPassword = pass;
                m_passwordHint = m_passwordHintTextField.getText();
            } else {
                m_newPassword = null;
                m_passwordHint = null;
            }
            setReturnCode(OK);
            close();
        }
    }

    /** Compares the password with the knime.com private key encrypted password.
     * This probably should go into the KNIME (safer) but all this is
     * weak anyway.
     * @param password The password as entered
     * @param knimeCOMPassword The hex string of the knime.com encrypted
     *        password (it's deciphered and compared with the
     * @throws Exception If decryption fails or the passwords don't match.
     */
    private static void verifyPassword(final String password,
            final String knimeCOMPassword) throws Exception {
        byte[] knimeCOMPasswordBytes = HexUtils.hexToBytes(knimeCOMPassword);
        ByteArrayInputStream in =
            new ByteArrayInputStream(knimeCOMPasswordBytes);
        KNIMEDecryptionStream decr = new KNIMEDecryptionStream(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(decr));
        String str = reader.readLine();
        if (!str.equals(password)) {
            throw new Exception("Passwords don't match.");
        }
    }

    /** @return password as entered or null if disabled. */
    String getPassword() {
        return m_newPassword;
    }

    /** @return password hint as entered or null if disabled. */
    String getPasswordHint() {
        return m_passwordHint;
    }

}

/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 */
package org.knime.workbench.ui.masterkey;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.Credentials;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG
 */
public class CredentialVariablesEditDialog extends Dialog {

    private Text m_name;
    private Text m_login;
    private Text m_pass;
    
    private Credentials m_credential;

    /**
     *
     */
    public CredentialVariablesEditDialog() {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
    }


    /** {@inheritDoc} */
    @Override
    protected boolean isResizable() {
        return false;
    }

    /**
     * Load the given {@link CredentialVariablesEditDialog} into this GUI 
     * component.
     * @param credential to load name, login, and password from
     */
    public void loadFrom(final Credentials credential) {
        m_name.setText(credential.getName());
        m_name.setEditable(false);
        String login = credential.getLogin();
        m_login.setText(login == null ? "" : login);
        String password = credential.getPassword();
        m_pass.setText(password == null ? "" : password);
        if (login == null || login.isEmpty()) {
            m_login.setFocus();
        } else {
            m_pass.setFocus();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        parent.getShell().setText("Add/Edit Credentials");
        Composite twoColComp = new Composite(parent, SWT.NONE);
        twoColComp.setLayout(new GridLayout(2, true));

        GridData horizontalFill = new GridData(GridData.FILL_HORIZONTAL);

        // first row: name
        Label nameLabel = new Label(twoColComp, SWT.NONE);
        nameLabel.setText("Credential Identifier: ");
        m_name = new Text(twoColComp, SWT.BORDER);
        m_name.setLayoutData(horizontalFill);

        // second row: login
        Label loginLabel = new Label(twoColComp, SWT.NONE);
        loginLabel.setText("User Login: ");

        m_login = new Text(twoColComp, SWT.BORDER);
        m_login.setLayoutData(horizontalFill);
        
        // third row: password
        Label passwordLabel = new Label(twoColComp, SWT.NONE);
        passwordLabel.setText("User Password: ");

        m_pass = new Text(twoColComp, SWT.BORDER | SWT.PASSWORD);
        m_pass.setLayoutData(horizontalFill);
        return twoColComp;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        String varName = m_name.getText().trim();
        if (varName.isEmpty()) {
            String msg = "Credential identifier must not be empty!";
            showError(msg);
            throw new OperationCanceledException(msg);
        }
        String name = m_name.getText();
        String login = m_login.getText();
        String pass = m_pass.getText();
        m_credential = new Credentials(name, login, pass);
        super.okPressed();
    }

    private void showError(final String message) {
        MessageDialog.openError(getParentShell(), "Error", message);
    }

    /**
     *
     * @return the created {@link Credentials} or <code>null</code>
     */
    Credentials getCredential() {
        return m_credential;
    }
}

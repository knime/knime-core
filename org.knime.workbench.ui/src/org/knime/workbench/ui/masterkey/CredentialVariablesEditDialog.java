/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
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

/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.CredentialsStore;

/**
 * Dialog that let the user add, edit or remove credentials. Existing
 * variables are listed in a {@link CredentialVariableTable} with name, login
 * and password. Credentials can be added or edited with the
 * {@link CredentialVariablesEditDialog}.
 *
 * @author Thomas Gabriel, KNIME.com AG
 */
public class CredentialVariablesDialog extends Dialog {

    private CredentialVariableTable m_table;

    private List<Credentials> m_credentials;

    private final String m_workflowName;

    private Button m_addVarBtn;

    private Button m_editVarBtn;

    private Button m_removeVarBtn;

   /**
    * Create a new dialog instance to edit credentials.
    * @param shell parent shell
    * @param store holding the current <code>Credentials</code>
    */
   public CredentialVariablesDialog(final Shell shell,
           final CredentialsStore store) {
       this(shell, store, null);
   }

    /**
     * Create a new dialog instance to edit credentials.
     * @param shell parent shell
     * @param credentials list of current <code>Credentials</code>
     */
    public CredentialVariablesDialog(final Shell shell,
            final List<Credentials> credentials) {
        this(shell, credentials, null);
    }

   /**
    * Create a new dialog instance to edit credentials.
    * @param shell parent shell
    * @param store holding the current <code>Credentials</code>
    * @param workflowName the name of the workflow to edit credentials
    */
   public CredentialVariablesDialog(final Shell shell,
           final CredentialsStore store, final String workflowName) {
       super(shell);
       m_workflowName = workflowName;
       m_credentials = new ArrayList<Credentials>();
       if (store != null) {
           for (Credentials cred : store.getCredentials()) {
               m_credentials.add(cred);
           }
       }
   }

    /**
     * Create a new dialog instance to edit credentials.
     * @param shell parent shell
     * @param credentials list of current <code>Credentials</code>
     * @param workflowName the name of the workflow to edit credentials
     */
    public CredentialVariablesDialog(final Shell shell,
            final List<Credentials> credentials, final String workflowName) {
        super(shell);
        m_workflowName = workflowName;
        if (credentials == null) {
            m_credentials = new ArrayList<Credentials>();
        } else {
            m_credentials = new ArrayList<Credentials>(credentials);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        parent.getShell().setText("Workflow Credentials...");
        Composite composite = new Composite(parent, SWT.NONE);
        if (m_workflowName != null) {
            Label label = new Label(composite, SWT.NONE);
            label.setText("Edit Credentials for Workflow '"
                + m_workflowName + "'.");
        }

        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // first row (new composite):
        Composite tableAndBtnsComp = new Composite(composite, SWT.NONE);
        tableAndBtnsComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        tableAndBtnsComp.setLayout(new GridLayout(2, false));

        // first column: table
        Composite tableComp = new Composite(tableAndBtnsComp, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = true;
        tableComp.setLayout(new FillLayout());
        tableComp.setLayoutData(gridData);
        m_table = new CredentialVariableTable(tableComp);
        for (Credentials cred : m_credentials) {
            m_table.add(cred);
        }
        m_table.getViewer().refresh();
        m_table.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                Table table = m_table.getViewer().getTable();
                int index = table.getSelectionIndex();
                // we only get a double-click event for existing items
                Credentials var = m_table.get(index);
                editCredentials(var, index);
            }
        });

        // second column: 3 buttons
        Composite btnsComp = new Composite(tableAndBtnsComp, SWT.NONE);
        btnsComp.setLayout(new GridLayout(1, false));
        gridData = new GridData();
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        btnsComp.setLayoutData(gridData);

        m_addVarBtn = new Button(btnsComp, SWT.PUSH);
        m_addVarBtn.setText("Add");
        m_addVarBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                addCredential();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        gridData.heightHint = 20;
        m_addVarBtn.setLayoutData(gridData);

        m_editVarBtn = new Button(btnsComp, SWT.PUSH);
        m_editVarBtn.setText("Edit");
        m_editVarBtn.setLayoutData(gridData);
        m_editVarBtn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                int selectionIdx = m_table.getViewer().getTable()
                    .getSelectionIndex();
                if (selectionIdx < 0) {
                    MessageDialog.openWarning(getShell(), "Empty selection",
                        "Please select the credential you want to edit.");
                    return;
                }
                Credentials selectedCred = m_table.get(selectionIdx);
                editCredentials(selectedCred, selectionIdx);
            }
        });

        m_removeVarBtn = new Button(btnsComp, SWT.PUSH);
        m_removeVarBtn.setText("Remove");
        m_removeVarBtn.setLayoutData(gridData);
        m_removeVarBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                int idx = m_table.getViewer().getTable().getSelectionIndex();
                if (idx < 0) {
                    MessageDialog.openWarning(getShell(), "Empty selection",
                            "Please select the parameter you want to remove.");
                    return;
                }
                Credentials cred =
                    (Credentials)((IStructuredSelection)m_table
                        .getViewer().getSelection()).getFirstElement();
                removeCredential(cred);
            }
        });
        return composite;
    }

    private void addCredential() {
        CredentialVariablesEditDialog dialog =
            new CredentialVariablesEditDialog();
        if (dialog.open() == Dialog.CANCEL) {
            // if the user has canceled the dialog there is nothing left to do
            return;
        }
        Credentials var = dialog.getCredential();
        if (var == null) {
            // variables was not created
            return;
        }
        // do not add it do WFM directly -> this is done when closing the dialog
        if (!m_table.add(var)) {
            MessageDialog.openWarning(getShell(), "Credential already exists",
                    " A credential with the same name and type already exists. "
                    + "Edit this one if you want to change the value.");
        } else {
            m_table.getViewer().refresh();
        }
        getShell().forceFocus();
    }

    private void editCredentials(final Credentials cred,
            final int selectionIdx) {
        CredentialVariablesEditDialog dlg = new CredentialVariablesEditDialog();
        dlg.create();
        dlg.loadFrom(cred);
        if (dlg.open() == Dialog.OK) {
            Credentials var = dlg.getCredential();
            if (var != null) {
                m_table.replace(selectionIdx, var);
                m_table.getViewer().refresh();
            }
        }
    }

    private void removeCredential(final Credentials var) {
        m_table.remove(var);
        m_table.getViewer().refresh();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        m_credentials = m_table.getCredentials();
        super.okPressed();
    }

    /**
     * @return a list of <code>Credentials</code> entered in the dialog
     */
    public List<Credentials> getCredentials() {
        return m_credentials;
    }

}

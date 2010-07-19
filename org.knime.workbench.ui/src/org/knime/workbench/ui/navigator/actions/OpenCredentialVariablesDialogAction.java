/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
package org.knime.workbench.ui.navigator.actions;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;

/**
 * 
 * @author Thomas Gabriel, KNIME.com GmbH
 */
public class OpenCredentialVariablesDialogAction 
        extends AbstractWorkflowAction {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        super.run();
        // get workflow
        final WorkflowManager wf = getWorkflow();
        // open the dialog
        final Display d = Display.getDefault();
        // run in UI thread 
        d.asyncExec(new Runnable() {
            @Override
            public void run() {
                CredentialsStore store = wf.getCredentialsStore();
                CredentialVariablesDialog dialog = 
                    new CredentialVariablesDialog(d.getActiveShell(), store);
                if (dialog.open() == Dialog.OK) {
                    for (Credentials cred : store.getCredentials()) {
                        store.remove(cred.getName());
                    }
                    List<Credentials> credentials = dialog.getCredentials();
                    for (Credentials cred : credentials) {
                        store.add(cred);
                    }
                }
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Workflow Credentials...";
    }

}

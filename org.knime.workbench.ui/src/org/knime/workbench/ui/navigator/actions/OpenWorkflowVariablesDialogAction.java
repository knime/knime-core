/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.wfvars.WorkflowVariablesDialog;

/**
 * Action which opens {@link WorkflowVariablesDialog} that let the user add, 
 * edit or remove workflow variables.
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class OpenWorkflowVariablesDialogAction extends AbstractWorkflowAction {
    
    /**
     * 
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
                // and put it into the workflow variables dialog
                WorkflowVariablesDialog dialog = new WorkflowVariablesDialog(
                        d.getActiveShell(), wf);
                dialog.open();
            }
        });
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Workflow Variables...";
    }

}

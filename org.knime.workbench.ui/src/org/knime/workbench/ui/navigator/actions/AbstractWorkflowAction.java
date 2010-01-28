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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 *
 * @author Fabian Dill, KNIME.com GmbH
 */
public abstract class AbstractWorkflowAction extends Action {

    private WorkflowManager m_workflow;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // does not work to ask for the selection service and get the selection
        // by part id: might be a derived class with different ID
        // get selection
        IStructuredSelection s = (IStructuredSelection)PlatformUI
                .getWorkbench().getActiveWorkbenchWindow()
                .getSelectionService().getSelection();
        if (s == null) {
            return false;
        }
        Object element = s.getFirstElement();
        // check if is KNIME workflow
        if (element instanceof IContainer) {
            IContainer cont = (IContainer)element;
            if (cont.exists(new Path(WorkflowPersistor.WORKFLOW_FILE))) {
                m_workflow = (WorkflowManager)ProjectWorkflowMap
                    .getWorkflow(cont.getFullPath());
                if (m_workflow != null) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     *
     * @return the underlying workflow
     */
    public WorkflowManager getWorkflow() {
        return m_workflow;
    }
}

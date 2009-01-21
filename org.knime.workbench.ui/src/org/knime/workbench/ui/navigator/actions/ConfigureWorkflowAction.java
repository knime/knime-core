/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.navigator.KnimeResourceNavigator;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class ConfigureWorkflowAction extends Action {
    
    private WorkflowManager m_workflow;
    
    @Override
    public String getText() {
        return "Configure...";
    }
    
    @Override
    public String getDescription() {
        return "Opens a configuration dialog for this workflow";
    }
    
    @Override
    public void run() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WrappedNodeDialog dialog = new WrappedNodeDialog(
                            Display.getDefault().getActiveShell(), m_workflow);
                    dialog.setBlockOnOpen(true);
                    dialog.open();
                } catch (final NotConfigurableException nce) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(
                                    Display.getDefault().getActiveShell(), 
                                    "Workflow Not Configurable", 
                                    "This workflow can not be configured: "
                                    + nce.getMessage());
                        }
                    });
                }
                
            }
        });
    }
    
    @Override
    public boolean isEnabled() {
        // get selection
          IStructuredSelection s = (IStructuredSelection)PlatformUI.getWorkbench()
          .getActiveWorkbenchWindow().getSelectionService()
          .getSelection(KnimeResourceNavigator.ID);
          Object element = s.getFirstElement();
          // check if is KNIME workflow
          if (element instanceof IContainer) {
              IContainer cont = (IContainer)element;
              if (cont.exists(new Path(WorkflowPersistor.WORKFLOW_FILE))) {
                  m_workflow = (WorkflowManager)ProjectWorkflowMap.getWorkflow(
                          cont.getFullPath().toString());
                  if (m_workflow != null && m_workflow.hasDialog()) {
                      return true;
                  }
              }
          }
        return false;
    }

}

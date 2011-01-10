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
package org.knime.workbench.ui.wizards.workflowgroup;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.wizards.project.NewProjectWizardPage;


/**
 * Wizard to create a new workflow group.
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class NewWorkflowGroupProjectWizard extends Wizard 
    implements INewWizard {

    /** ID as defined in plugin-xml. */
    public static final String ID = "org.knime.workbench.ui.newworkflowgroup";
    
    private NewProjectWizardPage m_page;

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        final IPath projectPath = m_page.getWorkflowPath();
        final WorkflowGroupCreationOperation op 
            = new WorkflowGroupCreationOperation(projectPath);
        try {
            getContainer().run(true, false, op);
            IContainer result = op.getCreatedWorkflowGroup();
            // try to reveal this resource in knime resource navigator
            KnimeResourceUtil.revealInNavigator(result);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // get the exception that issued this async exception
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            return false;
        }
        return true;
    }
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench, 
            final IStructuredSelection selection) {
        setWindowTitle("Create new workflow group");
//        m_page.setDescription("Creates a new )
        m_page = new NewProjectWizardPage(selection, false);
        m_page.setTitle("New Knime workflow group");
        m_page.setDescription("Creates a new workflow group, " 
                + "which allows for the grouping of several KNIME workflows.");
        addPage(m_page);
    }

}

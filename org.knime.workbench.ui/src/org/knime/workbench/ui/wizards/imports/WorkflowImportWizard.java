/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   12.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Wizard for importing workflows and workflow groups from a directory or an
 * archive file into the workspace.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowImportWizard extends Wizard {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowImportWizard.class);

    private WorkflowImportSelectionPage m_import;

    private IContainer m_initialDestination = ResourcesPlugin.getWorkspace()
        .getRoot();

    /**
     *
     */
    public WorkflowImportWizard() {
        super();
        setWindowTitle("Import");
        setNeedsProgressMonitor(true);
    }

    /**
     * Sets the initial destination of the import, only workflow groups or root
     * are allowed. If a workflow or node is passed the next higher workflow
     * group or - eventually - root is chosen.
     *
     * @param destination the inital destination of the import
     */
    public void setInitialDestination(final IContainer destination) {
        // TODO: make here all the necessary checks whether the initial
        // selection is valid: workflow group or root only...
        IContainer helper = destination;
        IContainer root = ResourcesPlugin.getWorkspace().getRoot();
        while (helper != null
                && !KnimeResourceUtil.isWorkflowGroup(helper)
                && !helper.equals(root)) {
            helper = helper.getParent();
        }
        m_initialDestination = helper;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
            super.addPages();
            m_import = new WorkflowImportSelectionPage();
            m_import.restoreDialogSettings();
            m_import.setInitialTarget(m_initialDestination);
            addPage(m_import);
            // the next page is returned by the import page
            setForcePreviousAndNextButtons(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        // save WidgetValues
        m_import.saveDialogSettings();
        return createWorkflows();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        return m_import.getNextPage();
    }



    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_import.canFinish();
    }

    /**
     * Create the selected workflows.
     *
     * @return boolean <code>true</code> if all project creations were
     *         successful.
     */
    public boolean createWorkflows() {
       final boolean copy = m_import.isCopyWorkflows();
       final String target = m_import.getDestination();
       final Collection<IWorkflowImportElement> workflows = m_import
           .getWorkflowsToImport();
       // validate target path
       final IPath targetPath = getValidatedTargetPath(target);
       WorkspaceModifyOperation op = new WorkflowImportOperation(
               workflows, targetPath, copy, getShell());
        // run the new project creation operation
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // one of the steps resulted in a core exception
            Throwable t = e.getTargetException();
            String message = "Error during import!";
            IStatus status;
            if (t instanceof CoreException) {
                status = ((CoreException) t).getStatus();
            } else {
                status = new Status(IStatus.ERROR,
                        KNIMEUIPlugin.PLUGIN_ID, 1, message, t);
            }
            LOGGER.error(message, t);
            ErrorDialog.openError(getShell(), message, null, status);
            return false;
        } catch (Exception e) {
            String message = "Error during import!";
            IStatus status = new Status(IStatus.ERROR,
                    KNIMEUIPlugin.PLUGIN_ID, 1, message, e);
            ErrorDialog.openError(getShell(), message, null, status);
            LOGGER.error(message, e);
        }
        return true;
    }

    private IPath getValidatedTargetPath(final String destination) {
        if (destination == null || destination.isEmpty()) {
            return ResourcesPlugin.getWorkspace().getRoot().getFullPath();
        }
        IPath path = new Path(destination);
        return path;
    }


}

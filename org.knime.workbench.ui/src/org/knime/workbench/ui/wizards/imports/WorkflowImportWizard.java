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

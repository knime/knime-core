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
 * @author Fabian Dill, KNIME.com AG
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
        setWindowTitle("New KNIME Workflow Group Wizard");
        m_page = new NewProjectWizardPage(selection, false);
        m_page.setDescription("Creates a new KNIME workflow group, "
                + "which allows for the grouping of several KNIME workflows.");
        addPage(m_page);
    }

}

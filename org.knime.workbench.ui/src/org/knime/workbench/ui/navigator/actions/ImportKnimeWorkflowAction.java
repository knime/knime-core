/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 * 
 * History
 *   20.10.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wizards.imports.WorkflowImportWizard;

/**
 * Action to invoke the KNIME import wizard.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class ImportKnimeWorkflowAction extends Action {
    
    private static final ImageDescriptor ICON 
        = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, "icons/knime_import.png");
    
    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEImport";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */
    private final IWorkbenchWindow m_workbenchWindow;

    /**
     * Create a new instance of this class.
     * 
     * @param window the window
     */
    public ImportKnimeWorkflowAction(final IWorkbenchWindow window) {
        super("Import KNIME workflow...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        this.m_workbenchWindow = window;
        setToolTipText("Imports a KNIME workflow from an archive"
                + " or a directory structure");
        setId(ID);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    @Override
    public void run() {
        WorkflowImportWizard wizard = new WorkflowImportWizard();
        WizardDialog dialog = new WizardDialog(
                Display.getDefault().getActiveShell(), wizard);
        IStructuredSelection selectionToPass;
        // get the current workbench selection
        ISelection workbenchSelection = m_workbenchWindow.getSelectionService()
            .getSelection();
        if (workbenchSelection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection)workbenchSelection;
            if (selectionToPass.size() == 1) {
                Object o = selectionToPass.getFirstElement();
                if (o instanceof IContainer) {
                    wizard.setInitialDestination((IContainer)o);
                }
            }
        }
        dialog.open();
    }

}

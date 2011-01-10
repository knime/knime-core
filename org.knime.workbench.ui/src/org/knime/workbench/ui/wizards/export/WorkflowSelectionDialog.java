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
 *   18.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.export;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.wizards.workflowgroup.KnimeResourceContentProviderWithRoot;
import org.knime.workbench.ui.wizards.workflowgroup.KnimeResourceLabelProviderWithRoot;

/**
 * A dialog to select several workflows that exist in the workspace. Used for 
 * example in the workflow export wizard. 
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowSelectionDialog extends Dialog {
    
    private TreeViewer m_viewer;

    private IStructuredSelection m_selectedObjs;
    
    private IStructuredSelection m_initialSelection;
    
    /**
     * 
     * @param parentShell parent shell
     */
    public WorkflowSelectionDialog(final Shell parentShell) {
        super(parentShell);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Workflow Selection");
    }
    
    /**
     * 
     * @param selection the initially selected workflow/workflow group
     */
    protected void setInitialSelection(final IStructuredSelection selection) {
        m_initialSelection = selection;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        // a tree viewer to select a workflow group or workflow
        Group overall = new Group(parent, SWT.SHADOW_ETCHED_IN);
        overall.setText("Export selection:");
        overall.setLayout(new GridLayout(1, false));
        GridData shellLayout = new GridData(GridData.FILL_BOTH);
        shellLayout.widthHint = 300;
        shellLayout.heightHint = 350;
        overall.setLayoutData(shellLayout);
        
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        m_viewer = new TreeViewer(overall, SWT.BORDER 
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        m_viewer.getTree().setLayoutData(fillBoth);
        m_viewer.setLabelProvider(new KnimeResourceLabelProviderWithRoot());
        m_viewer.setContentProvider(new KnimeResourceContentProviderWithRoot());
        m_viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(final Viewer viewer, 
                    final Object parentElement, final Object element) {
                if (element instanceof IWorkspaceRoot) {
                    return true;
                }
                IResource resource = null;
                if (element instanceof NodeContainer) {
                    ProjectWorkflowMap.findProjectFor(
                            ((NodeContainer)element).getID());
                } else if (element instanceof IResource) {
                    resource = (IResource)element;
                }
                if (KnimeResourceUtil.isWorkflow(resource)) {
                    return true;
                }
                if (KnimeResourceUtil.isWorkflowGroup(resource)) {
                    return true;
                }
                return false;
            }
        });
        m_viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
        m_viewer.addPostSelectionChangedListener(
                new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        m_selectedObjs = (IStructuredSelection)m_viewer
                                .getSelection();
                    }
            
        });
        if (m_initialSelection != null) {
            m_viewer.setSelection(m_initialSelection);
        }
        m_viewer.expandAll();
        return overall;
    }
    
    /**
     * 
     * @return the selected object (either workflow or workflow group as 
     * IResource or a NodeContainer)
     */
    public IStructuredSelection getSelection() {
        return m_selectedObjs;
    }

}

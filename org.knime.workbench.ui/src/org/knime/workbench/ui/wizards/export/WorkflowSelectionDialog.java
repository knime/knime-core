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

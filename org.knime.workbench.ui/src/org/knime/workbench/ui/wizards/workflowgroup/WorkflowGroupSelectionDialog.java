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
 *   13.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.workflowgroup;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Dialog to select one of the existing workflow groups.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowGroupSelectionDialog extends Dialog {

    private TreeViewer m_viewer;
    
    private IStructuredSelection m_initialSelection;
    
    private IContainer m_selectedLocation 
        = ResourcesPlugin.getWorkspace().getRoot();
    
    /**
     * 
     * @param parentShell parent shell
     */
    public WorkflowGroupSelectionDialog(final Shell parentShell) {
        super(parentShell);
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Workflow Group Selection");
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        overall.setLayout(new GridLayout(1, false));
        GridData overallData = new GridData(SWT.FILL, SWT.FILL, true, true);
        overallData.widthHint = 300;
        overallData.heightHint = 350;
        overall.setLayoutData(overallData);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        Label label = new Label(overall, SWT.NONE);
        label.setText("Select a workflow group (or nothing for root):  ");
        m_viewer = new TreeViewer(overall, SWT.BORDER | SWT.SINGLE 
                | SWT.H_SCROLL | SWT.V_SCROLL);
        m_viewer.getTree().setLayoutData(fillBoth);
        // set label provider
        m_viewer.setLabelProvider(new KnimeResourceLabelProviderWithRoot());
        // set content provider
        m_viewer.setContentProvider(new KnimeResourceContentProviderWithRoot());
        // set filter (retain only workflow groups)
        m_viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(final Viewer viewer, 
                    final Object parentElement, final Object element) {
                if (element instanceof IWorkspaceRoot) {
                    return true;
                }
                if (element instanceof IResource) {
                    return KnimeResourceUtil.isWorkflowGroup(
                            (IResource)element);
                }
                return false;
            }
        });
        // set input
        m_viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
        // add selection listener
        m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                m_selectedLocation = findSelectedWorkflowGroup();
            }
            
        });
        if (m_initialSelection != null && !m_initialSelection.equals(
                TreeSelection.EMPTY)) {
            m_viewer.setSelection(m_initialSelection);
            m_selectedLocation = findSelectedWorkflowGroup();
        } else {
            // empty selection
            m_viewer.setSelection(null);
            m_selectedLocation = ResourcesPlugin.getWorkspace().getRoot();
        }
        return overall; 
    }
    
    private IContainer findSelectedWorkflowGroup() {
        IStructuredSelection iss = (IStructuredSelection)m_viewer
            .getSelection();
        if (iss != null && !iss.isEmpty()) {
            Object o = iss.getFirstElement();
            if (o instanceof IContainer) {
                return (IContainer)o;
            }
        }
        return null;
    }
    
    /**
     * 
     * @param path the path to select initially
     */
    public void setInitialSelection(final IPath path) {
        m_initialSelection = pathToTreeSelection(path);
    }
    
    /**
     * 
     * @param path the path of a resource
     * @return the selection to be passed to a tree in order to select the 
     *  resource denoted by the given path
     */
    public static IStructuredSelection pathToTreeSelection(final IPath path) {
        if (path != null) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot(); 
            IResource r = root.findMember(path);
            if (r != null && !r.equals(root)) {
                IContainer c = r.getParent();
                if (r instanceof IContainer) {
                    c = (IContainer)r;
                }
                String[] segments = c.getFullPath().segments();
                Object[] treePathSegments = new Object[segments.length];
                // find all parents in order to create the path segments
                int i = 1;
                while (c.getParent() != null) {
                    treePathSegments[treePathSegments.length - i] = c;
                    c = c.getParent();
                    i++;
                }
                TreePath treePath = new TreePath(treePathSegments);
                return new TreeSelection(treePath);
            }
        }
        // default: return empty selection
        return new TreeSelection();
    }
    
    /**
     * 
     * @return the selected workflow group or the workspace root if nothing 
     *  was selected
     */
    public IContainer getSelectedWorkflowGroup() {
        return m_selectedLocation;
    }

}

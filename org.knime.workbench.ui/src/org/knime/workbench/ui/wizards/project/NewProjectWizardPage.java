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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.02.2005 (georg): created
 */
package org.knime.workbench.ui.wizards.project;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.wizards.workflowgroup.NewWorkflowGroupProjectWizard;
import org.knime.workbench.ui.wizards.workflowgroup.WorkflowGroupSelectionDialog;

/**
 * Page that is used to create either a workflow or a workflow group. 
 * 
 * @see NewProjectWizard
 * @see NewWorkflowGroupProjectWizard
 * 
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class NewProjectWizardPage extends WizardPage {
    private static final String INITIAL_PROJECT_NAME = "KNIME_project";
    
    private static final String DEFAULT_WORKFLOW_GROUP_NAME = "workflow_group";
    
    private static final String WORKFLOW_GROUP = "Workflow group";
    private static final String WORKFLOW = "Workflow";
    
    private static final ImageDescriptor ICON = KNIMEUIPlugin
        .imageDescriptorFromPlugin(
            KNIMEUIPlugin.PLUGIN_ID, "icons/new_knime55.png"); 
    
    private IResource m_initiallySelectedResource;
    
    private Text m_projectName;
    
    private Text m_destinationUI;
    
    private final boolean m_isWorkflow;
    
    private final String m_elementName;
    
    /**
     * Create and init the page.
     * 
     * @param initialSelection the initial selection
     * @param isWorkflow true if used to create a workflow, false if used to 
     *  create a workflow group
     * 
     */
    public NewProjectWizardPage(final IStructuredSelection initialSelection,
            final boolean isWorkflow) {
        super("wizardPage");
        m_isWorkflow = isWorkflow;
        if (m_isWorkflow) {
            m_elementName = WORKFLOW;
        } else {
            m_elementName = WORKFLOW_GROUP;
        }
        setTitle("New KNIME Workflow Wizard");
        setDescription("Create a new KNIME " 
                + m_elementName.toLowerCase() + ".");
        setImageDescriptor(ICON);
        extractInitiallySelectedResource(initialSelection);
    }
    
    private void extractInitiallySelectedResource(
            final IStructuredSelection selection) {
        // check if the selected resource is a workflow
        // and take the parent instead!
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (selection.size() != 1) {
            m_initiallySelectedResource = root;
        } else {
            Object selectedObj = selection.getFirstElement();
            if (selectedObj instanceof IResource) {
                IResource resource  = (IResource)selectedObj;
                if (KnimeResourceUtil.isWorkflow(resource)) {
                    m_initiallySelectedResource = resource.getParent();
                } else if (KnimeResourceUtil.isWorkflowGroup(resource)) {
                    m_initiallySelectedResource = resource;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NULL);
        overall.setLayout(new GridLayout(1, false));
        
        Group nameGroup = new Group(overall, SWT.NONE);
        nameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameGroup.setLayout(new GridLayout(2, false));
        // first row: workflow name
        final Label label = new Label(nameGroup, SWT.NONE);
        label.setText("Name of the " + m_elementName.toLowerCase() 
                + " to create:");
        m_projectName = new Text(nameGroup, SWT.BORDER);
        m_projectName.addModifyListener(new ModifyListener() {
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });
        m_projectName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // initialize the new project name field
        // set a default file name which is not already available
        String initialProjectName = INITIAL_PROJECT_NAME;
        if (!m_isWorkflow) {
            initialProjectName = DEFAULT_WORKFLOW_GROUP_NAME;
        }
        // if the default initial project name is already set, the name is
        // appended by numbers until a new name has been found
        IContainer project = (IContainer)ResourcesPlugin.getWorkspace()
                .getRoot().findMember(new Path(initialProjectName));
        String projectName = initialProjectName;
        for (int i = 2; project != null; i++) {
            projectName =  initialProjectName + i;
            project = (IContainer)ResourcesPlugin.getWorkspace().getRoot()
                    .findMember(new Path(projectName));
        }
        m_projectName.setText(projectName);

        if (KnimeResourceUtil.existsWorkflowGroupInWorkspace()) {
            createDestinationSelectionComposite(overall);
        } 
        setControl(overall);
    }
    
    
    private void createDestinationSelectionComposite(
            final Composite parent) {
        Group destGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
        destGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        destGroup.setLayout(new GridLayout(3, false));
        // second row: workflow destination
        Label destinationLabel = new Label(destGroup, SWT.NONE);
        destinationLabel.setText("Workspace destination:");
        m_destinationUI = new Text(destGroup, SWT.BORDER | SWT.READ_ONLY);
        m_destinationUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (m_initiallySelectedResource != null 
                && !(m_initiallySelectedResource instanceof IWorkspaceRoot)) {
            m_destinationUI.setText(m_initiallySelectedResource.getFullPath()
                    .toString());
        } else {
            m_destinationUI.setText("/");
        }
        Button browseBtn = new Button(destGroup, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleBrowseButton();
            }
        });
    }
    
    
    
    private void handleBrowseButton() {
        WorkflowGroupSelectionDialog dialog = new WorkflowGroupSelectionDialog(
                getShell());
        dialog.setInitialSelection(new Path(m_destinationUI.getText()));
        if (dialog.open() == IDialogConstants.OK_ID) {
            IContainer c = dialog.getSelectedWorkflowGroup();
            m_destinationUI.setText(c.getFullPath().toString());
        }
        dialogChanged();
    }

    private void updateStatus(final String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    /**
     * Ensures that the text field is set properly.
     */
    private void dialogChanged() {
        // check if a name was entered
        String projectName = m_projectName.getText().trim(); 
        if (projectName.length() == 0) {
            updateStatus(m_elementName + " name must be specified");
            return;
        }
        IStatus validName = ResourcesPlugin.getWorkspace().validateName(
                projectName, IResource.PROJECT);
        if (!validName.isOK()) {
            updateStatus(m_elementName + " name is not valid!");
            return;
        }
        // check whether this container already exists
        IPath path = getWorkflowPath();
        IContainer container = (IContainer)ResourcesPlugin.getWorkspace()
                .getRoot().findMember(path);
        if (container != null) {
            updateStatus(m_elementName + " name already exists.");
            return;
        }
        updateStatus(null);
    }

    
    /**
     * 
     * @return the workspace relative path with the selected name already 
     *  appended
     */
    public IPath getWorkflowPath() {
        IPath path = new Path(m_projectName.getText());
        if (m_destinationUI != null 
                && !m_destinationUI.getText().trim().isEmpty()) {
            path = new Path(m_destinationUI.getText());
            path = path.append(m_projectName.getText());
        }
        return path;
    }
}

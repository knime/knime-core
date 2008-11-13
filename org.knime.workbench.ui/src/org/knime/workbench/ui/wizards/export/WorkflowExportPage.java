/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.07.2006 (sieb): created
 */
package org.knime.workbench.ui.wizards.export;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.KNIMEUIPlugin;


/**
 * Page to enter the settings for the export of a workflow project.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowExportPage extends WizardPage {
    
    private static final String[] FILTER_EXTENSION = {"*.zip"};

    private static String exportPath;

    private Text m_containerText;

    private Text m_fileText;

    private Button m_excludeData;

    private final ISelection m_selection;

    /**
     * Constructor for NewWorkflowPage.
     *
     * @param selection The initial selection
     */
    public WorkflowExportPage(final ISelection selection) {
        super("wizardPage");
        setTitle("KNIME Workflow project");
        setDescription("This wizard exports a KNIME workflow project.");
        this.m_selection = selection;
    }

    /**
     * {@inheritDoc}
     */
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        Label label = new Label(container, SWT.NULL);
        label.setText("Select project to export:");

        m_containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        m_containerText.setLayoutData(gd);
        m_containerText.addModifyListener(new ModifyListener() {
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectProjectButton = new Button(container, SWT.PUSH);
        selectProjectButton.setText("Select...");
        selectProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleBrowse();
            }
        });

        label = new Label(container, SWT.NULL);
        label.setText("Export file name (zip):");

        m_fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        m_fileText.setLayoutData(gd);
        m_fileText.addModifyListener(new ModifyListener() {
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectExportFilebutton = new Button(container, SWT.PUSH);
        selectExportFilebutton.setText("Select...");
        selectExportFilebutton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleExportFileBrowse();
            }
        });

        final Group group = new Group(container, SWT.NONE);
        final GridLayout gridLayout1 = new GridLayout();
        group.setLayout(gridLayout1);
        group.setText("Options");
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_excludeData = new Button(group, SWT.CHECK);
        m_excludeData.setSelection(true);
        m_excludeData.setText("Exclude data from export.");

        initialize();
        dialogChanged();
        setControl(container);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */
    private void initialize() {

        IContainer container = null;
        if (m_selection instanceof IStructuredSelection
                && (!m_selection.isEmpty())) {
            IStructuredSelection ssel = (IStructuredSelection)m_selection;
            if (ssel.size() > 1) {
                return;
            }
            Object obj = ssel.getFirstElement();
            if (obj instanceof IResource) {

                if (obj instanceof IContainer) {
                    container = (IContainer)obj;
                } else {
                    container = ((IResource)obj).getParent();
                }
                m_containerText.setText(container.getName());
            }
        }

        if (exportPath != null) {
            m_fileText.setText(exportPath);
        }
    }

    /**
     * Uses the standard container selection dialog to choose the new value for
     * the container field.
     */

    private void handleBrowse() {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                getShell(), new LabelProvider() {
                    private final Image m_icon = KNIMEUIPlugin.getDefault()
                        .getImage("icons/knime_default.png");
                    @Override
                    public String getText(final Object element) {
                        if (element instanceof IProject) {
                            return ((IProject)element).getName();
                        }
                        return element.toString();
                    }
                    @Override
                    public Image getImage(final Object element) {
                        return m_icon;
                    }
                });
        // TODO: remove phantoms
        List<IProject>validProjects = new ArrayList<IProject>();
        for (Object o : ResourcesPlugin.getWorkspace().getRoot()
                .getProjects()) {
            if (o instanceof IProject && ((IProject)o).exists()) {
                IFile wfFile = ((IProject)o).getFile(
                        WorkflowPersistor.WORKFLOW_FILE);
                if (wfFile != null && wfFile.exists()) {
                    validProjects.add((IProject)o);
                }
            }
        }
        dialog.setElements(validProjects.toArray());
        IProject selected = getProjectForName(m_containerText.getText()); 
        if (selected != null) {
            dialog.setInitialSelections(new Object[] {selected});
        }
        if (dialog.open() == Window.OK) {
            Object[] result = dialog.getResult();
            if (result.length == 1) {
                if (result[0] instanceof IProject) {
                    m_containerText.setText(((IProject)result[0]).getName());
                } else {
                    setErrorMessage("Selected element " 
                            + result[0] + " is not a workflow");
                    setPageComplete(false);
                }
            }
        }
    }

    
    private IProject getProjectForName(final String projectName) {
        IResource resource = ResourcesPlugin.getWorkspace().getRoot()
            .findMember(projectName);
        if (resource instanceof IProject) {
            return (IProject)resource;
        }
        return null;
    }
    
    /**
     * @return true if the check box for excluding data is checked
     */
    boolean excludeData() {

        return m_excludeData.getSelection();
    }

    /**
     * Uses the standard file selection dialog to choose the export file name.
     */

    private void handleExportFileBrowse() {

        FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
        fileDialog.setFilterExtensions(FILTER_EXTENSION);
        fileDialog.setText("Specify export file.");

        if (exportPath != null) {
            fileDialog.setFileName(exportPath);
        }

        String filePath = fileDialog.open();

        if (filePath.trim().length() > 0) {

            // remember the selected path
            exportPath = filePath;

            // append "zip" extension if not there.
            String extension = filePath.substring(filePath.length() - 4,
                    filePath.length());

            if (!extension.equals(".zip")) {
                filePath = filePath + ".zip";
            }
        }

        m_fileText.setText(filePath);
    }

    /**
     * Ensures that both text fields are set.
     */

    private void dialogChanged() {
        IContainer container = (IContainer)ResourcesPlugin.getWorkspace()
                .getRoot().findMember(new Path(getContainerName()));
        String fileName = getFileName();

        if (getContainerName().length() == 0) {
            updateStatus("KNIME project must be specified");
            return;
        }

        if (container == null) {
            updateStatus("KNIME project must exist");
            return;
        }

        if (container.findMember(WorkflowPersistor.WORKFLOW_FILE) == null) {

            updateStatus("Project does not contain a workflow file.");
            return;
        }

        boolean f = false;
        f = (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0;
        if (f) {
            updateStatus("File container must exist");
            return;
        }

        if (fileName.length() == 0 || fileName.endsWith(File.separator)) {
            updateStatus("File name must be specified");
            return;
        }

        if (!fileName.toLowerCase().endsWith(".zip")) {
            updateStatus("File extension must be \"zip\"");
            return;
        }
        
        updateStatus(null);
    }

    private void updateStatus(final String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    /**
     * @return The container (=project) name
     */
    public String getContainerName() {
        return m_containerText.getText();
    }

    /**
     * @return The file name
     */
    public String getFileName() {
        return m_fileText.getText();
    }
}

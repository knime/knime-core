/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc - extensive changes to allow importing of Archive Files
 *     Philippe Ombredanne (pombredanne@nexb.com)
 *          - Bug 101180 [Import/Export] Import Existing Project into Workspace default widget is back button , should be text field
 *******************************************************************************/
package org.knime.workbench.ui.wizards.imports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.knime.core.node.NodeLogger;

/**
 * The WizardProjectsImportPage is the page that allows the user to import
 * projects from a particular location.
 */
public class WizardProjectsImportPage extends WizardPage implements
        IOverwriteQuery {

    // ------------ KNIME change,
    public static final String IMPORT_MARKER_FILE_NAME = "ImportMarker";
    
    /**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(WizardProjectsImportPage.class);

    /**
     * Class declared public only for test suite.
     * 
     */
    public class ProjectRecord {
        File projectSystemFile;

        Object projectArchiveFile;

        String projectName;

        Object parent;

        int level;

        IProjectDescription description;

        ILeveledImportStructureProvider provider;

        /**
         * Create a record for a project based on the info in the file.
         * 
         * @param file
         */
        ProjectRecord(File file) {
            projectSystemFile = file;
            setProjectName();
        }

        /**
         * @param file The Object representing the .project file
         * @param parent The parent folder of the .project file
         * @param level The number of levels deep in the provider the file is
         * @param entryProvider The provider for the archive file that contains
         *            it
         */
        ProjectRecord(Object file, Object parent, int level,
                ILeveledImportStructureProvider entryProvider) {
            this.projectArchiveFile = file;
            this.parent = parent;
            this.level = level;
            this.provider = entryProvider;
            setProjectName();
        }

        /**
         * Set the name of the project based on the projectFile.
         */
        private void setProjectName() {
            IProjectDescription newDescription = null;
            try {
                if (projectArchiveFile != null) {
                    InputStream stream =
                            provider.getContents(projectArchiveFile);
                    if (stream != null) {
                        newDescription =
                                IDEWorkbenchPlugin.getPluginWorkspace()
                                        .loadProjectDescription(stream);
                        stream.close();
                    }
                } else {
                    IPath path = new Path(projectSystemFile.getPath());
                    newDescription =
                            IDEWorkbenchPlugin.getPluginWorkspace()
                                    .loadProjectDescription(path);
                }
            } catch (CoreException e) {
                // no good couldn't get the name
            } catch (IOException e) {
                // no good couldn't get the name
            }

            if (newDescription == null) {
                this.description = null;
                projectName = ""; //$NON-NLS-1$
            } else {
                this.description = newDescription;
                projectName = this.description.getName();
            }
        }

        /**
         * Get the name of the project
         * 
         * @return String
         */
        public String getProjectName() {
            return projectName;
        }
    }

    // dialog store id constants
    private final static String STORE_COPY_PROJECT_ID =
            "WizardProjectsImportPage.STORE_COPY_PROJECT_ID"; //$NON-NLS-1$

    private final static String STORE_ARCHIVE_SELECTED =
            "WizardProjectsImportPage.STORE_ARCHIVE_SELECTED"; //$NON-NLS-1$

    private Text directoryPathField;

    private CheckboxTreeViewer projectsList;

    private Button copyCheckbox;

    private boolean copyFiles = false;

    private ProjectRecord[] selectedProjects = new ProjectRecord[0];

    // Keep track of the directory that we browsed to last time
    // the wizard was invoked.
    private static String previouslyBrowsedDirectory = ""; //$NON-NLS-1$

    // Keep track of the archive that we browsed to last time
    // the wizard was invoked.
    private static String previouslyBrowsedArchive = ""; //$NON-NLS-1$

    private Button projectFromDirectoryRadio;

    private Button projectFromArchiveRadio;

    private Text archivePathField;

    private Button browseDirectoriesButton;

    private Button browseArchivesButton;

    private IProject[] wsProjects;

    private boolean doublesToImport;

    private boolean renamePageShown;

    // constant from WizardArchiveFileResourceImportPage1
    private static final String[] FILE_IMPORT_MASK =
            {"*.jar;*.zip;*.tar;*.tar.gz;*.tgz", "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$

    // The last selected path to mimize searches
    private String lastPath;

    /**
     * Creates a new project creation wizard page.
     * 
     */
    public WizardProjectsImportPage() {
        this("wizardExternalProjectsPage"); //$NON-NLS-1$
    }

    /**
     * Create a new instance of the receiver.
     * 
     * @param pageName
     */
    public WizardProjectsImportPage(String pageName) {
        super(pageName);
        setPageComplete(false);
        setTitle(DataTransferMessages.WizardProjectsImportPage_ImportProjectsTitle);
        setDescription(DataTransferMessages.WizardProjectsImportPage_ImportProjectsDescription);
    }

    /**
     * Create a new instance of the receiver.
     * 
     * @param pageName
     * @param title
     * @param titleImage
     */
    public WizardProjectsImportPage(String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createProjectsRoot(workArea);
        createProjectsList(workArea);
        createOptionsArea(workArea);
        restoreWidgetValues();
        Dialog.applyDialogFont(workArea);
    }

    /**
     * Create the area with the extra options.
     * 
     * @param workArea
     */
    private void createOptionsArea(Composite workArea) {
        Composite optionsGroup = new Composite(workArea, SWT.NONE);
        optionsGroup.setLayout(new GridLayout());
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        copyCheckbox = new Button(optionsGroup, SWT.CHECK);
        copyCheckbox
                .setText(DataTransferMessages.WizardProjectsImportPage_CopyProjectsIntoWorkspace);
        copyCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        copyCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                copyFiles = copyCheckbox.getSelection();
            }
        });
    }

    /**
     * Create the checkbox list for the found projects.
     * 
     * @param workArea
     */
    private void createProjectsList(Composite workArea) {

        Label title = new Label(workArea, SWT.NONE);
        title
                .setText(DataTransferMessages.WizardProjectsImportPage_ProjectsListTitle);

        Composite listComposite = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.makeColumnsEqualWidth = false;
        listComposite.setLayout(layout);

        listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

        projectsList = new CheckboxTreeViewer(listComposite, SWT.BORDER);
        GridData listData =
                new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
                        | GridData.FILL_BOTH);
        projectsList.getControl().setLayoutData(listData);
        projectsList
                .addPostSelectionChangedListener(new ISelectionChangedListener() {

                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        updateRenamePage();
                    }

                });

        projectsList.setContentProvider(new ITreeContentProvider() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
             */
            public Object[] getChildren(Object parentElement) {
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
             */
            public Object[] getElements(Object inputElement) {
                return getValidProjects();
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
             */
            public boolean hasChildren(Object element) {
                return false;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
             */
            public Object getParent(Object element) {
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.IContentProvider#dispose()
             */
            public void dispose() {

            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
             *      java.lang.Object, java.lang.Object)
             */
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }

        });

        projectsList.setLabelProvider(new LabelProvider() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
             */
            public String getText(Object element) {
                return ((ProjectRecord)element).getProjectName();
            }
        });

        projectsList.setInput(this);
        projectsList.setSorter(new ViewerSorter());
        createSelectionButtons(listComposite);
    }

    private void updateRenamePage() {
        doublesToImport = false; 
        if (getProjectsToRename().length > 0) {
            renamePageShown = true;
            doublesToImport = true;
        }
        boolean additionalChanges =
                ((WizardProjectRenameDuplicatesPage)getWizard().getPages()[1])
                        .updateRenameList();
        renamePageShown |= additionalChanges;
        setPageComplete(!doublesToImport);
        //        if (renamePageShown) {
//        }

    }

    /**
     * Create the selection buttons in the listComposite.
     * 
     * @param listComposite
     */
    private void createSelectionButtons(Composite listComposite) {
        Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        buttonsComposite.setLayout(layout);

        buttonsComposite.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        Button selectAll = new Button(buttonsComposite, SWT.PUSH);
        selectAll.setText(DataTransferMessages.DataTransfer_selectAll);
        selectAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                projectsList.setCheckedElements(selectedProjects);
                updateRenamePage();
            }
        });
        Dialog.applyDialogFont(selectAll);
        setButtonLayoutData(selectAll);

        Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
        deselectAll.setText(DataTransferMessages.DataTransfer_deselectAll);
        deselectAll.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {

                projectsList.setCheckedElements(new Object[0]);
                updateRenamePage();

            }
        });
        Dialog.applyDialogFont(deselectAll);
        setButtonLayoutData(deselectAll);

        Button refresh = new Button(buttonsComposite, SWT.PUSH);
        refresh.setText(DataTransferMessages.DataTransfer_refresh);
        refresh.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                if (projectFromDirectoryRadio.getSelection()) {
                    updateProjectsList(directoryPathField.getText().trim());
                } else {
                    updateProjectsList(archivePathField.getText().trim());
                }
            }
        });
        Dialog.applyDialogFont(refresh);
        setButtonLayoutData(refresh);
    }

    /**
     * Create the area where you select the root directory for the projects.
     * 
     * @param workArea Composite
     */
    private void createProjectsRoot(Composite workArea) {

        // project specification group
        Composite projectGroup = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // new project from directory radio button
        projectFromDirectoryRadio = new Button(projectGroup, SWT.RADIO);
        projectFromDirectoryRadio
                .setText(DataTransferMessages.WizardProjectsImportPage_RootSelectTitle);

        // project location entry field
        this.directoryPathField = new Text(projectGroup, SWT.BORDER);

        this.directoryPathField.setLayoutData(new GridData(
                GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        // browse button
        browseDirectoriesButton = new Button(projectGroup, SWT.PUSH);
        browseDirectoriesButton
                .setText(DataTransferMessages.DataTransfer_browse);
        setButtonLayoutData(browseDirectoriesButton);

        // new project from archive radio button
        projectFromArchiveRadio = new Button(projectGroup, SWT.RADIO);
        projectFromArchiveRadio
                .setText(DataTransferMessages.WizardProjectsImportPage_ArchiveSelectTitle);

        // project location entry field
        archivePathField = new Text(projectGroup, SWT.BORDER);

        archivePathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.GRAB_HORIZONTAL));
        // browse button
        browseArchivesButton = new Button(projectGroup, SWT.PUSH);
        browseArchivesButton.setText(DataTransferMessages.DataTransfer_browse);
        setButtonLayoutData(browseArchivesButton);

        projectFromDirectoryRadio.setSelection(true);
        archivePathField.setEnabled(false);
        browseArchivesButton.setEnabled(false);

        browseDirectoriesButton.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetS
             *      elected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                handleLocationDirectoryButtonPressed();
            }

        });

        browseArchivesButton.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                handleLocationArchiveButtonPressed();
            }

        });

        directoryPathField.addTraverseListener(new TraverseListener() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
             */
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    updateProjectsList(directoryPathField.getText().trim());
                }
            }

        });

        directoryPathField.addFocusListener(new FocusAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
             */
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                updateProjectsList(directoryPathField.getText().trim());
            }

        });

        archivePathField.addTraverseListener(new TraverseListener() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
             */
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    updateProjectsList(archivePathField.getText().trim());
                }
            }

        });

        archivePathField.addFocusListener(new FocusAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
             */
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                updateProjectsList(archivePathField.getText().trim());
            }
        });

        projectFromDirectoryRadio.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                directoryRadioSelected();
            }
        });

        projectFromArchiveRadio.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                archiveRadioSelected();
            }
        });
    }

    private void archiveRadioSelected() {
        if (projectFromArchiveRadio.getSelection()) {
            directoryPathField.setEnabled(false);
            browseDirectoriesButton.setEnabled(false);
            archivePathField.setEnabled(true);
            browseArchivesButton.setEnabled(true);
            updateProjectsList(archivePathField.getText());
            archivePathField.setFocus();
            copyCheckbox.setSelection(true);
            copyCheckbox.setEnabled(false);
        }
    }

    private void directoryRadioSelected() {
        if (projectFromDirectoryRadio.getSelection()) {
            directoryPathField.setEnabled(true);
            browseDirectoriesButton.setEnabled(true);
            archivePathField.setEnabled(false);
            browseArchivesButton.setEnabled(false);
            updateProjectsList(directoryPathField.getText());
            directoryPathField.setFocus();
            copyCheckbox.setEnabled(true);
            copyCheckbox.setSelection(copyFiles);
        }
    }

    /*
     * (non-Javadoc) Method declared on IDialogPage. Set the focus on path
     * fields when page becomes visible.
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && this.projectFromDirectoryRadio.getSelection()) {
            this.directoryPathField.setFocus();
        }
        if (visible && this.projectFromArchiveRadio.getSelection()) {
            this.archivePathField.setFocus();
        }
    }

    /**
     * Update the list of projects based on path. Method declared public only
     * for test suite.
     * 
     * @param path
     */
    public void updateProjectsList(final String path) {

        if (path.equals(lastPath)) {
            return;
        }
        updateRenamePage();
        lastPath = path;

        // on an empty path empty selectedProjects
        if (path == null || path.length() == 0) {
            selectedProjects = new ProjectRecord[0];
            projectsList.refresh(true);
            projectsList.setCheckedElements(selectedProjects);
            setPageComplete(getValidProjects().length > 0);
            return;
        }
        // We can't access the radio button from the inner class so get the
        // status beforehand
        final boolean dirSelected =
                this.projectFromDirectoryRadio.getSelection();
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
                 */
                public void run(IProgressMonitor monitor) {

                    monitor
                            .beginTask(
                                    DataTransferMessages.WizardProjectsImportPage_SearchingMessage,
                                    100);
                    File directory = new File(path);
                    selectedProjects = new ProjectRecord[0];
                    Collection files = new ArrayList();
                    monitor.worked(10);
                    if (!dirSelected
                            && ArchiveFileManipulations.isTarFile(path)) {
                        TarFile sourceTarFile = getSpecifiedTarSourceFile(path);
                        if (sourceTarFile == null) {
                            return;
                        }

                        TarLeveledStructureProvider provider =
                                ArchiveFileManipulations
                                        .getTarStructureProvider(sourceTarFile,
                                                getContainer().getShell());
                        Object child = provider.getRoot();

                        if (!collectProjectFilesFromProvider(files, provider,
                                child, 0, monitor)) {
                            return;
                        }
                        Iterator filesIterator = files.iterator();
                        selectedProjects = new ProjectRecord[files.size()];
                        int index = 0;
                        monitor.worked(50);
                        monitor
                                .subTask(DataTransferMessages.WizardProjectsImportPage_ProcessingMessage);
                        while (filesIterator.hasNext()) {
                            selectedProjects[index++] =
                                    (ProjectRecord)filesIterator.next();
                        }
                    } else if (!dirSelected
                            && ArchiveFileManipulations.isZipFile(path)) {
                        ZipFile sourceFile = getSpecifiedZipSourceFile(path);
                        if (sourceFile == null) {
                            return;
                        }
                        ZipLeveledStructureProvider provider =
                                ArchiveFileManipulations
                                        .getZipStructureProvider(sourceFile,
                                                getContainer().getShell());
                        Object child = provider.getRoot();

                        if (!collectProjectFilesFromProvider(files, provider,
                                child, 0, monitor)) {
                            return;
                        }
                        Iterator filesIterator = files.iterator();
                        selectedProjects = new ProjectRecord[files.size()];
                        int index = 0;
                        monitor.worked(50);
                        monitor
                                .subTask(DataTransferMessages.WizardProjectsImportPage_ProcessingMessage);
                        while (filesIterator.hasNext()) {
                            selectedProjects[index++] =
                                    (ProjectRecord)filesIterator.next();
                        }
                    }

                    else if (dirSelected && directory.isDirectory()) {

                        if (!collectProjectFilesFromDirectory(files, directory,
                                monitor)) {
                            return;
                        }
                        Iterator filesIterator = files.iterator();
                        selectedProjects = new ProjectRecord[files.size()];
                        int index = 0;
                        monitor.worked(50);
                        monitor
                                .subTask(DataTransferMessages.WizardProjectsImportPage_ProcessingMessage);
                        while (filesIterator.hasNext()) {
                            File file = (File)filesIterator.next();
                            selectedProjects[index] = new ProjectRecord(file);
                            index++;
                        }
                    } else {
                        monitor.worked(60);
                    }
                    monitor.done();
                }

            });
        } catch (InvocationTargetException e) {
            IDEWorkbenchPlugin.log(e.getMessage(), e);
        } catch (InterruptedException e) {
            // Nothing to do if the user interrupts.
        }

        projectsList.refresh(true);
        projectsList.setCheckedElements(getValidProjects());
        updateRenamePage();
        setPageComplete(getValidProjects().length > 0);
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    protected ZipFile getSpecifiedZipSourceFile() {
        return getSpecifiedZipSourceFile(archivePathField.getText());
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    private ZipFile getSpecifiedZipSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new ZipFile(fileName);
        } catch (ZipException e) {
            displayErrorDialog(DataTransferMessages.ZipImport_badFormat);
        } catch (IOException e) {
            displayErrorDialog(DataTransferMessages.ZipImport_couldNotRead);
        }

        archivePathField.setFocus();
        return null;
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    protected TarFile getSpecifiedTarSourceFile() {
        return getSpecifiedTarSourceFile(archivePathField.getText());
    }

    /**
     * Answer a handle to the zip file currently specified as being the source.
     * Return null if this file does not exist or is not of valid format.
     */
    private TarFile getSpecifiedTarSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new TarFile(fileName);
        } catch (TarException e) {
            displayErrorDialog(DataTransferMessages.TarImport_badFormat);
        } catch (IOException e) {
            displayErrorDialog(DataTransferMessages.ZipImport_couldNotRead);
        }

        archivePathField.setFocus();
        return null;
    }

    /**
     * Display an error dialog with the specified message.
     * 
     * @param message the error message
     */
    protected void displayErrorDialog(String message) {
        MessageDialog.openError(getContainer().getShell(),
                getErrorDialogTitle(), message);
    }

    /**
     * Get the title for an error dialog. Subclasses should override.
     */
    protected String getErrorDialogTitle() {
        return IDEWorkbenchMessages.WizardExportPage_internalErrorTitle;
    }

    /**
     * Collect the list of .project files that are under directory into files.
     * 
     * @param files
     * @param directory
     * @param monitor The monitor to report to
     * @return boolean <code>true</code> if the operation was completed.
     */
    private boolean collectProjectFilesFromDirectory(Collection files,
            File directory, IProgressMonitor monitor) {

        if (monitor.isCanceled()) {
            return false;
        }
        monitor.subTask(NLS.bind(
                DataTransferMessages.WizardProjectsImportPage_CheckingMessage,
                directory.getPath()));
        File[] contents = directory.listFiles();
        if (contents == null) {
            // happens if the current user is not allowed to list the
            // directory
            return true;
        }
        // first look for project description files
        final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
        for (int i = 0; i < contents.length; i++) {
            File file = contents[i];
            if (file.isFile() && file.getName().equals(dotProject)) {
                files.add(file);
                // don't search sub-directories since we can't have nested
                // projects
                return true;
            }
        }
        // no project description found, so recurse into sub-directories
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                if (!contents[i].getName().equals(
                       METADATA_FOLDER)) {
                    collectProjectFilesFromDirectory(files, contents[i],
                            monitor);
                }
            }
        }
        return true;
    }

    /**
     * Collect the list of .project files that are under directory into files.
     * 
     * @param files
     * @param monitor The monitor to report to
     * @return boolean <code>true</code> if the operation was completed.
     */
    private boolean collectProjectFilesFromProvider(Collection files,
            ILeveledImportStructureProvider provider, Object entry, int level,
            IProgressMonitor monitor) {

        if (monitor.isCanceled()) {
            return false;
        }
        monitor.subTask(NLS.bind(
                DataTransferMessages.WizardProjectsImportPage_CheckingMessage,
                provider.getLabel(entry)));
        List children = provider.getChildren(entry);
        if (children == null) {
            children = new ArrayList(1);
        }
        Iterator childrenEnum = children.iterator();
        while (childrenEnum.hasNext()) {
            Object child = childrenEnum.next();
            if (provider.isFolder(child)) {
                collectProjectFilesFromProvider(files, provider, child,
                        level + 1, monitor);
            }
            String elementLabel = provider.getLabel(child);
            if (elementLabel.equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
                files.add(new ProjectRecord(child, entry, level, provider));
            }
        }
        return true;
    }

    /**
     * The browse button has been selected. Select the location.
     */
    protected void handleLocationDirectoryButtonPressed() {

        DirectoryDialog dialog =
                new DirectoryDialog(directoryPathField.getShell());
        dialog
                .setMessage(DataTransferMessages.WizardProjectsImportPage_SelectDialogTitle);

        String dirName = directoryPathField.getText().trim();
        if (dirName.length() == 0) {
            dirName = previouslyBrowsedDirectory;
        }

        if (dirName.length() == 0) {
            dialog.setFilterPath(IDEWorkbenchPlugin.getPluginWorkspace()
                    .getRoot().getLocation().toOSString());
        } else {
            File path = new File(dirName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirName).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            previouslyBrowsedDirectory = selectedDirectory;
            directoryPathField.setText(previouslyBrowsedDirectory);
            updateProjectsList(selectedDirectory);
        }

    }

    /**
     * The browse button has been selected. Select the location.
     */
    protected void handleLocationArchiveButtonPressed() {

        FileDialog dialog = new FileDialog(archivePathField.getShell());
        dialog.setFilterExtensions(FILE_IMPORT_MASK);
        dialog
                .setText(DataTransferMessages.WizardProjectsImportPage_SelectArchiveDialogTitle);

        String fileName = archivePathField.getText().trim();
        if (fileName.length() == 0) {
            fileName = previouslyBrowsedArchive;
        }

        if (fileName.length() == 0) {
            dialog.setFilterPath(IDEWorkbenchPlugin.getPluginWorkspace()
                    .getRoot().getLocation().toOSString());
        } else {
            File path = new File(fileName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(fileName).toOSString());
            }
        }

        String selectedArchive = dialog.open();
        if (selectedArchive != null) {
            previouslyBrowsedArchive = selectedArchive;
            archivePathField.setText(previouslyBrowsedArchive);
            updateProjectsList(selectedArchive);
        }

    }

    /**
     * Create the selected projects
     * 
     * @return boolean <code>true</code> if all project creations were
     *         successful.
     */
    public boolean createProjects() {
        saveWidgetValues();
        // put the renamed projet files into the project records before
        // performing the import
        ((WizardProjectRenameDuplicatesPage)getWizard().getPages()[1])
                .putRenameTextFieldContentIntoProjectRecord();
        final Object[] selected = projectsList.getCheckedElements();
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                try {
                    monitor.beginTask("", selected.length); //$NON-NLS-1$
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                    for (int i = 0; i < selected.length; i++) {
                        createExistingProject((ProjectRecord)selected[i],
                                new SubProgressMonitor(monitor, 1));

                        // -------------- Import Marking ----------------------
                        // KNIME code (dirty fix to avoid error logs during
                        // editor opening)
                        // also possible to put it to the KNIME subclass
                        final IWorkspace workspace =
                                ResourcesPlugin.getWorkspace();
                        String projectName =
                                ((ProjectRecord)selected[i]).projectName;
                        final IProject project =
                                workspace.getRoot().getProject(projectName);

                        final File file =
                                new File(project.getProjectRelativePath()
                                        .makeAbsolute().toFile(),
                                        IMPORT_MARKER_FILE_NAME);
                        if (!file.exists()) {
                            byte[] buf = {100};

                            try {
                                FileOutputStream fos =
                                        new FileOutputStream(file);
                                fos.write(buf);
                                fos.close();
                            } catch (Throwable t) {
                                // do nothging
                            }
                        }
                        // -------------- Import Marking End -----------------
                        // -------------- Rename Change KNIME ----------------
                        // Renames the project
                        // Necessary if the imported project was renamed as
                        // it already existed in the workspace. Here the
                        // .project
                        // file will be renamed to the already renamed file name
                        try {
                            IProjectDescription description =
                                    project.getDescription();
                            description.setName(projectName);

                            project.move(description, IResource.FORCE
                                    | IResource.SHALLOW, monitor);
                        } catch (Exception e) {
                            LOGGER.warn("The name of the imported project: "
                                    + projectName
                                    + " could not be renamed in the "
                                    + ".project file. I.e. the "
                                    + "file name is correct but"
                                    + " the name in the " + ".project"
                                    + " file differs.");
                        }
                    }
                } finally {
                    monitor.done();
                }
            }
        };
        // run the new project creation operation
        try {
            getContainer().run(false, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // one of the steps resulted in a core exception
            Throwable t = e.getTargetException();
            String message =
                    DataTransferMessages.WizardExternalProjectImportPage_errorMessage;
            IStatus status;
            if (t instanceof CoreException) {
                status = ((CoreException)t).getStatus();
            } else {
                status =
                        new Status(IStatus.ERROR,
                                IDEWorkbenchPlugin.IDE_WORKBENCH, 1, message, t);
            }
            ErrorDialog.openError(getShell(), message, null, status);
            return false;
        } finally {
            ArchiveFileManipulations.clearProviderCache(getContainer()
                    .getShell());
        }
        return true;
    }

    /**
     * Create the project described in record. If it is successful return true.
     * 
     * @param record
     * @return boolean <code>true</code> if successful
     * @throws InterruptedException
     */
    private boolean createExistingProject(final ProjectRecord record,
            IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        String projectName = record.getProjectName();
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject project = workspace.getRoot().getProject(projectName);
        if (record.description == null) {
            // error case
            record.description = workspace.newProjectDescription(projectName);
            IPath locationPath =
                    new Path(record.projectSystemFile.getAbsolutePath());
            // IPath locationPath = new
            // Path(record.projectFile.getFullPath(record.projectFile.getRoot()));

            // If it is under the root use the default location
            if (Platform.getLocation().isPrefixOf(locationPath)) {
                record.description.setLocation(null);
            } else {
                record.description.setLocation(locationPath);
            }
        } else {
            record.description.setName(projectName);
        }
        if (record.projectArchiveFile != null) {
            // import from archive
            ArrayList fileSystemObjects = new ArrayList();
            getFilesForProject(fileSystemObjects, record.provider,
                    record.parent);
            record.provider.setStrip(record.level);
            ImportOperation operation =
                    new ImportOperation(project.getFullPath(), record.provider
                            .getRoot(), record.provider, this,
                            fileSystemObjects);
            operation.setContext(getShell());
            operation.run(monitor);
            return true;
        }
        // import from file system
        File importSource = null;
        if (copyFiles) {
            // import project from location copying files - use default project
            // location for this workspace
            URI locationURI = record.description.getLocationURI();
            // if location is null, project already exists in this location or
            // some error condition occured.
            if (locationURI != null) {
                importSource = new File(locationURI);
                IProjectDescription desc =
                        workspace.newProjectDescription(projectName);
                desc.setBuildSpec(record.description.getBuildSpec());
                desc.setNatureIds(record.description.getNatureIds());
                desc.setDynamicReferences(record.description
                        .getDynamicReferences());
                desc.setReferencedProjects(record.description
                        .getReferencedProjects());
                record.description = desc;
            }
        }

        try {
            monitor
                    .beginTask(
                            DataTransferMessages.WizardProjectsImportPage_CreateProjectsTask,
                            100);
            project.create(record.description, new SubProgressMonitor(monitor,
                    30));
            project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
                    monitor, 70));
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }

        // import operation to import project files if copy checkbox is selected
        if (copyFiles && importSource != null) {
            List filesToImport = new ArrayList();
            FileSystemStructureProvider provider =
                    FileSystemStructureProvider.INSTANCE;
            getFilesForProject(filesToImport, provider, importSource);
            ImportOperation operation =
                    new ImportOperation(project.getFullPath(), importSource,
                            FileSystemStructureProvider.INSTANCE, this,
                            filesToImport);
            operation.setContext(getShell());
            operation.setOverwriteResources(true); // need to overwrite
            // .project, .classpath
            // files
            operation.setCreateContainerStructure(false);
            operation.run(monitor);
        }

        return true;
    }

    /**
     * Return a list of all files in the project
     * 
     * @param provider The provider for the parent file
     * @param entry The root directory of the project
     * @return A list of all files in the project
     */
    protected boolean getFilesForProject(Collection files,
            IImportStructureProvider provider, Object entry) {
        List children = provider.getChildren(entry);
        Iterator childrenEnum = children.iterator();

        while (childrenEnum.hasNext()) {
            Object child = childrenEnum.next();
            // Add the child, this way we get every files except the project
            // folder itself which we don't want
            files.add(child);
            // We don't have isDirectory for tar so must check for children
            // instead
            if (provider.isFolder(child)) {
                getFilesForProject(files, provider, child);
            }
        }
        return true;
    }

    /**
     * Execute the passed import operation. Answer a boolean indicating success.
     * 
     * @return true if the operation executed successfully, false otherwise
     */
    protected boolean executeImportOperation(ImportOperation op) {
        // initializeOperation(op);
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // displayErrorDialog(e.getTargetException());
            return false;
        }

        IStatus status = op.getStatus();
        if (!status.isOK()) {
            ErrorDialog.openError(getContainer().getShell(),
                    DataTransferMessages.FileImport_importProblems, null, // no
                    // special
                    // message
                    status);
            return false;
        }

        return true;
    }

    /**
     * The <code>WizardDataTransfer</code> implementation of this
     * <code>IOverwriteQuery</code> method asks the user whether the existing
     * resource at the given path should be overwritten.
     * 
     * @param pathString
     * @return the user's reply: one of <code>"YES"</code>, <code>"NO"</code>,
     *         <code>"ALL"</code>, or <code>"CANCEL"</code>
     */
    public String queryOverwrite(String pathString) {

        Path path = new Path(pathString);

        String messageString;
        // Break the message up if there is a file name and a directory
        // and there are at least 2 segments.
        if (path.getFileExtension() == null || path.segmentCount() < 2) {
            messageString =
                    NLS
                            .bind(
                                    IDEWorkbenchMessages.WizardDataTransfer_existsQuestion,
                                    pathString);
        } else {
            messageString =
                    NLS
                            .bind(
                                    IDEWorkbenchMessages.WizardDataTransfer_overwriteNameAndPathQuestion,
                                    path.lastSegment(), path
                                            .removeLastSegments(1).toOSString());
        }

        final MessageDialog dialog =
                new MessageDialog(getContainer().getShell(),
                        IDEWorkbenchMessages.Question, null, messageString,
                        MessageDialog.QUESTION, new String[]{
                                IDialogConstants.YES_LABEL,
                                IDialogConstants.YES_TO_ALL_LABEL,
                                IDialogConstants.NO_LABEL,
                                IDialogConstants.NO_TO_ALL_LABEL,
                                IDialogConstants.CANCEL_LABEL}, 0);
        String[] response = new String[]{YES, ALL, NO, NO_ALL, CANCEL};
        // run in syncExec because callback is from an operation,
        // which is probably not running in the UI thread.
        getControl().getDisplay().syncExec(new Runnable() {
            public void run() {
                dialog.open();
            }
        });
        return dialog.getReturnCode() < 0 ? CANCEL : response[dialog
                .getReturnCode()];
    }

    /**
     * Method used for test suite.
     * 
     * @return Button the Import from Directory RadioButton
     */
    public Button getProjectFromDirectoryRadio() {
        return projectFromDirectoryRadio;
    }

    /**
     * Method used for test suite.
     * 
     * @return CheckboxTreeViewer the viewer containing all the projects found
     */
    public CheckboxTreeViewer getProjectsList() {
        return projectsList;
    }

    /**
     * Retrieve all the projects in the current workspace.
     * 
     * @return IProject[] array of IProject in the current workspace
     */
    private IProject[] getProjectsInWorkspace() {
        if (wsProjects == null) {
            wsProjects =
                    IDEWorkbenchPlugin.getPluginWorkspace().getRoot()
                            .getProjects();
        }
        return wsProjects;
    }

    /**
     * Get the array of valid project records that can be imported from the
     * source workspace or archive, selected by the user. If a project with the
     * same name exists in both the source workspace and the current workspace,
     * it will not appear in the list of projects to import and thus cannot be
     * selected for import.
     * 
     * Method declared public for test suite.
     * 
     * @return ProjectRecord[] array of projects that can be imported into the
     *         workspace
     */
    public ProjectRecord[] getValidProjects() {
        List validProjects = new ArrayList();
        // to remember if projects should be imported, that are duplicates
        doublesToImport = false;
        for (int i = 0; i < selectedProjects.length; i++) {
            File file = selectedProjects[i].projectSystemFile;
            if (file != null) {
                // projects that are in the current workspace location
                // and are also in the workspace are not selectable
                IPath workspace = Platform.getLocation();
                if (workspace.isPrefixOf(new Path(file.getAbsolutePath()))
                        && isProjectInWorkspace(selectedProjects[i]
                                .getProjectName())) {
                    continue;
                }
            }
            if (isProjectInWorkspace(selectedProjects[i].getProjectName())) {
                doublesToImport = true;
            }
            validProjects.add(selectedProjects[i]);
        }
        renamePageShown = false;
        return (ProjectRecord[])validProjects
                .toArray(new ProjectRecord[validProjects.size()]);
    }

    /**
     * Determine if the project with the given name is in the current workspace.
     * 
     * @param projectName String the project name to check
     * @return boolean true if the project with the given name is in this
     *         workspace
     */
    boolean isProjectInWorkspace(final String projectName) {
        if (projectName == null) {
            return false;
        }
        IProject[] workspaceProjects = getProjectsInWorkspace();
        for (int i = 0; i < workspaceProjects.length; i++) {
            if (projectName.equals(workspaceProjects[i].getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use the dialog store to restore widget values to the values that they
     * held last time this wizard was used to completion.
     * 
     * Method declared public only for use of tests.
     */
    public void restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            // checkbox
            copyFiles = settings.getBoolean(STORE_COPY_PROJECT_ID);
            copyCheckbox.setSelection(copyFiles);

            // radio selection
            boolean archiveSelected =
                    settings.getBoolean(STORE_ARCHIVE_SELECTED);
            projectFromDirectoryRadio.setSelection(!archiveSelected);
            projectFromArchiveRadio.setSelection(archiveSelected);
            if (archiveSelected) {
                archiveRadioSelected();
            } else {
                directoryRadioSelected();
            }
        }
    }

    /**
     * Since Finish was pressed, write widget values to the dialog store so that
     * they will persist into the next invocation of this wizard page.
     * 
     * Method declared public only for use of tests.
     */
    public void saveWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            settings.put(STORE_COPY_PROJECT_ID, copyCheckbox.getSelection());

            settings.put(STORE_ARCHIVE_SELECTED, projectFromArchiveRadio
                    .getSelection());
        }
    }

    /**
     * Method used for test suite.
     * 
     * @return Button copy checkbox
     */
    public Button getCopyCheckbox() {
        return copyCheckbox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        // the next page is only necessary if there are doubles
        if (!getDoublesToImport()) {
            return null;
        }
        renamePageShown = true;
        return super.getNextPage();

    }

    public boolean getDoublesToImport() {
        return doublesToImport;
    }

    public boolean getRenamePageShown() {
        return renamePageShown;
    }

    ProjectRecord[] getProjectsToRename() {
        Object[] selected = getProjectsList().getCheckedElements();
        List<ProjectRecord> resultList = new ArrayList<ProjectRecord>();
        for (Object o : selected) {
            ProjectRecord project = (ProjectRecord)o;
            // the project is a duplicate (i.e. there exist already a
            // project with the given name in the workspace
            if (isProjectInWorkspace(project.getProjectName())) {
                resultList.add(project);
            }
        }
        doublesToImport = resultList.size() > 0;
        return resultList.toArray(new ProjectRecord[resultList.size()]);
    }
}

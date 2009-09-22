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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.KnimeResourceLabelProvider;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.wizards.workflowgroup.WorkflowGroupSelectionDialog;

/**
 * Import page providing the hierarchical user interface to select workflows and
 * workflow groups to import from either a directory or an archive file.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowImportSelectionPage extends WizardPage {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowImportSelectionPage.class);

    /** Identifier for this page within a wizard. */
    public static final String NAME = "Workflow import selection";

    private static final ImageDescriptor ICON = KNIMEUIPlugin
        .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
            "icons/knime_import55.png");

    private static final Image IMG_WARN
        = KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                "icons/warning.gif").createImage();

    // constant from WizardArchiveFileResourceImportPage1
    private static final String[] ZIP_FILE_MASK = {
            "*.jar;*.zip;*.tar;*.tar.gz;*.tgz", "*.*" }; //$NON-NLS-1$

    // keys and fields for dialog settings
    private static final String KEY_ZIP_LOC = "initialZipLocation";
    private static final String KEY_DIR_LOC = "initialDirLocation";
    private static final String KEY_FROM_DIR = "initiallyFromDir";

    private static String initialZipLocation = "";
    private static String initialDirLocation = "";
    private static boolean initialFromDir = true;
    // the initial destination should not be static
    private IContainer m_initialDestination;

    private static final GridData FILL_BOTH = new GridData(GridData.FILL_BOTH);
    private final GridData m_btnData;

    // source selection part components
    private Button m_fromDirUI;
    private Text m_fromDirTextUI;
    private Button m_dirBrowseBtn;
    private Button m_fromZipUI;
    private Text m_fromZipTextUI;
    private Button m_zipBrowseBtn;

    // target selection part components
    private Text m_targetTextUI;
    private Button m_browseWorkflowGroupsBtn;

    // workflow list part components
    private CheckboxTreeViewer m_workflowListUI;
    private Button m_copyProjectsUI;

    private IWorkflowImportElement m_importRoot;

    private final Collection<IWorkflowImportElement>m_invalidAndCheckedImports
        = new ArrayList<IWorkflowImportElement>();

    private final Collection<IWorkflowImportElement>m_validAndCheckedImports
        = new ArrayList<IWorkflowImportElement>();

    /*
     * We keep the next page (the rename page here), because it is not possible
     * to dynamically change the page in the wizard. See the getNextPage method
     * where the correct next page (depending on duplicate workflows) is
     * returned.
     */
    private RenameWorkflowImportPage m_renamePage;

    /**
     *
     */
    public WorkflowImportSelectionPage() {
        super(NAME);
        setTitle("Wokflow Import Selection");
        setDescription("Select the workflows to import.");
        setImageDescriptor(ICON);
        m_btnData = new GridData();
        m_btnData.widthHint = 70;
        m_btnData.horizontalAlignment = SWT.RIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.FILL);
        overall.setLayout(new GridLayout(1, false));
        overall.setLayoutData(FILL_BOTH);
        createImportComposite(overall);
        createTargetComposite(overall);
        createWorkflowListComposite(overall);
        setControl(overall);
    }

    /**
     *
     * @param parent parent
     * @return composite to select the destination of the import
     */
    protected Composite createTargetComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL | SWT.SHADOW_ETCHED_IN);
        overall.setLayout(new GridLayout(3, false));
        overall.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        overall.setText("Target: ");
        Label label = new Label(overall, SWT.NONE);
        label.setText("Select destination:");
        m_targetTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_targetTextUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_targetTextUI.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                validateWorkflows();
            }

        });
        m_browseWorkflowGroupsBtn = new Button(overall, SWT.PUSH);
        m_browseWorkflowGroupsBtn.setText("Browse...");
        m_browseWorkflowGroupsBtn.setLayoutData(m_btnData);
        m_browseWorkflowGroupsBtn.setEnabled(
                KnimeResourceUtil.existsWorkflowGroupInWorkspace());
        m_browseWorkflowGroupsBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleWorkflowGroupBrowseButtonPressed();
            }
        });
        // set initial selection
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (m_initialDestination != null
                && !m_initialDestination.equals(root)) {
            m_targetTextUI.setText(
                    m_initialDestination.getFullPath().toString());
        } else {
            m_targetTextUI.setText("/");
        }
        return overall;
    }

    /**
     *
     * @param parent the parent composite
     * @return the composite of the import page containing the selection
     *  between archive file or directory
     */
    protected Composite createImportComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL | SWT.SHADOW_ETCHED_IN);
        overall.setText("Source:");
        GridData fillHorizontal = new GridData(GridData.FILL_HORIZONTAL);
        overall.setLayoutData(fillHorizontal);

        overall.setLayout(new GridLayout(3, false));
        m_fromDirUI = new Button(overall, SWT.RADIO);
        m_fromDirUI.setText("Select root directory:");
        m_fromDirUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                boolean fromDir = m_fromDirUI.getSelection();
                initialFromDir = fromDir;
                enableDirComponents(fromDir);
                enableZipComponents(!fromDir);
                clear();
                // restore imports from previously selected dir
                if (m_fromDirTextUI.getText() != null
                        && !m_fromDirTextUI.getText().trim().isEmpty()) {
                    collectWorkflowsFromDir(m_fromDirTextUI.getText().trim());
                }
            }
        });

        m_fromDirTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_fromDirTextUI.setLayoutData(fillHorizontal);
        m_dirBrowseBtn = new Button(overall, SWT.PUSH);
        m_dirBrowseBtn.setText("Browse...");
        m_dirBrowseBtn.setLayoutData(m_btnData);
        m_dirBrowseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleDirBrowseButtonPressed();
            }
        });

        m_fromZipUI = new Button(overall, SWT.RADIO);
        m_fromZipUI.setText("Select archive file:");
        m_fromZipUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                boolean fromZip = m_fromZipUI.getSelection();
                initialFromDir = !fromZip;
                enableDirComponents(!fromZip);
                enableZipComponents(fromZip);
                clear();
                // restore imports from previously selected file
                if (m_fromZipTextUI.getText() != null
                        && !m_fromZipTextUI.getText().trim().isEmpty()) {
                    collectWorkflowsFromZipFile(
                            m_fromZipTextUI.getText().trim());
                }
            }

        });
        m_fromZipTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_fromZipTextUI.setLayoutData(fillHorizontal);
        m_zipBrowseBtn = new Button(overall, SWT.PUSH);
        m_zipBrowseBtn.setText("Browse...");
        m_zipBrowseBtn.setLayoutData(m_btnData);
        m_zipBrowseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleZipBrowseButtonPressed();
            }
        });

        // set the initial selection
        m_fromDirUI.setSelection(initialFromDir);
        enableDirComponents(initialFromDir);
        m_fromZipUI.setSelection(!initialFromDir);
        enableZipComponents(!initialFromDir);
        return overall;
    }

    /**
     *
     * @param enable true if the zip file selection UI elements should be
     *  enabled
     */
    public void enableZipComponents(final boolean enable) {
        m_fromZipTextUI.setEnabled(enable);
        m_zipBrowseBtn.setEnabled(enable);
        if (m_copyProjectsUI != null && !m_copyProjectsUI.isDisposed()) {
            m_copyProjectsUI.setEnabled(!enable);
        }
    }

    /**
     *
     * @param enable true if the directory selection UI elements should be
     *  enabled
     */
    public void enableDirComponents(final boolean enable) {
        m_fromDirTextUI.setEnabled(enable);
        m_dirBrowseBtn.setEnabled(enable);
    }

    /**
     *
     * @param parent parent composite
     * @return the lower part of the workflow selection page, the list with
     *  selected workflows to import
     */
    protected Composite createWorkflowListComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL);
        overall.setText("Workflows:");
        overall.setLayoutData(FILL_BOTH);
        overall.setLayout(new GridLayout(2, false));
        // list with checkboxes....
        m_workflowListUI = new CheckboxTreeViewer(overall);
        m_workflowListUI.getTree().setLayoutData(FILL_BOTH);
        m_workflowListUI.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getChildren(final Object parentElement) {
                if (parentElement instanceof IWorkflowImportElement) {
                    return ((IWorkflowImportElement)parentElement).getChildren()
                        .toArray();
                }
                return new Object[0];
            }
            @Override
            public Object getParent(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    return ((IWorkflowImportElement)element).getParent();
                }
                return null;
            }
            @Override
            public boolean hasChildren(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    return ((IWorkflowImportElement)element).getChildren()
                        .size() > 0;
                }
                return false;
            }
            @Override
            public Object[] getElements(final Object inputElement) {
                return getChildren(inputElement);
            }
            @Override
            public void dispose() {
            }
            @Override
            public void inputChanged(final Viewer viewer, final Object oldInput,
                    final Object newInput) {
                // should never happen
            }
        });
        m_workflowListUI.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    IWorkflowImportElement wf = (IWorkflowImportElement)element;
                    return wf.getName();
                }
                return element.toString();
            }
            @Override
            public Image getImage(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    IWorkflowImportElement wf = (IWorkflowImportElement)element;
                    // display invalid ones differently
                    if (wf.isInvalid()) {
                        return IMG_WARN;
                    } else if (wf.isWorkflow()) {
                        return KnimeResourceLabelProvider.CLOSED_WORKFLOW;
                    } else if (wf.isWorkflowGroup()) {
                        return KnimeResourceLabelProvider.WORKFLOW_GROUP;
                    }
                }
                return super.getImage(element);
            }
        });
        m_workflowListUI.setInput(m_importRoot);
        m_workflowListUI.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                Object o = event.getElement();
                boolean isChecked = event.getChecked();
                // first expand in order to be able to check children as well
                m_workflowListUI.expandToLevel(o,
                        AbstractTreeViewer.ALL_LEVELS);
                m_workflowListUI.setSubtreeChecked(o, isChecked);
                if (o instanceof IWorkflowImportElement) {
                    IWorkflowImportElement element = (IWorkflowImportElement)o;
                    setParentTreeChecked(m_workflowListUI, element, isChecked);
                }
                validateWorkflows();
            }
        });
        // and 3 buttons -> panel in second column
        GridData rightAlign = new GridData();
        rightAlign.horizontalAlignment = SWT.RIGHT;
        rightAlign.verticalAlignment = SWT.BEGINNING;
        Composite buttonPanel = new Composite(overall, SWT.NONE);
        buttonPanel.setLayout(new GridLayout(1, false));
        buttonPanel.setLayoutData(rightAlign);
        // select all
        Button selectAllBtn = new Button(buttonPanel, SWT.PUSH);
        selectAllBtn.setText("Select All");
        selectAllBtn.setLayoutData(m_btnData);
        selectAllBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_importRoot != null) {
                    m_workflowListUI.expandAll();
                    m_workflowListUI.setAllChecked(true);
                    validateWorkflows();
                }
            }
        });
        // deselect all
        Button deselectAllBtn = new Button(buttonPanel, SWT.PUSH);
        deselectAllBtn.setText("Deselect All");
        deselectAllBtn.setLayoutData(m_btnData);
        deselectAllBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_importRoot != null) {
                    m_workflowListUI.expandAll();
                    m_workflowListUI.setAllChecked(false);
                    validateWorkflows();
                }
            }
        });
        // below a check box -> copy projects
        m_copyProjectsUI = new Button(overall, SWT.CHECK);
        m_copyProjectsUI.setText("Copy projects into workspace");
        m_copyProjectsUI.setSelection(true);
        m_copyProjectsUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                validateWorkflows();
            }
        });
        // if from zip this checkbox must be disabled
        m_copyProjectsUI.setEnabled(initialFromDir);
        return parent;
    }

    /**
     *
     * @return the underlying tree that displays the workflows to import
     */
    public CheckboxTreeViewer getViewer() {
        return m_workflowListUI;
    }

    /**
     *
     * @return the valid and checked workflow imports
     */
    public Collection<IWorkflowImportElement> getWorkflowsToImport() {
        return m_validAndCheckedImports;
    }

    /**
     *
     * @return the destination path
     */
    protected String getDestination() {
        return m_targetTextUI.getText();
    }

    /**
     *
     * @param viewer the viewer which items should be (un-)checked
     * @param element the element whose parents should be checked
     * @param state true for checked, false for uncheck
     */
    private void setParentTreeChecked(final CheckboxTreeViewer viewer,
            final IWorkflowImportElement element,
            final boolean state) {
        // trivial case -> go up and set active
        if (state) {
            IWorkflowImportElement parent = element.getParent();
            while (parent != null) {
                viewer.setChecked(parent, state);
                parent = parent.getParent();
            }
        } else {
            // not so trivial -> only uncheck if the parent has no other checked
            // children than this one...
            // get all checked
            // but due to the fact that always the direct child is also checked
            IWorkflowImportElement parent = element.getParent();
            while (parent != null) {
                // since we change the checked elements in this while loop we
                // have to retrieve the currently checked elements in each
                // iteration
                List allChecked = Arrays.asList(
                        viewer.getCheckedElements());
                Collection children = new ArrayList();
                children.addAll(parent.getChildren());
                // if there are no common elements between
                // the parent's children and the uncheck elements ->
                // no other checked children
                // -> then we can uncheck it
                children.retainAll(allChecked);
                if (children.isEmpty()) {
                    viewer.setChecked(parent, state);
                } else {
                    break;
                }
                parent = parent.getParent();
            }
        }
    }

    private void handleZipBrowseButtonPressed() {
        // open a FileSelection dialog
        FileDialog dialog = new FileDialog(getShell());
        // allow only archive files
        dialog.setFilterExtensions(ZIP_FILE_MASK);

        // set initial location (either an already selected one)
        String fileName = m_fromZipTextUI.getText().trim();
        if (fileName.isEmpty()) {
            // restore
            fileName = initialZipLocation;
        }
        // if still empty -> set to workspace root
        if (fileName.isEmpty()) {
            fileName = ResourcesPlugin.getWorkspace().getRoot().getLocation()
                .toOSString();
        }
        File initialPath = new File(fileName);
        if (initialPath.exists()) {
            String filterPath = new Path(
                    initialPath.getParent()).toOSString();
            dialog.setFilterPath(filterPath);
            initialZipLocation =  filterPath;
        }
        String selectedFile = dialog.open();
        // null if dialog was canceled/error occurred
        if (selectedFile != null) {
            clear();
            initialZipLocation = selectedFile;
            m_fromZipTextUI.setText(selectedFile);
            // do this in separate thread...
            collectWorkflowsFromZipFile(selectedFile);
        }
        validateWorkflows();
    }


    private void handleDirBrowseButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        // get initial root
        // 1. already something entered?
        String fileName = m_fromDirTextUI.getText().trim();
        if (fileName.isEmpty()) {
            // 2. something stored?
            fileName = initialDirLocation;
        }
        if (fileName.isEmpty()) {
            // set to workspace root
            fileName = ResourcesPlugin.getWorkspace().getRoot().getLocation()
                .toOSString();
        }
        File initialDir = new File(fileName);
        if (initialDir.exists()) {
            if (!initialDir.isDirectory()) {
                initialDir = initialDir.getParentFile();
            }
            dialog.setFilterPath(initialDir.getAbsolutePath());
        }
        final String selectedDir = dialog.open();
        // null if dialog was canceled/error occurred
        if (selectedDir != null) {
            collectWorkflowsFromDir(selectedDir);
        }
        validateWorkflows();
    }

    private void handleWorkflowGroupBrowseButtonPressed() {
        // get the value from the text field
        String wfGroupPath = m_targetTextUI.getText();
        IPath initialPath = ResourcesPlugin.getWorkspace().getRoot()
            .getFullPath();
        // if not yet set take workflow root
        if (wfGroupPath != null && !wfGroupPath.trim().isEmpty()) {
            initialPath = new Path(wfGroupPath);
        }
        WorkflowGroupSelectionDialog dialog = new WorkflowGroupSelectionDialog(
                getShell());
        dialog.setInitialSelection(initialPath);
        int returnCode = dialog.open();
        if (returnCode == IDialogConstants.OK_ID) {
            // set the newly selected workflow group as destination
            IContainer target = dialog.getSelectedWorkflowGroup();
            if (target != null && !target.equals(ResourcesPlugin.getWorkspace()
                    // do not set the root path
                    .getRoot())) {
                m_targetTextUI.setText(target.getFullPath().toString());
            } else {
                m_targetTextUI.setText(Path.ROOT.toString());
            }
        }
        validateWorkflows();
    }

    /**
     *
     * @param selectedDir the directory to collect the contained workflows from
     */
    public void collectWorkflowsFromDir(final String selectedDir) {
        clear();
        initialDirLocation = selectedDir;
        m_fromDirTextUI.setText(selectedDir);
        File dir = new File(selectedDir);
        IWorkflowImportElement root = null;
        // FIXME: this is a hack - should work if the user selects a
        // workflow. Flag workflowSelected indicates that not the path but
        // the name only is considered when the resource is created
        if (WorkflowImportElementFromFile.isWorkflow(dir)) {
            // if the user selected a workflow we set the parent and the
            // child to this workflow - otherwise it would not be displayed
            root = new WorkflowImportElementFromFile(dir, true);
            root.addChild(new WorkflowImportElementFromFile(dir));
        } else {
            root = new WorkflowImportElementFromFile(dir);
        }
        m_importRoot = root;
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException,
                        InterruptedException {
                    monitor.beginTask("Looking for workflows in ",
                            IProgressMonitor.UNKNOWN);
                    collectWorkflowsFromDir(
                            (WorkflowImportElementFromFile)m_importRoot,
                            monitor);
                    monitor.done();
                }
            });
        } catch (Exception e) {
            String message = "Error while trying to import workflows from "
                + selectedDir;
            IStatus status = new Status(IStatus.ERROR,
                    KNIMEUIPlugin.PLUGIN_ID,
                    message, e);
            setErrorMessage(message);
            LOGGER.error(message, e);
            ErrorDialog.openError(getShell(), "Import Error", null, status);
        }
        validateWorkflows();
        m_workflowListUI.setInput(m_importRoot);
        m_workflowListUI.expandAll();
        m_workflowListUI.setAllChecked(true);
        m_workflowListUI.refresh(true);
    }

    private void collectWorkflowsFromDir(
            final WorkflowImportElementFromFile parent,
            final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            m_importRoot = null;
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    m_fromDirTextUI.setText("");
                }

            });
            return;
        }
        File rootDir = parent.getFile();
        if (rootDir == null) {
            throw new IllegalArgumentException(
                    "Directory to import from must not be null!");
        }
        if (rootDir.isFile()) {
            throw new IllegalArgumentException(
                    "Cannot import workflows from file: " + rootDir);
        }
        if (WorkflowImportElementFromFile.isWorkflow(rootDir)) {
            // abort recursion!
            return;
        }
        monitor.subTask(rootDir.getName());
        // else go through all files
        if (rootDir.canRead()) {
            for (File f : rootDir.listFiles()) {
                if (f.isDirectory()) {
                    WorkflowImportElementFromFile child
                        = new WorkflowImportElementFromFile(f);
                    collectWorkflowsFromDir(child, monitor);
                    if (child.isWorkflow() || child.isWorkflowGroup()) {
                        parent.addChild(child);
                    }
                }
            }
        }
    }


    private void collectWorkflowsFromZipFile(final String path) {
        ILeveledImportStructureProvider provider = null;
        if (ArchiveFileManipulations.isTarFile(path)) {
            try {
                TarFile sourceTarFile = new TarFile(path);
                provider = new TarLeveledStructureProvider(sourceTarFile);
            } catch (Exception io) {
                // no file -> list stays empty
                setErrorMessage("Invalid .tar file: "
                        + path
                        + ". Contains no workflow.");
            }
        } else if (ArchiveFileManipulations.isZipFile(path)) {
            try {
                ZipFile sourceFile = new ZipFile(path);
                provider = new ZipLeveledStructureProvider(sourceFile);
            } catch (Exception e) {
                // no file -> list stays empty
                setErrorMessage("Invalid .zip file: "
                        + path
                        + ". Contains no workflows");
            }
        }
        // TODO: store only the workflows (dirs are created automatically)
        final ILeveledImportStructureProvider finalProvider = provider;
        if (provider != null) {
            // reset error
            setErrorMessage(null);
            try {
                getContainer().run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException,
                            InterruptedException {
                        Object child = finalProvider.getRoot();
                        m_importRoot = new WorkflowImportElementFromArchive(
                                finalProvider, child, 0);
                        monitor.beginTask("Scanning for workflows in ",
                                IProgressMonitor.UNKNOWN);
                        collectWorkflowsFromProvider(
                                (WorkflowImportElementFromArchive)m_importRoot,
                                monitor);
                    }

                });
            } catch (Exception e) {
                String message = "Error while trying to import workflows from "
                    + path;
                IStatus status = new Status(IStatus.ERROR,
                        KNIMEUIPlugin.PLUGIN_ID,
                        message, e);
                setErrorMessage(message);
                LOGGER.error(message, e);
                ErrorDialog.openError(getShell(), "Import Error", null, status);
            }
            validateWorkflows();
            m_workflowListUI.setInput(m_importRoot);
            m_workflowListUI.expandAll();
            m_workflowListUI.setAllChecked(true);
            m_workflowListUI.refresh(true);
        }
    }

    /**
     *
     * @param parent the archive element to collect the workflows from
     * @param monitor progress monitor
     */
    public void collectWorkflowsFromProvider(
            final WorkflowImportElementFromArchive parent,
            final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            m_importRoot = null;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    m_fromZipTextUI.setText("");
                }
            });
            return;
        }
        // public in order to make it possible to import from a given zip entry
        ILeveledImportStructureProvider provider = parent.getProvider();
        Object entry = parent.getEntry();
        if (parent.isWorkflow()) {
            // abort recursion
            return;
        }
        List children = provider.getChildren(entry);
        if (children == null) {
            return;
        }
        monitor.subTask(provider.getLabel(entry));
        Iterator childrenEnum = children.iterator();
        while (childrenEnum.hasNext()) {
            Object child = childrenEnum.next();
            if (provider.isFolder(child)) {
                WorkflowImportElementFromArchive childElement
                    = new WorkflowImportElementFromArchive(provider, child,
                            parent.getLevel() + 1);
                collectWorkflowsFromProvider(childElement, monitor);
                // either it's a workflow
                if (childElement.isWorkflow()
                        // or it is a workflow group
                        || childElement.isWorkflowGroup()) {
                    // because only workflows and workflow groups  are added
                    parent.addChild(childElement);
                }
            }
        }
    }

    /**
     * Sets a predefined import root so that it is not selectable by the user.
     *
     * @param importRoot the workflow import element to import from
     *  (might be a zip entry)
     */
    public void setImportRoot(final IWorkflowImportElement importRoot) {
        // public in order to make it possible to use this wizard to import
        // workflows from a zip entry
        boolean isFile = importRoot instanceof WorkflowImportElementFromFile;
        m_fromDirUI.setSelection(isFile);
        m_fromZipUI.setSelection(!isFile);
        m_fromDirUI.setEnabled(false);
        m_fromZipUI.setEnabled(false);
        m_copyProjectsUI.setEnabled(isFile);
        m_importRoot = importRoot;
    }

    public void enableTargetSelection(final boolean enable) {
        m_browseWorkflowGroupsBtn.setEnabled(enable);
    }


    /**
     *
     * @return true if there are some workflows selected which already exist
     *  in the target location
     */
    public boolean containsInvalidAndCheckedImports() {
        return m_invalidAndCheckedImports.size() > 0;
    }

    /**
     * Checks the tree for selected workflows which already exist in the target
     * location.
     */
    protected void collectInvalidAndCheckedImports() {
        m_invalidAndCheckedImports.clear();
        m_validAndCheckedImports.clear();
        if (m_importRoot == null) {
            return;
        }
        collectInvalids(m_validAndCheckedImports,
                m_invalidAndCheckedImports, m_importRoot);
    }

    /**
     *
     * @param valids list of valid workflows
     * @param invalids list of invalid (already existing) workflows
     * @param node current tree node
     */
    protected void collectInvalids(
            final Collection<IWorkflowImportElement> valids,
            final Collection<IWorkflowImportElement> invalids,
            final IWorkflowImportElement node) {
        if (m_workflowListUI.getChecked(node)) {
            if (node.isInvalid()) {
                invalids.add(node);
            } else {
                valids.add(node);
            }
        }
        for (IWorkflowImportElement child : node.getChildren()) {
            collectInvalids(valids, invalids, child);
        }
    }

    /**
     * Collects those elements which have to be renamed (not their children) and
     * those which have been renamed in order to let the user change the name
     * again.
     *
     * @param renameElements list to collect the elements which have to be
     *  renamed or were renamed
     * @param node element to check
     */
    protected void collectRenameElements(
            final Collection<IWorkflowImportElement> renameElements,
            final IWorkflowImportElement node) {
        if (m_workflowListUI.getChecked(node)) {
            if (node.isInvalid()) {
                // only add top elements to rename
                // a resource within an invalid folder is valid if the folder
                // is successfully renamed
                if (node.getParent().equals(m_importRoot)) {
                    renameElements.add(node);
                } else if (!node.getParent().isInvalid()) {
                    renameElements.add(node);
                }
            }
            // and we also want to be able to change the name of a renamed
            // element again
            if (!node.getOriginalName().equals(node.getName())) {
                renameElements.add(node);
            }
        }
        for (IWorkflowImportElement child : node.getChildren()) {
            collectRenameElements(renameElements, child);
        }
    }

    /**
     * Updates the wizard for the current selected import and target location.
     */
    public void validateWorkflows() {
        if (m_importRoot == null) {
            setPageComplete(false);
            // nothing to validate
            return;
        }
        setErrorMessage(null);
        // get the path
        IPath destination = getDestinationPath();

        // clear invalid list
        m_invalidAndCheckedImports.clear();
        // traverse over all items in the tree
        isValidImport(destination, m_importRoot);
        collectInvalidAndCheckedImports();
        setPageComplete(!containsInvalidAndCheckedImports());
        updateMessages();
        // call can finish in order to update the buttons
        getWizard().canFinish();
        m_workflowListUI.refresh(true);
    }


    /**
     *
     * @return the destination path
     */
    protected IPath getDestinationPath() {
        IPath destination = Path.ROOT;
        if (!m_targetTextUI.getText().trim().isEmpty()) {
            destination = new Path(m_targetTextUI.getText().trim());
        }
        return destination;
    }


    /**
     * Sets the invalid flag of the import element to true if the a resource
     * with the same name already exists in the destination location.
     *
     * @param destination the destination path
     * @param element the workflow import element to check
     */
    protected void isValidImport(final IPath destination,
            final IWorkflowImportElement element) {
        // get path
        IPath childPath = element.getRenamedPath();
        // append to the destination path
        IPath result = destination.append(childPath);
        // check whether this exists
        IResource exists = ResourcesPlugin.getWorkspace().getRoot().findMember(
                result, true);
        if (exists != null) {
            // to be sure to set valid if e.g. the destination was changed...
            element.setInvalid(true);
        } else {
            element.setInvalid(false);
        }
        // if true -> mark as invalid
        for (IWorkflowImportElement child : element.getChildren()) {
            isValidImport(destination, child);
        }
    }

    /**
     *
     * @param destination the initial destination container
     */
    public void setInitialTarget(final IContainer destination) {
        m_initialDestination = destination;
    }

    /**
     * Saves the settings of this dialog.
     */
    protected void saveDialogSettings() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            settings.put(KEY_ZIP_LOC, initialZipLocation);
            settings.put(KEY_DIR_LOC, initialDirLocation);
            settings.put(KEY_FROM_DIR, initialFromDir);
        }
        // last selected dir/file
    }

    /**
     * Restores the settings of this dialog from last opened wizard.
     */
    protected void restoreDialogSettings() {
        // TODO: restore the stored settings - if there are any
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            initialZipLocation = settings.get(KEY_ZIP_LOC);
            initialDirLocation = settings.get(KEY_DIR_LOC);
            initialFromDir = settings.getBoolean(KEY_FROM_DIR);
            if (!initialFromDir) {
                if (m_copyProjectsUI != null
                        && !m_copyProjectsUI.isDisposed()) {
                    m_copyProjectsUI.setEnabled(false);
                }
            }
        }
    }

    /**
     * Updates the wizard messages.
     */
    protected void updateMessages() {
        if (containsInvalidAndCheckedImports()) {
            setErrorMessage("Some of the workflows already exists in the "
                    + "target destination! Change the target destination or "
                    + "rename them by clicking \"Next\".");
        } else {
            setErrorMessage(null);
        }
        if (m_importRoot == null) {
            setMessage("Select an import source: either from a directory"
                    + " containing the workflows or from an archive file.",
                    INFORMATION);
        } else if (m_validAndCheckedImports.size() == 0) {
            setMessage("Select the workflows to import, already existing"
                    + " workflows can be renamed on the next page.",
                    INFORMATION);
        } else if (!m_copyProjectsUI.getSelection()) {
            // check if target is != WorkspaceRoot -> fail
            if (!m_targetTextUI.getText().isEmpty()
                    && !m_targetTextUI.getText().equals("/")) {
                setMessage("Workflows cannot be linked into workflow "
                        + "groups!", ERROR);
                setPageComplete(false);
                getWizard().canFinish();
            } else {
                setMessage(
                        "Projects can only be linked into workspace root "
                        + "it will not be possible to use "
                        + "them within a workflow group!", WARNING);
            }
        } else {
            // clear message
            setMessage(null);
        }
    }

    /**
     * Clears everything, the import elements tree, messages, selected imports,
     * use only if different location was selected.
     */
    protected void clear() {
        // delete the rename page to prevent obsolete imports showing up
        setErrorMessage(null);
        m_renamePage = null;
        m_invalidAndCheckedImports.clear();
        m_validAndCheckedImports.clear();
        m_importRoot = null;
        m_workflowListUI.setInput(null);
        m_workflowListUI.refresh();
    }

    /**
     *
     * @return true if there are checked and valid imports and no checked an
     *  invalid imports
     */
    public boolean canFinish() {
        // check if we have checked and valid imports
         if (!isCurrentPage()) {
            return m_renamePage != null && m_renamePage.canFinish();
        }
        return isPageComplete() && m_validAndCheckedImports.size() > 0
            && (!containsInvalidAndCheckedImports());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCurrentPage() {
        // make this method publically available
        return super.isCurrentPage();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return !getRenameElements().isEmpty();
    }

    private Collection<IWorkflowImportElement> getRenameElements() {
        // use a set in order to ensure that each element is only added once
        // use the LinkedHashSet in order to achieve that the order is the same
        // when switching back and forth
        Collection<IWorkflowImportElement> rename
            = new LinkedHashSet<IWorkflowImportElement>();
        if (m_importRoot == null) {
            return rename;
        }
        collectRenameElements(rename, m_importRoot);
        return rename;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        if (canFlipToNextPage()) {
            setMessage(null);
        }
        m_renamePage = createRenamePage();
        return m_renamePage;
    }

    /**
     *
     * @return the rename page populated with the top level elements which have
     * to be renamed or which were renamed
     */
    private RenameWorkflowImportPage createRenamePage() {
        RenameWorkflowImportPage renamePage = null;
        Collection<IWorkflowImportElement> rename = getRenameElements();
        if (!rename.isEmpty()) {
            renamePage = new RenameWorkflowImportPage(this, rename);
            renamePage.setWizard(getWizard());
            renamePage.setPreviousPage(this);
        }
        return renamePage;
    }

    /**
     *
     * @return true if the copy workflow checkbox is selected (recommended),
     *
     */
    public boolean isCopyWorkflows() {
        return m_copyProjectsUI.getSelection();
    }
}

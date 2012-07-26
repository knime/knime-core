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
 *   02.07.2006 (sieb): created
 */
package org.knime.workbench.ui.wizards.export;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.ui.navigator.KnimeResourceLabelProvider;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 * Page to enter the select the workflows to export and enter the destination.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com AG, Zurich, Switzerland
 */
public class WorkflowExportPage extends WizardPage {

    private static final String[] FILTER_EXTENSION = {"*.zip"};

    private Text m_containerText;

    private Text m_fileText;

    private Button m_excludeData;

    private ISelection m_selection;

    private CheckboxTreeViewer m_treeViewer;

    private static String lastSelectedTargetLocation;

    /**
     * Constructor for NewWorkflowPage.
     *
     * @param selection The initial selection
     */
    public WorkflowExportPage(final ISelection selection) {
        super("wizardPage");
        setTitle("Export KNIME workflows");
        setDescription("Exports a KNIME workflow.");
        setImageDescriptor(ImageRepository
                .getImageDescriptor(SharedImages.ExportBig));
        this.m_selection = selection;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        // there are some elements selected
        return (!m_treeViewer.getTree().isVisible() || m_treeViewer
                .getCheckedElements().length > 0)
        // and a target location
                && !m_fileText.getText().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        // place components vertically
        container.setLayout(new GridLayout(1, false));

        Group exportGroup = new Group(container, SWT.NONE);
        exportGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout layout = new GridLayout();
        exportGroup.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        Label label = new Label(exportGroup, SWT.NULL);
        label.setText("Select workflow(s) to export:");

        m_containerText =
                new Text(exportGroup, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        m_containerText.setLayoutData(gd);
        m_containerText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectProjectButton = new Button(exportGroup, SWT.PUSH);
        selectProjectButton.setText("Select...");
        selectProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleBrowse();
            }
        });

        label = new Label(exportGroup, SWT.NULL);
        label.setText("Export file name (zip):");

        m_fileText = new Text(exportGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        m_fileText.setLayoutData(gd);
        m_fileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectExportFilebutton = new Button(exportGroup, SWT.PUSH);
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
        final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_excludeData = new Button(group, SWT.CHECK);
        m_excludeData.setSelection(true);
        m_excludeData.setText("Exclude data from export.");

        createTreeViewer(container);

        initialize();
        dialogChanged();
        setControl(container);
    }

    private void createTreeViewer(final Composite parent) {
        m_treeViewer = new CheckboxTreeViewer(parent);
        m_treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        m_treeViewer.setContentProvider(new ITreeContentProvider() {
            // assume to have IContainers as input
            @Override
            public Object[] getChildren(final Object parentElement) {
                if (parentElement instanceof IContainer) {
                    IContainer container = (IContainer)parentElement;
                    try {
                        // return only workflows and workflow groups
                        Collection<IContainer> children =
                                new ArrayList<IContainer>();
                        for (IResource r : container.members(false)) {
                            if (KnimeResourceUtil.isWorkflow(r)
                                    || KnimeResourceUtil.isWorkflowGroup(r)) {
                                // if it is a workflow or workflow group
                                // it must be a container
                                children.add((IContainer)r);
                            }
                        }
                        return children.toArray();
                    } catch (CoreException e) {
                        return new Object[0];
                    }
                }
                return new Object[0];
            }

            @Override
            public Object getParent(final Object element) {
                if (element instanceof IResource) {
                    return ((IResource)element).getParent();
                }
                return null;
            }

            @Override
            public boolean hasChildren(final Object element) {
                if (element instanceof IWorkspaceRoot) {
                    return true;
                }
                if (element instanceof IContainer) {
                    if (KnimeResourceUtil.isWorkflowGroup((IContainer)element)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Object[] getElements(final Object inputElement) {
                return getChildren(inputElement);
            }

            @Override
            public void dispose() {
                // nothing to do
            }

            @Override
            public void inputChanged(final Viewer viewer,
                    final Object oldInput, final Object newInput) {
                // nothing to do
            }
        });
        m_treeViewer.setLabelProvider(new KnimeResourceLabelProvider());
        m_treeViewer.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                Object o = event.getElement();
                boolean isChecked = event.getChecked();
                // first expand in order to be able to check children as well
                m_treeViewer.expandToLevel(o, AbstractTreeViewer.ALL_LEVELS);
                m_treeViewer.setSubtreeChecked(o, isChecked);
                if (o instanceof IResource) {
                    IResource element = (IResource)o;
                    setParentTreeChecked(m_treeViewer, element, isChecked);
                }
                dialogChanged();
                getWizard().getContainer().updateButtons();
            }

        });
        m_treeViewer.getTree().setVisible(false);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */
    private void initialize() {

        IContainer container = null;
        if (m_selection instanceof IStructuredSelection) {
            container = handleSelection((IStructuredSelection)m_selection);
        }
        // load last selected dir from dialog settings
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String lastSelected = settings.get(KEY_LOC);
            if (lastSelected != null && !lastSelected.isEmpty()) {
                lastSelectedTargetLocation = lastSelected;
            }
        }

        updateFileName(container);
    }

    private void updateFileName(final IContainer container) {
        if (container != null) {
            String fileName = container.getName() + ".zip";
            if (container instanceof IWorkspaceRoot) {
                fileName = "knime-export.zip";
            }
            File f = null;
            if (lastSelectedTargetLocation != null) {
                // create file in last used directory
                File parentFile = new File(lastSelectedTargetLocation);
                if (parentFile.exists() && parentFile.isDirectory()) {
                    f = new File(parentFile, fileName);
                }
            }
            if (f == null) {
                // no value for last selected target - or folder does not exists
                // anymore -> default case: create file in user.home
                f = new File(System.getProperty("user.home"), fileName);
            }
            m_fileText.setText(f.getAbsolutePath());
        }
    }

    private void setLastSelectedExportLocation() {
        File parent = new File(getFileName());
        lastSelectedTargetLocation = parent.getParent();
    }

    /**
     * Uses the standard container selection dialog to choose the new value for
     * the container field.
     */

    private void handleBrowse() {
        WorkflowSelectionDialog dialog =
                new WorkflowSelectionDialog(getShell());
        if (m_selection instanceof IStructuredSelection) {
            dialog.setInitialSelection((IStructuredSelection)m_selection);
        }
        if (dialog.open() != Window.OK) {
            return;
        }
        IStructuredSelection result = dialog.getSelection();
        m_selection = result;
        handleSelection(result);
    }

    private IContainer handleSelection(final IStructuredSelection selection) {
        IContainer resultContainer = null;
        // reset checked elements
        m_treeViewer.expandAll();
        m_treeViewer.setAllChecked(false);
        if (selection.size() > 1) {
            resultContainer = handleMultiSelection(selection);
        } else if (selection.size() == 1) {
            resultContainer = handleSingleSelection(selection);
        }

        if (resultContainer != null) {
            String containerSelectionText =
                    resultContainer.getFullPath().toString();
            if (resultContainer instanceof IWorkspaceRoot) {
                containerSelectionText = "/";
            }
            m_containerText.setText(containerSelectionText);
        }
        // also update the target file name
        updateFileName(resultContainer);
        return resultContainer;
    }

    private IContainer handleSingleSelection(
            final IStructuredSelection selection) {
        Object obj = selection.getFirstElement();
        IContainer resultContainer = null;
        // prepare default export file name
        if (obj instanceof IContainer) {
            resultContainer = (IContainer)obj;
        } else if (obj instanceof IResource) {
            resultContainer = ((IResource)obj).getParent();
        }
        if (obj instanceof NodeContainer) {
            resultContainer = getContainerForWorkflow((NodeContainer)obj);
        }
        if (obj instanceof IContainer) {
            IContainer container = (IContainer)obj;
            // check whether it is a workflow group -> list all workflows
            if (KnimeResourceUtil.isWorkflowGroup(container)
                    || container instanceof IWorkspaceRoot) {
                // get all contained containers and list them recursively
                m_treeViewer.setInput(container);
                m_treeViewer.getTree().setVisible(true);
                m_treeViewer.expandAll();
                // if the selection size is > 1 multiple workflows (-groups)
                // are
                // selected, hence the workspace root. Then we do not select
                // all
                if (selection.size() <= 1) {
                    /*
                     * Since the tree is expanded before the deprecated method
                     * should work. Reason why it is deprecated:
                     * "this method only checks or unchecks visible items" but
                     * CheckboxTreeViewer#setSubtreeChecked(Object, boolean)
                     * does not work.
                     */
                    m_treeViewer.setAllChecked(true);
                }
            } else if (KnimeResourceUtil.isWorkflow(container)) {
                // or a workflow -> list nothing
                m_treeViewer.getTree().setVisible(false);
            }
        }
        return resultContainer;
    }

    private IContainer handleMultiSelection(final IStructuredSelection selection) {
        // set root as input and select all objects
        IContainer resultContainer = ResourcesPlugin.getWorkspace().getRoot();
        m_treeViewer.setInput(resultContainer);
        m_treeViewer.getTree().setVisible(true);
        m_treeViewer.expandAll();
        for (Object o : selection.toArray()) {
            m_treeViewer.setChecked(o, true);
            if (o instanceof IResource) {
                setParentTreeChecked(m_treeViewer, (IResource)o, true);
            }
        }
        return resultContainer;
    }

    private IContainer getContainerForWorkflow(final NodeContainer wfm) {
        URI wfURI = ProjectWorkflowMap.findProjectFor(wfm.getID());
        if (wfURI == null) {
            return null;
        }
        URI rootURI = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
        if (!wfURI.toString().startsWith(rootURI.toString())) {
            // wf not in workspace
            return null;
        }
        return getContainerForFullPath(new Path(wfURI.toString().substring(
                rootURI.toString().length())));
    }

    private IContainer getContainerForFullPath(final IPath workflowPath) {
        IResource resource =
                ResourcesPlugin.getWorkspace().getRoot()
                        .findMember(workflowPath);
        if (resource instanceof IContainer) {
            return (IContainer)resource;
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
        if (m_fileText.getText() != null
                && !m_fileText.getText().trim().isEmpty()) {
            String exportString = m_fileText.getText().trim();
            IPath p = new Path(exportString);
            if (p.segmentCount() > 1) {
                fileDialog.setFilterPath(p.removeLastSegments(1).toOSString());
                fileDialog.setFileName(p.lastSegment());
            }
        }
        String filePath = fileDialog.open();
        if (filePath.trim().length() > 0) {
            // remember the selected path
            // append "zip" extension if not there.
            String extension =
                    filePath.substring(filePath.length() - 4, filePath.length());
            if (!extension.equals(".zip")) {
                filePath = filePath + ".zip";
            }
        }
        m_fileText.setText(filePath);
        setLastSelectedExportLocation();
    }

    /**
     * Ensures that both text fields are set.
     */

    private void dialogChanged() {
        Collection<IContainer> workflows = getWorkflows();
        final List<IResource> list = new ArrayList<IResource>();
        for (IContainer c : workflows) {
            WorkflowExportWizard.addResourcesFor(list, c, excludeData());
        }
        if (list.isEmpty()) {
            updateStatus("Select an element to export!");
            return;
        }
        String fileName = getFileName();
        if (fileName.length() == 0 || fileName.endsWith(File.separator)) {
            updateStatus("File name must be specified");
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
    public IContainer getSelectedContainer() {
        return getContainerForFullPath(new Path(m_containerText.getText()));
    }

    /**
     *
     * @return checked workflows and workflow groups
     */
    public Collection<IContainer> getWorkflows() {
        Object[] checkedObjs = m_treeViewer.getCheckedElements();
        List<IContainer> workflows = new ArrayList<IContainer>();
        // also add the root
        IContainer rootContainer =
                getContainerForFullPath(new Path(m_containerText.getText()));
        if (rootContainer != null) {
            workflows.add(rootContainer);
        }
        for (Object o : checkedObjs) {
            if (o instanceof IContainer) {
                IContainer container = (IContainer)o;
                if (KnimeResourceUtil.isWorkflowGroup(container)
                        || KnimeResourceUtil.isWorkflow(container)) {
                    workflows.add(container);
                }
            }
        }
        return workflows;
    }

    /**
     * @return The file name
     */
    public String getFileName() {
        return m_fileText.getText();
    }

    private static final String KEY_LOC = "destination-location";

    /**
     * Saves the last selected location (the parent of the last export file).
     *
     * @see WorkflowExportWizard#performFinish()
     */
    public void saveDialogSettings() {
        setLastSelectedExportLocation();
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            settings.put(KEY_LOC, lastSelectedTargetLocation);
        }
    }

    /**
     *
     * @param viewer the viewer which items should be (un-)checked
     * @param element the element whose parents should be checked
     * @param state true for checked, false for uncheck
     */
    private void setParentTreeChecked(final CheckboxTreeViewer viewer,
            final IResource element, final boolean state) {
        // see also WorkflowImportSelectionPage where the same method for
        // IWorkflowImportElement instead of IResources exists
        // -> no common super type for IWorkflowImportElement and IResource

        // trivial case -> go up and set active
        if (state) {
            IResource parent = element.getParent();
            while (parent != null) {
                viewer.setChecked(parent, state);
                parent = parent.getParent();
            }
        } else {
            // not so trivial -> only uncheck if the parent has no other checked
            // children than this one...
            // get all checked
            // but due to the fact that always the direct child is also checked
            IContainer parent = element.getParent();
            while (parent != null) {
                // since we change the checked elements in this while loop we
                // have to retrieve the currently checked elements in each
                // iteration
                List allChecked = Arrays.asList(viewer.getCheckedElements());
                Collection children = new ArrayList();
                try {
                    for (IResource c : parent.members()) {
                        // do not add files, since they are not
                        // displayed in the tree -> see #retainAll below
                        if (c instanceof IContainer) {
                            children.add(c);
                        }
                    }
                } catch (CoreException ce) {
                    // nothing to do -> simply skip it
                }
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
}

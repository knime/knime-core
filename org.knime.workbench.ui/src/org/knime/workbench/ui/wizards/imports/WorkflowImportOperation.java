/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   10.09.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.imports;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.nature.KNIMEWorkflowSetProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Imports workflows from a zip file or directory into the workspace.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class WorkflowImportOperation extends WorkspaceModifyOperation {

    private final Collection<IWorkflowImportElement> m_workflows;

    private final IPath m_targetPath;

    private final boolean m_copy;

    private final Shell m_shell;

    // stores those directories which not yet contain a metainfo file and
    // hence are not displayed - meta info file has to be created after the
    // import -> occurs when importing zip files containing directories
    private final List<IPath> m_missingMetaInfoLocations
        = new ArrayList<IPath>();

    /**
     *
     * @param workflows the import elements (file or zip entries) to import
     * @param targetPath the destination path within the workspace
     * @param copy true if the workflows should be copied (recommended), false
     *  if they should only be linked (only possible if they are imported
     *  directly into the workspace root)
     * @param shell the shell
     */
    public WorkflowImportOperation(
            final Collection<IWorkflowImportElement> workflows,
            final IPath targetPath, final boolean copy, final Shell shell) {
        m_workflows = workflows;
        m_targetPath = targetPath;
        m_copy = copy;
        m_shell = shell;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void execute(final IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        ILeveledImportStructureProvider provider = null;
        try {
            monitor.beginTask("", m_workflows.size());
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            for (IWorkflowImportElement wf : m_workflows) {
                if (!m_copy) {
                        handleLinkedProject(wf, monitor);
                } else {
                    provider = handleCopyProject(wf, monitor);
                }
            }
            if (!m_missingMetaInfoLocations.isEmpty()) {
                createMetaInfo();
            }
            // clean up afterwards
            m_missingMetaInfoLocations.clear();
            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
                    IResource.DEPTH_INFINITE, new NullProgressMonitor());
            final IResource result = ResourcesPlugin.getWorkspace().getRoot()
                .findMember(m_targetPath);
            if (result != null) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        KnimeResourceUtil.revealInNavigator(result);
                    }
                });
            }
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            if (provider != null) {
                ArchiveFileManipulations.closeStructureProvider(
                        provider, m_shell);
            }
            monitor.done();
        }
    }

    private ILeveledImportStructureProvider handleCopyProject(
            final IWorkflowImportElement importElement,
            final IProgressMonitor monitor) throws Exception {
        IPath destination = m_targetPath.append(importElement.getRenamedPath());
        ImportOperation operation = null;
        ILeveledImportStructureProvider provider = null;
        if (importElement instanceof WorkflowImportElementFromFile) {
            operation = createWorkflowFromFile(
                    (WorkflowImportElementFromFile)importElement,
                            destination,
                            new SubProgressMonitor(monitor, 1));
        } else if (importElement
                instanceof WorkflowImportElementFromArchive) {
            WorkflowImportElementFromArchive zip
                = (WorkflowImportElementFromArchive)importElement;
            if (provider != null) {
                // since we can only import from one archive
                // file we assume to have the very same provider
                // for all entries
                assert provider == zip.getProvider();
            }
            provider = zip.getProvider();
            operation = createWorkflowFromArchive(zip,
                    destination,
                    new SubProgressMonitor(monitor, 1));
        }
        if (operation != null) {
            operation.setContext(m_shell);
            operation.setOverwriteResources(true);
            operation.setCreateContainerStructure(false);
            operation.run(monitor);

            // if we created a project -> set the correct nature
            if (Path.ROOT.equals(m_targetPath)) {
                setProjectNature(importElement);
            }
            monitor.worked(1);
        }
        return provider;
    }


    private void setProjectNature(
            final IWorkflowImportElement importElement)
        throws Exception {
        // get name
        String projectName = importElement.getName();
        // get project
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(projectName);
        if (!project.exists()) {
            // if project not exists then the import element is a workflow or
            // workflow group somewhere down the hierarchy -> cannot set a
            // project description for it
            assert importElement.getRenamedPath().segmentCount() > 1;
            return;
        }
        IProjectDescription description = project.getDescription();
        if (description == null) {
            description = workspace.newProjectDescription(projectName);
        }
        String natureId = KNIMEWorkflowSetProjectNature.ID;
        // check whether workflow or workflow group
        if (importElement.isWorkflow()) {
            natureId = KNIMEProjectNature.ID;
        }
        // set nature in project description
        description.setNatureIds(new String[] {natureId});
        project.setDescription(description, new NullProgressMonitor());
    }


    private void handleLinkedProject(
            final IWorkflowImportElement importElement,
            final IProgressMonitor monitor) throws Exception {
        // assumptions: link them as projects into workspace root!
        // not from zip but from file
        // link to the referring destination
        if (!m_targetPath.equals(Path.ROOT)) {
            throw new IllegalArgumentException(
                    "Workflows must be linked into "
                    + "workspace root!");
        }
        if (!(importElement instanceof WorkflowImportElementFromFile)) {
            throw new IllegalArgumentException(
                    "Only unzipped workflows can be linked "
                    + "into workspace root!");
        }
        WorkflowImportElementFromFile fileImportElement
            = (WorkflowImportElementFromFile)importElement;
        String projectName = importElement.getName();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(projectName);
        File projectDescrFile = new File(fileImportElement.getFile(),
                IProjectDescription.DESCRIPTION_FILE_NAME);
        FileInputStream io = new FileInputStream(projectDescrFile);
        IProjectDescription description = null;
        if (io != null) {
            description = ResourcesPlugin.getWorkspace().loadProjectDescription(
                    io);
            io.close();
        }
        if (description == null) {
            // error case
            description = workspace.newProjectDescription(projectName);
        }
        IPath locationPath = new Path(fileImportElement.getFile()
                .getAbsolutePath());
        // If it is under the root use the default location
        if (Platform.getLocation().isPrefixOf(locationPath)) {
            description.setLocation(null);
        } else {
            description.setLocation(locationPath);
        }
        description.setName(projectName);
        project.create(description, new SubProgressMonitor(monitor,
                30));
        project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
                monitor, 70));
    }

    /**
     *
     * @param workflow the workflow dir to import
     * @param target the destination path to import the workflow to
     * @param monitor a progress monitor to report progress to
     * @return the created import operation
     * @throws InterruptedException if wizard was canceled
     * @throws InvocationTargetException if something goes wrong
     */
    protected ImportOperation createWorkflowFromFile(
            final WorkflowImportElementFromFile workflow, final IPath target,
            final IProgressMonitor monitor) throws InterruptedException,
            InvocationTargetException {
        monitor.beginTask(workflow.getName(), 1);
        ImportOperation operation = null;
        if (workflow.isWorkflow()) {
            List<File> filesToImport = new ArrayList<File>();
            getFilesForWorkflow(filesToImport, workflow.getFile());
            operation =
                    new ImportOperation(target, workflow.getFile(),
                            FileSystemStructureProvider.INSTANCE,
                            new IOverwriteQuery() {
                                @Override
                                public String queryOverwrite(
                                        final String pathString) {
                                    return IOverwriteQuery.YES;
                                }
                            });
        } else {
            // store path to create a meta info file
            m_missingMetaInfoLocations.add(target);
            // no workflow -> no import

        }
        monitor.done();
        return operation;
    }

    /**
     *
     * @param files the list of files contained in this directory
     * @param workflowDir the directory to add all contained files from
     */
    protected void getFilesForWorkflow(final List<File> files,
            final File workflowDir) {
        files.add(workflowDir);
        if (workflowDir.isDirectory()) {
            for (File f : workflowDir.listFiles()) {
                getFilesForWorkflow(files, f);
            }
        }
    }

    /**
     *
     * @param workflow workflow im port element
     * @param target the destination path of this workflow
     * @param monitor a submonitor to report progress to
     * @return the prepared import operation
     * @throws InvocationTargetException if something goes wrong
     * @throws InterruptedException if wizard was canceled
     */
    protected ImportOperation createWorkflowFromArchive(
            final WorkflowImportElementFromArchive workflow,
            final IPath target, final IProgressMonitor monitor)
    throws InvocationTargetException, InterruptedException {
        // import only workflow -> the path to them will be created anyway
        // by ContainerGenerator in ImportOperation
        monitor.beginTask(workflow.getName(), 1);
        ImportOperation op = null;
        if (workflow.isWorkflow()) {
            op =  new ImportOperation(target,
                    workflow.getEntry(),
                    workflow.getProvider(),
                    new IOverwriteQuery() {
                        public String queryOverwrite(final String pathString) {
                        return IOverwriteQuery.YES;
                    }
            });
        } else {
            // store path to create a meta info file
            m_missingMetaInfoLocations.add(target);
            // no workflow -> no import
        }
        monitor.done();
        return op;
    }




    private void createMetaInfo() throws Exception {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (IPath p : m_missingMetaInfoLocations) {
            // to be sure that target location indeed exists -> create it
            ContainerGenerator generator = new ContainerGenerator(p);
            generator.generateContainer(new NullProgressMonitor());
            p = root.getLocation()
                // append the workspace relative path to the workspace path
                .append(p);
            File parent = p.toFile();
            MetaInfoFile.createMetaInfoFile(parent, false);
        }
    }

}

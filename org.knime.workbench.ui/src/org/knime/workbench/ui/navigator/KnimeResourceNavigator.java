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
 *   Jun 7, 2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.CloseUnrelatedProjectsAction;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.navigator.ResourceNavigator;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.JobManagerChangedEvent;
import org.knime.core.node.workflow.JobManagerChangedListener;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.actions.CancelWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ConfigureWorkflowAction;
import org.knime.workbench.ui.navigator.actions.CreateWorkflowGroupAction;
import org.knime.workbench.ui.navigator.actions.EditMetaInfoAction;
import org.knime.workbench.ui.navigator.actions.ExecuteWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ExportKnimeWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ImportKnimeWorkflowAction;
import org.knime.workbench.ui.navigator.actions.OpenCredentialVariablesDialogAction;
import org.knime.workbench.ui.navigator.actions.OpenWorkflowVariablesDialogAction;
import org.knime.workbench.ui.navigator.actions.ResetWorkflowAction;
import org.knime.workbench.ui.navigator.actions.WFShowJobMgrViewAction;

/**
 * This class is a filtered view on a knime project which hides utitility files
 * from the tree. Such files include the data files and files being used to save
 * the internals of a node.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class KnimeResourceNavigator extends ResourceNavigator implements
        NodeStateChangeListener, NodeMessageListener, JobManagerChangedListener {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(KnimeResourceNavigator.class);

    /** ID as defined in plugin.xml. */
    public static final String ID =
            "org.knime.workbench.ui.navigator.KnimeResourceNavigator";

    /** Id for the group to add menu items to the context menu. */
    public static final String KNIME_ADDITIONS =
            "org.knime.workbench.ui.knimeadditions";

    /**
     * Creates a new <code>KnimeResourceNavigator</code> with an final
     * <code>OpenFileAction</code> to open workflows when open a knime project.
     */

    public KnimeResourceNavigator() {
        super();

        LOGGER.debug("KNIME resource navigator created");

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                new KnimeResourceChangeListener(this));

        ProjectWorkflowMap.addStateListener(this);
        ProjectWorkflowMap.addNodeMessageListener(this);
        ProjectWorkflowMap.addJobManagerChangedListener(this);
        // WorkflowManager.ROOT.addListener(
        ProjectWorkflowMap.addWorkflowListener(new WorkflowListener() {

            public void workflowChanged(final WorkflowEvent event) {
                LOGGER.debug("ROOT's workflow has changed " + event.getType());
                switch (event.getType()) {
                case NODE_ADDED:
                    NodeContainer ncAdded = (NodeContainer)event.getNewValue();
                    LOGGER.debug("Workflow " + ncAdded.getNameWithID()
                            + " added");
                    if (getViewer() != null) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                getViewer().refresh();
                            }
                        });
                    }
                    break;
                case NODE_REMOVED:
                    NodeContainer ncRem = (NodeContainer)event.getOldValue();
                    LOGGER.debug("Workflow " + ncRem.getNameWithID()
                            + " removed");
                    if (getViewer() != null) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                if (!getViewer().getControl().isDisposed()) {
                                    getViewer().refresh();
                                }
                            }
                        });
                    }
                    break;
                default:
                    // ignored, not interesting in this context
                }
            }

        });

        // to be sure register to all existing projects (in case they are added
        // before this constructor is called)
        // for (NodeContainer nc : WorkflowManager.ROOT.getNodeContainers()) {
        // // register here to this nc and listen to changes
        // // on change -> update labels
        // // TODO: remove the listener?
        // nc.addNodeStateChangeListener(this);
        // }

    }

    /**
     *
     * {@inheritDoc}
     */
    public void stateChanged(final NodeStateEvent state) {
        LOGGER.debug("state changed to " + state.getState());
        doRefresh(state.getSource());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void messageChanged(final NodeMessageEvent messageEvent) {
        LOGGER.debug("Node message changed: " + messageEvent.getMessage());
        doRefresh(messageEvent.getSource());
    }

    /** {@inheritDoc} */
    @Override
    public void jobManagerChanged(final JobManagerChangedEvent e) {
        LOGGER.debug("Job Manager changed for node  " + e.getSource());
        doRefresh(e.getSource());
    }

    private void doRefresh(final NodeID nodeResource) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            public void run() {
                try {
                    IPath path =
                            ProjectWorkflowMap.findProjectFor(nodeResource);
                    if (path != null) {
                        // we have to find the resource again, hence we cannot
                        // put the project's name with toLowercase into the map
                        IResource rsrc =
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .findMember(path);
                        if (rsrc != null) {
                            getTreeViewer().update(rsrc, null);
                        }
                    } else {
                        /*
                         * this is a meta node used in a project. Currently we
                         * don't need to refresh the tree because meta node
                         * states are not shown in the tree
                         */
                        // LOGGER.debug("didn't find project name - do refresh");
                        // getTreeViewer().refresh();
                    }
                } catch (IllegalArgumentException iae) {
                    // node couldn't be found -> so we don't make a refresh
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeViewer createViewer(final Composite parent) {
        TreeViewer viewer =
                new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL) {
                    @Override
                    protected void handleDoubleSelect(final SelectionEvent event) {
                        // the default implementation in the navigator
                        // performs a collapse/expand AND an open event.
                        // We only want either, depending on the selection.
                        Object sel =
                                ((StructuredSelection)getSelection())
                                        .getFirstElement();
                        if (isWorkflow(sel)) {
                            KnimeResourceNavigator.this.handleOpen(
                                    new OpenEvent(this, getSelection()));
                        } else {
                            // expand the double-clicked element one level down
                            KnimeResourceNavigator.super.handleDoubleClick(
                                    new DoubleClickEvent(this, getSelection()));
                        }
                    }
                };
        viewer.setUseHashlookup(true);
        initContentProvider(viewer);
        initLabelProvider(viewer);
        initFilters(viewer);
        initListeners(viewer);
        // viewer.getControl().setDragDetect(false);

        /*
         * // TODO: if we want to support linking to editor we have to enable
         * this and add a cast to WorkflowRootEditPart (for this we have to add
         * another dependency from ui to editor) get the name and select it
         * getSite().getPage().addPostSelectionListener(new ISelectionListener()
         * { public void selectionChanged(IWorkbenchPart part, ISelection
         * selection) { if (isLinkingEnabled()) { LOGGER.debug("linking to " +
         * selection.toString()); } } });
         */

        return viewer;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void initDragAndDrop() {
        TreeViewer viewer = getViewer();
        viewer.addDragSupport(DND.DROP_MOVE, new Transfer[]{ResourceTransfer
                .getInstance()}, new WorkflowMoveDragListener(viewer));
        viewer.addDropSupport(DND.DROP_MOVE, new Transfer[]{
                ResourceTransfer.getInstance(), RemoteFileTransfer.getInstance()},
                new WorkflowMoveDropListener(viewer));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        ProjectWorkflowMap.removeStateListener(this);
        ProjectWorkflowMap.removeNodeMessageListener(this);
    }

    /**
     * Adds the filters to the viewer.
     *
     * @param viewer the viewer
     * @since 2.0
     */
    @Override
    protected void initFilters(final TreeViewer viewer) {
        // super.initFilters(viewer);
        // viewer.resetFilters();
        viewer.addFilter(new KnimeResourcePatternFilter());
    }

    /**
     * Sets the label provider for the viewer.
     *
     * @param viewer the viewer
     * @since 2.0
     */
    @Override
    protected void initLabelProvider(final TreeViewer viewer) {
        viewer.setLabelProvider(new DecoratingLabelProvider(
                new KnimeResourceLabelProvider(), new JobManagerDecorator()));
    }

    /**
     * Handles an open event from the viewer. Opens an editor on the selected
     * knime project.
     *
     * @param event the open event
     */
    @Override
    protected void handleOpen(final OpenEvent event) {

        Object selection =
                ((IStructuredSelection)event.getSelection()).getFirstElement();

        if (selection instanceof IContainer) {
            IFile file = getWorkflowFile(selection);
            if (file != null && file.exists()) {
                if (isWorkflow(selection)) {
                    // don't open meta nodes
                    StructuredSelection selection2 =
                            new StructuredSelection(file);

                    OpenFileAction action =
                            new OpenFileAction(PlatformUI.getWorkbench()
                                   .getActiveWorkbenchWindow().getActivePage());
                    action.selectionChanged(selection2);
                    action.run();
                }
            } else {
                EditMetaInfoAction action = new EditMetaInfoAction();
                if (action.isEnabled()) {
                    action.run();
                }
                return;
            }
        }
    }

    /**
     * Returns the workflow file - of a flow or a meta node - or null.
     * @param o
     * @return
     */
    private IFile getWorkflowFile(final Object o) {
        if (o instanceof IContainer) {
            IContainer container = (IContainer)o;
            Path wfPath = new Path(WorkflowPersistor.WORKFLOW_FILE);
            if (container.exists(wfPath)) {
                    return (IFile)container.findMember(
                            WorkflowPersistor.WORKFLOW_FILE);
            }
        }
        return null;
    }

    private boolean isWorkflow(final Object o) {
        if (o instanceof IContainer) {
            IContainer container = (IContainer)o;
            Path wfPath = new Path(WorkflowPersistor.WORKFLOW_FILE);
            if (container.exists(wfPath)) {
                // container must have a workflow file
                IFile wFile =
                        (IFile)container
                                .findMember(WorkflowPersistor.WORKFLOW_FILE);
                if (wFile != null && wFile.exists()) {
                    // also parent must _not_ have a workflow file
                    return (container.getParent() == null)
                            || !container.getParent().exists(wfPath);
                }
            }
        }
        return false;
    }

    /**
     * Fills the context menu with the actions contained in this group and its
     * subgroups. Additionally the close project item is removed as not intended
     * for the knime projects. Note: Projects which are closed in the default
     * navigator are not shown in the knime navigator any more.
     *
     * @param menu the context menu
     */
    @Override
    public void fillContextMenu(final IMenuManager menu) {
        // fill the menu
        super.fillContextMenu(menu);

        // remove the close project item
        menu.remove(CloseResourceAction.ID);

        // remove some more items (this is more sophisticated, as these
        // items do not have an id
        for (IContributionItem item : menu.getItems()) {

            if (item instanceof ActionContributionItem) {

                ActionContributionItem aItem = (ActionContributionItem)item;
                // remove the gointo item
                if (aItem.getAction() instanceof GoIntoAction) {

                    menu.remove(aItem);
                } else if (aItem.getAction() instanceof OpenInNewWindowAction) {

                    menu.remove(aItem);
                } else if (aItem.getAction() instanceof CloseUnrelatedProjectsAction) {
                    menu.remove(aItem);
                }

            }
        }

        // remove the default import export actions to store the own one
        // that invokes the knime export wizard directly
        menu.remove("import");
        menu.insertBefore("export", new ImportKnimeWorkflowAction(PlatformUI
                .getWorkbench().getActiveWorkbenchWindow()));

        menu.remove("export");
        menu.insertAfter(ImportKnimeWorkflowAction.ID,
                new ExportKnimeWorkflowAction(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()));

        // TODO: this is hardcoded the copy item. should be retrieved more
        // dynamically
        String id = menu.getItems()[2].getId();

        // add an open action which is not listed as the project is normally
        // not openable.
        menu.insertBefore(id, new Separator());
        menu.insertBefore(id, new OpenKnimeProjectAction(this));

        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());
        menu
                .insertAfter(ExportKnimeWorkflowAction.ID,
                        new EditMetaInfoAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());

        if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() > 1) {
            menu.insertAfter(ExportKnimeWorkflowAction.ID,
                    new WFShowJobMgrViewAction());
        }
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new ResetWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new CancelWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new ExecuteWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new ConfigureWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new OpenCredentialVariablesDialogAction());
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_EXPERT_MODE)) {
            menu.insertAfter(ExportKnimeWorkflowAction.ID,
                    new OpenWorkflowVariablesDialogAction());
        }
        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());

        menu.insertBefore(RefreshAction.ID, new GroupMarker(KNIME_ADDITIONS));
        menu.insertBefore(RefreshAction.ID, new Separator());

        menu.insertBefore(id, new Separator());

        // another bad workaround to replace the first "New" menu manager
        // with the "Create New Workflow" action
        // store all items, remove all, add the action and then
        // add all but the first one
        IContributionItem[] items = menu.getItems();
        for (IContributionItem item : items) {
            menu.remove(item);
        }
        menu.add(new NewKnimeWorkflowAction(PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()));
        menu.add(new CreateWorkflowGroupAction());
        for (int i = 1; i < items.length; i++) {
            menu.add(items[i]);
        }

    }

    /**
     * Sets the content provider for the viewer.
     *
     * @param viewer the viewer
     * @since 2.0
     */
    @Override
    protected void initContentProvider(final TreeViewer viewer) {
        viewer.setContentProvider(new KnimeResourceContentProvider());
    }

}

/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Jun 7, 2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.CloseUnrelatedProjectsAction;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.internal.ide.model.WorkbenchAdapterBuilder;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.navigator.ResourceNavigator;
import org.knime.core.node.NodeLogger;
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
import org.knime.workbench.ui.navigator.actions.CreateSubfolderAction;
import org.knime.workbench.ui.navigator.actions.EditMetaInfoAction;
import org.knime.workbench.ui.navigator.actions.ExecuteWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ExportKnimeWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ImportKnimeWorkflowAction;
import org.knime.workbench.ui.navigator.actions.ResetWorkflowAction;
import org.knime.workbench.ui.navigator.actions.WFShowJobMgrViewAction;


/**
 * This class is a filtered view on a knime project which hides utitility files
 * from the tree. Such files include the data files, pmml files and files being
 * used to save the internals of a node.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeResourceNavigator extends ResourceNavigator implements
        IResourceChangeListener, NodeStateChangeListener, NodeMessageListener {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(KnimeResourceNavigator.class);

    /** ID as defined in plugin.xml. */
    public static final String ID
        = "org.knime.workbench.ui.navigator.KnimeResourceNavigator";

    /**
     * Creates a new <code>KnimeResourceNavigator</code> with an final
     * <code>OpenFileAction</code> to open workflows when open a knime
     * project.
     */

    public KnimeResourceNavigator() {
        super();
        
        WorkbenchAdapterBuilder.registerAdapters(); 
        
        LOGGER.debug("KNIME resource navigator created");

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                new KnimeResourceChangeListener(this));

        ProjectWorkflowMap.addStateListener(this);
        ProjectWorkflowMap.addNodeMessageListener(this);
        // WorkflowManager.ROOT.addListener(
        ProjectWorkflowMap.addWorkflowListener(new WorkflowListener() {

            public void workflowChanged(final WorkflowEvent event) {
                LOGGER.debug("ROOT's workflow has changed " + event.getType());
                switch (event.getType()) {
                case NODE_ADDED:
                    NodeContainer ncAdded = (NodeContainer)event.getNewValue();
                    LOGGER.debug(
                            "Workflow " + ncAdded.getNameWithID() + " added");
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
//        for (NodeContainer nc : WorkflowManager.ROOT.getNodeContainers()) {
//                // register here to this nc and listen to changes
//                // on change -> update labels
//                // TODO: remove the listener?
//                nc.addNodeStateChangeListener(this);
//        }

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

    private void doRefresh(final NodeID nodeResource) {
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            public void run() {
                try {
                    String name =  ProjectWorkflowMap.findProjectFor(
                            nodeResource);
                    if (name != null) {
                        // we have to find the resource again, hence we cannot
                        // put the project's name with toLowercase into the map
                        IResource rsrc = ResourcesPlugin.getWorkspace()
                            .getRoot().findMember(new Path(name));
                        if (rsrc != null) {
                            getTreeViewer().update(rsrc, null);
                        }
                    } else {
                        LOGGER.debug("didn't find project name - do refresh");
                        getTreeViewer().refresh();
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
        TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL) {
            @Override
            protected void handleDoubleSelect(final SelectionEvent event) {
                // we have to consume this event in order to avoid
                // expansion/collaps of the double clicked project
                // strangly enough it opens anyway and the collopased or
                // expanded state remains
            }
        };
        viewer.setUseHashlookup(true);
        initContentProvider(viewer);
        initLabelProvider(viewer);
        initFilters(viewer);
        initListeners(viewer);
//        viewer.getControl().setDragDetect(false);

        /*
        // TODO: if we want to support linking to editor
         * we have to enable this and add a cast to WorkflowRootEditPart
         * (for this we have to add another dependency from ui to editor)
         * get the name and select it
        getSite().getPage().addPostSelectionListener(new ISelectionListener() {
            public void selectionChanged(IWorkbenchPart part,
                    ISelection selection) {
                if (isLinkingEnabled()) {
                    LOGGER.debug("linking to " +  selection.toString());
                }
            }
        });
        */

        return viewer;
    }

    @Override
    protected void initDragAndDrop() {
        TreeViewer viewer = getViewer();
        viewer.addDragSupport(DND.DROP_MOVE, new Transfer[]{
                LocalSelectionTransfer.getTransfer(),
                FileTransfer.getInstance()}, new WorkflowMoveDragListener());
        viewer.addDropSupport(DND.DROP_MOVE, new Transfer[]{
                LocalSelectionTransfer.getTransfer(),
                FileTransfer.getInstance()}, new WorkflowMoveDropListener());
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
//        super.initFilters(viewer);
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
        viewer.setLabelProvider(
                new DecoratingLabelProvider(new KnimeResourceLabelProvider(),
                        new JobManagerDecorator()));
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
            IContainer container = (IContainer)selection;
            IFile file = null;
            Path wfPath = new Path(WorkflowPersistor.WORKFLOW_FILE);
            if (container.exists(wfPath)) {
                if (container.getParent() != null
                        && !container.getParent().exists(wfPath)) {
                    file = (IFile)container.findMember(
                            WorkflowPersistor.WORKFLOW_FILE);
                    LOGGER.debug("opening: " + container.getName());
                }
            } else {
                EditMetaInfoAction action = new EditMetaInfoAction();
                if (action.isEnabled()) {
                    action.run();
                }
                return;
            }
            if (file != null && file.exists()) {
                StructuredSelection selection2 =
                        new StructuredSelection(file);

                OpenFileAction action = new OpenFileAction(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage());
                action.selectionChanged(selection2);
                action.run();
            }
        }
    }

    /**
     * Fills the context menu with the actions contained in this group and its
     * subgroups. Additionally the close project item is removed as not intended
     * for the kinme projects. Note: Projects which are closed in the default
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
                } else if (aItem.getAction()
                        instanceof CloseUnrelatedProjectsAction) {
                    menu.remove(aItem);
                }

            }
        }

        // remove the default import export actions to store the own one
        // that invokes the knime export wizard directly
        menu.remove("import");
        menu.insertBefore("export", new ImportKnimeWorkflowAction(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()));

        menu.remove("export");
        menu.insertAfter(ImportKnimeWorkflowAction.ID,
                new ExportKnimeWorkflowAction(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()));

        // TODO: this is hardcoded the copy item. should be retreived more
        // dynamically
        String id = menu.getItems()[2].getId();

        // add an open action which is not listed as the project is normally
        // not openable.
        menu.insertBefore(id, new Separator());
        menu.insertBefore(id, new OpenKnimeProjectAction(this));

        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new CreateSubfolderAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new EditMetaInfoAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());

//        if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() > 1) {
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new WFShowJobMgrViewAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new ResetWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new CancelWorkflowAction());
        menu.insertAfter(ExportKnimeWorkflowAction.ID,
                new ExecuteWorkflowAction());
            menu.insertAfter(ExportKnimeWorkflowAction.ID,
                    new ConfigureWorkflowAction());
            menu.insertAfter(ExportKnimeWorkflowAction.ID, new Separator());
//        }

        // TODO: insert actions for
        // - execute
        // - cancel

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

    
    /// NOT REGISTERED!!!


    /**
     * {@inheritDoc}
     */
    public void resourceChanged(final IResourceChangeEvent event) {
            try {
                if (event == null || event.getDelta() == null) {
                    return;
                }
                event.getDelta().accept(new ResourceVisitor());
            } catch (CoreException e) {
                // should never happen, I think...
                e.printStackTrace();
            }


        /*
        // do nothing
        try {
            LOGGER.debug("refreshing " + event.getResource().getName());
            event.getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException ce) {
            // TODO: what to do?
            LOGGER.error("exception during resource change event", ce);
        }
        */
    }

    private class ResourceVisitor implements IResourceDeltaVisitor {
    private String getTypeString(final IResourceDelta delta) {
        StringBuffer buffer = new StringBuffer();

        if ((delta.getKind() & IResourceDelta.ADDED) != 0) {
            buffer.append("ADDED|");
        }
        if ((delta.getKind() & IResourceDelta.ADDED_PHANTOM) != 0) {
            buffer.append("ADDED_PHANTOM|");
        }
        if ((delta.getKind() & IResourceDelta.ALL_WITH_PHANTOMS) != 0) {
            buffer.append("ALL_WITH_PHANTOMS|");
        }
        if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
            buffer.append("CHANGED|");
        }
        if ((delta.getKind() & IResourceDelta.CONTENT) != 0) {
            buffer.append("CONTENT|");
        }
        if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
            buffer.append("DESCRIPTION|");
        }
        if ((delta.getKind() & IResourceDelta.ENCODING) != 0) {
            buffer.append("ENCODING|");
        }
        if ((delta.getKind() & IResourceDelta.MARKERS) != 0) {
            buffer.append("MARKERS|");
        }
        if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
            buffer.append("MOVED_FROM|");
        }
        if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
            buffer.append("MOVED_TO|");
        }
        if ((delta.getKind() & IResourceDelta.NO_CHANGE) != 0) {
            buffer.append("NO_CHANGE|");
        }
        if ((delta.getKind() & IResourceDelta.OPEN) != 0) {
            buffer.append("OPEN|");
        }
        if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
            buffer.append("REMOVED|");
        }
        if ((delta.getKind() & IResourceDelta.REMOVED_PHANTOM) != 0) {
            buffer.append("REMOVED_PHANTOM|");
        }
        if ((delta.getKind() & IResourceDelta.REPLACED) != 0) {
            buffer.append("REPLACED|");
        }
        if ((delta.getKind() & IResourceDelta.SYNC) != 0) {
            buffer.append("SYNC|");
        }
        if ((delta.getKind() & IResourceDelta.TYPE) != 0) {
            buffer.append("TYPE|");
        }
        return buffer.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean visit(final IResourceDelta delta) throws CoreException {

        LOGGER.debug("resource changed: " + getTypeString(delta));
        if ((delta.getKind() & IResourceDelta.ADDED) != 0) {
            LOGGER.debug("refreshing: " + delta.getResource());
        }
        return true;
    }
    }

}

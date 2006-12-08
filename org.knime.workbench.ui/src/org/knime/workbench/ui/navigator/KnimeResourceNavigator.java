/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.navigator.ResourceNavigator;
import org.knime.core.node.NodeLogger;

/**
 * This class is a filtered view on a knime project which hides utitility files
 * from the tree. Such files include the data files, pmml files and files being
 * used to save the internals of a node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeResourceNavigator extends ResourceNavigator implements
        IResourceChangeListener {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(KnimeResourceNavigator.class);

    private OpenFileAction m_openFileAction;

    /**
     * Creates a new <code>KnimeResourceNavigator</code> with an final
     * <code>OpenFileAction</code> to open workflows when open a knime
     * project.
     */

    public KnimeResourceNavigator() {
        super();

        LOGGER.debug("Knime resource navigator created");
        // register listener to check wether prjects have been added
        // or renamed
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

    }

    /**
     * Adds the filters to the viewer.
     * 
     * @param viewer the viewer
     * @since 2.0
     */
    @Override
    protected void initFilters(final TreeViewer viewer) {

        super.initFilters(viewer);
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
                new KnimeResourceLableProvider(), getPlugin().getWorkbench()
                        .getDecoratorManager().getLabelDecorator()));
    }

    /**
     * Handles an open event from the viewer. Opens an editor on the selected
     * knime project.
     * 
     * @param event the open event
     */
    @Override
    protected void handleOpen(final OpenEvent event) {

        IStructuredSelection selection =
                (IStructuredSelection)event.getSelection();

        Iterator<Object> elements = selection.iterator();
        while (elements.hasNext()) {
            Object element = elements.next();
            if (element instanceof IProject) {

                // get the workflow file of the project
                // must be "workflow.knime"
                IProject project = (IProject)element;

                IFile workflowFile = project.getFile("workflow.knime");

                if (workflowFile.exists()) {

                    StructuredSelection selection2 =
                            new StructuredSelection(workflowFile);
                    if (m_openFileAction == null) {
                        m_openFileAction =
                                new OpenFileAction(this.getSite().getPage());
                    }
                    m_openFileAction.selectionChanged(selection2);
                    m_openFileAction.run();
                }
            }
        }
    }

    /**
     * Overrides the <code>createPartControl</code> represents a workaround to
     * remove the + sings from the projects.
     * 
     * @see org.eclipse.ui.IWorkbenchPart#
     *      createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);

        // this forces the filter to run
        // => the viewer recognizes that no children are left and removes the
        // + signs
        getViewer().expandAll();
    }

    /**
     * we need to listen for resource changes to get informed if projects have
     * been changed or added. This is to repeatedly remove the + signs
     * 
     * @see org.eclipse.core.resources.IResourceChangeListener
     *      #resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        try {
            if (event == null || event.getDelta() == null) {
                return;
            }
            event.getDelta().accept(new NavigatorResourceDeltaVisitor());
        } catch (CoreException e) {
            // should never happen, I think...
            e.printStackTrace();
        }
    }

    /**
     * Visitor, checks wheter the projects have been changed in some way.
     * 
     * @author Christoph Sieb, University of Konstanz
     */
    private class NavigatorResourceDeltaVisitor implements
            IResourceDeltaVisitor {
        public String getTypeString(final IResourceDelta delta) {
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
         * Not properly working yet. Seems that another listener is refreshing
         * after this visitor refreshed and expanded, thus the + sign appears.
         * 
         * @see org.eclipse.core.resources.IResourceDeltaVisitor
         *      #visit(org.eclipse.core.resources.IResourceDelta)
         */
        public boolean visit(final IResourceDelta delta) throws CoreException {

            // LOGGER.debug("Path: " + delta.getResource().getName()
            // + " Deltat type: " + getTypeString(delta));

            // If delta resource is a project, refresh and expand the
            // navigation tree
            if (true || delta.getResource().getType() == IResource.PROJECT) {

                // this forces the filter to run
                // => the viewer recognizes that no children are left and
                // removes the
                // + signs

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {

                        try {
                            getViewer().refresh();
                            getViewer().expandToLevel(2);
                        } catch (RuntimeException re) {
                            // do nothing
                            // just ensure that nothing goes wrong
                        }

                    }
                });

                return false;
            }

            return true;
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
                }

            }
        }

        // remove the default import export actions to store the own one
        // that invokes the knime export wizard directly
        menu.remove("import");
        menu.insertBefore("export", new ImportKnimeWorkflowAction(Workbench
                .getInstance()));

        menu.remove("export");
        menu.insertAfter(ImportKnimeWorkflowAction.ID,
                new ExportKnimeWorkflowAction(Workbench.getInstance()));

        // TODO: this is hardcoded the copy item. should be retreived more
        // dynamically
        String id = menu.getItems()[2].getId();

        // add an open action which is not listed as the project is normally
        // not openable.
        menu.insertBefore(id, new Separator());
        menu.insertBefore(id, new OpenKnimeProjectAction(this));
        menu.insertBefore(id, new Separator());

        // another bad workaround to replace the first "New" menu manager
        // with the "Create New Workflow" action
        // store all items, remove all, add the action and then
        // add all but the first one
        IContributionItem[] items = menu.getItems();
        for (IContributionItem item : items) {
            menu.remove(item);
        }
        menu.add(new NewKnimeWorkflowAction(Workbench.getInstance()));
        for (int i = 1; i < items.length; i++) {
            menu.add(items[i]);
        }

    }
}

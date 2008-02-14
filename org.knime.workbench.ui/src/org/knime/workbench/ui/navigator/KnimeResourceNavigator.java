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
 *   Jun 7, 2006 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.CloseUnrelatedProjectsAction;
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
                } else if (aItem.getAction() instanceof 
                        CloseUnrelatedProjectsAction) {
                    menu.remove(aItem);
                }

            }
        }

        // remove the default import export actions to store the own one
        // that invokes the knime export wizard directly
        menu.remove("import");
        menu.insertBefore("export", new ImportKnimeWorkflowAction(Workbench
                .getInstance().getActiveWorkbenchWindow()));

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
        menu.add(new NewKnimeWorkflowAction(Workbench.getInstance()
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
    protected void initContentProvider(TreeViewer viewer) {
        viewer.setContentProvider(new KnimeContentProvider());
    }

    /**
     * {@inheritDoc}
     */
    public void resourceChanged(IResourceChangeEvent event) {
        // do nothing
    }
}

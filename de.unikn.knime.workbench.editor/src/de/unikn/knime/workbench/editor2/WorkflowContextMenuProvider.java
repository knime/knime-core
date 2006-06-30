/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   25.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2;

import java.util.List;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.meta.MetaNodeModel;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.workbench.editor2.actions.AbstractNodeAction;
import de.unikn.knime.workbench.editor2.actions.EditMetaWorkflowAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import de.unikn.knime.workbench.editor2.actions.OpenDialogAction;
import de.unikn.knime.workbench.editor2.actions.OpenPortViewAction;
import de.unikn.knime.workbench.editor2.actions.OpenViewAction;
import de.unikn.knime.workbench.editor2.actions.OpenViewEmbeddedAction;
import de.unikn.knime.workbench.editor2.actions.ResetAction;
import de.unikn.knime.workbench.editor2.actions.SetNameAndDescriptionAction;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;
import de.unikn.knime.workbench.ui.KNIMEUIPlugin;
import de.unikn.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Provider for the Workflow editor's context menus.
 * 
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowContextMenuProvider extends ContextMenuProvider {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowContextMenuProvider.class);

    private ActionRegistry m_actionRegistry;

    private GraphicalViewer m_viewer;

    /**
     * Creates a new context menu provider, that is, registers some actions from
     * the action registry.
     * 
     * @param actionRegistry The action registry of the editor
     * @param viewer The graphical viewer
     */
    public WorkflowContextMenuProvider(final ActionRegistry actionRegistry,
            final GraphicalViewer viewer) {
        super(viewer);
        m_viewer = viewer;

        assert actionRegistry != null : "WorkflowContextMenuProvider "
                + "needs an action registry !";

        m_actionRegistry = actionRegistry;

    }

    /**
     * @see org.eclipse.gef.ContextMenuProvider
     *      #buildContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    public void buildContextMenu(final IMenuManager manager) {
        LOGGER.debug("Building up context menu...");
        manager.add(new Separator(IWorkbenchActionConstants.GROUP_APP));
        GEFActionConstants.addStandardActionGroups(manager);

        IAction action;

        action = m_actionRegistry.getAction("cut");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        action = m_actionRegistry.getAction("copy");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        action = m_actionRegistry.getAction("paste");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        action = m_actionRegistry.getAction("undo");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        action = m_actionRegistry.getAction("redo");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        action = m_actionRegistry.getAction("delete");
        manager.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
        ((UpdateAction)action).update();

        // Add (some) available actions from the regisry to the context menu
        // manager

        // openDialog
        action = m_actionRegistry.getAction(OpenDialogAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // execute
        action = m_actionRegistry.getAction(ExecuteAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // execute and open first view
        action = m_actionRegistry.getAction(ExecuteAndOpenViewAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // reset
        action = m_actionRegistry.getAction(ResetAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // set name and description
        action = m_actionRegistry.getAction(SetNameAndDescriptionAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();

        // depending on the current selection: add the actions for the port
        // views and the node views
        // also check wether this node part is a meta-node
        // if so offer the "edit meta-node" option
        // all these feature are only offered if exactly 1 part is selected
        List parts = m_viewer.getSelectedEditParts();
        // by now, we only support one part...
        if (parts.size() == 1) {
            EditPart p = (EditPart)parts.get(0);
            if (p instanceof NodeContainerEditPart) {

                NodeContainer container = null;
                container = (NodeContainer)((NodeContainerEditPart)p)
                        .getModel();

                // add for node views option if applicable
                LOGGER.debug("adding open node-view action(s) "
                        + "to context menu...");
                int numNodeViews = container.getNumViews();
                // FG: if global setting is true, then open embedded views.
                //
                boolean openEmbedded = KNIMEUIPlugin.getDefault()
                        .getPreferenceStore().getString(
                                PreferenceConstants.P_CHOICE_VIEWMODE).equals(
                                PreferenceConstants.P_CHOICE_VIEWMODE_VIEW);
                for (int i = 0; i < numNodeViews; i++) {
                    if (openEmbedded) {
                        action = new OpenViewEmbeddedAction(container, i);
                    } else {
                        action = new OpenViewAction(container, i);
                    }
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP,
                            action);
                }

                // add meta node option if applicable
                if (MetaNodeModel.class.isAssignableFrom(container
                        .getModelClass())) {
                    LOGGER.debug("adding 'edit meta-node' option "
                            + "to context menu...");
                    action = new EditMetaWorkflowAction(container);
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP,
                            action);
                }

                // add port views
                LOGGER.debug("adding open port-view action(s) "
                        + "to context menu...");
                manager.add(new Separator("outPortViews"));

                int numOutPorts = container.getNrOutPorts();
                for (int i = 0; i < numOutPorts; i++) {
                    action = new OpenPortViewAction(container, i);
                    manager.appendToGroup("outPortViews", action);
                }

            }
        }

        manager.updateAll(true);

    }
}

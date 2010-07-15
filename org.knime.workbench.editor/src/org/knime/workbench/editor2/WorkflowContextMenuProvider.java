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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.OpenPortViewAction;
import org.knime.workbench.editor2.actions.OpenSubworkflowEditorAction;
import org.knime.workbench.editor2.actions.OpenViewAction;
import org.knime.workbench.editor2.actions.OpenWorkflowPortViewAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.SetNameAndDescriptionAction;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * Provider for the Workflow editor's context menus.
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowContextMenuProvider extends ContextMenuProvider {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(WorkflowContextMenuProvider.class);

    private final ActionRegistry m_actionRegistry;

    private final GraphicalViewer m_viewer;

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
     * {@inheritDoc}
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

        action = m_actionRegistry.getAction(PasteActionContextMenu.ID);
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
        // cancel execution
        action = m_actionRegistry.getAction(CancelAction.ID);
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
            LOGGER.debug("selected edit part: " + p);
            if (p instanceof WorkflowInPortBarEditPart) {
                WorkflowInPortBarEditPart root = (WorkflowInPortBarEditPart)p;
                manager.add(new Separator("outPortViews"));
                for (Object o : p.getChildren()) {
                    EditPart child = (EditPart)o;
                    if (child instanceof WorkflowInPortEditPart
                            && ((WorkflowInPortEditPart)child).isSelected()) {
                        action = new OpenWorkflowPortViewAction(
                                ((WorkflowPortBar)root.getModel())
                                    .getWorkflowManager(),
                                ((WorkflowInPortEditPart)child).getIndex());
                        manager.appendToGroup("outPortViews", action);
                        ((WorkflowInPortEditPart)child).setSelected(false);
                    }
                }
            }
            /*
            if (p instanceof SubworkflowEditPart) {
                // meta node -> add to template repository action
                SubworkflowEditPart metaNode = (SubworkflowEditPart)p;
                action = new CreateMetaNodeTemplateAction(
                        metaNode.getWorkflowManager(),
                        metaNode.getNodeContainer());
                manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP,
                        action);
            }
            */
            if (p instanceof NodeContainerEditPart) {

                NodeContainer container = null;
                container =
                        (NodeContainer)((NodeContainerEditPart)p).getModel();

                // add for node views option if applicable
                LOGGER.debug("adding open node-view action(s) "
                        + "to context menu...");
                int numNodeViews = container.getNrViews();
                for (int i = 0; i < numNodeViews; i++) {
                    action = new OpenViewAction(container, i);
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP,
                            action);
                }

                if (container instanceof WorkflowManager) {
                    action = new OpenSubworkflowEditorAction(
                            (NodeContainerEditPart)p);
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP,
                            action);
                }

                // add port views
                LOGGER.debug("adding open port-view action(s) "
                        + "to context menu...");
                manager.add(new Separator("outPortViews"));

                int numOutPorts = container.getNrOutPorts();
                for (int i = 0; i < numOutPorts; i++) {
                    if (i == 0 && !(container instanceof WorkflowManager)) {
                        // skip the implicit flow var ports on "normal" nodes
                        continue;
                    }
                    action = new OpenPortViewAction(container, i);
                    manager.appendToGroup("outPortViews", action);
                }

            }
        }

        manager.updateAll(true);
    }
}

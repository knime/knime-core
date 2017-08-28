/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.knime.core.def.node.workflow.INodeContainer;
import org.knime.core.def.node.workflow.ISingleNodeContainer;
import org.knime.core.def.node.workflow.ISubNodeContainer;
import org.knime.core.def.node.workflow.IWorkflowManager;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.util.CastUtil;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.AddAnnotationAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.ChangeMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.ChangeSubNodeLinkAction;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.CollapseMetaNodeAction;
import org.knime.workbench.editor2.actions.ConvertMetaNodeToSubNodeAction;
import org.knime.workbench.editor2.actions.ConvertSubNodeToMetaNodeAction;
import org.knime.workbench.editor2.actions.DisconnectMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.DisconnectSubNodeLinkAction;
import org.knime.workbench.editor2.actions.EncapsulateSubNodeAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.ExpandMetaNodeAction;
import org.knime.workbench.editor2.actions.ExpandSubNodeAction;
import org.knime.workbench.editor2.actions.LockMetaNodeAction;
import org.knime.workbench.editor2.actions.LockSubNodeAction;
import org.knime.workbench.editor2.actions.MetaNodeReconfigureAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.OpenInteractiveViewAction;
import org.knime.workbench.editor2.actions.OpenInteractiveWebViewAction;
import org.knime.workbench.editor2.actions.OpenPortViewAction;
import org.knime.workbench.editor2.actions.OpenSubNodeEditorAction;
import org.knime.workbench.editor2.actions.OpenSubnodeWebViewAction;
import org.knime.workbench.editor2.actions.OpenSubworkflowEditorAction;
import org.knime.workbench.editor2.actions.OpenViewAction;
import org.knime.workbench.editor2.actions.OpenWorkflowPortViewAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.PauseLoopExecutionAction;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.ResumeLoopAction;
import org.knime.workbench.editor2.actions.RevealMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.RevealSubNodeTemplateAction;
import org.knime.workbench.editor2.actions.SaveAsMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.SaveAsSubNodeTemplateAction;
import org.knime.workbench.editor2.actions.SelectLoopAction;
import org.knime.workbench.editor2.actions.SetNodeDescriptionAction;
import org.knime.workbench.editor2.actions.StepLoopAction;
import org.knime.workbench.editor2.actions.SubNodeReconfigureAction;
import org.knime.workbench.editor2.actions.ToggleFlowVarPortsAction;
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

    private static final String GROUP_METANODE = "group.knime.metanode";
    private static final String GROUP_METANODE_LINKS = "group.knime.metanode.links";
    private static final String GROUP_SUBNODE = "group.knime.subnode";
    private static final String GROUP_SUBNODE_LINKS = "group.knime.subnode.links";

    private final ActionRegistry m_actionRegistry;

    private final GraphicalViewer m_viewer;

    // it's final, but the content changes each time the menu opens
    private final Point m_lastLocation = new Point(0, 0);

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
        m_viewer.getControl().addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(final MenuDetectEvent e) {
                Point pt = m_viewer.getControl().toControl(e.x, e.y);
                m_lastLocation.x = pt.x;
                m_lastLocation.y = pt.y;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildContextMenu(final IMenuManager manager) {

        final String FLOW_VAR_PORT_GRP = "Flow Variable Ports";

        // add the groups (grouped by separators) in their order first
        manager.add(new Separator(IWorkbenchActionConstants.GROUP_APP));
        manager.add(new Separator(FLOW_VAR_PORT_GRP));
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

        // Add (some) available actions from the registry to the context menu
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
        // show some menu items on LoopEndNodes only
        List parts = m_viewer.getSelectedEditParts();
        if (parts.size() == 1) {
            EditPart p = (EditPart)parts.get(0);
            if (p instanceof NodeContainerEditPart) {
                INodeContainer container =
                        (INodeContainer)((NodeContainerEditPart)p).getModel();
                if (container instanceof SingleNodeContainer) {
                    ISingleNodeContainer snc = (ISingleNodeContainer)container;
                    CastUtil.castOptional(snc, SingleNodeContainer.class).ifPresent(sncImpl -> {
                        if (sncImpl.isModelCompatibleTo(LoopEndNode.class)) {
                            // pause loop execution
                            IAction loopAction;
                            loopAction = m_actionRegistry.getAction(PauseLoopExecutionAction.ID);
                            manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, loopAction);
                            ((AbstractNodeAction)loopAction).update();
                            // step loop execution
                            loopAction = m_actionRegistry.getAction(StepLoopAction.ID);
                            manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, loopAction);
                            ((AbstractNodeAction)loopAction).update();
                            // resume loop execution
                            loopAction = m_actionRegistry.getAction(ResumeLoopAction.ID);
                            manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, loopAction);
                            ((AbstractNodeAction)loopAction).update();
                        }
                    });
                }
            }
        }
        // reset
        action = m_actionRegistry.getAction(ResetAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // set name and description
        action = m_actionRegistry.getAction(SetNodeDescriptionAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // add workflow annotation
        action = m_actionRegistry.getAction(AddAnnotationAction.ID);
        AddAnnotationAction aaa = (AddAnnotationAction)action;
        aaa.setLocation(m_lastLocation.x, m_lastLocation.y);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();

        // collapse metanodes
        action = m_actionRegistry.getAction(CollapseMetaNodeAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        action = m_actionRegistry.getAction(EncapsulateSubNodeAction.ID);
        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
        ((AbstractNodeAction)action).update();
        // insert "select loop" if loop nodes are selected
        boolean addSelectLoop = true;
        for (Object p : parts) {
            if (!(p instanceof NodeContainerEditPart)) {
                addSelectLoop = false;
                break;
            }
            INodeContainer nc = ((NodeContainerEditPart)p).getNodeContainer();
            if (!(nc instanceof ISingleNodeContainer)) {
                addSelectLoop = false;
                break;
            }
            if (!((ISingleNodeContainer)nc).isMemberOfScope()) {
                addSelectLoop = false;
                break;
            }
        }
        if (addSelectLoop) {
            action = m_actionRegistry.getAction(SelectLoopAction.ID);
            manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
            ((AbstractNodeAction)action).update();
        }

        IMenuManager metanodeMenuMgr = null;
        IMenuManager subnodeMenuMgr = null;

        // depending on the current selection: add the actions for the port
        // views and the node views
        // also check whether this node part is a meta-node
        // if so offer the "edit meta-node" option
        // all these feature are only offered if exactly 1 part is selected
        parts = m_viewer.getSelectedEditParts();
        // by now, we only support one part...
        if (parts.size() == 1) {
            EditPart p = (EditPart)parts.get(0);
            if (p instanceof WorkflowInPortBarEditPart) {
                WorkflowInPortBarEditPart root = (WorkflowInPortBarEditPart)p;
                manager.add(new Separator("outPortViews"));
                for (Object o : p.getChildren()) {
                    EditPart child = (EditPart)o;
                    if (child instanceof WorkflowInPortEditPart
                            && ((WorkflowInPortEditPart)child).isSelected()) {
                        final WorkflowManager wm = CastUtil.castWFM(((WorkflowPortBar)root.getModel()).getWorkflowManager());
                        action = new OpenWorkflowPortViewAction(wm,
                            ((WorkflowInPortEditPart)child).getIndex(), wm.getNrInPorts());
                        manager.appendToGroup("outPortViews", action);
                        ((WorkflowInPortEditPart)child).setSelected(false);
                    }
                }
            }
            if (p instanceof NodeContainerEditPart) {

                INodeContainer container = null;
                container = (INodeContainer)((NodeContainerEditPart)p).getModel();

                if (!(container instanceof IWorkflowManager)) {
                    action = m_actionRegistry.getAction(ToggleFlowVarPortsAction.ID);
                    manager.appendToGroup(FLOW_VAR_PORT_GRP, action);
                    ((AbstractNodeAction)action).update();
                }

                // add for node views option if applicable
                int numNodeViews = container.getNrViews();
                for (int i = 0; i < numNodeViews; i++) {
                    action = new OpenViewAction(CastUtil.cast(container, NodeContainer.class), i);
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
                }

                // add interactive view options
                if (container.hasInteractiveView() || container.hasInteractiveWebView()) {
                    action = new OpenInteractiveViewAction(CastUtil.cast(container, NodeContainer.class));
                    manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
                } else {
                    // in the 'else' block? Yes:
                    // it's only one or the other -- do not support nodes that have
                    // both (standard swing) interactive and web interactive views
                    //TODO for subnodes move to submenu?
                    if (container instanceof NodeContainer) {
                        InteractiveWebViewsResult interactiveWebViewsResult =
                            CastUtil.cast(container, NodeContainer.class).getInteractiveWebViews();
                        for (int i = 0; i < interactiveWebViewsResult.size(); i++) {
                            action = new OpenInteractiveWebViewAction(CastUtil.cast(container, NodeContainer.class),
                                interactiveWebViewsResult.get(i));
                            manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
                        }
                    }
                }

                if (container instanceof IWorkflowManager) {
                    metanodeMenuMgr = getMetaNodeMenuManager(metanodeMenuMgr, manager);

                    // OPEN META NODE
                    action = new OpenSubworkflowEditorAction((NodeContainerEditPart)p);
                    metanodeMenuMgr.appendToGroup(GROUP_METANODE, action);

                    // EXPAND META NODE
                    action = m_actionRegistry.getAction(ExpandMetaNodeAction.ID);
                    metanodeMenuMgr.appendToGroup(GROUP_METANODE, action);
                    ((AbstractNodeAction)action).update();

                    // RECONFIGURE META NODE
                    if (parts.size() == 1) {
                        action = m_actionRegistry.getAction(MetaNodeReconfigureAction.ID);
                        metanodeMenuMgr.appendToGroup(GROUP_METANODE, action);
                        ((AbstractNodeAction)action).update();
                    }

                    // WRAP
                    action = m_actionRegistry.getAction(ConvertMetaNodeToSubNodeAction.ID);
                    metanodeMenuMgr.appendToGroup(GROUP_METANODE, action);
                    ((AbstractNodeAction)action).update();

                }

                // SUBNODE
                if (container instanceof ISubNodeContainer) {

                    subnodeMenuMgr = getSubNodeMenuManager(subnodeMenuMgr, manager);

                    // OPEN SUBNODE
                    action = new OpenSubNodeEditorAction((NodeContainerEditPart)p);
                    subnodeMenuMgr.appendToGroup(GROUP_SUBNODE, action);

                    // EXPAND SUBNODE
                    action = m_actionRegistry.getAction(ExpandSubNodeAction.ID);
                    subnodeMenuMgr.appendToGroup(GROUP_SUBNODE, action);
                    ((AbstractNodeAction)action).update();

                    // RECONFIGURE SUBNODE
                    action = m_actionRegistry.getAction(SubNodeReconfigureAction.ID);
                    subnodeMenuMgr.appendToGroup(GROUP_SUBNODE, action);
                    ((AbstractNodeAction)action).update();

                    // UNWRAP
                    action = m_actionRegistry.getAction(ConvertSubNodeToMetaNodeAction.ID);
                    subnodeMenuMgr.appendToGroup(GROUP_SUBNODE, action);
                    ((AbstractNodeAction)action).update();

                    if (container instanceof SubNodeContainer) {
                        action = new OpenSubnodeWebViewAction(CastUtil.cast(container, SubNodeContainer.class));
                        manager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, action);
                    }
                }

                // add port views
                manager.add(new Separator("outPortViews"));

                int numOutPorts = container.getNrOutPorts();
                for (int i = 0; i < numOutPorts; i++) {
                    if (i == 0 && !(container instanceof IWorkflowManager)) {
                        // skip the implicit flow var ports on "normal" nodes
                        continue;
                    }
                    if (container instanceof NodeContainer) {
                        action = new OpenPortViewAction(CastUtil.cast(container, NodeContainer.class), i, numOutPorts);
                        manager.appendToGroup("outPortViews", action);
                    }
                }

            }
        }

        boolean addMetaNodeActions = false;
        boolean addSubNodeActions = false;
        for (Object p : parts) {
            if (p instanceof NodeContainerEditPart) {
                INodeContainer model = ((NodeContainerEditPart)p).getNodeContainer();
                if (model instanceof IWorkflowManager) {
                    addMetaNodeActions = true;
                } else if (model instanceof ISubNodeContainer) {
                    addSubNodeActions = true;
                }
            }
        }

        if (addMetaNodeActions) {
            metanodeMenuMgr = getMetaNodeMenuManager(metanodeMenuMgr, manager);

            // SAVE AS TEMPLATE
            action = m_actionRegistry.getAction(SaveAsMetaNodeTemplateAction.ID);
            metanodeMenuMgr.appendToGroup(GROUP_METANODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // CHECK UPDATE
            action = m_actionRegistry.getAction(CheckUpdateMetaNodeLinkAction.ID);
            metanodeMenuMgr.appendToGroup(GROUP_METANODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // DISCONNECT
            action = m_actionRegistry.getAction(DisconnectMetaNodeLinkAction.ID);
            metanodeMenuMgr.appendToGroup(GROUP_METANODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // LINK TYPE
            action = m_actionRegistry.getAction(ChangeMetaNodeLinkAction.ID);
            metanodeMenuMgr.appendToGroup(GROUP_METANODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // REVEAL TEMPLATE
            action = m_actionRegistry.getAction(RevealMetaNodeTemplateAction.ID);
            metanodeMenuMgr.appendToGroup(GROUP_METANODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // LOCK
            if (Boolean.getBoolean(KNIMEConstants.PROPERTY_SHOW_METANODE_LOCK_ACTION)) {
                action = m_actionRegistry.getAction(LockMetaNodeAction.ID);
                metanodeMenuMgr.appendToGroup(GROUP_METANODE, action);
                ((AbstractNodeAction)action).update();
            }

        }

        if (addSubNodeActions) {

            subnodeMenuMgr = getSubNodeMenuManager(subnodeMenuMgr, manager);

            // SAVE AS TEMPLATE (SUBNODE)
            action = m_actionRegistry.getAction(SaveAsSubNodeTemplateAction.ID);
            subnodeMenuMgr.appendToGroup(GROUP_SUBNODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // CHECK UPDATE (SUBNODE)
            action = m_actionRegistry.getAction(CheckUpdateMetaNodeLinkAction.ID);
            subnodeMenuMgr.appendToGroup(GROUP_SUBNODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // DISCONNECT LINK (SUBNODE)
            action = m_actionRegistry.getAction(DisconnectSubNodeLinkAction.ID);
            subnodeMenuMgr.appendToGroup(GROUP_SUBNODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // CHANGE LINK (SUBNODE)
            action = m_actionRegistry.getAction(ChangeSubNodeLinkAction.ID);
            subnodeMenuMgr.appendToGroup(GROUP_SUBNODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // REVEAL TEMPLATE (SUBNODE)
            action = m_actionRegistry.getAction(RevealSubNodeTemplateAction.ID);
            subnodeMenuMgr.appendToGroup(GROUP_SUBNODE_LINKS, action);
            ((AbstractNodeAction)action).update();

            // LOCK SUBNODE
            if (Boolean.getBoolean(KNIMEConstants.PROPERTY_SHOW_METANODE_LOCK_ACTION)) {
                action = m_actionRegistry.getAction(LockSubNodeAction.ID);
                subnodeMenuMgr.appendToGroup(GROUP_SUBNODE, action);
                ((AbstractNodeAction)action).update();
            }
        }

        manager.updateAll(true);
    }

    private static IMenuManager getMetaNodeMenuManager(final IMenuManager metaNodeManagerOrNull,
        final IMenuManager parentMenuManager) {
        if (metaNodeManagerOrNull == null) {
            MenuManager m = new MenuManager("Metanode",
                ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta/meta_menu.png"),
                null);
            m.add(new Separator(GROUP_METANODE));
            m.add(new Separator(GROUP_METANODE_LINKS));
            parentMenuManager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, m);
            return m;
        }
        return metaNodeManagerOrNull;
    }

    private static IMenuManager getSubNodeMenuManager(final IMenuManager subNodeManagerOrNull,
        final IMenuManager parentMenuManager) {
        if (subNodeManagerOrNull == null) {
            MenuManager m = new MenuManager("Wrapped Metanode",
                ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta/meta_menu.png"),
                null);
            m.add(new Separator(GROUP_SUBNODE));
            m.add(new Separator(GROUP_SUBNODE_LINKS));
            parentMenuManager.appendToGroup(IWorkbenchActionConstants.GROUP_APP, m);
            return m;
        }
        return subNodeManagerOrNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        m_actionRegistry.dispose();
    }

}

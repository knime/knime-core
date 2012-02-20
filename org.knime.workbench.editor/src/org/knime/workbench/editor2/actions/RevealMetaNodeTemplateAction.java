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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.02.2012 (Dominik Morent): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Action to reveal the template of a linked meta node.
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class RevealMetaNodeTemplateAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RevealMetaNodeTemplateAction.class);

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_reveal_template";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public RevealMetaNodeTemplateAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Reveal Meta Node Template";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Reveals the meta node template this meta node is linked to.";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/meta/metanode_link_reveal.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] nodes =
            getSelectedParts(NodeContainerEditPart.class);
        if (nodes == null) {
            return false;
        }
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (model instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)model;
                if (wm.getTemplateInformation().getRole().equals(Role.Link)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        List<NodeID> candidateList = new ArrayList<NodeID>();
        List<AbstractExplorerFileStore> templates
                = new ArrayList<AbstractExplorerFileStore>();
        ExplorerFileSystem efs = new ExplorerFileSystem();
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (model instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)model;
                MetaNodeTemplateInformation i = wm.getTemplateInformation();
                if (Role.Link.equals(i.getRole())) {
                    candidateList.add(wm.getID());
                    AbstractExplorerFileStore template
                            = efs.getStore(i.getSourceURI());
                    if (template != null) {
                        templates.add(template);
                    }
                }
            }
        }
        List<Object> treeObjects
                = ContentDelegator.getTreeObjectList(templates);
        if (treeObjects != null && treeObjects.size() > 0) {
            IViewReference[] views = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getViewReferences();

            for (IViewReference view : views) {
                if (ExplorerView.ID.equals(view.getId())) {
                    ExplorerView explorerView
                            = (ExplorerView)view.getView(true);
                   explorerView.getViewer().setSelection(
                           new StructuredSelection(treeObjects), true);
                }
            }
        }

    }

}

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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.CollapseMetaNodeCommand;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to collapse selected set of nodes into metanode.
 *
 * @author M. Berthold, University of Konstanz
 */
public class CollapseMetaNodeAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(CollapseMetaNodeAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.collapsemetanode";

    /**
     * @param editor The workflow editor
     */
    public CollapseMetaNodeAction(final WorkflowEditor editor) {
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
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Collapse into Meta Node";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/meta/metanode_collapse.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Collapse Nodes into new Meta Node";
    }

    /**
     * @return <code>true</code>, if more than one node is selected.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        if (parts.length < 1) {
            return false;
        }
        return !getManager().isWriteProtected();
    }

    /**
     * collapse nodes and annotations into metanode.
     *
     * {@inheritDoc}
     */
    @Override
    public void runInSWT() {
        NodeContainerEditPart[] nodeParts
                            = getSelectedParts(NodeContainerEditPart.class);
        AnnotationEditPart[] annoParts
                            = getSelectedParts(AnnotationEditPart.class);

        LOGGER.debug("Creating 'Collapse MetaNode' job for "
                + nodeParts.length + " node(s)...");

        WorkflowManager manager = getManager();
        NodeID[] nodeIds = new NodeID[nodeParts.length];
        for (int i = 0; i < nodeParts.length; i++) {
            nodeIds[i] = nodeParts[i].getNodeContainer().getID();
        }
        WorkflowAnnotation[] annos =
            AnnotationEditPart.extractWorkflowAnnotations(annoParts);
        try {
            // before testing anything, let's see if we should reset
            // the selected nodes:
            Vector<NodeID> resetableIDs = new Vector<NodeID>();
            for (NodeID id : nodeIds) {
                if (manager.canResetNode(id)) {
                    resetableIDs.add(id);
                }
            }
            if (resetableIDs.size() > 0) {
                // found some: ask if we can reset, otherwise bail
                MessageBox mb = new MessageBox(Display.getCurrent().getActiveShell(),
                        SWT.OK | SWT.CANCEL);
                mb.setMessage("Executed Nodes will be reset - are you sure?");
                mb.setText("Reset Executed Nodes");
                int dialogreturn = mb.open();
                if (dialogreturn == SWT.CANCEL) {
                    return;
                }
            } else {
                // if there are no resetable nodes we can check if
                // we can collapse - otherwise we need to first reset
                // those nodes (which we don't want to do before we
                // have not gathered all info - and allowed the user
                // to cancel the operation!)
                String res = manager.canCollapseNodesIntoMetaNode(nodeIds);
                if (res != null) {
                    throw new IllegalArgumentException(res);
                }
            }
            // let the user enter a name
            String name = "Metanode";
            InputDialog idia = new InputDialog(Display.getCurrent().getActiveShell(),
                    "Enter Name of Metanode", "Enter name of metanode:", name, null);
            int dialogreturn = idia.open();
            if (dialogreturn == Dialog.CANCEL) {
                return;
            }
            if (dialogreturn == Dialog.OK) {
                if (resetableIDs.size() > 0) {
                    // do quick&dirty reset: just reset them in random order
                    // and skip the ones that were already reset in passing.
                    for (NodeID id : resetableIDs) {
                        if (manager.canResetNode(id)) {
                            manager.resetAndConfigureNode(id);
                        }
                    }
                }
                // check if there is another reason why we can not collapse
                String res = manager.canCollapseNodesIntoMetaNode(nodeIds);
                if (res != null) {
                    throw new IllegalArgumentException(res);
                }
                name = idia.getValue();
                // create a command and push on stack to enable UNDO
                CollapseMetaNodeCommand cmnc =
                    new CollapseMetaNodeCommand(manager, nodeIds, annos, name);
                getCommandStack().execute(cmnc);
            }
        } catch (IllegalArgumentException e) {
            MessageBox mb = new MessageBox(
                    Display.getCurrent().getActiveShell(), SWT.ERROR);
            final String error =
                "Collapsing to meta node failed: " + e.getMessage();
            LOGGER.error(error, e);
            mb.setMessage(error);
            mb.setText("Collapse failed");
            mb.open();
        }
        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException(
                "Not to be called as runInSWT is overwritten.");
    }

}

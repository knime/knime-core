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
 * -------------------------------------------------------------------
 *
 * History
 *   29.06.2012 (Peter Ohl): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ChangeAnnotationBoundsCommand;
import org.knime.workbench.editor2.commands.ChangeNodeBoundsCommand;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.snap.SnapIconToGrid;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * Action to move the selected node(s) up a bit.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class MoveNodeUpAction extends AbstractNodeAction {

    private static final int STEP = 1;

    /** unique ID for this action. * */
    public static final String ID = "knime.action.node.moveup";

    /**
     *
     * @param editor The workflow editor
     */
    public MoveNodeUpAction(final WorkflowEditor editor) {
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
        return "Move node up";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/move.png");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor("icons/move_dis.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Move the selected node(s) up";
    }

    /**
     * @return true if at least one node is selected
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return getSelectedParts(NodeContainerEditPart.class).length > 0;

    }

    /**
     * This starts an execution job for the selected nodes. Note that this is
     * all controlled by the WorkflowManager object of the currently open
     * editor.
     *
     * @see org.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // should be initialized from the pref page
        int offset = STEP;
        CompoundCommand cc = new CompoundCommand();
        NodeContainerEditPart[] selectedNodes = getSelectedParts(NodeContainerEditPart.class);
        AnnotationEditPart[] selectedAnnos = getSelectedParts(AnnotationEditPart.class);
        if (selectedNodes.length < 1) {
            return;
        }

        if (getEditor().getEditorSnapToGrid()) {
            // adjust offset to grid size
            if (selectedNodes.length > 1) {
                // if we have multiple nodes, the step size is always the grid size
                // to maintain the inner distances in the selected chunk
                offset = getEditor().getEditorGridYOffset(offset);
            } else {
                // with one node we move the node onto the grid if it is off
                NodeContainerFigure figure = (NodeContainerFigure)selectedNodes[0].getFigure();
                Point iconOffset = SnapIconToGrid.getGridRefPointOffset(figure);
                Point refLoc = new Point(figure.getBounds().x, figure.getBounds().y);
                refLoc.translate(iconOffset);
                Point gridLoc = getEditor().getPrevGridLocation(refLoc);
                if (gridLoc.y != refLoc.y) {
                    // node is off the grid: place node on next grid line
                    offset = refLoc.y - gridLoc.y;
                } else {
                    // node already on the grid: step grid size
                    offset = getEditor().getEditorGridYOffset(offset);
                }
            }
        }
        // apply the offset to all selected nodes
        for (NodeContainerEditPart ep : selectedNodes) {
            NodeContainer nc = ep.getNodeContainer();
            NodeContainerFigure figure = (NodeContainerFigure)ep.getFigure();
            Rectangle bounds = figure.getBounds().getCopy();
            bounds.y -= offset;
            ChangeNodeBoundsCommand cmd = new ChangeNodeBoundsCommand(nc, figure, bounds);
            cc.add(cmd);
        }
        // apply to all selected annotations
        for (AnnotationEditPart anno : selectedAnnos) {
            Rectangle bounds = anno.getFigure().getBounds().getCopy();
            bounds.y -= offset;
            ChangeAnnotationBoundsCommand cmd = new ChangeAnnotationBoundsCommand(getManager(), anno, bounds);
            cc.add(cmd);
        }
        getCommandStack().execute(cc);
    }
}

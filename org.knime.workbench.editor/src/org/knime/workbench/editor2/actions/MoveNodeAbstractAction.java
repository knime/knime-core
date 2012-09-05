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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.commands.ChangeAnnotationBoundsCommand;
import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.commands.ChangeNodeBoundsCommand;
import org.knime.workbench.editor2.editparts.AbstractWorkflowEditPart;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.snap.SnapIconToGrid;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * Action to move the selected node(s) a bit. Abstract class - derivatives provide the method determining the direction
 * of the move.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public abstract class MoveNodeAbstractAction extends AbstractNodeAction {

    /**
     *
     * @param editor The workflow editor
     */
    public MoveNodeAbstractAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getId();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getText();

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
    public abstract String getToolTipText();

    /**
     * @return true if at least one node is selected
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean calculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return getMoveableSelectedEditParts().size() > 0;
    }

    /**
     * Provide a point containing the move instructions: Non-null coordinates moves the node along that axis - negative
     * coordinates move it towards the origin. E.g. (-1, 0) moves causes items to move to the left; (0, 1) moves down.
     * @return point with move instructions.
     */
    public abstract Point getMoveDirection();

    /**
     *
     * @see org.knime.workbench.editor2.actions.AbstractNodeAction
     *      #runOnNodes(org.knime.workbench.editor2.
     *      editparts.NodeContainerEditPart[])
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        List<EditPart> selParts = getMoveableSelectedEditParts();
        if (selParts.size() < 1) {
            return;
        }
        // should be initialized from the pref page
        Point offset = getMoveDirection(); // (0, 1) moves down, (-1, 0) moves left
        int signX = (int)Math.signum(offset.x);
        int signY = (int)Math.signum(offset.y);
        CompoundCommand cc = new CompoundCommand();

        if (getEditor().getEditorSnapToGrid()) {
            // adjust offset to grid size (note: arguments must be not-negative numbers)
            offset = new Point(signX * getEditor().getEditorGridXOffset(signX * offset.x),
                    signY * getEditor().getEditorGridYOffset(signY * offset.y));
            if (selParts.size() == 1) {
                // with one element we move the element onto the grid if it is off
                Point refLoc = null;
                if (selParts.get(0) instanceof NodeContainerEditPart) {
                    NodeContainerEditPart node = (NodeContainerEditPart)selParts.get(0);
                    NodeContainerFigure figure = (NodeContainerFigure)node.getFigure();
                    Point iconOffset = SnapIconToGrid.getGridRefPointOffset(figure);
                    refLoc = new Point(figure.getBounds().x, figure.getBounds().y);
                    refLoc.translate(iconOffset);
                } else {
                    IFigure fig = ((AbstractWorkflowEditPart)selParts.get(0)).getFigure();
                    refLoc = new Point(fig.getBounds().x, fig.getBounds().y);
                }
                Point gridLoc = new Point(0,0);
                Point prevGridLoc = getEditor().getPrevGridLocation(refLoc);
                Point nextGridLoc = getEditor().getNextGridLocation(refLoc);
                boolean toGrid = false;
                if (signX < 0) {
                    gridLoc.x = prevGridLoc.x;
                    toGrid = (gridLoc.x != refLoc.x);
                }
                if (signX > 0) {
                    gridLoc.x = nextGridLoc.x;
                    toGrid = (gridLoc.x != refLoc.x);
                }
                if (signY < 0) {
                    gridLoc.y = prevGridLoc.y;
                    toGrid = (gridLoc.y != refLoc.y);
                }
                if (signY > 0) {
                    gridLoc.y = nextGridLoc.y;
                    toGrid = (gridLoc.y != refLoc.y);
                }
                if (toGrid) {
                    offset = new Point(Math.abs(gridLoc.x - refLoc.x) * signX, Math.abs(gridLoc.y - refLoc.y) * signY);
                }
            }
        }
        int noNodes = 0;
        // apply the offset to all selected elements
        for (EditPart epart: selParts) {
            // apply to selected nodes
            if (epart instanceof NodeContainerEditPart) {
                NodeContainerEditPart node = (NodeContainerEditPart)epart;
                noNodes++;
                NodeContainer nc = node.getNodeContainer();
                NodeContainerFigure figure = (NodeContainerFigure)node.getFigure();
                Rectangle bounds = figure.getBounds().getCopy();
                bounds.translate(offset);
                ChangeNodeBoundsCommand cmd = new ChangeNodeBoundsCommand(nc, figure, bounds);
                cc.add(cmd);
            }
            // apply to all selected workflow annotations
            if ((epart instanceof AnnotationEditPart) && !(epart instanceof NodeAnnotationEditPart)) {
                AnnotationEditPart anno = (AnnotationEditPart)epart;
                Rectangle bounds = anno.getFigure().getBounds().getCopy();
                bounds.translate(offset);
                ChangeAnnotationBoundsCommand cmd = new ChangeAnnotationBoundsCommand(getManager(), anno, bounds);
                cc.add(cmd);
            }
        }
        if (noNodes > 1) {
            // if multiple nodes are selected/moved we need to move fully contained connections as well
            ConnectionContainerEditPart[] conns =
                    WorkflowSelectionDragEditPartsTracker.getEmbracedConnections(selParts);
            for (ConnectionContainerEditPart conn : conns) {
                ChangeBendPointLocationCommand connCmd =
                    new ChangeBendPointLocationCommand(conn, offset.getCopy(), null);
                cc.add(connCmd);
            }
        }
        getCommandStack().execute(cc);
    }

    private List<EditPart> getMoveableSelectedEditParts() {
        @SuppressWarnings("rawtypes")
        List selectedObjects = getSelectedObjects();
        LinkedList<EditPart> result = new LinkedList<EditPart>();
        for (Object o : selectedObjects) {
            if ((o instanceof AnnotationEditPart) && !(o instanceof NodeAnnotationEditPart)) {
                result.add((AnnotationEditPart)o);
                continue;
            }
            if (o instanceof NodeContainerEditPart) {
                result.add((NodeContainerEditPart)o);
                continue;
            }
        }
        return result;
    }

}

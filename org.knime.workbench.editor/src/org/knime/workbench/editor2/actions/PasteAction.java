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
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand.ShiftCalculator;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * Implements the clipboard paste action to paste nodes and connections from the clipboard into the editor.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteAction extends AbstractClipboardAction {

    /**
     * Constructs a new clipboard paste action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public PasteAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.PASTE.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Paste";
    }

    /**
     * At least one <code>NodeSettings</code> object must be in the clipboard.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return getEditor().getClipboardContent() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        ClipboardObject clipObject = getEditor().getClipboardContent();
        ShiftCalculator shiftCalculator = newShiftCalculator();
        PasteFromWorkflowPersistorCommand pasteCommand =
            new PasteFromWorkflowPersistorCommand(getEditor(), clipObject, shiftCalculator);
        getCommandStack().execute(pasteCommand); // enables undo

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection) is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }

    /**
     * A shift operator that calculates a fixed offset. The sub class {@link PasteActionContextMenu} overrides this
     * method to return a different shift calculator that respects the current mouse pointer location.
     *
     * @return A new shift calculator.
     */
    protected ShiftCalculator newShiftCalculator() {
        return new ShiftCalculator() {
            /** {@inheritDoc} */
            @Override
            public int[] calculateShift(final Iterable<int[]> boundsList, final WorkflowManager manager,
                final ClipboardObject clipObject) {


                GraphicalViewer viewer = getEditor().getViewer();
                ZoomManager zoomManager = (ZoomManager)viewer.getProperty(ZoomManager.class.toString());
                Viewport viewPort = zoomManager.getViewport();
                Rectangle box = getSurroundingRect(boundsList);

                // determine destination location (either right of selection or middle of viewport)
                int destX;
                int destY;
                @SuppressWarnings("unchecked")
                List<EditPart> selectedEditParts = viewer.getSelectedEditParts();
                if (selectedEditParts.isEmpty()) {
                    destX = viewPort.getSize().width / 2;
                    destY = viewPort.getSize().height / 2;
                    // account for viewport scrolling and zoom
                    Point viewPortLocation = viewPort.getViewLocation();
                    destX += viewPortLocation.x;
                    destY += viewPortLocation.y;
                    double zoom = zoomManager.getZoom();
                    destX /= zoom;
                    destY /= zoom;
                    Point dest = new Point(destX, destY);
                    if (WorkflowEditor.isNodeAtAbs(viewer, dest)) {
                        EditPart conflict = viewer.findObjectAt(dest);
                        if (conflict instanceof NodeContainerEditPart) {
                            dest = WorkflowEditor.getLocationRightOf(viewer, (NodeContainerEditPart)conflict);
                        }
                    }
                } else {
                    Rectangle selectionBox = getSurroundingRect(selectedEditParts);
                    // these coordinates are absolute (no need to add zoom/scrolling)
                    destX = selectionBox.x + selectionBox.width;
                    int distance = (int)(0.5 * NodeContainerFigure.WIDTH);
                    destX += getEditor().getEditorGridXOffset(distance);
                    destY = selectionBox.y;
                    if (selectedEditParts.size() == 1) {
                        // make sure there is nothing at the destination
                        Point locationRightOf = WorkflowEditor.getLocationRightOf(viewer, (NodeContainerEditPart)selectedEditParts.get(0));
                        destX = locationRightOf.x;
                        destY = locationRightOf.y;
                    }
                }

                int shiftx = destX - box.x;
                int shifty = destY - box.y;
                if (getEditor().getEditorSnapToGrid()) {
                    shiftx = getEditor().getEditorGridXOffset(shiftx);
                    shifty = getEditor().getEditorGridYOffset(shifty);
                }
                return new int[]{shiftx, shifty};
            }

            private Rectangle getSurroundingRect(final Iterable<int[]> boundsList) {
                int smallestX = Integer.MAX_VALUE;
                int smallestY = Integer.MAX_VALUE;
                int biggestX = Integer.MIN_VALUE;
                int biggestY = Integer.MIN_VALUE;

                for (int[] bounds : boundsList) {
                    int currentX = bounds[0];
                    int currentY = bounds[1];
                    int width = bounds[2];
                    int height = bounds[3];
                    if (currentX < smallestX) {
                        smallestX = currentX;
                    }
                    if (currentY < smallestY) {
                        smallestY = currentY;
                    }
                    if (currentX + width > biggestX) {
                        biggestX = currentX + width;
                    }
                    if (currentY + height > biggestY) {
                        biggestY = currentY + height;
                    }
                }
                return new Rectangle(smallestX, smallestY, biggestX - smallestX, biggestY - smallestY);
            }

            private Rectangle getSurroundingRect(final List<EditPart> elements) {
                ArrayList<int[]> bounds = new ArrayList<int[]>();
                for (EditPart e : elements) {
                    if (e instanceof NodeContainerEditPart) {
                        bounds.add(((NodeContainerEditPart)e).getNodeContainer().getUIInformation().getBounds());
                    }
                    if (e instanceof AnnotationEditPart) {
                        int[] b = new int[4];
                        b[0] = ((AnnotationEditPart)e).getModel().getX();
                        b[1] = ((AnnotationEditPart)e).getModel().getY();
                        b[2] = ((AnnotationEditPart)e).getModel().getWidth();
                        b[3] = ((AnnotationEditPart)e).getModel().getHeight();
                        bounds.add(b);
                    }
                }
                return getSurroundingRect(bounds);
            }
        };
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jun 7, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.editor2.figures.WorkflowAnnotationFigure;
import org.knime.workbench.editor2.figures.WorkflowFigure;

/**
 * The origin of this class is AP-8594 which sought to have the workflow canvas always be larger than the minimal
 * bounding box of all of its elements - specifically, per Michael, that one should always be able to scroll the canvas
 * to a location of entirely blank space save the nearest most node being diametrically opposed but still slightly
 * visible.
 *
 * @author loki der quaeler
 */
public class WorkflowCanvasExpander implements ControlListener, MouseListener, WorkflowListener {
    private static Rectangle calculateMinimumBoundingRectangle(final WorkflowFigure workflowFigure) {
        final List<?> children = workflowFigure.getChildren();
        final int count = children.size();

        if (count == 0) {
            return new Rectangle(0, 0, 0, 0);
        }

        final int[] leftCandidates = new int[count];
        final int[] rightCandidates = new int[count];
        final int[] topCandidates = new int[count];
        final int[] bottomCandidates = new int[count];
        int index = 0;

        for (Object child : children) {
            final IFigure f = (IFigure)child;

            if ((f instanceof NodeContainerFigure) || (f instanceof WorkflowAnnotationFigure)) {
                final Rectangle bounds = f.getBounds();

                leftCandidates[index] = bounds.x;
                rightCandidates[index] = bounds.x + bounds.width;
                topCandidates[index] = bounds.y;
                bottomCandidates[index] = bounds.y + bounds.height;

                index++;
            }
        }

        if (index == 0) {
            return new Rectangle(0, 0, 0, 0);
        }

        if (index < count) {
            // Some of the children were NodeAnnotationFigure instances; load up the null populated indices of the arrays
            // for sorting with values that won't affect the min-max conclusions.
            for (int i = index; i < count; i++) {
                leftCandidates[i] = leftCandidates[i - 1];
                rightCandidates[i] = rightCandidates[i - 1];
                topCandidates[i] = topCandidates[i - 1];
                bottomCandidates[i] = bottomCandidates[i - 1];
            }
        }

        Arrays.sort(leftCandidates);
        Arrays.sort(rightCandidates);
        Arrays.sort(topCandidates);
        Arrays.sort(bottomCandidates);

        final int width = (rightCandidates[count - 1] - leftCandidates[0]);
        final int height = (bottomCandidates[count - 1] - topCandidates[0]);

        return new Rectangle(leftCandidates[0], topCandidates[0], width, height);
    }


    // These will only be consulted from the SWT thread
    private NodeContainerEditPart m_nodeInDrag;
    private TentStakeFigure m_northwestTentStake;
    private TentStakeFigure m_southeastTentStake;

    private final EditPartViewer m_parentViewer;

    /**
     * @param viewer the viewer containing the workflow canvas of which we seek to keep expanded
     */
    @SuppressWarnings("unchecked") // generic casting on the stream usage
    public WorkflowCanvasExpander(final GraphicalViewer viewer) {
        m_parentViewer = viewer;

        final FigureCanvas fc = (FigureCanvas)viewer.getControl();
        fc.addControlListener(this);
        fc.addMouseListener(this);

        final WorkflowFigure wf = ((WorkflowRootEditPart)viewer.getRootEditPart().getContents()).getFigure();
        final List<TentStakeFigure> foundStakes = (List<TentStakeFigure>)wf.getChildren().stream()
            .filter(child -> child instanceof TentStakeFigure).collect(Collectors.toList());

        // We could try to recycle them, make sure we have only 2, warn if we have somewhat more, etc etc - or we
        //      could just blow them all away and put two fresh ones down... the latter.
        for (TentStakeFigure stake : foundStakes) {
            wf.remove(stake);
        }

        m_northwestTentStake = new TentStakeFigure();
        m_southeastTentStake = new TentStakeFigure();
        wf.add(m_northwestTentStake);
        wf.add(m_southeastTentStake);

        placeTentStakes();
    }

    /**
     * This should be called as part of the parent disposal cycle.
     */
    public void dispose() {
        if (m_parentViewer != null) {
            final FigureCanvas fc = (FigureCanvas)m_parentViewer.getControl();

            if (fc != null) {
                fc.removeControlListener(this);
                fc.removeMouseListener(this);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void controlMoved(final ControlEvent ce) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void controlResized(final ControlEvent ce) {
        // Since the viewport size has changed, so should the locations of the tent stakes.
        placeTentStakes();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDoubleClick(final MouseEvent me) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent me) {
        final org.eclipse.draw2d.geometry.Point localized = getLocalLocation(me.display.getCursorLocation());
        final EditPart ep = m_parentViewer.findObjectAt(localized);

        if (ep instanceof NodeContainerEditPart) {
            m_nodeInDrag = (NodeContainerEditPart)ep;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent me) {
        // To be technically correct, we wouldn't bother stretching the canvas unless the move resulted in the node
        //      being a new minimum-encapsulating-bounds extender, but the work to determine that is far exceeded
        //      then by simply recalculating where the tent stakes should be and moving them if necessary.
        if (m_nodeInDrag != null) {
            // We don't need to do it *right* now, let the rest of the mouse chain have its chance
            Display.getDefault().asyncExec(() -> {
                placeTentStakes();
            });

            m_nodeInDrag = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent we) {
        switch (we.getType()) {
            case NODE_REMOVED:
            case NODE_ADDED:
                Display.getDefault().asyncExec(() -> {
                    placeTentStakes();
                });

                break;
            default:
        }
    }


    /**
     * Converts the event mouse location to editor relative coordinates.
     *
     * We do the exact same thing in DragPositionProcessor and likely elsewhere; were it not so incredibly lacking on
     * code, i'd suggest condensing them to a utility class.
     *
     * @param the position (relative to whole display)
     * @return point converted to the editor coordinates
     */
    private Point getLocalLocation(final org.eclipse.swt.graphics.Point p) {
        org.eclipse.swt.graphics.Point swtPoint = m_parentViewer.getControl().toControl(p.x, p.y);

        return new Point(swtPoint.x, swtPoint.y);
    }

    // This should be called on the SWT thread.
    private void placeTentStakes() {
        final WorkflowRootEditPart wrep = (WorkflowRootEditPart)m_parentViewer.getRootEditPart().getContents();
        final WorkflowFigure wf = wrep.getFigure();

        if (wf.getChildren().size () == 2) {
            // There are only stakes at 0,0; don't expand an 'empty' canvas.
            return;
        }

        final FigureCanvas fc = (FigureCanvas)m_parentViewer.getControl();
        final Viewport vp = fc.getViewport();
        final Rectangle viewportBounds = vp.getClientArea();
        final int xDelta = (int)(viewportBounds.width * 0.93);
        final int yDelta = (int)(viewportBounds.height * 0.93);
        final Rectangle mbr = calculateMinimumBoundingRectangle(wf);

        m_northwestTentStake.setLocation(new Point((mbr.x - xDelta), (mbr.y - yDelta)));
        m_southeastTentStake.setLocation(new Point((mbr.x + mbr.width + xDelta), (mbr.y + mbr.height + yDelta)));
    }


    /**
     * This figure is an invisible 1 x 1 figure which represents one of the two stakes we 'stretch the canvas with by
     * continual re-placement (not replacement) in the northwest and southeast corners of the workflow canvas. We do
     * this trivial subclassing just to make our lives easier to find the figures again on workflow opening.
     */
    public static class TentStakeFigure extends Figure {

        /**
         * Simply set the size of our figure to be 1 x 1.
         */
        public TentStakeFigure() {
            setSize(new Dimension(1, 1));
        }

    }
}

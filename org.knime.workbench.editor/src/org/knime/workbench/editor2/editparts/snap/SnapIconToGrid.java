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
 *   13.06.2012 (ohl): created
 */
package org.knime.workbench.editor2.editparts.snap;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * Default grid size is defined in the preference initializer.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class SnapIconToGrid extends SnapToGrid {

    private final GraphicalEditPart m_container;

    /**
     * @param cntr
     */
    public SnapIconToGrid(final GraphicalEditPart cntr) {
        super(cntr);
        m_container = cntr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int snapRectangle(final Request request, final int snapLocations, final PrecisionRectangle rect,
            final PrecisionRectangle result) {
        PrecisionRectangle r = rect;
        if (request instanceof ChangeBoundsRequest) {
            EditPart refPart = getReferencePart(((ChangeBoundsRequest)request).getEditParts(),
                    ((ChangeBoundsRequest)request).getLocation(), ((ChangeBoundsRequest)request).getMoveDelta());
            if (refPart instanceof NodeContainerEditPart) {
                // adjust the rectangle to snap the center of the icon of the node
                NodeContainerEditPart contPart = (NodeContainerEditPart)refPart;
                NodeContainerFigure fig = (NodeContainerFigure)contPart.getFigure();
                Point iconOffset = getIconOffset(fig);
                r = rect.getPreciseCopy();
                r.translate(iconOffset);
            } else if (refPart instanceof NodeAnnotationEditPart) {
                // the rect is the annotation outline - adjust it to snap the center of the corresponding node icon
                NodeAnnotationEditPart annoPart = (NodeAnnotationEditPart)refPart;
                IFigure annoFig = annoPart.getFigure();
                NodeAnnotation anno = (NodeAnnotation)annoPart.getModel();
                NodeContainerEditPart nodePart =
                        (NodeContainerEditPart)m_container.getViewer().getEditPartRegistry()
                                .get(anno.getNodeContainer());
                NodeContainerFigure nodeFig = (NodeContainerFigure)nodePart.getFigure();
                Point iconOffset = getIconOffset(nodeFig);
                int xOff = nodeFig.getBounds().x - annoFig.getBounds().x;
                xOff += iconOffset.x;
                int yOff = iconOffset.y - nodeFig.getBounds().height;
                r = rect.getPreciseCopy();
                r.translate(new Point(xOff, yOff));
            }
        }
        return super.snapRectangle(request, snapLocations, r, result);
    }

    private Point getIconOffset(final NodeContainerFigure nodeFigure) {
        Point iconOffset = getGridRefPointOffset(nodeFigure);
        double zoomFactor = ((ZoomManager)(m_container.getViewer().getProperty(ZoomManager.class.toString()))).getZoom();
        iconOffset = iconOffset.getScaled(zoomFactor);
        return iconOffset;
    }

    /**
     * Returns the part the request started on (the part that was dragged to move all selected nodes)
     * @param parts
     * @param mouseLoc
     * @param moveDelta
     * @return
     */
    private EditPart getReferencePart(@SuppressWarnings("rawtypes") final List parts, final Point mouseLoc,
            final Point moveDelta) {
        int i = 0;
        Point loc = mouseLoc.getCopy().translate(moveDelta.getCopy().negate());
        makeRelative(m_container.getContentPane(), loc);
        while (i < parts.size()) {
            GraphicalEditPart result = (GraphicalEditPart)parts.get(i);
            if (result.getFigure().containsPoint(loc)) {
                return result;
            }
            if (result instanceof NodeContainerEditPart) {
                NodeAnnotationEditPart annoPart = ((NodeContainerEditPart)result).getNodeAnnotationEditPart();
                if (annoPart.getFigure().containsPoint(loc)) {
                    return annoPart;
                }
            }
            i++;
        }
        return null;
    }

    /**
     * Returns the point in the figure that should be placed onto the grid points.
     * @param nodeFig reference figure
     * @return the point in the figure that should be placed onto the grid points
     */
    public static Point getGridRefPointOffset(final NodeContainerFigure nodeFig) {
        Point iconOffset = nodeFig.getOffsetToRefPoint(new NodeUIInformation());
        iconOffset.translate(nodeFig.getSymbolFigure().getPreferredSize().width / 2,
                nodeFig.getSymbolFigure().getPreferredSize().height / 2 - 1);
        return iconOffset;
    }
}

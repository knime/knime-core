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

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.workflow.NodeUIInformation;
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
            ChangeBoundsRequest changerequest = (ChangeBoundsRequest)request;
            EditPart editPart = (EditPart)changerequest.getEditParts().get(0);
            if (editPart instanceof NodeContainerEditPart) {
                // adjust the rectangle to snap the center of the icon of the node
                NodeContainerEditPart contPart = (NodeContainerEditPart)editPart;
                EditPartViewer viewer = m_container.getViewer();
                NodeContainerFigure fig = (NodeContainerFigure)contPart.getFigure();
                Point iconOffset = getGridRefPointOffset(fig);
                double zoomFactor = ((ZoomManager)(viewer.getProperty(ZoomManager.class.toString()))).getZoom();
                iconOffset = iconOffset.getScaled(zoomFactor);
                r = rect.getPreciseCopy();
                r.translate(iconOffset);
            }
        }
        return super.snapRectangle(request, snapLocations, r, result);
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

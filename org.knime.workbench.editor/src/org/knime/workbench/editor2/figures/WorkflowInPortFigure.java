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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortFigure extends AbstractPortFigure {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowInPortFigure.class);



    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     * @param tooltip initial tooltip
     */
    public WorkflowInPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex, final String tooltip) {
        super(type, nrOfPorts, portIndex, false);
        setToolTip(new NewToolTipFigure(tooltip));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locator getLocator() {
        return new WorkflowPortLocator(getType(), getPortIndex(),
                true, getNrPorts());
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected PointList createShapePoints(final Rectangle r) {
//        Rectangle parent = getParent().getBounds().getCopy();
//        int yPos = (parent.height / (getNrPorts() + 1))
//            * (getPortIndex() + 1);
        Rectangle rect = getBounds().getCopy();
        if (getType().equals(BufferedDataTable.TYPE)) {
            // triangle
            PointList list = new PointList(3);
            list.addPoint(rect.x, rect.y);
            list.addPoint(rect.x + rect.width, rect.y + (rect.height / 2));
            list.addPoint(rect.x, rect.y + rect.height);
            return list;
        } else {
            // square
            PointList list = new PointList(4);
            list.addPoint(new Point(rect.x, rect.y));
            list.addPoint(new Point(rect.x + rect.width, rect.y));
            list.addPoint(new Point(rect.x + rect.width, rect.y + rect.height));
            list.addPoint(new Point(rect.x, rect.y + rect.height));
            return list;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle computePortShapeBounds(final Rectangle bounds) {
        return bounds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int hint, final int hint2) {
        return new Dimension(WF_PORT_SIZE, WF_PORT_SIZE);
    }

}

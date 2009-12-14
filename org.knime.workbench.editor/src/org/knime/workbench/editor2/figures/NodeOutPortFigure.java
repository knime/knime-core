/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 * Figure for displaying a <code>NodeOutPort</code> inside a node.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeOutPortFigure extends AbstractPortFigure {
    private final int m_id;

    /**
     * 
     * @param id The id of the port, needed to determine the position inside the
     *            surrounding node visual
     * @param type the port type
     * @param numPorts total number of ports
     * @param tooltip the tooltip for the node
     */
    public NodeOutPortFigure(final PortType type,
            final int id,
            final int numPorts, final String tooltip) {

        super(type, numPorts);
        m_id = id;
//        setOpaque(false);
        setToolTip(new NewToolTipFigure(tooltip));
//        setFill(true);
//        setOutline(true);
    }

    /**
     * Create a point list for the triangular figure (a polygon).
     * 
     * @param r The bounds
     * @return the pointlist (size=3)
     * {@inheritDoc}
     */
    @Override
    protected PointList createShapePoints(final Rectangle r) {
        if (getType().equals(BufferedDataTable.TYPE)) {
            PointList points = new PointList(3);
            points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                    -(NODE_PORT_SIZE / 2)));
            points.addPoint(r.getLeft().getCopy().translate(
                    NODE_PORT_SIZE * 2, 0));
            points.addPoint(r.getLeft().getCopy()
                    .translate(NODE_PORT_SIZE, (NODE_PORT_SIZE / 2)));
            return points;
        } 
        PointList points = new PointList(4);
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                -(NODE_PORT_SIZE / 2 - 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE * 2,
                -(NODE_PORT_SIZE / 2 - 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE * 2,
                (NODE_PORT_SIZE / 2 + 0)));
        points.addPoint(r.getLeft().getCopy().translate(NODE_PORT_SIZE,
                (NODE_PORT_SIZE / 2 + 0)));
        return points;
    }

    /**
     * Returns the preffered size of a port. A port is streched in length,
     * depending on the number of ports. Always try to fill up as much height as
     * possible.
     * 
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        Dimension d = new Dimension();

        d.height = (getParent().getBounds().height) / getNrPorts();
        d.width = getParent().getBounds().width / 4;
        return d;
    }

    /**
     * @return The <code>RelativeLocator</code> that places this figure on the
     *         right side (y offset corresponds to the number of the port).
     */
    @Override
    public Locator getLocator() {
        return new NodePortLocator(
                (NodeContainerFigure)getParent().getParent(),
            false, getNrPorts(), m_id, getType());
    }
}

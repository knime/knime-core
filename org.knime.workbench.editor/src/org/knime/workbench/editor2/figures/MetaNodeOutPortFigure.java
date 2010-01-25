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
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.04.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.editparts.MetaNodeOutPortEditPart;

/**
 * Figure to a {@link MetaNodeOutPortEditPart}.
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeOutPortFigure extends NodeOutPortFigure {
    
    /** Red traffic light. * */
    public static final Image RED =
            ImageRepository.getImage("icons/ports/port_idle.png");

    /** Yellow traffic light. * */
    public static final Image YELLOW =
            ImageRepository.getImage("icons/ports/port_configured.png");

    /** Green traffic light. * */
    public static final Image GREEN =
            ImageRepository.getImage("icons/ports/port_executed.png");

    private NodeContainer.State m_currentState;
    private Image m_currentImage;
    /**
     * @param type type of the port (data, db, model)
     * @param id index of the port
     * @param numPorts total number of ports
     * @param tooltip the tooltip for this port
     * @param state the loaded state of the underlying node
     */
    public MetaNodeOutPortFigure(final PortType type, 
            final int id, final int numPorts,
            final String tooltip, final NodeContainer.State state) {
        super(type, id, numPorts, tooltip);
        if (state != null) {
            m_currentState = state;
        } else {
            m_currentState = NodeContainer.State.IDLE;
        }
        m_currentImage = RED;
    }
    
    /**
     * Called by the 
     * {@link MetaNodeOutPortEditPart#stateChanged(NodeStateEvent)} in order 
     * to provide a correct tooltip and icon. 
     * 
     * @param state current state of the port (idle/spec/data)
     */
    public void setState(final NodeContainer.State state) {
        m_currentState = state; 
    }

    
    /**
     * Outlines the shape, the points of the actual shape are set in 
     * {@link NodeInPortFigure#createShapePoints(Rectangle)} and 
     * {@link NodeOutPortFigure#createShapePoints(Rectangle)}. Only data ports
     * (ports of type {@link BufferedDataTable#TYPE})are outlined, all other 
     * port types are filled.
     *
     * {@inheritDoc}
     * @see NodeInPortFigure#createShapePoints(Rectangle)
     * @see NodeOutPortFigure#createShapePoints(Rectangle)
     */
    @Override
    protected void outlineShape(final Graphics graphics) {
        super.outlineShape(graphics);
        if (m_currentState.equals(NodeContainer.State.IDLE)) {
            m_currentImage = RED;
//            graphics.setBackgroundColor(ColorConstants.red);
        } else if (m_currentState.equals(NodeContainer.State.CONFIGURED)) {
            m_currentImage = YELLOW;
        } else if (m_currentState.equals(NodeContainer.State.EXECUTED)) {
            m_currentImage = GREEN;
        }
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
//        graphics.fillPolygon(points);
        Point p1 = points.getPoint(0);
        graphics.drawImage(m_currentImage, new Point(p1.x + 7, p1.y - 5));
    }
    
    
    // TODO implement a listener interface which gets informed about state 
    // changes in the underlying node port 
    // render 
    // red: no spec, no data
    // yellow: node data, spec
    // green: data, spec
    

}

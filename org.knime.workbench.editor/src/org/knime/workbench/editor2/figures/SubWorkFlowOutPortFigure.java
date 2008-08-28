/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.workbench.editor2.ImageRepository;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubWorkFlowOutPortFigure extends NodeOutPortFigure 
    implements NodeStateChangeListener {
    
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
    public SubWorkFlowOutPortFigure(final PortType type, 
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
     * 
     * {@inheritDoc}
     */
    public void stateChanged(final NodeStateEvent state) {
        NodeLogger.getLogger(SubWorkFlowOutPortFigure.class)
            .debug("port state changed to " + state.getState());
        m_currentState = state.getState();
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                SubWorkFlowOutPortFigure.this.repaint();
            }
            
        });
        
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

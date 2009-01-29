/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortFigure extends AbstractWorkflowPortFigure {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowOutPortFigure.class);
    

    /**
     *
     * @param type port type (data, model, etc)
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     * @param wfmName name of the subworkflow for tooltip
     */
    public WorkflowOutPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex, final String wfmName) {
        super(type, nrOfPorts, portIndex);
        setToolTip(new NewToolTipFigure(wfmName + " out port: " + portIndex));
    }
    
    
//    @Override
//    protected void outlineShape(Graphics graphics) {
//        Rectangle r = getBounds().getCopy();
//        PointList list = new PointList(3);
//        list.addPoint(r.x, r.y);
//        list.addPoint(r.x + r.width, r.y + (r.height / 2));
//        list.addPoint(r.x, r.y + r.height);
//        graphics.drawPolygon(list);
//    }
//    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected PointList createShapePoints(final Rectangle rect) {
        Rectangle r = getBounds().getCopy();
        if (getType().equals(BufferedDataTable.TYPE)) {
            // triangle
            PointList list = new PointList(3);
            list.addPoint(r.x, r.y);
            list.addPoint(r.x + r.width, r.y + (r.height / 2));
            list.addPoint(r.x, r.y + r.height);
            return list;
        } else {
            // square
            PointList list = new PointList(4);
            list.addPoint(new Point(r.x, r.y));
            list.addPoint(new Point(r.x + r.width, r.y));
            list.addPoint(new Point(r.x + r.width, r.y + r.height));
            list.addPoint(new Point(r.x, r.y + r.height));
            return list;
        }
    }
    

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Locator getLocator() {
        return new WorkflowPortLocator(getType(), getPortIndex(),
                false, getNrPorts());
    }
}

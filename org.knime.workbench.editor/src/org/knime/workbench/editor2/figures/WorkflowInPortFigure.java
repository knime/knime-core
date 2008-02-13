/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
import org.knime.core.node.PortType;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortFigure extends AbstractWorkflowPortFigure {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowInPortFigure.class);
    
    private boolean m_isSelected = false;

    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     * @param tooltip initial tooltip
     */
    public WorkflowInPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex, final String tooltip) {
        super(type, nrOfPorts, portIndex);
        setToolTip(new NewToolTipFigure(tooltip));
    }
    
    /**
     * This state is read by the mouse listener added in 
     * {@link WorkflowInPortEditPart#createFigure()}.
     * 
     * @return true if the figure was clicked, false otherwise
     * @see WorkflowInPortEditPart
     */
    public boolean isSelected() {
        return m_isSelected;
    }
    
    /**
     * This state is set by the mouse listener added in 
     * {@link WorkflowInPortEditPart#createFigure()}.
     * 
     * @param selected true if the figure was clicked, false otherwise
     * @see WorkflowInPortEditPart
     */
    public void setSelected(final boolean selected) {
        m_isSelected = selected;
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
        if (getType().equals(BufferedDataTable.TYPE)) {
            // triangle
            Rectangle parent = getParent().getBounds().getCopy();
            int yPos = (parent.height / (getNrPorts() + 1)) 
                * (getPortIndex() + 1);
            getBounds().x = 0;
            getBounds().y = yPos;
            getBounds().width = SIZE;
            getBounds().height = SIZE;
            Rectangle rect = getBounds().getCopy();
            PointList list = new PointList(3);
            list.addPoint(rect.x, rect.y);
            list.addPoint(rect.width, rect.y + (rect.height / 2));
            list.addPoint(rect.x, rect.y + rect.height);
            return list;
        } else {
            // square
            Rectangle parent = getParent().getBounds().getCopy();
            int yPos = (parent.height / (getNrPorts() + 1)) 
                * (getPortIndex() + 1);
            getBounds().x = 0;
            getBounds().y = yPos;
            getBounds().width = SIZE;
            getBounds().height = SIZE;
            PointList list = new PointList(4);
            list.addPoint(new Point(0, yPos));
            list.addPoint(new Point(SIZE, yPos));
            list.addPoint(new Point(SIZE, yPos + SIZE));
            list.addPoint(new Point(0, yPos + SIZE));
            return list;
        }
    }

}

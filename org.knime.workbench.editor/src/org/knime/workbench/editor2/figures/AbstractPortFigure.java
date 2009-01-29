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
 * ---------------------------------------------------------------------
 * 
 * History
 *   13.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.workbench.KNIMEEditorPlugin;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractPortFigure extends Shape {
    
    
    private final int m_nrOfPorts;
    private final PortType m_portType;
    
    /** Size constant for the workflow ports. */
    public static final int WF_PORT_SIZE = 20;
    
    /** Size constant for node ports. */
    public static final int NODE_PORT_SIZE = 9;

    /**
     *
     * @param type port type
     * @param nrOfPorts total number of ports
     */
    public AbstractPortFigure(final PortType type,
            final int nrOfPorts) {
        m_portType = type;
        m_nrOfPorts = nrOfPorts;
        setFill(true);
        setOutline(true);
        setOpaque(true);
    }

    /**
     * 
     * @return the type of the port
     */
    protected PortType getType() {
        return m_portType;
    }

    /**
     *
     * @return total number of ports
     */
    protected int getNrPorts() {
        return m_nrOfPorts;
    }
    
    /**
     * We need to set the colors explicitly dependend on the {@link PortType}.
     * Currently supported are {@link BufferedDataTable#TYPE} : black, 
     * {@link ModelPortObject#TYPE} : blue, {@link DatabasePortObject#TYPE} : 
     * dark yellow.
     * 
     * @return the background color, dependend on the {@link PortType}
     * 
     * {@inheritDoc}
     */
    @Override
    public Color getBackgroundColor() {
        Color color = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        if (KNIMEEditorPlugin.PMML_PORT_TYPE.isSuperTypeOf(getType())) {
            // model
            color = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
        } else if (AbstractSimplePortObject.class.isAssignableFrom(
                getType().getPortObjectClass())) {
            // model
            color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN);
        } else if (getType().equals(BufferedDataTable.TYPE)) {
            // data
            color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
        } else if (getType().equals(DatabasePortObject.TYPE)) {
            // database
            color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
        } else if (getType().equals(FlowVariablePortObject.TYPE)) {
            color = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
        }
        return color;
    }
    
    /**
     * The color is determined with {@link #getBackgroundColor()} and set.
     * 
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());        
        super.paintFigure(graphics);
    }

    /**
     * Fills the shape, the points of the actual shape are set in 
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
    protected void fillShape(final Graphics graphics) {
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
        // data ports are not filled, model ports are filled
        if (!getType().equals(BufferedDataTable.TYPE)) {
            graphics.fillPolygon(points);
        }
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
        Rectangle r = getBounds().getCopy().shrink(3, 3);
        PointList points = createShapePoints(r);
        if (getType().equals(BufferedDataTable.TYPE)) {            
            graphics.drawPolygon(points);
        }
    }
    
    //////////////////////////////////////////////////////////////////
    //
    // ABSTRACT METHODS
    //
    //////////////////////////////////////////////////////////////////
    
    /**
     * Create a point list for the triangular figure (a polygon).
     *
     * @param r The bounds
     * @return the pointlist (size=3)
     */
    protected abstract PointList createShapePoints(final Rectangle r);

    
    /**
     * Children must return a <code>Locator</code> that calculate the position
     * inside the hosting figure.
     *
     * @return The locator
     */
    public abstract Locator getLocator();
    
}

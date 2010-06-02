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
import org.knime.core.node.port.pmml.PMMLPortObject;


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
        if (PMMLPortObject.TYPE.isSuperTypeOf(getType())) {
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

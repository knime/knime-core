/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.Serializable;

import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;

/**
 * This class represents the smallest element of a histogram and corresponds
 * to the color in which the rows of this element are colored. The BarElements
 * belong to one
 * {@link org.knime.base.node.viz.histogram.datamodel.BarDataModel}.
 * @author Tobias Koetter, University of Konstanz
 */
public class BarElementDataModel extends AggregationValSubModel <Rectangle,
Rectangle>
implements Serializable {

    private static final long serialVersionUID = 2537898631338523620L;

    /**Constructor for class BarElementDataModel.
     * @param color the color of this element
     */
    protected BarElementDataModel(final Color color) {
        this(color, false);
    }

    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     * @param supportHiliting if hiliting should be supported
     */
    protected BarElementDataModel(final Color color,
            final boolean supportHiliting) {
       super(color, supportHiliting);
    }

    /**Constructor for class BarElementDataModel (used for cloning).
     * @param color
     * @param aggrSum
     * @param valueCounter
     * @param rowCounter
     * @param selected
     */
    private BarElementDataModel(final Color color, final boolean enableHiliting,
            final double aggrSum, final int valueCounter, final int rowCounter,
            final boolean selected) {
        super(color, enableHiliting, aggrSum, valueCounter, rowCounter,
                selected);
    }

    /**
     * @return the {@link Rectangle} the element should be drawn on the
     * screen
     */
    public Rectangle getElementRectangle() {
        return getShape();
    }

    /**
     * @param elementRect the {@link Rectangle} the element should be drawn
     * on the screen
     * @param calculator the hilite shape calculator
     */
    protected void setElementRectangle(final Rectangle elementRect,
            final HistogramHiliteCalculator calculator) {
        if (elementRect == null) {
            setRectangle(elementRect, calculator);
        } else if (getShape() == null
                || !getShape().equals(elementRect)) {
            setRectangle(elementRect, calculator);
        }
    }

    /**
     * @param rect
     * @param calculator the hilite shape calculator
     */
    private void setRectangle(final Rectangle rect,
            final HistogramHiliteCalculator calculator) {
        setShape(rect, calculator);
    }

    /**
     * @param xCoord the new x coordinate
     * @param elementWidth the new element width
     * @param calculator the hilite shape calculator
     */
    public void updateElementWidth(final int xCoord, final int elementWidth,
            final HistogramHiliteCalculator calculator) {
        final Rectangle rectangle = getShape();
        if (rectangle == null) {
            return;
        }
        final int yCoord = (int)rectangle.getY();
        final int elementHeight = (int)rectangle.getHeight();
        final Rectangle rect = new Rectangle(xCoord, yCoord,
                elementWidth, elementHeight);
        setRectangle(rect, calculator);
//        m_elementRectangle.setBounds(xCoord, yCoord,
//                elementWidth, elementHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel clone() {
        final BarElementDataModel clone = new BarElementDataModel(getColor(),
                supportsHiliting(), getAggregationSum(), getValueCount(),
                getRowCount(), isSelected());
        return clone;
    }
}

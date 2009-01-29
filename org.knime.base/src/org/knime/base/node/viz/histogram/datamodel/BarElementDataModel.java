/*
 * -------------------------------------------------------------------
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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;

/**
 * This class represents the smallest element of a histogram and corresponds
 * to the color in which the rows of this element are colored. The BarElements
 * belong to one
 * {@link org.knime.base.node.viz.histogram.datamodel.BarDataModel}.
 * @author Tobias Koetter, University of Konstanz
 */
public class BarElementDataModel extends AggregationValSubModel <Rectangle2D,
Rectangle2D>
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
     */
    private BarElementDataModel(final Color color, final boolean enableHiliting,
            final double aggrSum, final int valueCounter,
            final int rowCounter) {
        super(color, enableHiliting, aggrSum, valueCounter, rowCounter);
    }

    private BarElementDataModel(final ConfigRO config)
    throws InvalidSettingsException {
        super(config);
    }

    /**
     * @return the {@link Rectangle} the element should be drawn on the
     * screen
     */
    public Rectangle2D getElementRectangle() {
        return getShape();
    }

    /**
     * @param rect the {@link Rectangle} to set or <code>null</code>
     * @param calculator the hilite shape calculator
     */
    protected void setRectangle(final Rectangle2D rect,
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
        final Rectangle2D rectangle = getShape();
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
                getRowCount());
        return clone;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the loaded {@link BarElementDataModel}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static BarElementDataModel loadFromFile(final Config config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        exec.checkCanceled();
        return new BarElementDataModel(config);
    }
}

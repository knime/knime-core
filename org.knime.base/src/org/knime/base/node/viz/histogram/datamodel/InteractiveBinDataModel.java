/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;

/**
 * This class extends the {@link BinDataModel} to support hiliting.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBinDataModel extends BinDataModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveBinDataModel.class);
    private static final long serialVersionUID = 4709538043061219689L;

    /**If the different bars of this bin can't be draw because the bin
     * is to small this rectangle is calculated to reflect the proportion
     * of hilited rows in this bin. */
    private Rectangle2D m_hiliteRectangle;

    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     */
    public InteractiveBinDataModel(final String xAxisCaption,
            final double lowerBound, final double upperBound) {
        super(xAxisCaption, lowerBound, upperBound);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarDataModel createBar(final String barName, final Color color) {
        return new InteractiveBarDataModel(barName, color);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setBinRectangle(final Rectangle binRectangle,
            final int baseLine,
            final SortedSet<Color> barElementColors,
            final Collection<ColorColumn> aggrColumns,
            final HistogramHiliteCalculator calculator) {
        super.setBinRectangle(binRectangle, baseLine, barElementColors,
                aggrColumns, calculator);
        calculateHiliteRectangle(calculator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBinWidth(final int startX, final int binWidth,
            final SortedSet<Color> barElementColors,
            final Collection<ColorColumn> aggrColumns, final int baseLine,
            final HistogramHiliteCalculator calculator) {
        super.updateBinWidth(startX, binWidth, barElementColors,
                aggrColumns, baseLine, calculator);
        calculateHiliteRectangle(calculator);
    }

    /**
     * @return the number of hilited rows in this bin.
     */
    public int getNoOfHilitedRows() {
        int noOfHilitedKeys = 0;
        for (final BarDataModel bar : getBars()) {
            noOfHilitedKeys +=
                (bar).getHiliteRowCount();
            //we need the number of hilited keys only from one bar
            //since the number is equal for all bars
            break;
        }
        return noOfHilitedKeys;
    }

    /**
     * @param hilited the row keys to hilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    protected boolean setHilitedKeys(final Set<RowKey> hilited,
            final HistogramHiliteCalculator calculator) {
        boolean changed = false;
        for (final BarDataModel bar : getBars()) {
            changed = bar.setHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(calculator);
        }
        return changed;
    }

    /**
     * @param hilited the row keys to unhilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    protected boolean removeHilitedKeys(final Set<RowKey> hilited,
            final HistogramHiliteCalculator calculator) {
        boolean changed = false;
        for (final BarDataModel bar : getBars()) {
            changed = bar.removeHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(calculator);
        }
        return changed;
    }

    /**
     * Clears the hilite information.
     */
    public void clearHilite() {
        for (final BarDataModel bar : getBars()) {
            (bar).clearHilite();
        }
        m_hiliteRectangle = null;
    }


    /**
     * The proportional hilite rectangle of this bin which
     * could be displayed if the bars of this bin can't be draw.
     * @return the proportional hilite rectangle
     */
    public Rectangle2D getHiliteRectangle() {
        return m_hiliteRectangle;
    }

    /**
     * This calculates the proportional hilite rectangle of this bar which
     * could be displayed if the elements of this bar can't be draw.
     * Set the hilite rectangle in the middle of the bar since we
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     */
    private void calculateHiliteRectangle(
            final HistogramHiliteCalculator calculator) {
        final Rectangle2D binRectangle = getBinRectangle();
        if (isPresentable() || binRectangle == null) {
            m_hiliteRectangle = null;
            return;
        }
        final int noOfHilitedRows = getNoOfHilitedRows();
        if (noOfHilitedRows <= 0) {
            m_hiliteRectangle = null;
            return;
        }
        if (calculator == null) {
            return;
        }
        final AggregationMethod aggrMethod = calculator.getAggrMethod();
        final HistogramLayout layout = calculator.getLayout();
        final int binY = (int)binRectangle.getY();
        final int binHeight = (int)binRectangle.getHeight();
        final int binWidth = (int)binRectangle.getWidth();
        final int rowCount = getBinRowCount();
        final double fraction = noOfHilitedRows / (double)rowCount;
        int hiliteHeight = (int)(binHeight * fraction);
        final int hiliteWidth = Math.max(
                (int)(binWidth
        * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR),
        1);
        final int hiliteX = (int) (binRectangle.getX()
                + (binWidth - hiliteWidth) / 2);
        int hiliteY = binY;
        if (getMinAggregationValue(aggrMethod, layout) < 0
                && getMaxAggregationValue(aggrMethod, layout) > 0) {
            //set the hilite rectangle in the side by side mode in the middle
            //if the minimum aggregation value is negative and the maximum
            //aggregation value is positive
            final int middleY = (int)(binY + (binHeight / 2.0));
            hiliteY = middleY - (hiliteHeight / 2);
        } else  if (getMaxAggregationValue(aggrMethod, layout) > 0) {
            hiliteY = hiliteY + binHeight - hiliteHeight;
        }
        //check for possible rounding errors
        if (hiliteHeight > binHeight) {
            hiliteHeight = binHeight;
            LOGGER.warn("Hilite rectangle higher than surrounding bar");
        }
        if (hiliteY < binY) {
            hiliteY = binY;
            LOGGER.warn("Hilite rectangle y coordinate above "
                    + "surrounding bar y coordinate");
        }
        m_hiliteRectangle =
            new Rectangle(hiliteX, hiliteY, hiliteWidth, hiliteHeight);
    }
}

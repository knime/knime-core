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
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;

import java.awt.Color;
import java.util.List;

/**
 * This class extends the {@link BarDataModel} to support hiliting.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBarDataModel extends BarDataModel {

    private static final long serialVersionUID = -5525319030969754880L;

    /**Constructor for class BarDataModel.
     * @param barName the name of this bar
     * @param color the color to use for this bar
     */
    protected InteractiveBarDataModel(final String barName, final Color color) {
        super(barName, color, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel createElement(final Color color) {
        return new InteractiveBarElementDataModel(color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBarWidth(final int startX, final int barWidth,
            final List<Color> barElementColors, final int baseLine,
            final HistogramHiliteCalculator calculator) {
        super.updateBarWidth(startX, barWidth, barElementColors,
                baseLine, calculator);
        calculateHiliteShape(calculator);
    }
}

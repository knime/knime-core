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

import java.awt.Color;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;

/**
 * This class extends the {@link BarElementDataModel} to support hiliting.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBarElementDataModel extends BarElementDataModel {

    private static final long serialVersionUID = -6612680797726333107L;

    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     */
    protected InteractiveBarElementDataModel(final Color color) {
        super(color, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateElementWidth(final int xCoord, final int elementWidth,
            final HistogramHiliteCalculator calculator) {
        super.updateElementWidth(xCoord, elementWidth, calculator);
        calculateHilitedShape(calculator);
    }
}

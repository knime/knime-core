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
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.Set;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;

/**
 * This class holds the information of a histogram bin. A bin consists of at 
 * least one {@link InteractiveBarDataModel} object which consists of 
 * one or more {@link InteractiveBarElementDataModel} objects.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBinDataModel extends BinDataModel {
    
    private static final long serialVersionUID = 4709538043061219689L;

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
     * @see org.knime.base.node.viz.histogram.datamodel.BinDataModel
     * #createBar(java.awt.Color)
     */
    @Override
    protected BarDataModel createBar(final Color color) {
        return new InteractiveBarDataModel(color);
    }
    /**
     * @param hilited the row keys to hilite
     * @param aggrMethod the current aggregation method
     * @param layout the current {@link HistogramLayout}
     */
    protected void setHilitedKeys(final Set<DataCell> hilited, 
            final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        for (final BarDataModel bar : getBars()) {
            ((InteractiveBarDataModel)bar).setHilitedKeys(hilited, 
                    aggrMethod, layout);
        }
    }

    /**
     * @param hilited the row keys to unhilite
     * @param aggrMethod the current aggregation method
     * @param layout the current {@link HistogramLayout}
     */
    protected void removeHilitedKeys(final Set<DataCell> hilited, 
            final AggregationMethod aggrMethod, 
            final HistogramLayout layout) {
        for (final BarDataModel bar : getBars()) {
            ((InteractiveBarDataModel)bar).removeHilitedKeys(hilited, 
                    aggrMethod, layout);
        }
    }

    /**
     * Clears the hilite information.
     */
    public void clearHilite() {
        for (final BarDataModel bar : getBars()) {
            ((InteractiveBarDataModel)bar).clearHilite();
        }
    }
}

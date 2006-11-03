/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.histogram.impl.fixed;

import org.knime.base.node.viz.histogram.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataTableSpec;


/**
 * The properties panel of the Histogram plotter which allows the user to change
 * the look and behaviour of the histogram plotter. The following options are
 * available:
 * <ol>
 * <li>Bar width</li>
 * <li>Number of bars for a numeric x column</li>
 * <li>different aggregation methods</li>
 * <li>hide empty bars</li>
 * <li>show missing value bar</li>
 * </ol>
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramProperties extends 
    AbstractHistogramProperties {


    /**
     * Constructor for class FixedColumnHistogramProperties.
     * 
     * @param aggrMethod the aggregation method which should be set
     */
    public FixedColumnHistogramProperties(final AggregationMethod aggrMethod) {
        super(aggrMethod);
        //disable the select boxes
        disableSelectBoxes();
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramProperties
     *  #onSelectAggrMethod(java.lang.String)
     */
    @Override
    protected void onSelectAggrMethod(final String actionCommand) {
        super.onSelectAggrMethod(actionCommand);
        //disable the select box
        disableSelectBoxes();
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramProperties
     * #updateColumnSelection(org.knime.core.data.DataTableSpec, 
     * java.lang.String, java.lang.String)
     */
    @Override
    public void updateColumnSelection(final DataTableSpec spec, 
            final String xColName, final String yColName) {
        super.updateColumnSelection(spec, xColName, yColName);
        //disable the column select boxes
        disableSelectBoxes();
    }
    
    /**
     * Disables the column select boxes which shouldn't be used in this 
     * histogram implementation.
     */
    private void disableSelectBoxes() {
        setXColEnabled(false);
        setAggrColEnabled(false);
    }
}

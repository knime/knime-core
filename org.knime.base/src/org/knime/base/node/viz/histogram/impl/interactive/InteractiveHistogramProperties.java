/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.awt.event.ActionListener;

import org.knime.base.node.viz.histogram.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.AggregationMethod;

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
public class InteractiveHistogramProperties extends 
    AbstractHistogramProperties {

    private static final long serialVersionUID = -5763365762779483362L;

    /**
     * Constructor for class FixedColumnHistogramProperties.
     * 
     * @param aggrMethod the aggregation method which should be set
     */
    public InteractiveHistogramProperties(final AggregationMethod aggrMethod) {
       super(aggrMethod);
    }
    
    /**
     * @param listerner the listener to listen if the x column has changed
     */
    public void addXColActionListener(final ActionListener listerner) {
        getXColSelectBox().addActionListener(listerner);
    }
    
    /**
     * @param listerner the listener to listen if the x column has changed
     */
    public void addAggrColActionListener(final ActionListener listerner) {
        getAggrColSelectBox().addActionListener(listerner);
    }
}

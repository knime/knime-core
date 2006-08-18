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
package org.knime.base.node.viz.histogram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.knime.base.node.viz.plotter2D.AbstractPlotter2D;

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
public class InteractiveHistogramProperties extends AbstractHistogramProperties {

    /**
     * Constructor for class FixedColumnHistogramProperties.
     * 
     * @param aggrMethod the aggregation method which should be set
     */
    public InteractiveHistogramProperties(final AggregationMethod aggrMethod) {
       super(aggrMethod);
       m_xCol.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
               onXColChanged(m_xCol.getSelectedColumn());
           }
       });
    }

    /**
     * Called whenever user changes the x column selection.
     * 
     * @param xColName the new selected x column
     */
    private void onXColChanged(final String xColName) {
        final InteractiveHistogramPlotter plotter = getHistogramPlotter();
        if (plotter == null || xColName == null) {
            return;
        }
        plotter.setXColumn(xColName);
        m_xCol.setToolTipText(xColName);
        // repaint the plotter
        plotter.updatePaintModel();
        // update the slider values and the select boxes
        setUpdateHistogramSettings(plotter);
    }

    /**
     * @return the <code>FixedColumnHistogramPlotter</code> object to whom this
     *         properties panel belongs
     */
    protected InteractiveHistogramPlotter getHistogramPlotter() {
        AbstractPlotter2D plotter = getPlotter();
        if (plotter instanceof InteractiveHistogramPlotter) {
            return (InteractiveHistogramPlotter)plotter;
        }
        return null;
    }
}

/*  
 * -------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   07.08.2006 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import javax.swing.JPopupMenu;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.scatterplot.ScatterPlotter;
import org.knime.base.node.viz.scatterplot.ScatterProps;

/**
 * An Errorplot. The error in each iteration is plotted into a 
 * ScatterPlot.
 * 
 * @author cebron, University of Konstanz
 */
public class ErrorPlot extends ScatterPlotter {

    /**
     * 
     * @param rowContainer the rows we are getting the data to plot from
     * @param initialWidth The width at zoom 1x.
     * @param props the scatterplot properties associated with this scatterplot
     */
    public ErrorPlot(final DataArray rowContainer, final int initialWidth,
            final ScatterProps props) {
        super(rowContainer, initialWidth, props);
    }

    /**
     * Overridden, because there is no highlighting in this view.
     * 
     * @see org.knime.base.node.viz.plotter2D.AbstractPlotter2D#
     *      fillPopupMenu(javax.swing.JPopupMenu)
     */
    @Override
    protected void fillPopupMenu(final JPopupMenu menu) {

    }
    
    
   

}

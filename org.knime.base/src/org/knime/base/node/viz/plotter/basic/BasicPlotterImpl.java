/*
 * ------------------------------------------------------------------
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
 *   12.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JMenu;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicPlotterImpl extends BasicPlotter {
    /**
     * 
     * @param pane the drawing pane
     */
    public BasicPlotterImpl(final BasicDrawingPane pane, 
            final AbstractPlotterProperties props) {
        super(pane, props);
    }

    /**
     * 
     * @param pane the drawing pane
     */
    public BasicPlotterImpl(final BasicDrawingPane pane) {
        super(pane, new AbstractPlotterProperties());
    }
   
    /**
     * 
     *
     */
    public BasicPlotterImpl() {
        super(new BasicDrawingPaneImpl(), new AbstractPlotterProperties());
    }
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    public JMenu getHiLiteMenu() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {

    }
 

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {

    }
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaintModel() {
        updateSize();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
        setXAxis(null);
        setYAxis(null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
    } 
}

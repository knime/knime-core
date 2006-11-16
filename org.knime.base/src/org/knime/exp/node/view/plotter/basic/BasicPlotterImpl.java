/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   12.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.basic;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.Action;

import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.exp.node.view.plotter.AbstractPlotterProperties;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BasicPlotterImpl extends BasicPlotter {
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
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLite(
     * org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {

    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#unHiLite(
     * org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {

    }
 

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     */
    public void unHiLiteAll() {

    }

    

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     * getClearHiliteAction()
     */
    @Override
    public Action getClearHiliteAction() {
        return null;
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#getHiliteAction()
     */
    @Override
    public Action getHiliteAction() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#getUnhiliteAction()
     */
    @Override
    public Action getUnhiliteAction() {
        // TODO Auto-generated method stub
        return null;
    }
    
    

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLiteSelected()
     */
    @Override
    public void hiLiteSelected() {
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#unHiLiteSelected()
     */
    @Override
    public void unHiLiteSelected() {
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        setXAxis(null);
        setYAxis(null);
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#selectElementsIn(
     * java.awt.Rectangle)
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     * selectClickedElement(java.awt.Point)
     */
    @Override
    public void selectClickedElement(final Point clicked) {
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#clearSelection()
     */
    @Override
    public void clearSelection() {
    } 
}

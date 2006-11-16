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
 *   13.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scatter;

import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.columns.TwoColumnProperties;
import org.knime.base.node.viz.plotter.props.ScatterPlotterAppearanceTab;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterProperties extends TwoColumnProperties {

    /** the default initial dot size. */
    public static final int DEFAULT_DOT_SIZE = 5;
    
    
    private final ScatterPlotterAppearanceTab m_appearance;
    
    /**
     * 
     *
     */
    public ScatterPlotterProperties() {
        super();
        m_appearance = new ScatterPlotterAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
    }
    
    /**
     * 
     * @return the slider to adjust the jitter rate.
     */
    public JSlider getJitterSlider() {
        return m_appearance.getJitterSlider();
    }
    
    /**
     * 
     * @param listener change listener for the dot size.
     */
    public void addDotSizeChangeListener(final ChangeListener listener) {
        m_appearance.addDotSizeChangeListener(listener);
    }
    
    
    /**
     * Read the current value from the spinner assuming it contains Integers.
     * @return int the current value of the dot size spinner.
     */
    protected int getDotSize() {
        return m_appearance.getDotSize();
    }
}

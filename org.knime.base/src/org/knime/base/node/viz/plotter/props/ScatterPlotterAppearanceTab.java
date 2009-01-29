/*
 * ------------------------------------------------------------------
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import java.awt.Dimension;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotterProperties;
import org.knime.core.node.NodeLogger;

/**
 * Control elements to adjust the dot size and the jitter rate.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterAppearanceTab extends PropertiesTab {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ScatterPlotterAppearanceTab.class);
    
    
    private final JSpinner m_dotSizeSpinner;
    
    private final JSlider m_jitterSlider;
    
    private static final int MIN_DOT_SIZE = 1;
    private static final int MAX_DOT_SIZE = 150;
    
    
    /**
     * Dot size, shape and jitter.
     *
     */
    public ScatterPlotterAppearanceTab() {
        m_dotSizeSpinner = new JSpinner(new SpinnerNumberModel(
                ScatterPlotterProperties.DEFAULT_DOT_SIZE,
                MIN_DOT_SIZE, MAX_DOT_SIZE, 1));
        m_jitterSlider = new JSlider();
        
        m_jitterSlider.setPreferredSize(new Dimension(
                COMPONENT_WIDTH, 
                m_jitterSlider.getPreferredSize().height)); 
        Box all = Box.createHorizontalBox();
        all.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(new JLabel("Dot Size"));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(m_dotSizeSpinner);
        all.add(Box.createHorizontalStrut(SPACE));
        all.add(new JLabel("Jitter"));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(m_jitterSlider);
        all.add(Box.createHorizontalGlue());
        add(all);
    }
    
    /**
     * 
     * @return the slider to adjust the jitter rate.
     */
    public JSlider getJitterSlider() {
        return m_jitterSlider;
    }
    
    /**
     * 
     * @param listener change listener for the dot size.
     */
    public void addDotSizeChangeListener(final ChangeListener listener) {
        m_dotSizeSpinner.addChangeListener(listener);
    }
    
    /**
     * 
     * @param dotSize sets the dotsize value.
     */
    public void setDotSize(final int dotSize) {
        m_dotSizeSpinner.setValue(dotSize);
    }
    
    
    
    /**
     * Read the current value from the spinner assuming it contains Integers.
     * @return int the current value of the dot size spinner.
     */
    public int getDotSize() {
        try {
            m_dotSizeSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
            LOGGER.warn(e, e);
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_dotSizeSpinner
            .getModel();
        return snm.getNumber().intValue();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }

}

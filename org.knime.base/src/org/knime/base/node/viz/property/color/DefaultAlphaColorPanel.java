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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   30.05.2008 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A default panel to adjust the alpha color value.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultAlphaColorPanel extends AbstractColorChooserPanel {

    private JSlider m_slider;
    private JSpinner m_spinner;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildChooser() {
        super.setLayout(new BorderLayout());
        m_slider = new JSlider(JSlider.HORIZONTAL, 0, 255, 255);
        m_slider.setMajorTickSpacing(85);
        m_slider.setMinorTickSpacing(17);
        m_slider.setPaintTicks(true);
        m_slider.setPaintLabels(true);
        m_spinner = new JSpinner(new SpinnerNumberModel(255, 0, 255, 5));
        JPanel spinnerPanel = new JPanel(new FlowLayout());
        spinnerPanel.add(m_spinner);
        
        super.add(new JLabel("Alpha "), BorderLayout.WEST);
        super.add(m_slider, BorderLayout.CENTER);
        super.add(spinnerPanel, BorderLayout.EAST);
        super.add(new JLabel("\n(Alpha composition is "
                + "expensive in cases when operations performed are not "
                + "hardware-accelerated.)"),
                BorderLayout.SOUTH);
        
        m_slider.addChangeListener(new ChangeListener() {
            
            public void stateChanged(final ChangeEvent e) {
                setAlpha(m_slider.getValue());
            }
        });
        m_spinner.addChangeListener(new ChangeListener() {
            
            public void stateChanged(final ChangeEvent e) {
                try {
                    m_spinner.commitEdit();
                    setAlpha((Integer) m_spinner.getValue());
                } catch (ParseException pe) {
                    setAlpha(255);
                }
            }
        });
        m_slider.addFocusListener(new FocusAdapter() {
           @Override
           public void focusLost(final FocusEvent fe) {
               getAlpha();
           } 
        });
        
        m_spinner.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent fe) {
                getAlpha();
            } 
         });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Alpha";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getMnemonic() {
        return KeyEvent.VK_A;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getDisplayedMnemonicIndex() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateChooser() {
        
    }
    
    /**
     * @return current alpha value between 0..255
     */
    public int getAlpha() {
        try {
            m_spinner.commitEdit();
            int value = (Integer) m_spinner.getValue();
            m_slider.setValue(value);
            return value;
        } catch (ParseException pe) {
            setAlpha(255);
            return 255;
        }
    }
    
    /**
     * Sets a (new) alpha value into this component.
     * @param alpha value to set
     */
    public void setAlpha(final int alpha) {
        m_slider.setValue(alpha);
        m_spinner.setValue(alpha);
    }
        
}

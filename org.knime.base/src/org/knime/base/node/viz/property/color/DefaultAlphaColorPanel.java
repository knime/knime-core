/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

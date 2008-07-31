/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   23.07.2007 (thiel): created
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provides a component containing a button and a label. The label's and 
 * button's text can be set individually and an <code>ActionListener</code>
 * can be added to the button, to respond to action events. A model is not
 * needed, since no input component like a text field etc. is provided here.  
 * Thus no setting values will be saved. The button will appear at the left 
 * side of the label.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class DialogComponentButtonLabel extends DialogComponent {

    private JButton m_button;
    
    private JLabel m_label;
    

    /**
     * Creates new instance of <code>DialogComponentButtonLabel</code> with
     * given text to set as button text and label text.
     * 
     * @param buttonText The text to set as button text.
     * @param labelText The text to set as label text.
     */
    public DialogComponentButtonLabel(final String buttonText, 
            final String labelText) {
        super(new EmptySettingsModel());
        
        getComponentPanel().setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EtchedBorder());
        m_button = new JButton(buttonText);
        m_label = new JLabel(labelText);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel.add(m_button, gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 100;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel.add(m_label, gbc);
        
        getComponentPanel().add(panel, BorderLayout.CENTER);
    }
    
    /**
     * @param listener The listener to add.
     */
    public void addActionListener(final ActionListener listener) {
        m_button.addActionListener(listener);
    }
    
    /**
     * @param listener The listener to remove.
     */
    public void removeActionListener(final ActionListener listener) {
        m_button.removeActionListener(listener);
    }
    
    /**
     * @param text The text to set as button text.
     */
    public void setButtonText(final String text) {
        m_button.setText(text);
    }
    
    /**
     * @param text The text to set as label text.
     */
    public void setLabelText(final String text) {
        m_label.setText(text);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_button.setEnabled(enabled);
        m_label.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_button.setToolTipText(text);
        m_label.setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // Nothing to do ...
    }
}

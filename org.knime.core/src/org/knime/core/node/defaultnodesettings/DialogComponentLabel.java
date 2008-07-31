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

import java.awt.FlowLayout;

import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provides a component containing a label. The label's text can be set 
 * individually. A model is not needed, since no input component like a 
 * text field etc. is provided here. Thus no setting values will be saved. 
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class DialogComponentLabel extends DialogComponent {

    private JLabel m_label;
    
    /**
     * Creates new instance of <code>DialogComponentLabel</code> with given
     * label to set.
     * 
     * @param label The label to set.
     */
    public DialogComponentLabel(final String label) {
        super(new EmptySettingsModel());
        
        m_label = new JLabel(label);
        getComponentPanel().setLayout(new FlowLayout());
        getComponentPanel().add(m_label);
    }
    
    /**
     * Sets the given text.
     * 
     * @param text The text to set.
     */
    public void setText(final String text) {
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
        m_label.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
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

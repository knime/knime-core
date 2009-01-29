/*
 * -------------------------------------------------------------------
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
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 *   25.09.2006 (ohl): using SettingsModel
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provides a standard component for a dialog that allows to edit a boolean
 * value. Provides a checkbox as well as functionality to load/store the value
 * into a config object.
 *
 * @author M. Berthold, University of Konstanz
 */
public final class DialogComponentBoolean extends DialogComponent {
    private final JCheckBox m_checkbox;

    /**
     * Constructor puts a checkbox with the specified label into the panel.
     *
     * @param booleanModel an already created settings model
     * @param label the label for checkbox.
     */
    public DialogComponentBoolean(final SettingsModelBoolean booleanModel,
            final String label) {
        super(booleanModel);

        m_checkbox = new JCheckBox(label);
        m_checkbox.setSelected(booleanModel.getBooleanValue());
        // update the model, if the user changes the component
        m_checkbox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final SettingsModelBoolean model =
                    (SettingsModelBoolean)getModel();
                model.setBooleanValue(m_checkbox.isSelected());
            }
        });

        // update the checkbox, whenever the model changes - make sure we get
        // notified first.
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        getComponentPanel().add(m_checkbox);
        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // only update component if values are off
        final SettingsModelBoolean model = (SettingsModelBoolean)getModel();
        setEnabledComponents(model.isEnabled());
        if (model.getBooleanValue() != m_checkbox.isSelected()) {
            m_checkbox.setSelected(model.getBooleanValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we're always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_checkbox.setEnabled(enabled);
    }

    /**
     * @return <code>true</code> if the checkbox is selected,
     *         <code>false</code> otherwise
     */
    public boolean isSelected() {
        return m_checkbox.isSelected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_checkbox.setToolTipText(text);
    }
}

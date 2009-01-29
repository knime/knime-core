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
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 *
 * @author Thomas Gabriel, University of Konstanz
 *
 */
public final class DialogComponentString extends DialogComponent {

    // the min/max and default width of the editfield, if not set explicitly
    private static final int FIELD_MINWIDTH = 5;

    private static final int FIELD_DEFWIDTH = 15;

    private static final int FIELD_MAXWIDTH = 30;

    private final JTextField m_valueField;

    private final JLabel m_label;

    private final boolean m_disallowEmtpy;

    /**
     * Constructor put label and JTextField into panel. It will accept empty
     * strings as legal input.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of JTextField
     */
    public DialogComponentString(final SettingsModelString stringModel,
            final String label) {
        this(stringModel, label, false, calcDefaultWidth(stringModel
                .getStringValue()));
    }

    /**
     * Constructor put label and JTextField into panel.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of JTextField
     * @param disallowEmptyString if set true, the component request a non-empty
     *            string from the user.
     * @param compWidth the width of the edit field (in columns/characters)
     */
    public DialogComponentString(final SettingsModelString stringModel,
            final String label, final boolean disallowEmptyString,
            final int compWidth) {
        super(stringModel);

        m_disallowEmtpy = disallowEmptyString;

        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_valueField = new JTextField();
        m_valueField.setColumns(compWidth);

        m_valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }
        });

        // update the text field, whenever the model changes
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        getComponentPanel().add(m_valueField);

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * @param defaultValue the default value in the component
     * @return the width of the spinner, derived from the defaultValue.
     */
    private static int calcDefaultWidth(final String defaultValue) {
        if ((defaultValue == null) || (defaultValue.length() == 0)) {
            // no default value, return the default width of 15
            return FIELD_DEFWIDTH;
        }
        if (defaultValue.length() < FIELD_MINWIDTH) {
            // the editfield should be at least 15 columns wide
            return FIELD_MINWIDTH;
        }
        if (defaultValue.length() > FIELD_MAXWIDTH) {
            return FIELD_MAXWIDTH;
        }
        return defaultValue.length();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        clearError(m_valueField);

        // update component only if values are out of sync
        final String str = ((SettingsModelString)getModel()).getStringValue();
        if (!m_valueField.getText().equals(str)) {
            m_valueField.setText(str);
        }

        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     *
     * @throws InvalidSettingsException if the string was not accepted.
     */
    private void updateModel() throws InvalidSettingsException {
        if (m_disallowEmtpy
                && ((m_valueField.getText() == null) || (m_valueField.getText()
                        .length() == 0))) {
            showError(m_valueField);
            throw new InvalidSettingsException("Please enter a string value.");
        }

        // we transfer the value from the field into the model
        ((SettingsModelString)getModel())
                .setStringValue(m_valueField.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_valueField.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_valueField.setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_valueField.setToolTipText(text);
    }

}

/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provide a standard component for a dialog that allows to edit a text field.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class DialogComponentString extends DialogComponent {

    private final JTextField m_valueField;

    private final boolean m_disallowEmtpy;

    /**
     * Constructor put label and JTextField into panel. It will accept empty
     * strings as legal input.
     * 
     * @param label label for dialog in front of JTextField
     * @param stringModel the model that stores the value for this component.
     */
    public DialogComponentString(final SettingsModelString stringModel,
            final String label) {
        this(stringModel, label, false);
    }

    /**
     * Constructor put label and JTextField into panel.
     * 
     * @param label label for dialog in front of JTextField
     * @param stringModel the model that stores the value for this component.
     * @param disallowEmptyString if set true, the component request a non-empty
     *            string from the user.
     */
    public DialogComponentString(final SettingsModelString stringModel,
            final String label, final boolean disallowEmptyString) {
        super(stringModel);

        m_disallowEmtpy = disallowEmptyString;

        this.add(new JLabel(label));
        m_valueField = new JTextField();
        m_valueField.setColumns(30);

        m_valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // Ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
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

        this.add(m_valueField);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        // update copmonent only if values are out of sync
        final String str = ((SettingsModelString)getModel()).getStringValue();
        if (!m_valueField.getText().equals(str)) {
            m_valueField.setText(str);
        }
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
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * @see DialogComponent #setEnabledComponents(boolean)
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

}

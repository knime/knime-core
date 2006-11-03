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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

        // we are not updating the settings model when the field content
        // changes, we set the model value right before save.

        // update the text field, whenever the model changes
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final String str =
                        ((SettingsModelString)getModel()).getStringValue();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        m_valueField.setText(str);
                    }
                });
            }
        });

        this.add(m_valueField);
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
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
    public void setEnabledComponents(final boolean enabled) {
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

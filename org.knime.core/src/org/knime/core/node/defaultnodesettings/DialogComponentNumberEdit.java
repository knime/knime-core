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
 *   2006-05-26 (tm): reviewed
 *   25.09.2006 (ohl): using SettingsModel
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
 * Provides a standard component for a dialog that allows to edit a number
 * value. Provides label and {@link javax.swing.JFormattedTextField} that checks
 * ranges as well as functionality to load/store into config object. The kind of
 * number the component accepts depends on the {@link SettingsModel} passed to
 * the constructor (currently doubles or integers).
 * 
 * @author Giuseppe Di Fatta, University of Konstanz
 * 
 */
public class DialogComponentNumberEdit extends DialogComponent {

    private final JTextField m_valueField;

    /**
     * Constructor that puts label and JTextField into panel.
     * 
     * @param numberModel the model handling the value
     * @param label text to be displayed in front of the edit box
     */
    public DialogComponentNumberEdit(final SettingsModelNumber numberModel,
            final String label) {
        super(numberModel);

        this.add(new JLabel(label));
        m_valueField = new JTextField();
        m_valueField.setText(numberModel.getNumberValueStr());
        m_valueField.setColumns(6);
        this.add(m_valueField);

        m_valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException ise) {
                    // ignore it here.
                }
            }
        });

        // update the editField, whenever the model changed
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        // update component only if its out of sync with model
        SettingsModelNumber model = (SettingsModelNumber)getModel();
        String compString = m_valueField.getText();
        if (!model.getNumberValueStr().equals(compString)) {
            m_valueField.setText(model.getNumberValueStr());
        }
    }

    /**
     * Transfers the new value from the component into the model. Colors the
     * textfield red if the entered value is invalid and throws an exception.
     * 
     * @throws InvalidSettingsException if the entered value is not acceptable.
     */
    private void updateModel() throws InvalidSettingsException {
        try {
            // update the model
            ((SettingsModelNumber)getModel()).setNumberValueStr(m_valueField
                    .getText());
        } catch (Exception e) {
            // an exception will fly if the entered value is not a double or
            // is out of bounds, or whatever the model has to tell us
            showError(m_valueField);
            throw new InvalidSettingsException(e.getMessage());
        }

        if (m_valueField.getText() == "") {
            // user must enter a value
            showError(m_valueField);
            throw new InvalidSettingsException("Please enter a value.");
        }
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        // make sure the component contains a valid value
        updateModel();
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    /**
     * @see DialogComponent#setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_valueField.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     * 
     * @param width the width
     * @param height the height
     */
    public void setSizeComponents(final int width, final int height) {
        m_valueField.setPreferredSize(new Dimension(width, height));
    }

}

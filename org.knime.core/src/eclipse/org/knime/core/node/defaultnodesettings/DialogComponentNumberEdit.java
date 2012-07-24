/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

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

    /*
     * the minimum and default width of the text field, if not set otherwise.
     */
    private static final int FIELD_MINWIDTH = 2;

    private static final int FIELD_DEFWIDTH = 10;

    private final JTextField m_valueField;

    private final FlowVariableModelButton m_fvmButton;

    private final JLabel m_label;

    /**
     * Constructor that puts label and JTextField into panel.
     *
     * @param numberModel the model handling the value
     * @param label text to be displayed in front of the edit box
     */
    public DialogComponentNumberEdit(final SettingsModelNumber numberModel,
            final String label) {
        this(numberModel, label, null);
    }

    /**
     * Constructor that puts label and JTextField into panel.
     * It also enables the definition of a flow variable model to overwrite
     * the user setting using a custom variable.
     *
     * @param numberModel the model handling the value
     * @param label text to be displayed in front of the edit box
     * @param fvm A variable model or null. (If not null, a small button
     * opening an input dialog is added.)
     */
    public DialogComponentNumberEdit(final SettingsModelNumber numberModel,
            final String label, final FlowVariableModel fvm) {
        this(numberModel, label, calcDefaultWidth(numberModel
                .getNumberValueStr()), fvm);
    }

    /**
     * Constructor that puts label and JTextField into panel.
     *
     * @param numberModel the model handling the value
     * @param label text to be displayed in front of the edit box
     * @param compWidth the width (in columns/characters) of the edit field.
     */
    public DialogComponentNumberEdit(final SettingsModelNumber numberModel,
            final String label, final int compWidth) {
        this(numberModel, label, compWidth, null);
    }

    /**
     * Constructor that puts label and JTextField into panel.
     * It also enables the definition of a flow variable model to overwrite
     * the user setting using a custom variable.
     *
     * @param numberModel the model handling the value
     * @param label text to be displayed in front of the edit box
     * @param compWidth the width (in columns/characters) of the edit field.
     * @param fvm A variable model or null. (If not null, a small button
     * opening an input dialog is added.)
     */
    public DialogComponentNumberEdit(final SettingsModelNumber numberModel,
            final String label, final int compWidth,
            final FlowVariableModel fvm) {
        super(numberModel);

        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_valueField = new JTextField();
        final String defValue = numberModel.getNumberValueStr();
        m_valueField.setText(defValue);
        m_valueField.setColumns(compWidth);
        getComponentPanel().add(m_valueField);

        m_valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // ignore it here.
                }
            }

            public void insertUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // ignore it here.
                }
            }

            public void changedUpdate(final DocumentEvent e) {
                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
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

        // add variable editor button if so desired
        if (fvm != null) {
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    m_valueField.setEnabled(
                            !fvm.isVariableReplacementEnabled());
                }
            });
            m_fvmButton = new FlowVariableModelButton(fvm);
            getComponentPanel().add(m_fvmButton);
        } else {
            m_fvmButton = null;
        }

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * @param defaultValue the default value in the component
     * @return the width of the spinner, derived from the defaultValue.
     */
    private static int calcDefaultWidth(final String defaultValue) {
        if ((defaultValue == null) || (defaultValue.length() == 0)) {
            // no default value, return the default width of 10
            return FIELD_DEFWIDTH;
        }
        if (defaultValue.length() < FIELD_MINWIDTH) {
            // spinner should be at least 2 columns wide.
            return FIELD_MINWIDTH;
        }
        return defaultValue.length();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        clearError(m_valueField);

        // update component only if its out of sync with model
        final SettingsModelNumber model = (SettingsModelNumber)getModel();
        final String compString = m_valueField.getText();
        if (!model.getNumberValueStr().equals(compString)) {
            m_valueField.setText(model.getNumberValueStr());
        }

        setEnabledComponents(model.isEnabled());
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
        } catch (final Exception e) {
            // an exception will fly if the entered value is not a double or
            // is out of bounds, or whatever the model has to tell us
            showError(m_valueField);
            throw new InvalidSettingsException(e.getMessage());
        }

        if ("".equals(m_valueField.getText())) {
            // user must enter a value
            showError(m_valueField);
            throw new InvalidSettingsException("Please enter a value.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // make sure the component contains a valid value
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        boolean valueFieldEnabled = enabled;
        // enable the spinner according to the variable model
        if (m_fvmButton != null) {
            FlowVariableModel svmModel = m_fvmButton.getFlowVariableModel();
            if (svmModel.isVariableReplacementEnabled()) {
                valueFieldEnabled = false;
            }
            m_fvmButton.setEnabled(enabled);
        }
        m_valueField.setEnabled(valueFieldEnabled);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_valueField.setToolTipText(text);
    }

}

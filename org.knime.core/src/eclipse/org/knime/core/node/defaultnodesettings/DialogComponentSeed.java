/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.defaultnodesettings;

import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JSpinner.DefaultEditor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This Dialog Component offers a editable long field and
 * a checkbox for disabling the use of the checkbox as well as a button to create
 * a new long value.
 * It's main use-case is the creation of random seeds.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 */
public class DialogComponentSeed extends DialogComponent {

    private static final String BUTTON_LABEL = "New";

    private final JFormattedTextField m_longField;
    private final JButton m_drawNewButton;
    private final JCheckBox m_enableLongCheckBox;


    /**
     * Constructor puts a label and a checkbox into a panel.
     *
     * @param numberModel the SettingsModel
     * @param label label for dialog in front of the spinner
     */
    public DialogComponentSeed(
            final SettingsModelSeed numberModel,
            final String label) {
        this(numberModel, label, getDefaultWidth());
    }


    /**
     * Constructor puts label and checkbox into panel and allows to specify
     * width (in #characters) of component.
     *
     * @param numberModel the SettingsModel
     * @param label label for dialog in front of the spinner
     * @param compWidth the width (number of columns/characters) of the spinner
     */
    public DialogComponentSeed(
            final SettingsModelSeed numberModel,
            final String label, final int compWidth) {
        super(numberModel);

        if (compWidth < 1) {
            throw new IllegalArgumentException("Width of component can't be "
                    + "smaller than 1");
        }

        m_longField = new JFormattedTextField(new AbstractFormatter() {
            /**
             * The serial Version ID.
             */
          private static final long serialVersionUID =
               -9216404673651012759L;
               /**
             * {@inheritDoc}
             */
            @Override
            public Object stringToValue(
                    final String text) throws ParseException {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Contains non-numeric chars", 0);
                }
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String valueToString(
                    final Object value) throws ParseException {
                return value == null ? null : value.toString();
            }
        });
        m_longField.setColumns(compWidth);


        m_drawNewButton = new JButton(BUTTON_LABEL);
        m_drawNewButton.addActionListener(e -> {
               long l1 = Double.doubleToLongBits(Math.random());
               long l2 = Double.doubleToLongBits(Math.random());
               long l = ((0xFFFFFFFFL & l1) << 32)
                   + (0xFFFFFFFFL & l2);
               m_longField.setText(Long.toString(l));
        });

        m_enableLongCheckBox = new JCheckBox(label);
        m_enableLongCheckBox.addItemListener(e -> {
                checkEnableState();
                try {
                    updateModel();
                } catch (InvalidSettingsException e1) {
                    // ignore it here
                }
        });
        getComponentPanel().add(m_enableLongCheckBox);
        getComponentPanel().add(m_longField);
        getComponentPanel().add(m_drawNewButton);

        m_longField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        m_longField.setColumns(compWidth);
        // We are not updating the model immediately when the user changes
        // the value. We update the model right before save.
        m_longField.addActionListener(e -> {

                try {
                    updateModel();
                } catch (final InvalidSettingsException ise) {
                    // ignore it here.
                }

        });


        // update the values, whenever the model changed
        getModel().addChangeListener(e -> updateComponent());

        // call this method to be in sync with the settings model
        updateComponent();
    }

    private void checkEnableState() {
        boolean enabled = m_enableLongCheckBox.isSelected();
        m_drawNewButton.setEnabled(enabled);
        m_longField.setEnabled(enabled);
    }

    /**
     * @return the width of the label, needed for a long.
     */
    private static int getDefaultWidth() {
        String value = Long.toString(Long.MAX_VALUE);
        return value.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void updateComponent() {

        final JComponent editor = m_longField;
        if (editor instanceof DefaultEditor) {
            clearError(((DefaultEditor)editor).getTextField());
        }

        // update the component only if it contains a different value than the
        // model
        try {
            m_longField.commitEdit();
            SettingsModelSeed model =
                    (SettingsModelSeed)getModel();
            long val = Long.parseLong(m_longField.getText());
            boolean b = m_enableLongCheckBox.isSelected();
            if (val != model.getLongValue()) {
                m_longField.setValue((Long.valueOf(model.getLongValue())).toString());
            }
            if (b != model.getIsActive()) {
                m_enableLongCheckBox.setSelected(model.getIsActive());
                checkEnableState();
            }
        } catch (final ParseException e) {
            // contains invalid value - update component!
            SettingsModelSeed model =
                    (SettingsModelSeed)getModel();
            m_longField.setValue(Long.toString(model.getLongValue()));
            m_enableLongCheckBox.setSelected(model.getIsActive());
            checkEnableState();
        }

        // also update the enable status of all components...
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the value from the spinner into the model. Colors the spinner
     * red, if the number is not accepted by the settings model. And throws an
     * exception then.
     *
     * @throws InvalidSettingsException if the number was not accepted by the
     *             model (reason could be an out of range, or just an invalid
     *             input).
     *
     */
    private void updateModel() throws InvalidSettingsException {
        SettingsModelSeed model
                    = (SettingsModelSeed)getModel();
        try {
            m_longField.commitEdit();
            String longtext = m_longField.getText();
            long l = Long.parseLong(longtext);
            model.setLongValue(l);
            model.setIsActive(m_enableLongCheckBox.isSelected());

        } catch (final ParseException e) {
            final JComponent editor = m_longField;
            if (editor instanceof DefaultEditor) {
                showError(((DefaultEditor)editor).getTextField());
            }
            String errMsg = "Invalid number format. ";
            errMsg += "Please enter a number.";

            throw new InvalidSettingsException(errMsg, e);
        }
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
        // nothing needed
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        boolean allEnabled = enabled;


        m_enableLongCheckBox.setEnabled(allEnabled);
        allEnabled = enabled && m_enableLongCheckBox.isSelected();
        m_longField.setEnabled(allEnabled);
        m_drawNewButton.setEnabled(allEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_longField.setToolTipText(text);
        m_drawNewButton.setToolTipText(text);
        m_enableLongCheckBox.setToolTipText(text);
    }

}

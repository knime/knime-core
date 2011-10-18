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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This Dialog Component offers an editable string field and
 * a checkbox for disabling the use of the field.
 *
 * @author Adae, University of Konstanz
 */
public class DialogComponentOptionalString extends DialogComponent {



    private final JTextField m_stringField;
    private final JCheckBox m_enableBox;


    /**
     * Constructor puts a label and a checkbox into a panel.
     *
     * @param model the SettingsModel
     * @param label label for dialog in front of the edit field
     */
    public DialogComponentOptionalString(
            final SettingsModelOptionalString model,
            final String label) {
        this(model, label, getDefaultWidth());
    }


    /**
     * Constructor puts label and checkbox into panel and allows to specify
     * width (in #characters) of component.
     *
     * @param model the SettingsModel
     * @param label label for dialog in front of the spinner
     * @param compWidth the width (number of columns/characters) of the spinner
     */
    public DialogComponentOptionalString(
            final SettingsModelOptionalString model,
            final String label, final int compWidth) {
        super(model);

        if (compWidth < 1) {
            throw new IllegalArgumentException("Width of component can't be "
                    + "smaller than 1");
        }

        m_stringField = new JTextField();
        m_stringField.setColumns(compWidth);

        m_enableBox = new JCheckBox(label);
        m_enableBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                checkEnableState();
                updateModel();
            }
        });
        getComponentPanel().add(m_enableBox);
        getComponentPanel().add(m_stringField);

        m_stringField.setColumns(compWidth);
        // We are not updating the model immediately when the user changes
        // the value. We update the model right before save.
        m_stringField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateModel();
            }
        });

        // update the values, whenever the model changed
        getModel().addChangeListener(
                new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        // call this method to be in sync with the settings model
        updateComponent();
    }

    private void checkEnableState() {
        boolean enabled = m_enableBox.isSelected();
        m_stringField.setEnabled(enabled);
    }

    /**
     * @return the default width of the label.
     */
    private static int getDefaultWidth() {
        return 15;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        // update the component only if it contains a different value than the
        // model
        SettingsModelOptionalString model =
                (SettingsModelOptionalString)getModel();
        String val = m_stringField.getText();
        boolean b = m_enableBox.isSelected();
        if (!val.equals(model.getStringValue())) {
            m_stringField.setText(model.getStringValue());
        }
        if (b != model.isActive()) {
            m_enableBox.setSelected(model.isActive());
            checkEnableState();
        }

        // also update the enable status of all components...
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the value from the edit field and the checkbox into the model.
     */
    private void updateModel() {
        SettingsModelOptionalString model =
                (SettingsModelOptionalString)getModel();
        String text = m_stringField.getText();

        model.setStringValue(text);
        model.setIsActive(m_enableBox.isSelected());

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
        m_enableBox.setEnabled(allEnabled);
        allEnabled = enabled && m_enableBox.isSelected();
        m_stringField.setEnabled(allEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_stringField.setToolTipText(text);
        m_enableBox.setToolTipText(text);
    }

}

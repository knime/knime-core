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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * Provide a standard component for a dialog that allows to select a string from
 * a list of strings.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class DialogComponentStringSelection extends DialogComponent {

    private JComboBox m_combobox;

    private final JLabel m_label;
    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     * 
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list (not empty) of strings (not null) for the combobox
     * 
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final List<String> list) {
        super(stringModel);

        if ((list == null) || (list.size() == 0)) {
            throw new IllegalArgumentException("Selection list of strings "
                    + "shouldn't be null or empty");
        }

        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_combobox = new JComboBox();

        for (String s : list) {
            if (s == null) {
                throw new NullPointerException("Strings in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(s);
        }
        getComponentPanel().add(m_combobox);

        m_combobox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    try {
                        updateModel();
                    } catch (InvalidSettingsException ise) {
                        // ignore it here
                    }
                }
            }
        });

        // we need to update the selection, when the model changes.
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
        String val = ((SettingsModelString)getModel()).getStringValue();
        boolean update;
        if (val == null) {
            update = (m_combobox.getSelectedItem() == null);
        } else {
            update = val.equals(m_combobox.getSelectedItem());
        }
        if (update) {
            m_combobox.setSelectedItem(val);
        }
    }

    /**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() throws InvalidSettingsException {

        if (m_combobox.getSelectedItem() == null) {
            m_combobox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_combobox.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    m_combobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException(
                    "Please select an item from the list.");
        }
        // we transfer the value from the field into the model
        ((SettingsModelString)getModel()).setStringValue((String)m_combobox
                .getSelectedItem());

    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     * 
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list of items for the combobox
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final String... list) {
        this(stringModel, label, Arrays.asList(list));
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
        m_combobox.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     * 
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_combobox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #setToolTipText(java.lang.String)
     */
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_combobox.setToolTipText(text);
    }

}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */

package org.knime.core.node.defaultnodesettings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComboBox;
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
import org.knime.core.node.util.DefaultStringIconOption;
import org.knime.core.node.util.StringIconListCellEditor;
import org.knime.core.node.util.StringIconListCellRenderer;
import org.knime.core.node.util.StringIconOption;

/**
 * Provide a standard component for a dialog that allows to select a string from
 * a list of strings.
 *
 * @author Thomas Gabriel, University of Konstanz
 *
 */
public final class DialogComponentStringSelection extends DialogComponent {

    private final JComboBox m_combobox;

    private final JLabel m_label;

    private final FlowVariableModelButton m_fvmButton;

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
        this(stringModel, label,
                DefaultStringIconOption.createOptionArray(Arrays.asList(list)));
    }

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
            final Collection<String> list) {
        this(stringModel, label,
                DefaultStringIconOption.createOptionArray(list));
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list (not empty) of strings (not null) for the combobox
     * @param editable true if the user should be able to add a value to the
     *  combo box
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final Collection<String> list, final boolean editable) {
        this(stringModel, label, list, editable, null);
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list (not empty) of strings (not null) for the combobox
     * @param editable true if the user should be able to add a value to the
     *        combo box
     * @param fvm model exposed to choose from available flow variables
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final Collection<String> list, final boolean editable,
            final FlowVariableModel fvm) {
        this(stringModel, label,
                DefaultStringIconOption.createOptionArray(list), fvm);
        m_combobox.setEditable(editable);
        if (editable) {
            final StringIconListCellEditor editor
                = new StringIconListCellEditor();
            ((JTextField)editor.getEditorComponent()).getDocument()
                // in order to get informed about model changes...
                .addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    try {
                        m_combobox.setSelectedItem(editor.getItem());
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // Ignore it here.
                    }
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    try {
                        m_combobox.setSelectedItem(editor.getItem());
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // Ignore it here.
                    }
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    try {
                        m_combobox.setSelectedItem(editor.getItem());
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // Ignore it here.
                    }
                }
            });
            m_combobox.setEditor(editor);
        }
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list (not empty) of {@link StringIconOption}s for
     * the combobox. The text of the selected component is stored in the
     * {@link SettingsModelString}.
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final StringIconOption[] list) {
        this(stringModel, label, list, null);
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the strings of the provided list.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list list (not empty) of {@link StringIconOption}s for
     *        the combobox. The text of the selected component is stored in the
     *        {@link SettingsModelString}.
     * @param fvm model exposed to choose from available flow variables
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentStringSelection(
            final SettingsModelString stringModel, final String label,
            final StringIconOption[] list, final FlowVariableModel fvm) {
        super(stringModel);

        if ((list == null) || (list.length == 0)) {
            throw new IllegalArgumentException("Selection list of options "
                    + "shouldn't be null or empty");
        }
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_combobox = new JComboBox();
        m_combobox.setRenderer(new StringIconListCellRenderer());

        for (final StringIconOption o : list) {
            if (o == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(o);
        }

        getComponentPanel().add(m_combobox);

        m_combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    try {
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // ignore it here
                    }
                }
            }
        });

        // we need to update the selection, when the model changes.
        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        // add variable editor button if so desired
        if (fvm != null) {
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                     getModel().setEnabled(!fvm.isVariableReplacementEnabled());
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
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final String strVal =
            ((SettingsModelString)getModel()).getStringValue();
        StringIconOption val = null;
        if (strVal == null) {
            val = null;
        } else {
            for (int i = 0, length = m_combobox.getItemCount();
                i < length; i++) {
                final StringIconOption curVal =
                    (StringIconOption)m_combobox.getItemAt(i);
                if (curVal.getText().equals(strVal)) {
                    val = curVal;
                    break;
                }
            }
            if (val == null) {
                val = new DefaultStringIconOption(strVal);
            }
        }
        boolean update;
        if (val == null) {
            update = m_combobox.getSelectedItem() != null;
        } else {
            update = !val.equals(m_combobox.getSelectedItem());
        }
        if (update) {
            m_combobox.setSelectedItem(val);
        }
        // also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() throws InvalidSettingsException {

        if (m_combobox.getSelectedItem() == null) {
            m_combobox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_combobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_combobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException(
                    "Please select an item from the list.");
        }
        // we transfer the value from the field into the model
        ((SettingsModelString)getModel()).setStringValue(
                ((StringIconOption)m_combobox.getSelectedItem()).getText());
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
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_combobox.setToolTipText(text);
    }

    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new strings for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final Collection<String> newItems,
            final String select) {
        if (newItems == null || newItems.size() < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        final StringIconOption[] options =
            DefaultStringIconOption.createOptionArray(newItems);
        replaceListItems(options, select);
    }

    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new {@link StringIconOption}s for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final StringIconOption[] newItems,
            final String select) {
        if (newItems == null || newItems.length < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        final String sel;
        if (select == null) {
            sel = ((SettingsModelString)getModel()).getStringValue();
        } else {
            sel = select;
        }

        m_combobox.removeAllItems();
        StringIconOption selOption = null;
        for (final StringIconOption option : newItems) {
            if (option == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(option);
            if (option.getText().equals(sel)) {
                selOption = option;
            }
        }

        if (selOption == null) {
            m_combobox.setSelectedIndex(0);
        } else {
            m_combobox.setSelectedItem(selOption);
        }
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_combobox.setSize(m_combobox.getPreferredSize());
        getComponentPanel().validate();
    }
}

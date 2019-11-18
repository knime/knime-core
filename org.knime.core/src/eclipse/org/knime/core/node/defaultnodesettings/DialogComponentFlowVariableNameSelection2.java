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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * Provides a standard component for a dialog that allows to select a flow variable from a list of flow variables.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public final class DialogComponentFlowVariableNameSelection2 extends DialogComponent {

    private final JComboBox<FlowVariable> m_jcombobox;

    private final JLabel m_label;

    private final VariableType<?>[] m_types;

    private final boolean m_hasNone;

    /**
     * Constructor creates a label and a combobox and adds them to the component panel. The given flow variables, which
     * are of the specified types are added as items to the combobox. If no types are specified all variables will be
     * added.
     *
     * @param model The string model to store the name of the selected variable.
     * @param label The title of the label to show.
     * @param flowVars The flow variables to add to combobox.
     * @param types The types of flow variables which are added to combobox.
     */
    public DialogComponentFlowVariableNameSelection2(final SettingsModelString model, final String label,
        final Collection<FlowVariable> flowVars, final VariableType<?>... types) {
        this(model, label, flowVars, false, types);
    }

    /**
     * Constructor creates a label and a combobox and adds them to the component panel. The given flow variables, which
     * are of the specified types are added as items to the combobox. If no types are specified all variables will be
     * added.
     *
     * @param model The string model to store the name of the selected variable.
     * @param label The title of the label to show.
     * @param flowVars The flow variables to add to combobox.
     * @param hasNone if true the field is optional and can be set to "NONE"
     * @param types The types of flow variables which are added to combobox.
     *
     * @throws IllegalArgumentException Collection of FlowVariables cannot be null
     */
    public DialogComponentFlowVariableNameSelection2(final SettingsModelString model, final String label,
        final Collection<FlowVariable> flowVars, final boolean hasNone, final VariableType<?>... types) {
        super(model);
        m_hasNone = hasNone;
        if (flowVars == null) {
            throw new IllegalArgumentException("Flow Variables may not be null!");
        }

        if (label != null) {
            m_label = new JLabel(label);
            getComponentPanel().add(m_label);
        } else {
            m_label = null;
        }

        // save types, the will be needed again when items are replaced
        m_types = types;

        m_jcombobox = new JComboBox<>(getFilteredFlowVariables(flowVars));
        m_jcombobox.setRenderer(new FlowVariableListCellRenderer());
        m_jcombobox.setEditable(false);

        getComponentPanel().add(m_jcombobox);

        m_jcombobox.addItemListener(new ItemListener() {
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

        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        updateComponent();
    }

    private void updateModel() throws InvalidSettingsException {
        if (m_jcombobox.getSelectedItem() == null) {
            ((SettingsModelString)getModel()).setStringValue(null);
            m_jcombobox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_jcombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_jcombobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException("Please select an item from the list.");
        }
        // save the value of the flow variable into the model
        ((SettingsModelString)getModel()).setStringValue(((FlowVariable)m_jcombobox.getSelectedItem()).getName());
    }

    @Override
    protected void updateComponent() {
        final String strVal = ((SettingsModelString)getModel()).getStringValue();
        FlowVariable val = null;
        if (strVal != null) {
            for (int i = 0, length = m_jcombobox.getItemCount(); i < length; i++) {
                final FlowVariable curVal = m_jcombobox.getItemAt(i);
                if (curVal.getName().equals(strVal)) {
                    val = curVal;
                    break;
                }
            }
            if (val == null) {
                val = new FlowVariable("NONE", "");
            }
        }
        final boolean update =
            val == null ? m_jcombobox.getSelectedItem() != null : !val.equals(m_jcombobox.getSelectedItem());
        if (update) {
            m_jcombobox.setSelectedItem(val);
        }
        // also update the enable status
        setEnabledComponents(getModel().isEnabled());

        // make sure the model is in sync (in case model value isn't selected)
        final FlowVariable selItem = (FlowVariable)m_jcombobox.getSelectedItem();

        try {
            if ((selItem == null && strVal != null) || (selItem != null && !selItem.getName().equals(strVal))) {
                // if the (initial) value in the model is not in the list
                updateModel();
            }
        } catch (InvalidSettingsException e) {
            // ignore invalid values here
        }
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // Nothing to do ...
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_jcombobox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_jcombobox.setToolTipText(text);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_jcombobox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * Replaces the list of selectable flow variables in the component. If <code>select</code> is specified (not null)
     * and it exists in the collection it will be selected. If <code>select</code> is null, the previous value will stay
     * selected (if it exists in the new list).
     *
     * @param newItems new flow variables for the combo box
     * @param select the item to select after the replace. Can be null, in which case the previous selection remains -
     *            if it exists in the new list.
     * @throws IllegalArgumentException if set of flow variables is null or empty
     */
    public void replaceListItems(final Collection<FlowVariable> newItems, final String select) {

        final String sel;
        if (select == null) {
            sel = ((SettingsModelString)getModel()).getStringValue();
        } else {
            sel = select;
        }

        m_jcombobox.removeAllItems();
        if (m_hasNone) {
            m_jcombobox.addItem(new FlowVariable("NONE", " "));
        }

        if (newItems == null || newItems.size() < 1) {
            if (!m_hasNone) {
                throw new IllegalArgumentException("The container with the new items can't be null or empty.");
            }
        } else {
            Vector<FlowVariable> filteredItems = getFilteredFlowVariables(newItems);

            FlowVariable selOption = null;
            for (final FlowVariable option : filteredItems) {
                if (option == null) {
                    throw new NullPointerException("Options in the selection list can't be null");
                }
                m_jcombobox.addItem(option);
                if (option.getName().equals(sel)) {
                    selOption = option;
                }
            }

            if (selOption == null) {
                m_jcombobox.setSelectedIndex(0);
            } else {
                m_jcombobox.setSelectedItem(selOption);
            }
        }
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_jcombobox.setSize(m_jcombobox.getPreferredSize());
        getComponentPanel().validate();
    }

    /**
     * Checks if the given flow variable is of the same type of any of the specified types are returns <code>true</code>
     * if so, otherwise <code>false</code>.
     *
     * @param var The flow variable to check
     * @param types the valid types.
     * @return <code>true</code> if flow variable is of any of the specified types, otherwise <code>false</code>.
     */
    private boolean isFlowVariableCompatible(final FlowVariable var) {
        if (m_types != null) {
            for (VariableType<?> type : m_types) {
                if (var.getVariableType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a vector of the given flow variables that match any of the specified types.
     *
     * @param flowVars The flow variables to filter.
     * @return The vector of filtered flow variables.
     */
    private Vector<FlowVariable> getFilteredFlowVariables(final Collection<FlowVariable> flowVars) {
        final Vector<FlowVariable> flowVarsAsVector = new Vector<>(flowVars.size());
        if (m_types == null || m_types.length <= 0) {
            flowVarsAsVector.addAll(flowVars);
        } else {
            for (FlowVariable var : flowVars) {
                if (isFlowVariableCompatible(var)) {
                    flowVarsAsVector.add(var);
                }
            }
        }
        return flowVarsAsVector;
    }
}

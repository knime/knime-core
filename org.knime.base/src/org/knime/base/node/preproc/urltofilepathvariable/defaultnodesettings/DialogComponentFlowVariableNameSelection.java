/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   30.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepathvariable.defaultnodesettings;

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
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Provides a standard component for a dialog that allows to select a flow 
 * variable from a list of flow variables.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
public final class DialogComponentFlowVariableNameSelection 
extends DialogComponent {

    private JComboBox m_jcombobox;
    
    private JLabel m_label;
    
    private FlowVariable.Type[] m_flowVarTypes;
        
    /**
     * Constructor creates a label and a combobox and adds them to the 
     * component panel. The given flow variables, which are of the specified 
     * types are added as items to the combobox. If no types are specified
     * all variables will be added.
     * 
     * @param model The string model to store the name of the selected variable.
     * @param label The title of the label to show.
     * @param flowVars The flow variables to add to combobox.
     * @param flowVarTypes The types of flow variables which are added to 
     * combobox.
     */
    public DialogComponentFlowVariableNameSelection(
            final SettingsModelString model, final String label,
            final Collection<FlowVariable> flowVars, 
            final FlowVariable.Type... flowVarTypes) {
        super(model);
        if (flowVars == null) {
            throw new NullPointerException("Flow Variables may not be null!");
        }
        
        if (label != null) {
            m_label = new JLabel(label);
            getComponentPanel().add(m_label);
        }
        
        // save types, the will be needed again when items are replaced
        m_flowVarTypes = flowVarTypes;
        
        m_jcombobox = new JComboBox(getFilteredFlowVariables(flowVars));
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

        // we need to update the selection, when the model changes.
        // TODO in other dialog components instead of addChangeListener
        // prependChangeListener is called, why? Should it be called here too?
        // Problem is: prependChangeListener is only package visible :-(
        getModel().addChangeListener(new ChangeListener() {
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
            throw new InvalidSettingsException(
                    "Please select an item from the list.");
        }
        // save the value of the flow variable into the model
        ((SettingsModelString)getModel()).setStringValue(
                ((FlowVariable)m_jcombobox.getSelectedItem()).getName());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final String strVal =
            ((SettingsModelString)getModel()).getStringValue();
        FlowVariable val = null;
        if (strVal == null) {
            val = null;
        } else {
            for (int i = 0, length = m_jcombobox.getItemCount();
                i < length; i++) {
                final FlowVariable curVal =
                    (FlowVariable)m_jcombobox.getItemAt(i);
                if (curVal.getName().equals(strVal)) {
                    val = curVal;
                    break;
                }
            }
            if (val == null) {
                val = new FlowVariable("", "");
            }
        }
        boolean update;
        if (val == null) {
            update = m_jcombobox.getSelectedItem() != null;
        } else {
            update = !val.equals(m_jcombobox.getSelectedItem());
        }
        if (update) {
            m_jcombobox.setSelectedItem(val);
        }
        // also update the enable status
        setEnabledComponents(getModel().isEnabled());

        // make sure the model is in sync (in case model value isn't selected)
        FlowVariable selItem =
            (FlowVariable)m_jcombobox.getSelectedItem();
        try {
            if ((selItem == null && strVal != null)
                    || (selItem != null && !selItem.getName().equals(strVal))) {
                // if the (initial) value in the model is not in the list
                updateModel();
            }
        } catch (InvalidSettingsException e) {
            // ignore invalid values here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws 
    InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_jcombobox.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
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
     * Replaces the list of selectable flow variables in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new flow variables for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final Collection<FlowVariable> newItems,
            final String select) {        
        if (newItems == null || newItems.size() < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        Vector<FlowVariable> filteredItems = getFilteredFlowVariables(newItems);
        
        final String sel;
        if (select == null) {
            sel = ((SettingsModelString)getModel()).getStringValue();
        } else {
            sel = select;
        }

        m_jcombobox.removeAllItems();
        FlowVariable selOption = null;
        for (final FlowVariable option : filteredItems) {
            if (option == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
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
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_jcombobox.setSize(m_jcombobox.getPreferredSize());
        getComponentPanel().validate();
    }
    
    /**
     * Checks if the given flow variable is of the same type of any of the
     * specified types are returns <code>true</code> if so, otherwise
     * <code>false</code>.
     * 
     * @param var The flow variable to check
     * @param flowVarTypes the valid types.
     * @return <code>true</code> if flow variable is of any of the specified 
     * types, otherwise <code>false</code>.
     */
    private boolean isFlowVariableCompatible(final FlowVariable var, 
            final FlowVariable.Type ...flowVarTypes) {
        if (flowVarTypes != null) {
            for (FlowVariable.Type type : flowVarTypes) {
                if (var.getType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns a vector of the given flow variables that match any of the
     * specified types.
     * 
     * @param flowVars The flow variables to filter.
     * @return The vector of filtered flow variables.
     */
    private Vector<FlowVariable> getFilteredFlowVariables(
            final Collection<FlowVariable> flowVars) {
        Vector<FlowVariable> flowVarsAsVector = 
            new Vector<FlowVariable>(flowVars.size());
        if (m_flowVarTypes == null || m_flowVarTypes.length <= 0) {
            flowVarsAsVector.addAll(flowVars);
        } else {
            for (FlowVariable var : flowVars) {
                if (isFlowVariableCompatible(var, m_flowVarTypes)) {
                    flowVarsAsVector.add(var);
                }
            }
        }
        return flowVarsAsVector;
    }    
}

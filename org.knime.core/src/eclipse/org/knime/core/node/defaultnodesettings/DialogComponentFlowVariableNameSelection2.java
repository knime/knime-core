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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer.FlowVariableCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.VariableType;

/**
 * Provides a standard component for a dialog that allows to select a flow variable from a list of flow variables.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public final class DialogComponentFlowVariableNameSelection2 extends DialogComponent {

    private final JComboBox<FlowVariableCell> m_jcombobox;

    private final ItemListener m_listener;

    private final boolean m_hasNone;

    private final Supplier<Map<String, FlowVariable>> m_getAvailableFlowVariables;

    private boolean m_selectionIsValid = false;

    /**
     * Constructor creates a label and a combobox and adds them to the component panel. The given flow variables, which
     * are of the specified types are added as items to the combobox. If no types are specified all variables will be
     * added.
     *
     * @param model The string model to store the name of the selected variable
     * @param label The title of the label to show
     * @param getAvailableFlowVariables reference to the invoking NodeDialogPane's
     *            {@link NodeDialogPane#getAvailableFlowVariables(VariableType[]) getAvailableFlowVariables} method
     */
    public DialogComponentFlowVariableNameSelection2(final SettingsModelString model, final String label,
        final Supplier<Map<String, FlowVariable>> getAvailableFlowVariables) {
        this(model, label, getAvailableFlowVariables, false);
    }

    /**
     * Constructor creates a label and a combobox and adds them to the component panel. The given flow variables, which
     * are of the specified types are added as items to the combobox. If no types are specified all variables will be
     * added.
     *
     * @param model The string model to store the name of the selected variable
     * @param label The title of the label to show
     * @param getAvailableFlowVariables reference to the invoking NodeDialogPane's
     *            {@link NodeDialogPane#getAvailableFlowVariables(VariableType[]) getAvailableFlowVariables} method
     * @param hasNone if true the field is optional and can be set to "NONE"
     */
    public DialogComponentFlowVariableNameSelection2(final SettingsModelString model, final String label,
        final Supplier<Map<String, FlowVariable>> getAvailableFlowVariables, final boolean hasNone) {
        super(model);

        m_getAvailableFlowVariables = CheckUtils.checkArgumentNotNull(getAvailableFlowVariables);
        m_hasNone = hasNone;

        if (label != null) {
            getComponentPanel().add(new JLabel(label));
        }

        m_listener = new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    try {
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                    }
                }
            }
        };

        m_jcombobox = new JComboBox<>();
        m_jcombobox.setRenderer(new FlowVariableListCellRenderer());
        m_jcombobox.setEditable(false);
        m_jcombobox.addItemListener(m_listener);
        getComponentPanel().add(m_jcombobox);

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
        } else {
            // save the value of the flow variable into the model
            ((SettingsModelString)getModel()).setStringValue(((FlowVariableCell)m_jcombobox.getSelectedItem()).getName());
        }
    }

    @Override
    protected void updateComponent() {
        final String selection = ((SettingsModelString)getModel()).getStringValue();
        final List<FlowVariableCell> newVars =
            m_getAvailableFlowVariables.get().values().stream().filter(v -> (v.getScope() == Scope.Flow))
                .map(FlowVariableCell::new).collect(Collectors.toCollection(ArrayList::new));
        if (m_hasNone) {
            newVars.add(new FlowVariableCell(new FlowVariable("NONE", "")));
        }

        m_jcombobox.removeItemListener(m_listener);
        m_jcombobox.removeAllItems();
        m_selectionIsValid = false;
        for (FlowVariableCell var : newVars) {
            m_jcombobox.addItem(var);
            if (var.getName().equals(selection)) {
                m_jcombobox.setSelectedItem(var);
                m_selectionIsValid = true;
            }
        }

        if (!m_selectionIsValid) {
            if (selection != null && selection.length() > 0) {
                final FlowVariableCell selectedVar = new FlowVariableCell(selection);
                m_jcombobox.addItem(selectedVar);
                m_jcombobox.setSelectedItem(selectedVar);
            } else {
                m_jcombobox.setSelectedIndex(-1);
            }
        }
        m_jcombobox.addItemListener(m_listener);

        setEnabledComponents(getModel().isEnabled());
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (getModel().isEnabled() && !m_selectionIsValid) {
            throw new InvalidSettingsException("No valid flow variable selected.");
        }
        updateModel();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_jcombobox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_jcombobox.setToolTipText(text);
    }

}

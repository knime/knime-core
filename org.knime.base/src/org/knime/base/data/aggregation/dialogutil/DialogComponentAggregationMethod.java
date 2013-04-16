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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on 07.12.2012 by koetter
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * {@link DialogComponent} that allows the user to select an {@link AggregationMethod} and
 * the corresponding parameters.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.8
 */
public class DialogComponentAggregationMethod extends DialogComponent
    implements ItemListener, DocumentListener, ActionListener, ChangeListener {

    private final JComboBox m_aggregationMethod;

    private final JLabel m_label;

    private JCheckBox m_inclMissing;

    private JButton m_parameter;

    private JTextField m_maxUniqueVals;

    private JTextField m_valDelimiter;

    /**
     * @param model the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param addGlobalSetttings <code>true</code> if the user should be able to change the
     * {@link GlobalSettings}
     * @param supportedType the {@link DataType} the {@link AggregationMethod} must be compatible to
     */
    public DialogComponentAggregationMethod(final SettingsModelAggregationMethod model, final String label,
                                final boolean addGlobalSetttings, final DataType supportedType) {
        this(model, label, addGlobalSetttings,
             AggregationMethods.getCompatibleMethods(supportedType).toArray(new AggregationMethod[0]));
    }

    /**
     *
     * @param model the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param addGlobalSetttings <code>true</code> if the user should be able to change the
     * {@link GlobalSettings}
     * @param methods list (not empty) of {@link AggregationMethod}s for
     *        the combobox. The selected method is stored in the
     *        {@link SettingsModelAggregationMethod}.
     *
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentAggregationMethod(final SettingsModelAggregationMethod model, final String label,
                                final boolean addGlobalSetttings, final AggregationMethod... methods) {
        super(model);
        if ((methods == null) || (methods.length == 0)) {
            throw new IllegalArgumentException("Selection list of options "
                    + "shouldn't be null or empty");
        }
        m_label = new JLabel(label);
        m_aggregationMethod = new JComboBox();
        m_aggregationMethod.setRenderer(new AggregationMethodListCellRenderer());

        for (final AggregationMethod o : methods) {
            if (o == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_aggregationMethod.addItem(o);
        }
        m_aggregationMethod.addItemListener(this);
        // we need to update the selection, when the model changes.
        ((SettingsModelAggregationMethod)getModel()).prependChangeListener(this);

        GlobalSettings defaultSettings = GlobalSettings.DEFAULT;
        m_inclMissing = new JCheckBox("Include missing");
        m_inclMissing.addItemListener(this);
        m_parameter = new JButton("Parameter");
        m_parameter.addActionListener(this);
        m_maxUniqueVals = new JTextField(5);
        m_maxUniqueVals.setText(Integer.toString(defaultSettings.getMaxUniqueValues()));
        m_maxUniqueVals.getDocument().addDocumentListener(this);
        m_valDelimiter = new JTextField(defaultSettings.getValueDelimiter());
        m_valDelimiter.setColumns(2);
        m_valDelimiter.getDocument().addDocumentListener(this);
        m_inclMissing.addItemListener(this);

        //Layout the panel
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        int y = 0;
        final GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = y++;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(m_label, gc);
        gc.gridy = y++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        panel.add(m_aggregationMethod, gc);
        gc.gridwidth = 1;
        gc.gridy = y++;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(m_inclMissing, gc);
        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(m_parameter, gc);
        if (addGlobalSetttings) {
            gc.gridx = 0;
            gc.gridy = y++;
            gc.anchor = GridBagConstraints.LINE_END;
            panel.add(new JLabel("Maximum unique values: "), gc);
            gc.gridx = 1;
            gc.anchor = GridBagConstraints.LINE_START;
            panel.add(m_maxUniqueVals, gc);

            gc.gridx = 0;
            gc.gridy = y++;
            gc.anchor = GridBagConstraints.LINE_END;
            panel.add(new JLabel("Vallue delimitier: "), gc);
            gc.gridx = 1;
            gc.anchor = GridBagConstraints.LINE_START;
            panel.add(m_valDelimiter, gc);
        }

        //call this method to be in sync with the settings model
        updateComponent();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelAggregationMethod model = getMethodModel();
        //remove all listeners
        m_valDelimiter.getDocument().removeDocumentListener(this);
        m_maxUniqueVals.getDocument().removeDocumentListener(this);
        m_aggregationMethod.removeItemListener(this);
        m_inclMissing.removeItemListener(this);

        m_valDelimiter.setText(model.getValueDelimiter());
        m_maxUniqueVals.setText(Integer.toString(model.getMaxUniqueValues()));


        final AggregationMethod modelVal = model.getAggregationMethod();
        AggregationMethod val = null;
        if (modelVal != null) {
            for (int i = 0, length = m_aggregationMethod.getItemCount();
                i < length; i++) {
                final AggregationMethod curVal =
                    (AggregationMethod)m_aggregationMethod.getItemAt(i);
                if (curVal.equals(modelVal)) {
                    val = curVal;
                    break;
                }
            }
            if (val == null) {
                val = modelVal;
            }
        }
        boolean update;
        if (val == null) {
            update = m_aggregationMethod.getSelectedItem() != null;
        } else {
            update = !val.equals(m_aggregationMethod.getSelectedItem());
        }
        if (update) {
            m_aggregationMethod.setSelectedItem(val);
        }

        final AggregationMethod selItem = getSelectedAggregationMethod();
        boolean updateModel = (selItem == null && modelVal != null)
                || (selItem != null && !selItem.equals(modelVal));
        if (val != null) {
            //check if this has changed
            if (m_inclMissing.isSelected() != val.inclMissingCells()) {
                //it has changed set the new value and force the model update
                updateModel =  true;
                m_inclMissing.setSelected(val.inclMissingCells());
            }
        }
        // also update the enable status of all components
        setEnabledComponents(getModel().isEnabled());

        //add all listener
        m_valDelimiter.getDocument().addDocumentListener(this);
        m_maxUniqueVals.getDocument().addDocumentListener(this);
        m_aggregationMethod.addItemListener(this);
        m_inclMissing.addItemListener(this);

        // make sure the model is in sync (in case model value isn't selected)
        if (updateModel) {
            // if the (initial) value in the model is not in the list
            try {
                updateModel();
            } catch (InvalidSettingsException e) {
                // ignore it here
            }
        }
    }

    /**
     * Transfers the current value from the component into the model.
     * @throws InvalidSettingsException
     */
    private void updateModel() throws InvalidSettingsException {
        // we transfer the value from the field into the model
        final SettingsModelAggregationMethod model = getMethodModel();
        model.removeChangeListener(this);
        model.setValues(getSelectedAggregationMethod(), getValueDelimiter(), getMaxUniqueValues());
        model.prependChangeListener(this);
    }

    /**
     * @return the currently entered maximum unique values
     * @throws InvalidSettingsException if the maximum unique values are invalid
     */
    protected int getMaxUniqueValues() throws InvalidSettingsException {
        final String maxVal = m_maxUniqueVals.getText();
        try {
            final int val = Integer.parseInt(maxVal);
            if (val < 0) {
                throw new InvalidSettingsException("Maximum unique values should be positive");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException("No valid maximum unique values entered");
        }
    }

    /**
     * @return the currently entered value delimiter
     */
    protected String getValueDelimiter() {
        return m_valDelimiter.getText();
    }

    /**
     * @return the currently selected aggregation method
     */
    private AggregationMethod getSelectedAggregationMethod() {
        return (AggregationMethod)m_aggregationMethod.getSelectedItem();
    }

    /**
     * @return the {@link SettingsModelAggregationMethod} model
     */
    private SettingsModelAggregationMethod getMethodModel() {
        final SettingsModelAggregationMethod model = (SettingsModelAggregationMethod)getModel();
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        try {
            getMethodModel().validateSettingsBeforeSave();
        } catch (InvalidSettingsException e) {
            getComponentPanel().setBorder(BorderFactory.createLineBorder(Color.red));
            throw e;
        }
        getComponentPanel().setBorder(BorderFactory.createEmptyBorder());
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
        m_aggregationMethod.setEnabled(enabled);
        final AggregationMethod method = getSelectedAggregationMethod();
        m_inclMissing.setEnabled(method != null && method.supportsMissingValueOption() && enabled);
        m_parameter.setEnabled(method != null && method.hasOptionalSettings() && enabled);
        m_maxUniqueVals.setEnabled(enabled);
        m_valDelimiter.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_aggregationMethod.setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
    }

    /**
     * @param supportedType the {@link DataType} the methods must be compatible with
     */
    public void replaceListItems(final DataType supportedType) {
        final List<AggregationMethod> compatibleMethods =
                AggregationMethods.getCompatibleMethods(supportedType);
        replaceListItems(((SettingsModelAggregationMethod)getModel()).getAggregationMethod(),
                         compatibleMethods.toArray(new AggregationMethod[0]));
    }

    /**
     * @param select the selected method
     * @param newItems the list of new {@link AggregationMethod}s to select from
     */
    public void replaceListItems(final AggregationMethod select,
            final AggregationMethod... newItems) {
        if (newItems == null || newItems.length < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        final AggregationMethod sel;
        if (select == null) {
            sel = ((SettingsModelAggregationMethod)getModel()).getAggregationMethod();
        } else {
            sel = select;
        }

        m_aggregationMethod.removeItemListener(this);
        m_aggregationMethod.removeAllItems();
        AggregationMethod selOption = null;
        for (final AggregationMethod option : newItems) {
            if (option == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_aggregationMethod.addItem(option);
            if (option.equals(sel)) {
                selOption = option;
            }
        }
        m_aggregationMethod.addItemListener(this);
        if (selOption == null) {
            m_aggregationMethod.setSelectedIndex(0);
        } else {
            m_aggregationMethod.setSelectedItem(selOption);
        }
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_aggregationMethod.setSize(m_aggregationMethod.getPreferredSize());
        getComponentPanel().validate();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(final ItemEvent e) {
        //update the model
        try {
            updateModel();
            //update the enabled status especially of the parameters button
            setEnabledComponents(getModel().isEnabled());
        } catch (InvalidSettingsException e1) {
            // ignore it here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUpdate(final DocumentEvent e) {
        try {
            updateModel();
        } catch (InvalidSettingsException e1) {
            // ignore it here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertUpdate(final DocumentEvent e) {
        try {
            updateModel();
        } catch (InvalidSettingsException e1) {
            // ignore it here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changedUpdate(final DocumentEvent e) {
        try {
            updateModel();
        } catch (InvalidSettingsException e1) {
            // ignore it here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        //the user has pressed the additional parameters button
        Frame f = null;
        Container c = getComponentPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        try {
            int specIdx = getMethodModel().getInputPortIndex();
            final DataTableSpec spec;
            if (specIdx < 0 || getLastTableSpecs() == null) {
                spec = new DataTableSpec();
            } else {
                spec = (DataTableSpec)getLastTableSpec(specIdx);
            }
            final AggregationParameterDialog dialog = new AggregationParameterDialog(f,
                                           getSelectedAggregationMethod(), spec);
            //center the dialog
            dialog.setLocationRelativeTo(c);
            //show it
            dialog.setVisible(true);
        } catch (NotConfigurableException ex) {
            //show the error message
              final String erroMessage = ex.getMessage();
              JOptionPane.showMessageDialog(getComponentPanel(), erroMessage,
                                "Unable to open dialog", JOptionPane.ERROR_MESSAGE);
              return;
          }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        updateComponent();
    }
}

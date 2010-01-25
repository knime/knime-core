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
 */
package org.knime.base.node.preproc.missingval;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;


/**
 * Panel on a ColSetting object. It holds properties for missing values for one
 * individual column or all columns of one type.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class MissingValuePanel extends JPanel {
    /** Identifier for property change event when Remove was pressed. */
    public static final String REMOVE_ACTION = "remove_panel";

    private final JRadioButton m_nothingButton;

    private final JRadioButton m_removeButton;

    private final JRadioButton m_minButton;

    private final JRadioButton m_maxButton;

    private final JRadioButton m_meanButton;

    private final JRadioButton m_mostFrequentButton;
    
    private final JRadioButton m_fixButton;

    private final JComponent m_fixText;

    private final ColSetting m_setting;

    /**
     * Constructor for one individual column, invoked when Add in dialog was
     * pressed.
     * 
     * @param spec the spec to that column
     */
    public MissingValuePanel(final DataColumnSpec spec) {
        this(new ColSetting(spec), spec);
    }

    /**
     * Constructor that uses settings from <code>setting</code> given a column
     * spec or <code>null</code> if the ColSetting is a meta-config.
     * 
     * @param setting to get settings from
     * @param spec the spec of the column or <code>null</code>
     */
    public MissingValuePanel(
            final ColSetting setting, final DataColumnSpec spec) {
        super(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JPanel panel = new JPanel(new GridLayout(0, 2));
        Icon icon;
        String name;
        Border border;
        JComponent removePanel;
        if (setting.isMetaConfig()) {
            switch (setting.getType()) {
            case ColSetting.TYPE_INT:
                icon = IntCell.TYPE.getIcon();
                name = "Integer";
                border = BorderFactory.createTitledBorder("Integer Columns");
                break;
            case ColSetting.TYPE_STRING:
                icon = StringCell.TYPE.getIcon();
                name = "String";
                border = BorderFactory.createTitledBorder("String Columns");
                break;
            case ColSetting.TYPE_DOUBLE:
                icon = DoubleCell.TYPE.getIcon();
                name = "Double";
                border = BorderFactory.createTitledBorder("Double Columns");
                break;
            case ColSetting.TYPE_UNKNOWN:
                icon = DataType.getType(DataCell.class).getIcon();
                name = "Unknown";
                border = BorderFactory.createTitledBorder("Unknown Columns");
                break;
            default:
                throw new InternalError("No such type.");
            }
            removePanel = new JLabel();
        } else {
            name = setting.getName();
            if (!spec.getName().equals(name)) {
                throw new NullPointerException("Not equal on init: '" + name
                        + "' vs. '" + spec.getName() + "'");
            }
            icon = spec.getType().getIcon();
            border = BorderFactory.createLineBorder(Color.BLACK);
            JButton requestRemoveButton = new JButton("Remove");
            requestRemoveButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    firePropertyChange(REMOVE_ACTION, null, null);
                }
            });
            removePanel = new JPanel(new FlowLayout());
            removePanel.add(requestRemoveButton);
        }
        setBorder(border);
        String shortName = name;
        if (name.length() > 15) {
            shortName = name.substring(0, 14).concat("...");
        }
        JLabel label = new JLabel(shortName, icon, SwingConstants.LEFT);
        label.setToolTipText(name);
        panel.add(label);
        panel.add(removePanel);

        ButtonGroup buttonGroup = new ButtonGroup();
        ActionListener actionListener = new ButtonListener();
        // NO HANDLING Button
        m_nothingButton = new JRadioButton("Do Nothing");
        m_nothingButton.setToolTipText("No missing value handling.");
        m_nothingButton.addActionListener(actionListener);
        buttonGroup.add(m_nothingButton);
        panel.add(m_nothingButton);

        // REMOVE Button
        m_removeButton = new JRadioButton("Remove Row");
        m_removeButton.setToolTipText("Ignore rows that contain a "
                + "missing value");
        m_removeButton.addActionListener(actionListener);
        buttonGroup.add(m_removeButton);
        panel.add(m_removeButton);

        if (setting.getType() == ColSetting.TYPE_DOUBLE
                || setting.getType() == ColSetting.TYPE_INT) {
            // MIN Button
            m_minButton = new JRadioButton("Min");
            m_minButton.setToolTipText("Replaces missing values by the minimum "
                            + "of the values in a column");
            m_minButton.addActionListener(actionListener);
            buttonGroup.add(m_minButton);
            panel.add(m_minButton);

            // MAX Button
            m_maxButton = new JRadioButton("Max");
            m_maxButton.setToolTipText("Replaces missing values by the "
                            + "maximum of the values in a column");
            m_maxButton.addActionListener(actionListener);
            buttonGroup.add(m_maxButton);
            panel.add(m_maxButton);

            // MEAN Button
            m_meanButton = new JRadioButton("Mean");
            m_meanButton.setToolTipText("Replaces missing values by the mean "
                    + "of the values in a column");
            m_meanButton.addActionListener(actionListener);
            buttonGroup.add(m_meanButton);
            panel.add(m_meanButton);
            if (setting.getType() == ColSetting.TYPE_DOUBLE) {
                panel.add(new JLabel()); // even number of components
            }
        } else {
            m_meanButton = null;
            m_minButton = null;
            m_maxButton = null;
        }
        if (setting.getType() == ColSetting.TYPE_INT
                || setting.getType() == ColSetting.TYPE_STRING) {
            m_mostFrequentButton = new JRadioButton("Most Frequent");
            m_mostFrequentButton.setToolTipText("Replaces missing values "
                    + "by the most frequent value in a column");
            m_mostFrequentButton.addActionListener(actionListener);
            buttonGroup.add(m_mostFrequentButton);
            panel.add(m_mostFrequentButton);
            if (setting.getType() == ColSetting.TYPE_STRING) {
                panel.add(new JLabel()); // even number of components
            }
        } else {
            m_mostFrequentButton = null;
        }

        if (setting.getType() != ColSetting.TYPE_UNKNOWN) {
            // FIX Button
            m_fixButton = new JRadioButton("Fix Value: ");
            m_fixButton.setToolTipText(
                    "Replaces missing values by a fixed value");
            m_fixButton.addActionListener(actionListener);
            buttonGroup.add(m_fixButton);
            panel.add(m_fixButton);
            m_fixText = getFixTextField(setting, spec);
            JPanel fixPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            fixPanel.add(m_fixText);
            panel.add(fixPanel);
        } else {
            m_fixButton = null;
            m_fixText = null;
        }
        switch (setting.getMethod()) {
        case ColSetting.METHOD_FIX_VAL:
            m_fixButton.doClick();
            break;
        case ColSetting.METHOD_IGNORE_ROWS:
            m_removeButton.doClick();
            break;
        case ColSetting.METHOD_MOST_FREQUENT:
            m_mostFrequentButton.doClick();
            break;
        case ColSetting.METHOD_MAX:
            m_maxButton.doClick();
            break;
        case ColSetting.METHOD_MEAN:
            m_meanButton.doClick();
            break;
        case ColSetting.METHOD_MIN:
            m_minButton.doClick();
            break;
        default:
            m_nothingButton.doClick();
        }
        m_setting = setting;
        add(panel);
    }

    /**
     * Get the settings currently entered in the dialog.
     * 
     * @return the current settings
     */
    public ColSetting getSettings() {
        int method;
        if (m_nothingButton.isSelected()) {
            method = ColSetting.METHOD_NO_HANDLING;
        } else if (m_removeButton.isSelected()) {
            method = ColSetting.METHOD_IGNORE_ROWS;
        } else if (m_fixButton != null && m_fixButton.isSelected()) {
            method = ColSetting.METHOD_FIX_VAL;
            DataCell cell;
            switch (m_setting.getType()) {
            case ColSetting.TYPE_INT:
                Object value = ((JFormattedTextField)m_fixText).getValue();
                cell = new IntCell(((Number)value).intValue());
                break;
            case ColSetting.TYPE_DOUBLE:
                value = ((JFormattedTextField)m_fixText).getValue();
                cell = new DoubleCell(((Number)value).doubleValue());
                break;
            case ColSetting.TYPE_STRING:
                value = ((JComboBox)m_fixText).getEditor().getItem();
                cell = new StringCell(value.toString());
                break;
            default:
                throw new RuntimeException("You shouldn't have come here.");
            }
            m_setting.setFixCell(cell);
        } else if (m_maxButton != null && m_maxButton.isSelected()) {
            method = ColSetting.METHOD_MAX;
        } else if (m_minButton != null && m_minButton.isSelected()) {
            method = ColSetting.METHOD_MIN;
        } else if (m_meanButton != null && m_meanButton.isSelected()) {
            method = ColSetting.METHOD_MEAN;
        } else if (m_mostFrequentButton != null 
                && m_mostFrequentButton.isSelected()) {
            method = ColSetting.METHOD_MOST_FREQUENT;
        } else {
            assert false : "One button must be selected.";
            method = ColSetting.METHOD_NO_HANDLING;
        }
        m_setting.setMethod(method);
        return m_setting;
    }

    /*
     * Helper in constructor, generates the text field to enter the replacement
     * value.
     */
    private static JComponent getFixTextField(final ColSetting setting,
            final DataColumnSpec spec) {
        JComponent fixText;
        // FIX text field
        DataCell fixCell = setting.getFixCell();
        switch (setting.getType()) {
        case ColSetting.TYPE_DOUBLE:
            fixText = new JFormattedTextField();
            ((JFormattedTextField)fixText).setColumns(8);
            Double doubel;
            if (fixCell == null) {
                doubel = new Double(0.0);
            } else {
                double d = ((DoubleValue)fixCell).getDoubleValue();
                doubel = new Double(d);
            }
            ((JFormattedTextField)fixText).setValue(doubel);
            break;
        case ColSetting.TYPE_INT:
            fixText = new JFormattedTextField();
            ((JFormattedTextField)fixText).setColumns(8);
            Integer integer;
            if (fixCell == null) {
                integer = new Integer(0);
            } else {
                int i = ((IntValue)fixCell).getIntValue();
                integer = new Integer(i);
            }
            ((JFormattedTextField)fixText).setValue(integer);
            break;
        case ColSetting.TYPE_STRING:
            DataCell[] vals;
            if (spec != null && spec.getDomain().hasValues()) {
                vals = spec.getDomain().getValues().toArray(new DataCell[0]);
            } else {
                vals = new DataCell[0];
            }
            DefaultComboBoxModel model = new DefaultComboBoxModel(vals);
            fixText = new JComboBox(model);
            ((JComboBox)fixText).setPrototypeDisplayValue("#########");
            ((JComboBox)fixText).setEditable(true);
            ((JComboBox)fixText).setRenderer(new DefaultListCellRenderer() {
                /**
                 * Overridden to set tooltip text properly.
                 * @see DefaultListCellRenderer#getListCellRendererComponent(
                 * JList, Object, int, boolean, boolean)
                 */
               @Override
               public Component getListCellRendererComponent(final JList list, 
                    final Object value, final int index, 
                    final boolean isSelected, final boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(
                            list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JComponent) {
                        ((JComponent)c).setToolTipText(value.toString());
                    }
                    return c; 
               }
            });
            String string;
            if (fixCell == null) {
                string = "";
            } else {
                string = ((StringValue)fixCell).getStringValue();
            }
            model.setSelectedItem(string);
            break;
        default:
            throw new InternalError("No such type");
        }
        return fixText;
    }

    /** Action Listener for buttons. */
    private class ButtonListener implements ActionListener {
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            if (m_fixButton == null) {
                return;
            }
            m_fixText.setEnabled(m_fixButton.isSelected());
            if (m_fixButton.isSelected()) {
                m_fixText.requestFocus();
            }
        }
    }
}

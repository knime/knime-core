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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.08.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * The dialog ot the Category2Number node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberNodeDialogPane  extends NodeDialogPane {
    private final CategoryToNumberNodeSettings m_settings;

    private ColumnFilterPanel m_includedColumns;
    private JCheckBox m_appendColums;
    private JTextField m_columnSuffix;
    private JSpinner m_startIndex;
    private JSpinner m_increment;
    private JSpinner m_maxCategories;
    private JTextField m_defaultValue;
    private JTextField m_mapMissingTo;

    /** Create a new instance. */
    CategoryToNumberNodeDialogPane() {
        m_settings = new CategoryToNumberNodeSettings();
        addTab("Columns to transform", createCategoryToNumberSettingsTab());
    }

    @SuppressWarnings("unchecked")
    private JPanel createCategoryToNumberSettingsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets middleInsets = new Insets(3, 0, 3, 0);

        c.gridwidth = 3;
        m_includedColumns = new ColumnFilterPanel(true,
                StringValue.class);
        p.add(m_includedColumns, c);

        c.weighty = 0;
        c.weightx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.gridx = 0;
        c.insets = leftInsets;
        m_appendColums = new JCheckBox("Append columns");
        m_appendColums.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_columnSuffix.setEnabled(m_appendColums.isSelected());
            }
        });
        p.add(m_appendColums, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Column suffix:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_columnSuffix = new JTextField();
        p.add(m_columnSuffix, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Start value:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_startIndex = new JSpinner(
                new SpinnerNumberModel(0, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, 1));
        p.add(m_startIndex, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Increment:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_increment = new JSpinner(
                new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        p.add(m_increment, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Max. categories:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_maxCategories = new JSpinner(
                new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 100));
        p.add(m_maxCategories, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Default value:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_defaultValue = new JTextField();
        m_defaultValue.setHorizontalAlignment(JTextField.RIGHT);
        p.add(m_defaultValue, c);

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.insets = leftInsets;
        p.add(new JLabel("Map missing to:"), c);
        c.gridx = 1;
        c.insets = middleInsets;
        m_mapMissingTo = new JTextField();
        m_mapMissingTo.setHorizontalAlignment(JTextField.RIGHT);
        p.add(m_mapMissingTo, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);

        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> included = m_includedColumns.getIncludedColumnSet();
        m_settings.setIncludedColumns(included.toArray(
                    new String[included.size()]));
        m_settings.setIncludeAll(m_includedColumns.isKeepAllSelected());
        m_settings.setAppendColumns(m_appendColums.isSelected());
        m_settings.setColumnSuffix(m_columnSuffix.getText());
        m_settings.setStartIndex((Integer)m_startIndex.getValue());
        m_settings.setIncrement((Integer)m_increment.getValue());
        m_settings.setMaxCategories((Integer)m_maxCategories.getValue());
        if (!m_defaultValue.getText().trim().isEmpty()) {
            int value = Integer.valueOf(m_defaultValue.getText());
            m_settings.setDefaultValue(new IntCell(value));
        } else {
            m_settings.setDefaultValue(DataType.getMissingCell());
        }
        if (!m_mapMissingTo.getText().trim().isEmpty()) {
            int value = Integer.valueOf(m_mapMissingTo.getText());
            m_settings.setMapMissingTo(new IntCell(value));
        } else {
            m_settings.setMapMissingTo(DataType.getMissingCell());
        }
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        String[] included = null != m_settings.getIncludedColumns()
            ? m_settings.getIncludedColumns() : new String[0];
        m_includedColumns.update((DataTableSpec)specs[0], false, included);
        if (m_includedColumns.getExcludedColumnSet().size()
                + m_includedColumns.getIncludedColumnSet().size() == 0) {
            throw new NotConfigurableException("No column in "
                    + "the input compatible to \"StringValue\".");
        }
        m_includedColumns.setKeepAllSelected(m_settings.getIncludeAll());
        m_appendColums.setSelected(m_settings.getAppendColumns());
        m_columnSuffix.setText(m_settings.getColumnSuffix());
        m_columnSuffix.setEnabled(m_appendColums.isSelected());
        m_startIndex.setValue(m_settings.getStartIndex());
        m_increment.setValue(m_settings.getIncrement());
        m_maxCategories.setValue(m_settings.getMaxCategories());
        if (!m_settings.getDefaultValue().isMissing()) {
            IntValue value = (IntValue)m_settings.getDefaultValue();
            m_defaultValue.setText(Integer.toString(value.getIntValue()));
        } else {
            m_defaultValue.setText("");
        }
        if (!m_settings.getMapMissingTo().isMissing()) {
            IntValue value = (IntValue)m_settings.getMapMissingTo();
            m_mapMissingTo.setText(Integer.toString(value.getIntValue()));
        } else {
            m_mapMissingTo.setText("");
        }
    }

}

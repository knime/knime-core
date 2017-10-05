/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog for the linear regression learner.
 *
 * @author Heiko Hofer
 */
final class LinReg2LearnerNodeDialogPane extends NodeDialogPane {
    private final DataColumnSpecFilterPanel m_filterPanel;

    private final ColumnSelectionPanel m_selectionPanel;

    private JCheckBox m_predefinedOffsetValue;

    private JSpinner m_offsetValue;

    private JSpinner m_scatterPlotFirstRow;
    private JSpinner m_scatterPlotRowCount;

    private JRadioButton m_missingValueHandlingIgnore;
    private JRadioButton m_missingValueHandlingFail;

    /**
     * Create new dialog for linear regression model.
     */
    @SuppressWarnings("unchecked")
    public LinReg2LearnerNodeDialogPane() {
        super();
        m_filterPanel = new DataColumnSpecFilterPanel();
        m_selectionPanel = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0),
                DoubleValue.class);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(5, 5, 0, 0);
        c.gridwidth = 2;

        JPanel columnSelectionPanel = new JPanel(new FlowLayout());
        columnSelectionPanel.setBorder(BorderFactory.createTitledBorder("Target"));
        columnSelectionPanel.add(m_selectionPanel);
        panel.add(columnSelectionPanel, c);

        c.gridy++;
        c.weighty = 1;
        m_filterPanel.setBorder(BorderFactory.createTitledBorder("Values"));
        panel.add(m_filterPanel, c);

        c.gridy++;
        c.weighty = 0;
        JPanel regrPropertiesPanel = new JPanel(new FlowLayout());
        regrPropertiesPanel.setBorder(BorderFactory.createTitledBorder("Regression Properties"));

        regrPropertiesPanel.add(createRegressionPropertiesPanel());
        panel.add(regrPropertiesPanel, c);

        c.gridwidth = 1;
        c.gridy++;
        c.weighty = 0;
        JPanel missingValueHandlingPanel = new JPanel(new FlowLayout());
        missingValueHandlingPanel.setBorder(BorderFactory.createTitledBorder("Missing Values in Input Data"));

        missingValueHandlingPanel.add(createMissingValueHandlingPanel());
        panel.add(missingValueHandlingPanel, c);

        c.gridx++;
        c.weighty = 0;
        JPanel scatterPlotPropertiesPanel = new JPanel(new FlowLayout());
        scatterPlotPropertiesPanel.setBorder(BorderFactory.createTitledBorder("Scatter Plot View"));

        scatterPlotPropertiesPanel.add(createScatterPlotPropertiesPanel());
        panel.add(scatterPlotPropertiesPanel, c);

        addTab("Settings", panel);

        m_selectionPanel.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                Object selected = e.getItem();
                if (selected instanceof DataColumnSpec) {
                    updateHiddenColumns((DataColumnSpec)selected);
                }
            }
        });
    }



    private JPanel createRegressionPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        m_predefinedOffsetValue = new JCheckBox("Predefined Offset Value:");
        panel.add(m_predefinedOffsetValue, c);

        c.gridx++;
        m_offsetValue = new JSpinner(new SpinnerNumberModel(0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        m_offsetValue.setPreferredSize(new Dimension(
            m_offsetValue.getPreferredSize().width + 70,
            m_offsetValue.getPreferredSize().height));
        panel.add(m_offsetValue, c);

        m_predefinedOffsetValue.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_offsetValue.setEnabled(m_predefinedOffsetValue.isSelected());
            }
        });

        return panel;
    }

    private JPanel createMissingValueHandlingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        m_missingValueHandlingIgnore = new JRadioButton("Ignore rows with missing values.");
        panel.add(m_missingValueHandlingIgnore, c);

        c.gridy++;
        m_missingValueHandlingFail = new JRadioButton("Fail on observing missing values.");
        panel.add(m_missingValueHandlingFail, c);

        ButtonGroup missingValueHandling = new ButtonGroup();
        missingValueHandling.add(m_missingValueHandlingIgnore);
        missingValueHandling.add(m_missingValueHandlingFail);

        return panel;
    }

    private JPanel createScatterPlotPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        panel.add(new JLabel("First Row:"), c);
        c.gridx++;
        m_scatterPlotFirstRow = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_scatterPlotFirstRow.setPreferredSize(new Dimension(
            m_scatterPlotFirstRow.getPreferredSize().width - 50,
            m_scatterPlotFirstRow.getPreferredSize().height));
        panel.add(m_scatterPlotFirstRow, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = new Insets(5, 5, 0, 0);
        panel.add(new JLabel("Row Count:"), c);
        c.gridx++;
        c.insets = new Insets(5, 5, 0, 0);
        m_scatterPlotRowCount = new JSpinner(new SpinnerNumberModel(20000, 1, Integer.MAX_VALUE, 1));
        m_scatterPlotRowCount.setPreferredSize(new Dimension(
            m_scatterPlotRowCount.getPreferredSize().width - 50,
            m_scatterPlotRowCount.getPreferredSize().height));
        panel.add(m_scatterPlotRowCount, c);

        return panel;
    }

    private void updateHiddenColumns(final DataColumnSpec toHide) {
        m_filterPanel.resetHiding();
        m_filterPanel.hideNames(toHide);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO s, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        DataTableSpec spec = (DataTableSpec)specs[0];
        LinReg2LearnerSettings settings = new LinReg2LearnerSettings();
        settings.loadSettingsInDialog(s, spec);

        String target = settings.getTargetColumn();
        m_selectionPanel.update(spec, target);

        m_filterPanel.loadConfiguration(settings.getFilterConfiguration(), spec);
        updateHiddenColumns(m_selectionPanel.getSelectedColumnAsSpec());
        m_predefinedOffsetValue.setSelected(!settings.getIncludeConstant());
        m_offsetValue.setValue(settings.getOffsetValue());
        m_offsetValue.setEnabled(m_predefinedOffsetValue.isSelected());

        m_scatterPlotFirstRow.setValue(settings.getScatterPlotFirstRow());
        m_scatterPlotRowCount.setValue(settings.getScatterPlotRowCount());

        m_missingValueHandlingIgnore.setSelected(
            settings.getMissingValueHandling2().equals(MissingValueHandling.ignore));
        m_missingValueHandlingFail.setSelected(
            settings.getMissingValueHandling2().equals(MissingValueHandling.fail));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO s)
            throws InvalidSettingsException {
        LinReg2LearnerSettings settings = new LinReg2LearnerSettings();

        m_filterPanel.saveConfiguration(settings.getFilterConfiguration());
        settings.setTargetColumn(m_selectionPanel.getSelectedColumn());
        settings.setIncludeConstant(!m_predefinedOffsetValue.isSelected());
        settings.setOffsetValue((Double)m_offsetValue.getValue());
        settings.setScatterPlotFirstRow((Integer)m_scatterPlotFirstRow.getValue());
        settings.setScatterPlotRowCount((Integer)m_scatterPlotRowCount.getValue());
        if (m_missingValueHandlingFail.isSelected()) {
            settings.setMissingValueHandling(MissingValueHandling.fail);
        } else {
            settings.setMissingValueHandling(MissingValueHandling.ignore);
        }

        settings.saveSettings(s);
    }

}

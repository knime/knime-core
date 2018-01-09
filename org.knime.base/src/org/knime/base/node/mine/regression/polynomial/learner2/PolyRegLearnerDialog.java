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
 */
package org.knime.base.node.mine.regression.polynomial.learner2;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * This is the dialog for the polynomial regression learner node. The user can
 * select the target column with the dependent variable and the degree of the
 * polynomial used for regression.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.10
 */
public class PolyRegLearnerDialog extends NodeDialogPane {
    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_targetColumn =
        new ColumnSelectionComboxBox((Border)null, DoubleValue.class);

    private final JSpinner m_degree = new JSpinner(new SpinnerNumberModel(2, 1,
            10, 1));

    private final PolyRegLearnerSettings m_settings =
        new PolyRegLearnerSettings();

    private final JSpinner m_viewRows = new JSpinner(new SpinnerNumberModel(
            10000, 1, Integer.MAX_VALUE, 10));

    @SuppressWarnings("unchecked")
    private final DataColumnSpecFilterPanel m_colSelectionPanel = new DataColumnSpecFilterPanel(
            false, DoubleValue.class);

    private JRadioButton m_missingValueHandlingIgnore;
    private JRadioButton m_missingValueHandlingFail;

    /**
     * Creates a new dialog for the polynomial regression learner node.
     */
    public PolyRegLearnerDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Target column (dependent variable)   "), c);
        c.gridx = 1;
        p.add(m_targetColumn, c);
        m_targetColumn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                if (m_targetColumn.getSelectedItem() != null) {
                    m_colSelectionPanel.resetHiding();
                    m_colSelectionPanel
                            .hideNames((DataColumnSpec)m_targetColumn
                                    .getSelectedItem());
                }
            }
        });

        c.insets = new Insets(4, 0, 0, 0);
        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Maximum polynomial degree    "), c);
        c.gridx = 1;
        p.add(m_degree, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        p.add(new JSeparator(SwingConstants.HORIZONTAL), c);

        c.gridy++;
        p.add(new JLabel("Select the independent variables"), c);

        c.gridy++;
        p.add(m_colSelectionPanel, c);

        c.gridy++;
        JPanel missingValueHandlingPanel = new JPanel(new FlowLayout());
        missingValueHandlingPanel.setBorder(BorderFactory.createTitledBorder("Missing Values in Input Data"));

        missingValueHandlingPanel.add(createMissingValueHandlingPanel());
        p.add(missingValueHandlingPanel, c);

        addTab("Regression settings", p);

        p = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        p.add(new JLabel("Number of data points to show in view   "), c);
        c.gridx = 1;
        p.add(m_viewRows, c);

        addTab("View settings", p);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec dataTableSpec = (DataTableSpec)specs[0];
        try {
            m_settings.loadSettingsInDialog(settings, dataTableSpec);
        } catch (InvalidSettingsException ex) {
            LinkedHashSet<String> defSelected = new LinkedHashSet<String>();
            for (DataColumnSpec s : dataTableSpec) {
                if (s.getType().isCompatible(DoubleValue.class)) {
                    defSelected.add(s.getName());
                }
            }
            NodeSettings fakeSettings =
                PolyRegLearnerSettings.createFakeSettings("column filter", defSelected.toArray(new String[0]),
                    new String[0], false/*includeAll*/);
            DataColumnSpecFilterConfiguration conf = new DataColumnSpecFilterConfiguration("column filter");
            conf.loadConfigurationInDialog(fakeSettings, dataTableSpec);
            m_settings.getFilterConfiguration().loadConfigurationInDialog(fakeSettings, dataTableSpec);
            // for the rest: ignore it, defaults are used instead
        }
        m_targetColumn.update(dataTableSpec, m_settings.getTargetColumn());
        m_degree.getModel().setValue(m_settings.getDegree());
        m_viewRows.getModel().setValue(m_settings.getMaxRowsForView());
        m_colSelectionPanel.loadConfiguration(m_settings.getFilterConfiguration(), dataTableSpec);

        m_colSelectionPanel.resetHiding();
        m_colSelectionPanel.hideNames((DataColumnSpec)m_targetColumn
                .getSelectedItem());
        m_missingValueHandlingIgnore.setSelected(
            m_settings.getMissingValueHandling().equals(MissingValueHandling.ignore));
        m_missingValueHandlingFail.setSelected(
            m_settings.getMissingValueHandling().equals(MissingValueHandling.fail));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setTargetColumn(m_targetColumn.getSelectedColumn());
        m_settings.setDegree((Integer)m_degree.getModel().getValue());
        m_settings.setMaxRowsForView((Integer)m_viewRows.getModel().getValue());
        m_colSelectionPanel.saveConfiguration(m_settings.getFilterConfiguration());
        if (m_missingValueHandlingFail.isSelected()) {
            m_settings.setMissingValueHandling(MissingValueHandling.fail);
        } else {
            m_settings.setMissingValueHandling(MissingValueHandling.ignore);
        }
        m_settings.saveSettingsTo(settings);
    }
}

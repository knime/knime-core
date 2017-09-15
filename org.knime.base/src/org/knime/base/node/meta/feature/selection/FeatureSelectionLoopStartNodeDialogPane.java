/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategies.Strategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionLoopStartNodeDialogPane extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_constantColumnsFilterPanel;

    private final JComboBox<Strategy> m_selectionStrategySelectionComboBox;

    private final JSpinner m_nrFeaturesThresholdSpinner;

    private final JLabel m_nrFeaturesLabel;

    private final JCheckBox m_useNrFeaturesThresholdCheckBox;

    /**
     *
     */
    public FeatureSelectionLoopStartNodeDialogPane() {
        m_constantColumnsFilterPanel = new DataColumnSpecFilterPanel();
        m_constantColumnsFilterPanel.setExcludeTitle("Static Columns");
        m_constantColumnsFilterPanel.setIncludeTitle("Variable Columns ('Features')");
        m_selectionStrategySelectionComboBox = new JComboBox<Strategy>();
        for (Strategy strategy : Strategy.values()) {
            m_selectionStrategySelectionComboBox.addItem(strategy);
        }
        m_nrFeaturesThresholdSpinner = new JSpinner(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 1));
        m_useNrFeaturesThresholdCheckBox = new JCheckBox("Use threshold for number of features");
        m_nrFeaturesLabel = new JLabel("Threshold for number of features in subset");
        m_useNrFeaturesThresholdCheckBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                final boolean enable = m_useNrFeaturesThresholdCheckBox.isSelected();
                m_nrFeaturesThresholdSpinner.setEnabled(enable);
                m_nrFeaturesLabel.setEnabled(enable);
            }
        });
        m_useNrFeaturesThresholdCheckBox.doClick();
        layout();
    }

    private void layout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(new JLabel("<html>"
            + "<body>"
            + "The list on the left contains 'static' columns such as the target column. <br />"
            + " The columns to choose from need to be in the list on the right."
            + "</body></html>"), gbc);
        gbc.gridy += 1;
        panel.add(m_constantColumnsFilterPanel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy += 1;
        panel.add(new JLabel("Feature selection strategy"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_selectionStrategySelectionComboBox, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        panel.add(m_useNrFeaturesThresholdCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(new JLabel("Select threshold for number of features"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_nrFeaturesThresholdSpinner, gbc);

        addTab("Options", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final FeatureSelectionLoopStartSettings cfg = new FeatureSelectionLoopStartSettings();
        if (m_constantColumnsFilterPanel.getExcludeList().isEmpty()) {
            throw new InvalidSettingsException("All input columns are declared as constant. Please make at least one column non constant.");
        }
        m_constantColumnsFilterPanel.saveConfiguration(cfg.getStaticColumnsFilterConfiguration());
        cfg.setSelectionStrategy((Strategy)m_selectionStrategySelectionComboBox.getSelectedItem());
        if (!m_useNrFeaturesThresholdCheckBox.isSelected()) {
            cfg.setNrFeaturesThreshold(-1);
        } else {
            cfg.setNrFeaturesThreshold((int)m_nrFeaturesThresholdSpinner.getValue());
        }
        cfg.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final FeatureSelectionLoopStartSettings cfg = new FeatureSelectionLoopStartSettings();
        cfg.loadInDialog(settings, specs[0]);
        m_constantColumnsFilterPanel.loadConfiguration(cfg.getStaticColumnsFilterConfiguration(), specs[0]);

        final boolean useFeatureThreshold = cfg.useNrFeaturesThreshold();
        m_useNrFeaturesThresholdCheckBox.setSelected(useFeatureThreshold);
        // only spinner value if the threshold is used
        if (useFeatureThreshold) {
            m_nrFeaturesThresholdSpinner.setValue(cfg.getNrFeaturesThreshold());
        }
        m_selectionStrategySelectionComboBox.setSelectedItem(cfg.getSelectionStrategy());
    }

}

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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 30, 2012 (Patrick Winter): created
 */
package org.knime.base.node.preproc.bootstrap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the node.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
final class BootstrapNodeDialog extends NodeDialogPane {

    private ButtonGroup m_sizeOrPercent;

    private JRadioButton m_inSize;

    private JRadioButton m_inPercent;

    private JSpinner m_size;

    private JSpinner m_percent;

    private JCheckBox m_useSeed;

    private JTextField m_seed;

    private JCheckBox m_appendOccurrences;

    private JCheckBox m_appendOriginalRowId;

    private JTextField m_rowIdSeparator;

    /**
     * New pane for configuring the node dialog.
     */
    public BootstrapNodeDialog() {
        m_sizeOrPercent = new ButtonGroup();
        m_inSize = new JRadioButton("Absolute sample size");
        m_inPercent = new JRadioButton("Sample size in %");
        m_sizeOrPercent.add(m_inSize);
        m_sizeOrPercent.add(m_inPercent);
        m_sizeOrPercent.setSelected(m_inPercent.getModel(), true);
        m_inSize.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateSizePanels();
            }
        });
        m_inPercent.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateSizePanels();
            }
        });
        m_size = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        m_percent = new JSpinner(new SpinnerNumberModel(100, 0, Float.MAX_VALUE, 1));
        m_useSeed = new JCheckBox("Use random seed", false);
        m_useSeed.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_seed.setEnabled(m_useSeed.isSelected());
            }
        });
        m_seed = new JTextField();
        m_seed.setText("" + System.currentTimeMillis());
        m_appendOccurrences = new JCheckBox("Append count of occurrences", false);
        m_appendOriginalRowId = new JCheckBox("Append original RowID", false);
        JLabel rowIdSeparatorLabel = new JLabel("RowID separator");
        m_rowIdSeparator = new JTextField("_");
        GridBagConstraints gbc = new GridBagConstraints();
        // Panel
        resetGBC(gbc);
        JPanel panel = new JPanel(new GridBagLayout());
        // Percent
        panel.add(m_inPercent, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_percent, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        // Size
        panel.add(m_inSize, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_size, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        // Seed
        panel.add(m_useSeed, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_seed, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        // Append occurrences
        panel.add(m_appendOccurrences, gbc);
        gbc.gridy++;
        // Append original row ID
        panel.add(m_appendOriginalRowId, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        // Row ID separator
        panel.add(rowIdSeparatorLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_rowIdSeparator, gbc);
        // Update enabled / disabled states
        m_seed.setEnabled(m_useSeed.isSelected());
        updateSizePanels();
        addTab("Options", panel);
    }

    /**
     * Reset the grid bag constraints to useful defaults.
     *
     *
     * The defaults are all insets to 5, anchor northwest, fill both, x and y 0 and x and y weight 0.
     *
     * @param gbc The constraints object.
     */
    private static void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
    }

    private void updateSizePanels() {
        boolean inPercent = m_sizeOrPercent.getSelection().equals(m_inPercent.getModel());
        m_size.setEnabled(!inPercent);
        m_percent.setEnabled(inPercent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        BootstrapConfiguration config = new BootstrapConfiguration();
        config.load(settings);
        m_sizeOrPercent.setSelected(config.getInPercent() ? m_inPercent.getModel() : m_inSize.getModel(), true);
        m_size.setValue(config.getSize());
        m_percent.setValue(config.getPercent());
        m_useSeed.setSelected(config.getUseSeed());
        m_seed.setText("" + config.getSeed());
        m_appendOccurrences.setSelected(config.getAppendOccurrences());
        m_appendOriginalRowId.setSelected(config.getAppendOriginalRowId());
        m_rowIdSeparator.setText(config.getRowIdSeparator());
        m_seed.setEnabled(m_useSeed.isSelected());
        updateSizePanels();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        BootstrapConfiguration config = new BootstrapConfiguration();
        config.setInPercent(m_sizeOrPercent.getSelection().equals(m_inPercent.getModel()));
        config.setSize((Integer)m_size.getValue());
        config.setPercent(((Double)m_percent.getValue()).floatValue());
        config.setUseSeed(m_useSeed.isSelected());
        // Throw error if seed is no long
        try {
            config.setSeed(Long.parseLong(m_seed.getText()));
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException("Seed needs to be of the type long");
        }
        config.setAppendOccurrences(m_appendOccurrences.isSelected());
        config.setAppendOriginalRowId(m_appendOriginalRowId.isSelected());
        config.setRowIdSeparator(m_rowIdSeparator.getText());
        config.save(settings);
    }

}

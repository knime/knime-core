/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 01, 2018 (Mor Kalla): created
 */
package org.knime.core.util.binning.auto;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Implementation for auto binner node's number format settings tab.
 *
 * @author Mor Kalla
 * @since 3.6
 *
 * @deprecated No longer used by modern UI nodes.
 */
@Deprecated
public class NumberFormatSettingsPanel extends JPanel {

    private static final long serialVersionUID = 4476999170649693854L;

    private static final Insets INSETS_INDENTED = new Insets(5, 15, 5, 5);

    private static final Insets INSETS_NORMAL = new Insets(5, 5, 5, 5);

    private static final int DEFAULT_VALUE_PRECISION = 3;

    private final AutoBinnerLearnSettings m_settings;

    private JRadioButton m_defaultFormatting;

    private JRadioButton m_advancedFormatting;

    private JComboBox<OutputFormat> m_outputFormat;

    private JSpinner m_precision;

    private JComboBox<PrecisionMode> m_precisionMode;

    private JComboBox<RoundingMode> m_roundingMode;

    /**
     * Constructs a {@link NumberFormatSettingsPanel} object.
     *
     * @param settings the {@link AutoBinnerLearnSettings} object
     */
    public NumberFormatSettingsPanel(final AutoBinnerLearnSettings settings) {

        m_settings = settings;
        setLayout(new GridBagLayout());

        m_defaultFormatting = new JRadioButton("Default formatting");
        m_advancedFormatting = new JRadioButton("Advanced formatting");
        final ButtonGroup formatting = new ButtonGroup();
        formatting.add(m_defaultFormatting);
        formatting.add(m_advancedFormatting);
        m_outputFormat = new JComboBox<>(OutputFormat.values());
        m_precision = new JSpinner(new SpinnerNumberModel(DEFAULT_VALUE_PRECISION, 0, Integer.MAX_VALUE, 1));
        m_precisionMode = new JComboBox<>(PrecisionMode.values());
        m_roundingMode = new JComboBox<>(getRoundingModes());
        final ActionListener formattingListener = e -> {
            m_outputFormat.setEnabled(m_advancedFormatting.isSelected());
            m_precision.setEnabled(m_advancedFormatting.isSelected());
            m_precisionMode.setEnabled(m_advancedFormatting.isSelected());
            m_roundingMode.setEnabled(m_advancedFormatting.isSelected());
        };
        m_defaultFormatting.addActionListener(formattingListener);
        m_advancedFormatting.addActionListener(formattingListener);
        final GridBagConstraints gbc = new GridBagConstraints();
        final Insets indentedInsets = INSETS_INDENTED;
        final Insets normalInsets = INSETS_NORMAL;

        gbc.insets = normalInsets;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(m_defaultFormatting, gbc);

        gbc.gridy++;
        add(m_advancedFormatting, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.insets = indentedInsets;
        add(new JLabel("Output format"), gbc);

        gbc.gridx++;
        gbc.insets = normalInsets;
        add(m_outputFormat, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = indentedInsets;
        add(new JLabel("Precision"), gbc);

        gbc.gridx++;
        gbc.insets = normalInsets;
        add(m_precision, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = indentedInsets;
        add(new JLabel("Precision mode"), gbc);

        gbc.gridx++;
        gbc.insets = normalInsets;
        add(m_precisionMode, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = indentedInsets;
        add(new JLabel("Rounding mode"), gbc);

        gbc.gridx++;
        gbc.insets = normalInsets;
        add(m_roundingMode, gbc);

        gbc.gridx++;
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(new JLabel(), gbc);
    }

    private static RoundingMode[] getRoundingModes() {
        final List<RoundingMode> roundingModes = new ArrayList<>();
        roundingModes.add(RoundingMode.UP);
        roundingModes.add(RoundingMode.DOWN);
        roundingModes.add(RoundingMode.CEILING);
        roundingModes.add(RoundingMode.FLOOR);
        roundingModes.add(RoundingMode.HALF_UP);
        roundingModes.add(RoundingMode.HALF_DOWN);
        roundingModes.add(RoundingMode.HALF_EVEN);
        return roundingModes.toArray(new RoundingMode[0]);
    }

    /**
     * Loads settings for dialog.
     *
     * @param settings to read intervals from
     * @param specs the input table spec
     * @throws NotConfigurableException if the spec contains no columns
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings, (DataTableSpec)specs[0]);

        m_defaultFormatting.setSelected(!m_settings.getAdvancedFormatting());
        m_advancedFormatting.setSelected(m_settings.getAdvancedFormatting());
        m_outputFormat.setEnabled(m_settings.getAdvancedFormatting());
        m_precision.setEnabled(m_settings.getAdvancedFormatting());
        m_precisionMode.setEnabled(m_settings.getAdvancedFormatting());
        m_roundingMode.setEnabled(m_settings.getAdvancedFormatting());
        m_outputFormat.setSelectedItem(m_settings.getOutputFormat());
        m_precision.setValue(m_settings.getPrecision());
        m_precisionMode.setSelectedItem(m_settings.getPrecisionMode());
        m_roundingMode.setSelectedItem(m_settings.getRoundingMode());
    }

    /**
     * Saves the settings.
     *
     * @param settings write intervals to
     * @throws InvalidSettingsException if a bin name is empty
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        m_settings.setAdvancedFormatting(m_advancedFormatting.isSelected());
        m_settings.setOutputFormat((OutputFormat)m_outputFormat.getSelectedItem());
        m_settings.setPrecision(((Number)m_precision.getValue()).intValue());
        m_settings.setPrecisionMode((PrecisionMode)m_precisionMode.getSelectedItem());
        m_settings.setRoundingMode((RoundingMode)m_roundingMode.getSelectedItem());

        m_settings.saveSettings(settings);
    }

}

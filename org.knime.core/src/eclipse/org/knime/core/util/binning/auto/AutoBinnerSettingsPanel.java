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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Implementation for auto binner node settings tab.
 *
 * @author Mor Kalla
 * @since 3.6
 *
 * @deprecated No longer used in modern nodes.
 */
@Deprecated
public class AutoBinnerSettingsPanel extends JPanel {

    private static final long serialVersionUID = 6289032947872308466L;

    private static final int DEFAULT_VALUE_BINS = 5;

    private static final Border BORDER_EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 17, 0, 0);

    private static final Insets INSETS_BIGGER_SPACE = new Insets(6, 2, 2, 2);

    private static final Insets INSETS_SMALLER_SPACE = new Insets(2, 2, 2, 2);

    private final AutoBinnerLearnSettings m_settings;

    private DataColumnSpecFilterPanel m_filterPanel;

    private JCheckBox m_integerBounds;

    private JCheckBox m_replaceColumn;

    private JRadioButton m_methodFixedNumber;

    private JSpinner m_numBins;

    private JComboBox<EqualityMethod> m_equalityMethod;

    private JTextField m_sampleQuantiles;

    private JRadioButton m_methodSampleQuantiles;

    private JRadioButton m_binNamingNumbered;

    private JRadioButton m_binNamingEdges;

    private JRadioButton m_binNamingMidpoints;

    /**
     * Constructs a new {@link AutoBinnerSettingsPanel}.
     *
     * @param supportsQuantile {@code true} if dialog supports quantile
     * @param settings {@link AutoBinnerLearnSettings} object from the node
     */
    public AutoBinnerSettingsPanel(final boolean supportsQuantile, final AutoBinnerLearnSettings settings) {

        m_settings = settings;
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;

        m_filterPanel = new DataColumnSpecFilterPanel();
        add(m_filterPanel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.gridy++;
        add(createMethodUIControls(supportsQuantile), gbc);

        gbc.gridy++;
        add(createBinNamingUIControls(), gbc);

        m_integerBounds = new JCheckBox("Force integer bounds");
        gbc.gridy++;
        add(m_integerBounds, gbc);

        gbc.gridy++;
        m_replaceColumn = new JCheckBox("Replace target column(s)");
        add(m_replaceColumn, gbc);
    }

    private JPanel createMethodUIControls(final boolean supportsQuantile) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.weightx = 0;

        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        m_methodFixedNumber = new JRadioButton("Fixed number of bins");
        m_methodFixedNumber.addActionListener(e -> {
            m_numBins.setEnabled(m_methodFixedNumber.isSelected());
            m_equalityMethod.setEnabled(m_methodFixedNumber.isSelected());
            m_sampleQuantiles.setEnabled(!m_methodFixedNumber.isSelected());
        });

        if (supportsQuantile) {
            panel.add(m_methodFixedNumber, c);
        }

        c.gridy++;
        final JPanel numBinsPanel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        numBinsPanel.add(new JLabel("Number of bins:"), gbc);
        gbc.gridx++;
        m_numBins = new JSpinner(new SpinnerNumberModel(DEFAULT_VALUE_BINS, 1, Integer.MAX_VALUE, 1));
        numBinsPanel.add(m_numBins, gbc);
        m_equalityMethod = new JComboBox<>(EqualityMethod.values());
        if (supportsQuantile) {
            gbc.weightx = 1;
            gbc.gridx++;
            numBinsPanel.add(new JLabel(), gbc);
            gbc.weightx = 0;
            gbc.gridx = 0;
            gbc.gridy++;
            numBinsPanel.add(new JLabel("Equal:"), gbc);
            gbc.gridx++;
            numBinsPanel.add(m_equalityMethod, gbc);
            numBinsPanel.setBorder(BORDER_EMPTY_BORDER);
        }
        panel.add(numBinsPanel, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        m_methodSampleQuantiles = new JRadioButton("Sample quantiles");
        m_methodSampleQuantiles.addActionListener(e -> {
            m_numBins.setEnabled(!m_methodSampleQuantiles.isSelected());
            m_equalityMethod.setEnabled(!m_methodSampleQuantiles.isSelected());
            m_sampleQuantiles.setEnabled(m_methodSampleQuantiles.isSelected());
        });
        if (supportsQuantile) {
            panel.add(m_methodSampleQuantiles, c);
        }

        c.gridy++;
        final JPanel quantilesPanel = new JPanel(new GridBagLayout());
        final int gridy = c.gridy;
        c.gridy = 0;
        c.gridwidth = 1;
        quantilesPanel.add(new JLabel("Quantiles (comma separated):"), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        m_sampleQuantiles = new JTextField();
        quantilesPanel.add(m_sampleQuantiles, c);
        quantilesPanel.setBorder(BORDER_EMPTY_BORDER);
        c.gridy = gridy;
        c.gridx = 0;
        c.gridwidth = 1;
        if (supportsQuantile) {
            panel.add(quantilesPanel, c);
        }

        final ButtonGroup method = new ButtonGroup();
        method.add(m_methodFixedNumber);
        method.add(m_methodSampleQuantiles);

        if (supportsQuantile) {
            panel.setBorder(BorderFactory.createTitledBorder("Binning BinningMethod"));
        }
        return panel;
    }

    private JPanel createBinNamingUIControls() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = INSETS_SMALLER_SPACE;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        m_binNamingNumbered = new JRadioButton("Numbered");
        panel.add(m_binNamingNumbered, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.insets = INSETS_BIGGER_SPACE;
        final JLabel exampleNumbered = new JLabel("e.g.: Bin 1, Bin 2, Bin 3");
        exampleNumbered.setEnabled(false);
        panel.add(exampleNumbered, gbc);

        gbc.weightx = 0;
        gbc.insets = INSETS_SMALLER_SPACE;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        m_binNamingEdges = new JRadioButton("Borders");
        panel.add(m_binNamingEdges, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = INSETS_BIGGER_SPACE;
        final JLabel exampleEdges = new JLabel("e.g.: [-10,0], (0,10], (10,20]");
        exampleEdges.setEnabled(false);
        panel.add(exampleEdges, gbc);

        gbc.weightx = 0;
        gbc.insets = INSETS_SMALLER_SPACE;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        m_binNamingMidpoints = new JRadioButton("Midpoints");
        panel.add(m_binNamingMidpoints, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = INSETS_BIGGER_SPACE;
        final JLabel exampleMidpoints = new JLabel("e.g.: -5, 5, 15");
        exampleMidpoints.setEnabled(false);
        panel.add(exampleMidpoints, gbc);

        gbc.weightx = 0;
        gbc.insets = INSETS_SMALLER_SPACE;
        final ButtonGroup method = new ButtonGroup();
        method.add(m_binNamingNumbered);
        method.add(m_binNamingEdges);
        method.add(m_binNamingMidpoints);
        panel.setBorder(BorderFactory.createTitledBorder("Bin Naming"));

        return panel;
    }

    /**
     * Loads settings for the panel from the {@link AutoBinnerSettingsPanel}.
     *
     * @param settings to read intervals from
     * @param specs The input table spec
     * @throws NotConfigurableException if the spec contains no columns
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings, (DataTableSpec)specs[0]);

        final DataColumnSpecFilterConfiguration config = m_settings.getFilterConfiguration();
        m_filterPanel.loadConfiguration(config, (DataTableSpec)specs[0]);

        m_methodFixedNumber.setSelected(m_settings.getMethod().equals(BinningMethod.FIXED_NUMBER));
        m_methodSampleQuantiles.setSelected(m_settings.getMethod().equals(BinningMethod.SAMPLE_QUANTILES));
        m_numBins.setEnabled(!m_methodSampleQuantiles.isSelected());
        m_equalityMethod.setEnabled(!m_methodSampleQuantiles.isSelected());
        m_sampleQuantiles.setEnabled(m_methodSampleQuantiles.isSelected());
        m_numBins.setValue(m_settings.getBinCount());
        m_equalityMethod.setSelectedItem(m_settings.getEqualityMethod());
        m_sampleQuantiles.setText(implode(m_settings.getSampleQuantiles()));
        m_binNamingNumbered.setSelected(m_settings.getBinNaming().equals(BinNaming.NUMBERED));
        m_binNamingEdges.setSelected(m_settings.getBinNaming().equals(BinNaming.EDGES));
        m_binNamingMidpoints.setSelected(m_settings.getBinNaming().equals(BinNaming.MIDPOINTS));
        m_replaceColumn.setSelected(m_settings.getReplaceColumn());
        m_integerBounds.setSelected(m_settings.getIntegerBounds());
    }

    private static String implode(final double[] values) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length - 1; i++) {
            builder.append(Double.toString(values[i]));
            builder.append(", ");
        }
        if (values.length > 0) {
            builder.append(values[values.length - 1]);
        }
        return builder.toString();
    }

    /**
     * Saves the settings for node dialog.
     *
     * @param settings write intervals to
     * @throws InvalidSettingsException if a bin name is empty
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final DataColumnSpecFilterConfiguration config = AutoBinnerLearnSettings.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        m_settings.setFilterConfiguration(config);

        final BinningMethod method =
            m_methodFixedNumber.isSelected() ? BinningMethod.FIXED_NUMBER : BinningMethod.SAMPLE_QUANTILES;
        m_settings.setMethod(method);
        m_settings.setBinCount(((Number)m_numBins.getValue()).intValue());
        m_settings.setEqualityMethod((EqualityMethod)m_equalityMethod.getSelectedItem());
        try {
            final List<Double> sampleQuantiles = explode(m_sampleQuantiles.getText());
            m_settings.setSampleQuantiles(normalizeSampleQuantiles(sampleQuantiles));
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException("Cannot parse the list of quantiles. Changes are not applied.");
        }
        BinNaming binNaming = null;
        if (m_binNamingNumbered.isSelected()) {
            binNaming = BinNaming.NUMBERED;
        }
        if (m_binNamingEdges.isSelected()) {
            binNaming = BinNaming.EDGES;
        }
        if (m_binNamingMidpoints.isSelected()) {
            binNaming = BinNaming.MIDPOINTS;
        }
        m_settings.setBinNaming(binNaming);
        m_settings.setReplaceColumn(m_replaceColumn.isSelected());
        m_settings.setIntegerBounds(m_integerBounds.isSelected());

        m_settings.saveSettings(settings);
    }

    private static List<Double> explode(final String str) {
        final StringTokenizer tokenizer = new StringTokenizer(str, ", \t");
        final int tokenCount = tokenizer.countTokens();
        final List<Double> values = new ArrayList<>();
        for (int i = 0; i < tokenCount; i++) {
            values.add(Double.valueOf(tokenizer.nextToken()));
        }
        return values;
    }

    private static double[] normalizeSampleQuantiles(final List<Double> sampleQuantiles) {
        // remove elements which are either < 0 or > 1
        sampleQuantiles.removeIf(d -> d < 0 || d > 1);
        Collections.sort(sampleQuantiles);
        // remove equal elements
        double prev = -1.0;
        double curr;
        for (Iterator<Double> iter = sampleQuantiles.iterator(); iter.hasNext();) {
            curr = iter.next();
            if (prev == curr) {
                iter.remove();
            } else {
                prev = curr;
            }
        }
        // add zero if not present
        if (sampleQuantiles.get(0) != 0) {
            sampleQuantiles.add(0, 0.0);
        }
        // add one if not present
        if (sampleQuantiles.get(sampleQuantiles.size() - 1) != 1) {
            sampleQuantiles.add(1.0);
        }
        final double[] quantilesArray = new double[sampleQuantiles.size()];
        for (int i = 0; i < quantilesArray.length; i++) {
            quantilesArray[i] = sampleQuantiles.get(i);
        }
        return quantilesArray;
    }
}

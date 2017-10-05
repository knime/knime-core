/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   22.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.panels;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ViewUtils;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class AdvancedOptionsPanel extends JPanel {

    private final JCheckBox m_dataFractionPerTreeChecker;

    private final JSpinner m_dataFractionPerTreeSpinner;

    private final JRadioButton m_dataSamplingWithReplacementChecker;

    private final JRadioButton m_dataSamplingWithOutReplacementChecker;

    private final JRadioButton m_columnFractionNoneButton;

    private final JRadioButton m_columnFractionSqrtButton;

    private final JRadioButton m_columnFractionLinearButton;

    private final JSpinner m_columnFractionLinearTreeSpinner;

    private final JRadioButton m_columnFractionAbsoluteButton;

    private final JSpinner m_columnFractionAbsoluteTreeSpinner;

    private final JRadioButton m_columnUseSameSetOfAttributesForNodes;

    private final JRadioButton m_columnUseDifferentSetOfAttributesForNodes;

    private final JCheckBox m_seedChecker;

    private final JButton m_newSeedButton;

    private final JTextField m_seedTextField;

    private final JCheckBox m_useAverageSplitPointsChecker;

    private final JCheckBox m_useBinaryNominalSplitsChecker;

    private final JSpinner m_alphaFractionSpinner;

    private final JComboBox<MissingValueHandling> m_missingValueHandlingComboBox;

    private final boolean m_isRegression;

    public AdvancedOptionsPanel(final boolean isRegression) {
        super(new GridBagLayout());
        m_isRegression = isRegression;
        m_alphaFractionSpinner = new JSpinner(new SpinnerNumberModel(GradientBoostingLearnerConfiguration.DEF_ALPHA_FRACTION, 0, 1, 0.05));
        m_dataSamplingWithOutReplacementChecker = new JRadioButton("Without replacement");
        m_dataSamplingWithReplacementChecker = new JRadioButton("With replacement");
        ButtonGroup samplingButtonGroup = new ButtonGroup();
        samplingButtonGroup.add(m_dataSamplingWithOutReplacementChecker);
        samplingButtonGroup.add(m_dataSamplingWithReplacementChecker);
        m_dataSamplingWithOutReplacementChecker.doClick();

        m_dataFractionPerTreeSpinner =
            new JSpinner(new SpinnerNumberModel(TreeEnsembleLearnerConfiguration.DEF_DATA_FRACTION, 0.001, 1.0, 0.1));
        m_dataFractionPerTreeChecker = new JCheckBox("Fraction of data to learn single model");
        m_dataFractionPerTreeChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean enbl = m_dataFractionPerTreeChecker.isSelected();
                if (!enbl) {
                    m_dataSamplingWithOutReplacementChecker.doClick();
                }
                m_dataFractionPerTreeSpinner.setEnabled(enbl);
                m_dataSamplingWithOutReplacementChecker.setEnabled(enbl);
                m_dataSamplingWithReplacementChecker.setEnabled(enbl);
            }
        });
        m_dataFractionPerTreeChecker.doClick();

        m_columnFractionNoneButton = new JRadioButton("All columns (no sampling)");
        m_columnFractionSqrtButton = new JRadioButton("Sample (square root)");
        m_columnFractionLinearButton = new JRadioButton("Sample (linear fraction)  ");
        m_columnFractionAbsoluteButton = new JRadioButton("Sample (absolute value)  ");

        ButtonGroup columnFractionButtonGroup = new ButtonGroup();
        columnFractionButtonGroup.add(m_columnFractionNoneButton);
        columnFractionButtonGroup.add(m_columnFractionSqrtButton);
        columnFractionButtonGroup.add(m_columnFractionLinearButton);
        columnFractionButtonGroup.add(m_columnFractionAbsoluteButton);
        m_columnFractionLinearTreeSpinner =
            new JSpinner(new SpinnerNumberModel(TreeEnsembleLearnerConfiguration.DEF_COLUMN_FRACTION, 0.001, 1.0, 0.1));
        m_columnFractionLinearButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_columnFractionLinearButton.isSelected();
                m_columnFractionLinearTreeSpinner.setEnabled(s);
                if (s) {
                    m_columnFractionLinearTreeSpinner.requestFocus();
                }
            }
        });
        m_columnFractionLinearButton.doClick();
        m_columnFractionAbsoluteTreeSpinner =
            new JSpinner(new SpinnerNumberModel(TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE, 1,
                Integer.MAX_VALUE, 1));
        m_columnFractionAbsoluteButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_columnFractionAbsoluteButton.isSelected();
                m_columnFractionAbsoluteTreeSpinner.setEnabled(s);
                if (s) {
                    m_columnFractionAbsoluteTreeSpinner.requestFocus();
                }
            }
        });
        m_columnFractionAbsoluteButton.doClick();

        m_columnUseSameSetOfAttributesForNodes = new JRadioButton("Use same set of attributes for entire tree");
        m_columnUseDifferentSetOfAttributesForNodes =
            new JRadioButton("Use different set of attributes for each tree node");
        ButtonGroup attrSelectButtonGroup = new ButtonGroup();
        attrSelectButtonGroup.add(m_columnUseSameSetOfAttributesForNodes);
        attrSelectButtonGroup.add(m_columnUseDifferentSetOfAttributesForNodes);
        m_columnUseSameSetOfAttributesForNodes.doClick();

        m_seedTextField = new JTextField(20);
        m_newSeedButton = new JButton("New");
        m_seedChecker = new JCheckBox("Use static random seed");
        m_seedChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean selected = m_seedChecker.isSelected();
                m_seedTextField.setEnabled(selected);
                m_newSeedButton.setEnabled(selected);
            }
        });
        m_newSeedButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_seedTextField.setText(Long.toString(new Random().nextLong()));
            }
        });
        m_seedChecker.doClick();

        m_useAverageSplitPointsChecker = new JCheckBox("Use mid point splits (only for numeric attributes)");
        m_useBinaryNominalSplitsChecker = new JCheckBox("Use binary splits for nominal columns");
        m_missingValueHandlingComboBox = new JComboBox<MissingValueHandling>(MissingValueHandling.values());

        initPanel();
    }

    private void initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        Insets defaultInsets = new Insets(5, 5, 5, 5);
        Insets noYSpaceInsets = new Insets(0, 5, 0, 5);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Tree Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2;
        add(m_useAverageSplitPointsChecker, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2;
        add(m_useBinaryNominalSplitsChecker, gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridwidth = 2;
        add(new JLabel("Missing value handling"), gbc);
        gbc.gridx = 1;
        add(m_missingValueHandlingComboBox, gbc);


        gbc.insets = defaultInsets;
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        if (m_isRegression) {
            gbc.gridy += 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            add(new JLabel("Boosting Options"), gbc);
            gbc.gridwidth = 1;

            gbc.gridy += 1;
            add(new JLabel("Alpha (percentage of the data that are not treated as outlier)"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            add(m_alphaFractionSpinner, gbc);

            gbc.gridy += 1;
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.gridwidth = 3;
            add(new JSeparator(), gbc);
        }

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Bagging Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Data Sampling (Rows)"), gbc);
        gbc.insets = noYSpaceInsets;
        gbc.gridx += 1;
        add(m_dataFractionPerTreeChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_dataFractionPerTreeSpinner, gbc);

        gbc.weightx = 0.0;
        gbc.gridy += 1;
        gbc.gridx = 1;
        add(m_dataSamplingWithReplacementChecker, gbc);
        gbc.gridx += 1;
        add(m_dataSamplingWithOutReplacementChecker, gbc);

        gbc.insets = defaultInsets;
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Attribute Sampling (Columns)"), gbc);
        gbc.insets = noYSpaceInsets;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        add(m_columnFractionNoneButton, gbc);
        gbc.gridy += 1;
        add(m_columnFractionSqrtButton, gbc);
        gbc.gridy += 1;
        gbc.gridwidth = 1;
        add(m_columnFractionLinearButton, gbc);
        gbc.gridx += 1;
        add(m_columnFractionLinearTreeSpinner, gbc);
        gbc.gridx = 1;
        gbc.gridy += 1;
        add(m_columnFractionAbsoluteButton, gbc);
        gbc.gridx += 1;
        add(m_columnFractionAbsoluteTreeSpinner, gbc);
        gbc.insets = defaultInsets;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JLabel(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Attribute Selection"), gbc);
        gbc.insets = noYSpaceInsets;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        add(m_columnUseSameSetOfAttributesForNodes, gbc);
        gbc.gridy += 1;
        add(m_columnUseDifferentSetOfAttributesForNodes, gbc);
        gbc.insets = defaultInsets;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(m_seedChecker, gbc);
        gbc.gridx += 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        add(ViewUtils.getInFlowLayout(FlowLayout.LEFT, m_seedTextField, m_newSeedButton), gbc);
    }

    public void loadSettings(final GradientBoostingLearnerConfiguration cfg) {
        m_alphaFractionSpinner.setValue(cfg.getAlpha());
        m_useAverageSplitPointsChecker.setSelected(cfg.isUseAverageSplitPoints());
        m_useBinaryNominalSplitsChecker.setSelected(cfg.isUseBinaryNominalSplits());
        m_missingValueHandlingComboBox.setSelectedItem(cfg.getMissingValueHandling());

        double dataFrac = cfg.getDataFractionPerTree();
        boolean isDataWithReplacement = cfg.isDataSelectionWithReplacement();
        boolean doesSampling = dataFrac < 1.0 || isDataWithReplacement;

        m_dataFractionPerTreeSpinner.setValue(dataFrac);
        if (m_dataFractionPerTreeChecker.isSelected() != doesSampling) {
            m_dataFractionPerTreeChecker.doClick();
        }
        if (isDataWithReplacement) {
            m_dataSamplingWithReplacementChecker.doClick();
        } else {
            m_dataSamplingWithOutReplacementChecker.doClick();
        }

        double colFrac = cfg.getColumnFractionLinearValue();
        int colAbsolute = cfg.getColumnAbsoluteValue();
        boolean useDifferentAttributesAtEachNode = cfg.isUseDifferentAttributesAtEachNode();
        ColumnSamplingMode columnFraction = cfg.getColumnSamplingMode();
        switch (columnFraction) {
            case None:
                m_columnFractionNoneButton.doClick();
                useDifferentAttributesAtEachNode = false;
                colFrac = 1.0;
                break;
            case Linear:
                m_columnFractionLinearButton.doClick();
                break;
            case Absolute:
                m_columnFractionAbsoluteButton.doClick();
                break;
            case SquareRoot:
                m_columnFractionSqrtButton.doClick();
                colFrac = 1.0;
                break;
        }
        m_columnFractionLinearTreeSpinner.setValue(colFrac);
        m_columnFractionAbsoluteTreeSpinner.setValue(colAbsolute);
        if (useDifferentAttributesAtEachNode) {
            m_columnUseDifferentSetOfAttributesForNodes.doClick();
        } else {
            m_columnUseSameSetOfAttributesForNodes.doClick();
        }

        Long seed = cfg.getSeed();
        if (m_seedChecker.isSelected() != (seed != null)) {
            m_seedChecker.doClick();
        }
        m_seedTextField.setText(Long.toString(seed != null ? seed : System.currentTimeMillis()));
    }

    public void saveSettings(final GradientBoostingLearnerConfiguration cfg) throws InvalidSettingsException {
        cfg.setAlpha((Double)m_alphaFractionSpinner.getValue());
        cfg.setUseAverageSplitPoints(m_useAverageSplitPointsChecker.isSelected());
        cfg.setUseBinaryNominalSplits(m_useBinaryNominalSplitsChecker.isSelected());
        final MissingValueHandling missValHandling = (MissingValueHandling)m_missingValueHandlingComboBox.getSelectedItem();
        if (missValHandling == MissingValueHandling.Surrogate && !m_useBinaryNominalSplitsChecker.isSelected()) {
            throw new InvalidSettingsException("Surrogate missing value handling can only be used if binary nominal splits are enabled.");
        }
        cfg.setMissingValueHandling((MissingValueHandling)m_missingValueHandlingComboBox.getSelectedItem());

        double dataFrac;
        boolean isSamplingWithReplacement;
        if (m_dataFractionPerTreeChecker.isSelected()) {
            dataFrac = (Double)m_dataFractionPerTreeSpinner.getValue();
            isSamplingWithReplacement = m_dataSamplingWithReplacementChecker.isSelected();
        } else {
            dataFrac = 1.0;
            isSamplingWithReplacement = false;
        }
        cfg.setDataFractionPerTree(dataFrac);
        cfg.setDataSelectionWithReplacement(isSamplingWithReplacement);

        ColumnSamplingMode cf;
        double columnFrac = 1.0;
        int columnAbsolute = TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE;
        boolean isUseDifferentAttributesAtEachNode = m_columnUseDifferentSetOfAttributesForNodes.isSelected();
        if (m_columnFractionNoneButton.isSelected()) {
            cf = ColumnSamplingMode.None;
            isUseDifferentAttributesAtEachNode = false;
        } else if (m_columnFractionLinearButton.isSelected()) {
            cf = ColumnSamplingMode.Linear;
            columnFrac = (Double)m_columnFractionLinearTreeSpinner.getValue();
        } else if (m_columnFractionAbsoluteButton.isSelected()) {
            cf = ColumnSamplingMode.Absolute;
            columnAbsolute = (Integer)m_columnFractionAbsoluteTreeSpinner.getValue();
        } else if (m_columnFractionSqrtButton.isSelected()) {
            cf = ColumnSamplingMode.SquareRoot;
        } else {
            throw new InvalidSettingsException("No column selection policy selected");
        }
        cfg.setColumnSamplingMode(cf);
        cfg.setColumnFractionLinearValue(columnFrac);
        cfg.setColumnAbsoluteValue(columnAbsolute);
        cfg.setUseDifferentAttributesAtEachNode(isUseDifferentAttributesAtEachNode);

        Long seed;
        if (m_seedChecker.isSelected()) {
            final String seedText = m_seedTextField.getText();
            try {
                seed = Long.valueOf(seedText);
            } catch (Exception e) {
                throw new InvalidSettingsException("Unable to parse seed \"" + seedText + "\"", e);
            }
        } else {
            seed = null;
        }
        cfg.setSeed(seed);
    }
}

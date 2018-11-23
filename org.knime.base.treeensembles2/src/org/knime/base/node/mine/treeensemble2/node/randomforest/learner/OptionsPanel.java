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
 *
 * History
 *   Jun 9, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.randomforest.learner;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;
import java.util.Set;

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
import javax.swing.border.Border;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class OptionsPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OptionsPanel.class);

    static final DataTableSpec NO_VALID_INPUT_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("<no valid input>", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("<no valid fingerprint input>", DenseBitVectorCell.TYPE).createSpec());

    // Attribute selection

    private final ColumnSelectionComboxBox m_targetColumnBox;

    private final JRadioButton m_useFingerprintColumnRadio;

    private final JRadioButton m_useOrdinaryColumnsRadio;

    private final ColumnSelectionComboxBox m_fingerprintColumnBox;

    private final DataColumnSpecFilterPanel m_includeColumnsFilterPanel2;

    private final JCheckBox m_enableHiliteChecker;

    private final JSpinner m_hiliteCountSpinner;

    private final JCheckBox m_saveTargetDistributionInNodesChecker;

    // Tree Options

    private final JComboBox<SplitCriterion> m_splitCriterionsBox;

    private final JCheckBox m_maxLevelChecker;

    private final JSpinner m_maxLevelSpinner;

    private final JCheckBox m_minChildNodeSizeChecker;

    private final JSpinner m_minChildNodeSizeSpinner;

    // Forest Options

    private final JCheckBox m_seedChecker;

    private final JSpinner m_nrModelsSpinner;

    private final JButton m_newSeedButton;

    private final JTextField m_seedTextField;

    private final boolean m_isRegression;

    /**
     * @param isRegression
     */
    @SuppressWarnings("unchecked")
    public OptionsPanel(final boolean isRegression) {
        super(new GridBagLayout());
        Class<? extends DataValue> targetClass = isRegression ? DoubleValue.class : NominalValue.class;
        m_targetColumnBox = new ColumnSelectionComboxBox((Border)null, targetClass);
        m_targetColumnBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    newTargetSelected((DataColumnSpec)e.getItem());
                }
            }
        });
        m_fingerprintColumnBox = new ColumnSelectionComboxBox((Border)null,
            new DataValueColumnFilter(BitVectorValue.class, ByteVectorValue.class, DoubleVectorValue.class));
        m_includeColumnsFilterPanel2 = new DataColumnSpecFilterPanel();

        m_useFingerprintColumnRadio = new JRadioButton("Use fingerprint attribute");
        m_useOrdinaryColumnsRadio = new JRadioButton("Use column attributes");
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_useFingerprintColumnRadio);
        bg.add(m_useOrdinaryColumnsRadio);
        ActionListener actListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                boolean isFP = bg.getSelection() == m_useFingerprintColumnRadio.getModel();
                m_fingerprintColumnBox.setEnabled(isFP);
                m_includeColumnsFilterPanel2.setEnabled(!isFP);
            }
        };
        m_useFingerprintColumnRadio.addActionListener(actListener);
        m_useOrdinaryColumnsRadio.addActionListener(actListener);
        m_useFingerprintColumnRadio.doClick();
        m_hiliteCountSpinner = new JSpinner(new SpinnerNumberModel(2000, 1, Integer.MAX_VALUE, 100));
        m_enableHiliteChecker = new JCheckBox("Enable Hilighting (#patterns to store)", true);
        m_enableHiliteChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_hiliteCountSpinner.setEnabled(m_enableHiliteChecker.isSelected());
            }
        });
        m_enableHiliteChecker.doClick();
        m_saveTargetDistributionInNodesChecker = new JCheckBox(
            "Save target distribution in tree nodes (memory expensive - only important for tree view and PMML export)");

        // Tree Options

        m_splitCriterionsBox = new JComboBox(SplitCriterion.values());

        m_maxLevelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
        m_maxLevelChecker = new JCheckBox("Limit number of levels (tree depth)");
        m_maxLevelChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_maxLevelSpinner.setEnabled(m_maxLevelChecker.isSelected());
            }
        });
        m_maxLevelChecker.doClick();

        if (isRegression) {
            m_minChildNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1));
        } else {
            m_minChildNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        }
        m_minChildNodeSizeChecker = new JCheckBox("Minimum node size");
        m_minChildNodeSizeChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_minChildNodeSizeChecker.isSelected();
                m_minChildNodeSizeSpinner.setEnabled(s);
            }
        });
        m_minChildNodeSizeChecker.doClick();

        // Forest Options

        m_nrModelsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 2));

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

        m_isRegression = isRegression;
        initPanel();
    }

    private void initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Target Column "), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_targetColumnBox, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel(""), gbc);
        gbc.gridy += 1;
        add(new JLabel("Attribute Selection"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_useFingerprintColumnRadio, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_fingerprintColumnBox, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_useOrdinaryColumnsRadio, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        add(m_includeColumnsFilterPanel2, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel(""), gbc);
        gbc.gridy += 1;
        add(new JLabel("Misc Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        add(m_enableHiliteChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_hiliteCountSpinner, gbc);

        if (!isRegression()) {
            gbc.gridy += 1;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            add(m_saveTargetDistributionInNodesChecker, gbc);
        }

        // Tree Options

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Tree Options"), gbc);
        gbc.gridwidth = 1;

        if (!m_isRegression) {
            // regression doesn't know about gini, info gain, etc.
            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy += 1;
            add(new JLabel("Split Criterion"), gbc);
            gbc.gridx += 1;
            gbc.weightx = 1.0;
            add(m_splitCriterionsBox, gbc);
        }

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_maxLevelChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_maxLevelSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_minChildNodeSizeChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_minChildNodeSizeSpinner, gbc);

        // Forest Options

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        add(new JLabel("Forest Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Number of models"), gbc);
        gbc.gridwidth = 2;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_nrModelsSpinner, gbc);

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

    /**
     * Load settings from config <b>cfg</b> and table spec <b>inSpec</b>
     *
     * @param inSpec
     * @param cfg
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final DataTableSpec inSpec, final TreeEnsembleLearnerConfiguration cfg)
        throws NotConfigurableException {
        int nrNominalCols = 0;
        int nrNumericCols = 0;
        for (DataColumnSpec col : inSpec) {
            DataType type = col.getType();
            if (type.isCompatible(NominalValue.class)) {
                nrNominalCols += 1;
            } else if (type.isCompatible(DoubleValue.class)) {
                nrNumericCols += 1;
            }
        }
        boolean hasOrdinaryColumnsInInput = nrNominalCols > 1 || nrNumericCols > 0;
        boolean hasFPColumnInInput =
            inSpec.containsCompatibleType(BitVectorValue.class) || inSpec.containsCompatibleType(ByteVectorValue.class)
                || inSpec.containsCompatibleType(DoubleVectorValue.class);

        String fpColumn = cfg.getFingerprintColumn();
        m_useOrdinaryColumnsRadio.setEnabled(true);
        m_useFingerprintColumnRadio.setEnabled(true);
        m_useOrdinaryColumnsRadio.doClick(); // default, fix later
        if (hasOrdinaryColumnsInInput) {
            m_includeColumnsFilterPanel2.loadConfiguration(cfg.getColumnFilterConfig(), inSpec);
        } else {
            m_useOrdinaryColumnsRadio.setEnabled(false);
            m_useFingerprintColumnRadio.doClick();
            m_includeColumnsFilterPanel2.loadConfiguration(cfg.getColumnFilterConfig(), NO_VALID_INPUT_SPEC);
        }
        if (hasFPColumnInInput) {
            m_fingerprintColumnBox.update(inSpec, fpColumn);
        } else {
            m_useOrdinaryColumnsRadio.doClick();
            m_fingerprintColumnBox.update(NO_VALID_INPUT_SPEC, "");
            m_useFingerprintColumnRadio.setEnabled(false);
            fpColumn = null;
        }

        if (fpColumn != null || !hasOrdinaryColumnsInInput) {
            m_useFingerprintColumnRadio.doClick();
        } else {
            m_useOrdinaryColumnsRadio.doClick();
        }

        m_targetColumnBox.update(inSpec, cfg.getTargetColumn());

        int hiliteCount = cfg.getNrHilitePatterns();
        if (hiliteCount > 0) {
            m_enableHiliteChecker.setSelected(true);
            m_hiliteCountSpinner.setValue(hiliteCount);
        } else {
            m_enableHiliteChecker.setSelected(false);
            m_hiliteCountSpinner.setValue(2000);
        }
        m_saveTargetDistributionInNodesChecker.setSelected(cfg.isSaveTargetDistributionInNodes());

        // Tree Options

        m_splitCriterionsBox.setSelectedItem(cfg.getSplitCriterion());
        int maxLevel = cfg.getMaxLevels();
        if ((maxLevel != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) != m_maxLevelChecker.isSelected()) {
            m_maxLevelChecker.doClick();
        }
        if (maxLevel == TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            m_maxLevelSpinner.setValue(10);
        } else {
            m_maxLevelSpinner.setValue(maxLevel);
        }

        int minChildNodeSize = cfg.getMinChildSize();
        if ((minChildNodeSize != TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED) != m_minChildNodeSizeChecker
            .isSelected()) {
            m_minChildNodeSizeChecker.doClick();
        }
        if (minChildNodeSize == TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED) {
            if (m_isRegression) {
                m_minChildNodeSizeSpinner.setValue(5);
            } else {
                m_minChildNodeSizeSpinner.setValue(1);
            }
        } else {
            m_minChildNodeSizeSpinner.setValue(minChildNodeSize);
        }

        // Forest Options

        m_nrModelsSpinner.setValue(cfg.getNrModels());

        Long seed = cfg.getSeed();
        if (m_seedChecker.isSelected() != (seed != null)) {
            m_seedChecker.doClick();
        }
        m_seedTextField.setText(Long.toString(seed != null ? seed : System.currentTimeMillis()));
    }

    /**
     * Save settings in config <b>cfg</b>
     *
     * @param cfg
     * @throws InvalidSettingsException
     */
    public void saveSettings(final TreeEnsembleLearnerConfiguration cfg) throws InvalidSettingsException {
        cfg.setTargetColumn(m_targetColumnBox.getSelectedColumn());
        if (m_useFingerprintColumnRadio.isSelected()) {
            String fpColumn = m_fingerprintColumnBox.getSelectedColumn();
            cfg.setFingerprintColumn(fpColumn);
        } else {
            assert m_useOrdinaryColumnsRadio.isSelected();
            Set<String> incls = m_includeColumnsFilterPanel2.getIncludedNamesAsSet();
            if (incls.isEmpty()) {
                throw new InvalidSettingsException("No learn columns selected");
            }
        }
        m_includeColumnsFilterPanel2.saveConfiguration(cfg.getColumnFilterConfig());
        int hiliteCount = m_enableHiliteChecker.isSelected() ? (Integer)m_hiliteCountSpinner.getValue() : -1;
        cfg.setNrHilitePatterns(hiliteCount);
        cfg.setIgnoreColumnsWithoutDomain(true);
        cfg.setSaveTargetDistributionInNodes(false);
        cfg.setSaveTargetDistributionInNodes(m_saveTargetDistributionInNodesChecker.isSelected());

        // Tree Options

        cfg.setSplitCriterion((SplitCriterion)m_splitCriterionsBox.getSelectedItem());
        cfg.setUseAverageSplitPoints(true);

        int maxLevel = m_maxLevelChecker.isSelected() ? (Integer)m_maxLevelSpinner.getValue()
            : TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE;
        cfg.setMaxLevels(maxLevel);

        int minChildNodeSize = m_minChildNodeSizeChecker.isSelected() ? (Integer)m_minChildNodeSizeSpinner.getValue()
            : TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED;
        cfg.setMinSizes(TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED, minChildNodeSize);

        cfg.setHardCodedRootColumn(null);

        // Forest Options

        cfg.setNrModels((Integer)m_nrModelsSpinner.getValue());
        cfg.setDataFractionPerTree(1);
        cfg.setDataSelectionWithReplacement(true);

        ColumnSamplingMode cf = ColumnSamplingMode.SquareRoot;
        double columnFrac = 1.0;
        int columnAbsolute = TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE;

        cfg.setColumnSamplingMode(cf);
        cfg.setColumnFractionLinearValue(columnFrac);
        cfg.setColumnAbsoluteValue(columnAbsolute);
        cfg.setUseDifferentAttributesAtEachNode(true);

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


    /**
     * @param item
     */
    private void newTargetSelected(final DataColumnSpec item) {
        DataColumnSpec col = (DataColumnSpec)m_targetColumnBox.getSelectedItem();
        if (col == null) {
            return;
        }
        m_includeColumnsFilterPanel2.resetHiding();
        m_includeColumnsFilterPanel2.hideNames(col);
    }

    /**
     * @return the isRegression
     */
    boolean isRegression() {
        return m_isRegression;
    }

}

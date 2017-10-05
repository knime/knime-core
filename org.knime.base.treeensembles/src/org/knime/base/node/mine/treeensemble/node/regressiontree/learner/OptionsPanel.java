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
 *   Jun 9, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.regressiontree.learner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class OptionsPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OptionsPanel.class);

    static final DataTableSpec NO_VALID_INPUT_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("<no valid input>", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("<no valid fingerprint input>", DenseBitVectorCell.TYPE).createSpec());


    private final ArrayList<ChangeListener> m_changeListenerList;

    // Attribute selection

    private final ColumnSelectionComboxBox m_targetColumnBox;

    private final JRadioButton m_useFingerprintColumnRadio;

    private final JRadioButton m_useOrdinaryColumnsRadio;

    private final ColumnSelectionComboxBox m_fingerprintColumnBox;

    private final ColumnFilterPanel m_includeColumnsFilterPanel;

    private final JCheckBox m_ignoreColumnsWithoutDomainChecker;

    private final JCheckBox m_enableHiliteChecker;

    private final JSpinner m_hiliteCountSpinner;

//    private final JCheckBox m_saveTargetDistributionInNodesChecker;


    // Tree Options

//    private final JComboBox m_splitCriterionsBox;

    private final JCheckBox m_maxLevelChecker;

    private final JSpinner m_maxLevelSpinner;

//    private final JCheckBox m_useAverageSplitPointsChecker;

    private final JCheckBox m_minNodeSizeChecker;

    private final JSpinner m_minNodeSizeSpinner;

    private final JCheckBox m_minChildNodeSizeChecker;

    private final JSpinner m_minChildNodeSizeSpinner;

//    private final JCheckBox m_hardCodedRootColumnChecker;

//    private final ColumnSelectionComboxBox m_hardCodedRootColumnBox;

    // Forest Options

//    private final JRadioButton m_columnFractionNoneButton;
//
//    private final JRadioButton m_columnFractionSqrtButton;
//
//    private final JRadioButton m_columnFractionLinearButton;
//
//    private final JSpinner m_columnFractionLinearTreeSpinner;
//
//    private final JRadioButton m_columnFractionAbsoluteButton;
//
//    private final JSpinner m_columnFractionAbsoluteTreeSpinner;
//
//    private final JRadioButton m_columnUseSameSetOfAttributesForNodes;
//
//    private final JRadioButton m_columnUseDifferentSetOfAttributesForNodes;
//
//    private final JCheckBox m_seedChecker;
//
//    private final JButton m_newSeedButton;
//
//    private final JTextField m_seedTextField;

    // other members
    private DataTableSpec m_lastTableSpec;

    /**
     *  */
    @SuppressWarnings("unchecked")
    public OptionsPanel() {
        super(new GridBagLayout());
        Class<DoubleValue> targetClass = DoubleValue.class;
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
            new DataValueColumnFilter(BitVectorValue.class, ByteVectorValue.class));
        m_includeColumnsFilterPanel = new ColumnFilterPanel(true, NominalValue.class, DoubleValue.class);

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
                m_includeColumnsFilterPanel.setEnabled(!isFP);
            }
        };
        m_useFingerprintColumnRadio.addActionListener(actListener);
        m_useOrdinaryColumnsRadio.addActionListener(actListener);
        m_useFingerprintColumnRadio.doClick();
        m_ignoreColumnsWithoutDomainChecker = new JCheckBox("Ignore columns without domain information");
        m_hiliteCountSpinner = new JSpinner(new SpinnerNumberModel(2000, 1, Integer.MAX_VALUE, 100));
        m_enableHiliteChecker = new JCheckBox("Enable Hilighting (#patterns to store)", true);
        m_enableHiliteChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_hiliteCountSpinner.setEnabled(m_enableHiliteChecker.isSelected());
            }
        });
        m_enableHiliteChecker.doClick();
//        m_saveTargetDistributionInNodesChecker = new JCheckBox(
//            "Save target distribution in tree nodes (memory expensive - only important for tree view and PMML export)");


        // Tree Options

//        m_splitCriterionsBox = new JComboBox(SplitCriterion.values());

        m_maxLevelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
        m_maxLevelChecker = new JCheckBox("Limit number of levels (tree depth)");
        m_maxLevelChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_maxLevelSpinner.setEnabled(m_maxLevelChecker.isSelected());
            }
        });
        m_maxLevelChecker.doClick();

//        m_useAverageSplitPointsChecker = new JCheckBox("Use mid point splits (only for numeric attributes)");

        m_minNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1));
        m_minNodeSizeChecker = new JCheckBox("Minimum split node size");
        m_minNodeSizeChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_minNodeSizeChecker.isSelected();
                m_minNodeSizeSpinner.setEnabled(s);
            }
        });
        m_minNodeSizeChecker.doClick();

//        m_hardCodedRootColumnBox = new ColumnSelectionComboxBox((Border)null, NominalValue.class, DoubleValue.class);
//        m_hardCodedRootColumnChecker = new JCheckBox("Use fixed root attribute");
//        m_hardCodedRootColumnChecker.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(final ItemEvent e) {
//                m_hardCodedRootColumnBox.setEnabled(m_hardCodedRootColumnChecker.isSelected());
//            }
//        });
//        m_hardCodedRootColumnChecker.doClick();

        m_minChildNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1));
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

//        m_columnFractionNoneButton = new JRadioButton("All columns (no sampling)");
//        m_columnFractionSqrtButton = new JRadioButton("Sample (square root)");
//        m_columnFractionLinearButton = new JRadioButton("Sample (linear fraction)  ");
//        m_columnFractionAbsoluteButton = new JRadioButton("Sample (absolute value)  ");

//        ButtonGroup columnFractionButtonGroup = new ButtonGroup();
//        columnFractionButtonGroup.add(m_columnFractionNoneButton);
//        columnFractionButtonGroup.add(m_columnFractionSqrtButton);
//        columnFractionButtonGroup.add(m_columnFractionLinearButton);
//        columnFractionButtonGroup.add(m_columnFractionAbsoluteButton);
//        m_columnFractionLinearTreeSpinner =
//            new JSpinner(new SpinnerNumberModel(TreeEnsembleLearnerConfiguration.DEF_COLUMN_FRACTION, 0.001, 1.0, 0.1));
//        m_columnFractionLinearButton.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(final ItemEvent e) {
//                final boolean s = m_columnFractionLinearButton.isSelected();
//                m_columnFractionLinearTreeSpinner.setEnabled(s);
//                if (s) {
//                    m_columnFractionLinearTreeSpinner.requestFocus();
//                }
//            }
//        });
//        m_columnFractionLinearButton.doClick();
//        m_columnFractionAbsoluteTreeSpinner =
//            new JSpinner(new SpinnerNumberModel(TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE, 1,
//                Integer.MAX_VALUE, 1));
//        m_columnFractionAbsoluteButton.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(final ItemEvent e) {
//                final boolean s = m_columnFractionAbsoluteButton.isSelected();
//                m_columnFractionAbsoluteTreeSpinner.setEnabled(s);
//                if (s) {
//                    m_columnFractionAbsoluteTreeSpinner.requestFocus();
//                }
//            }
//        });
//        m_columnFractionAbsoluteButton.doClick();
//
//        m_columnUseSameSetOfAttributesForNodes = new JRadioButton("Use same set of attributes for entire tree");
//        m_columnUseDifferentSetOfAttributesForNodes =
//            new JRadioButton("Use different set of attributes for each tree node");
//        ButtonGroup attrSelectButtonGroup = new ButtonGroup();
//        attrSelectButtonGroup.add(m_columnUseSameSetOfAttributesForNodes);
//        attrSelectButtonGroup.add(m_columnUseDifferentSetOfAttributesForNodes);
//        m_columnUseSameSetOfAttributesForNodes.doClick();
//
//        m_seedTextField = new JTextField(20);
//        m_newSeedButton = new JButton("New");
//        m_seedChecker = new JCheckBox("Use static random seed");
//        m_seedChecker.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(final ItemEvent e) {
//                final boolean selected = m_seedChecker.isSelected();
//                m_seedTextField.setEnabled(selected);
//                m_newSeedButton.setEnabled(selected);
//            }
//        });
//        m_newSeedButton.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                m_seedTextField.setText(Long.toString(new Random().nextLong()));
//            }
//        });
//        m_seedChecker.doClick();


        m_changeListenerList = new ArrayList<ChangeListener>();
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
        add(m_includeColumnsFilterPanel, gbc);

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
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        add(m_ignoreColumnsWithoutDomainChecker, gbc);

        gbc.gridy += 1;
        add(m_enableHiliteChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_hiliteCountSpinner, gbc);


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

//        gbc.gridx = 0;
//        gbc.weightx = 0.0;
//        gbc.gridwidth = 2;
//        add(m_useAverageSplitPointsChecker, gbc);
//        gbc.gridwidth = 1;

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
        add(m_minNodeSizeChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_minNodeSizeSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_minChildNodeSizeChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_minChildNodeSizeSpinner, gbc);

//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 0.0;
//        add(m_hardCodedRootColumnChecker, gbc);
//        gbc.gridx += 1;
//        gbc.weightx = 1.0;
//        add(m_hardCodedRootColumnBox, gbc);


        // Forest Options

//        Insets defaultInsets = new Insets(5, 5, 5, 5);
//        Insets noYSpaceInsets = new Insets(0, 5, 0, 5);
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 1.0;
//        gbc.gridwidth = 3;
//        add(new JSeparator(), gbc);
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        add(new JLabel("Forest Options"), gbc);
//        gbc.gridwidth = 1;
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 0.0;
//        gbc.gridwidth = 1;
//        add(new JLabel("Attribute Sampling (Columns)"), gbc);
//        gbc.insets = noYSpaceInsets;
//        gbc.gridx += 1;
//        gbc.weightx = 1.0;
//        gbc.gridwidth = 2;
//        add(m_columnFractionNoneButton, gbc);
//        gbc.gridy += 1;
//        add(m_columnFractionSqrtButton, gbc);
//        gbc.gridy += 1;
//        gbc.gridwidth = 1;
//        add(m_columnFractionLinearButton, gbc);
//        gbc.gridx += 1;
//        add(m_columnFractionLinearTreeSpinner, gbc);
//        gbc.gridx = 1;
//        gbc.gridy += 1;
//        add(m_columnFractionAbsoluteButton, gbc);
//        gbc.gridx += 1;
//        add(m_columnFractionAbsoluteTreeSpinner, gbc);
//        gbc.insets = defaultInsets;
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 1.0;
//        gbc.gridwidth = 3;
//        add(new JLabel(), gbc);
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 0.0;
//        gbc.gridwidth = 1;
//        add(new JLabel("Attribute Selection"), gbc);
//        gbc.insets = noYSpaceInsets;
//        gbc.gridx += 1;
//        gbc.weightx = 1.0;
//        gbc.gridwidth = 2;
//        add(m_columnUseSameSetOfAttributesForNodes, gbc);
//        gbc.gridy += 1;
//        add(m_columnUseDifferentSetOfAttributesForNodes, gbc);
//        gbc.insets = defaultInsets;
//
//        gbc.gridy += 1;
//        gbc.gridx = 0;
//        gbc.weightx = 0.0;
//        gbc.gridwidth = 1;
//        add(m_seedChecker, gbc);
//        gbc.gridx += 1;
//        gbc.gridwidth = 2;
//        gbc.weightx = 1.0;
//        add(ViewUtils.getInFlowLayout(FlowLayout.LEFT, m_seedTextField, m_newSeedButton), gbc);

    }

    void addChangeListener(final ChangeListener listener) {
        m_changeListenerList.add(listener);
    }

    void removeChangeListener(final ChangeListener listener) {
        m_changeListenerList.remove(listener);
    }

    /**
     * Load settings from config <b>cfg</b>
     *
     * @param inSpec
     * @param cfg
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final DataTableSpec inSpec, final TreeEnsembleLearnerConfiguration cfg)
        throws NotConfigurableException {
        m_lastTableSpec = null; // disabled automatic propagation of table specs
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
        boolean hasFPColumnInInput = inSpec.containsCompatibleType(BitVectorValue.class) || inSpec.containsCompatibleType(ByteVectorValue.class);
        m_targetColumnBox.update(inSpec, cfg.getTargetColumn());
        DataTableSpec attSpec = removeColumn(inSpec, m_targetColumnBox.getSelectedColumn());
        String fpColumn = cfg.getFingerprintColumn();
        boolean includeAll = cfg.isIncludeAllColumns();
        String[] includeCols = cfg.getIncludeColumns();
        if (includeCols == null) {
            includeCols = new String[0];
        }
        m_useOrdinaryColumnsRadio.setEnabled(true);
        m_useFingerprintColumnRadio.setEnabled(true);
//        m_useByteVectorColumnRadio.setEnabled(true);
        m_useOrdinaryColumnsRadio.doClick(); // default, fix later
        m_includeColumnsFilterPanel.setKeepAllSelected(includeAll);
        if (hasOrdinaryColumnsInInput) {
            m_includeColumnsFilterPanel.update(attSpec, false, includeCols);
            m_includeColumnsFilterPanel.setKeepAllSelected(includeAll);
        } else {
            m_useOrdinaryColumnsRadio.setEnabled(false);
            m_useFingerprintColumnRadio.doClick();
            m_includeColumnsFilterPanel.update(NO_VALID_INPUT_SPEC, true);
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

//        boolean ignoreColsNoDomain = cfg.isIgnoreColumnsWithoutDomain();
//        m_ignoreColumnsWithoutDomainChecker.setSelected(ignoreColsNoDomain);
        int hiliteCount = cfg.getNrHilitePatterns();
        if (hiliteCount > 0) {
            m_enableHiliteChecker.setSelected(true);
            m_hiliteCountSpinner.setValue(hiliteCount);
        } else {
            m_enableHiliteChecker.setSelected(false);
            m_hiliteCountSpinner.setValue(2000);
        }
//        m_saveTargetDistributionInNodesChecker.setSelected(cfg.isSaveTargetDistributionInNodes());


        // Tree Options

//        m_splitCriterionsBox.setSelectedItem(cfg.getSplitCriterion());

//        m_useAverageSplitPointsChecker.setSelected(cfg.isUseAverageSplitPoints());
        int maxLevel = cfg.getMaxLevels();
        if ((maxLevel != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) != m_maxLevelChecker.isSelected()) {
            m_maxLevelChecker.doClick();
        }
        if (maxLevel == TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            m_maxLevelSpinner.setValue(10);
        } else {
            m_maxLevelSpinner.setValue(maxLevel);
        }

        int minNodeSize = cfg.getMinNodeSize();
        if ((minNodeSize != TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED) != m_minNodeSizeChecker
            .isSelected()) {
            m_minNodeSizeChecker.doClick();
        }
        if (minNodeSize == TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED) {
            m_minNodeSizeSpinner.setValue(10);
        } else {
            m_minNodeSizeSpinner.setValue(minNodeSize);
        }

        int minChildNodeSize = cfg.getMinChildSize();
        if ((minChildNodeSize != TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED) != m_minChildNodeSizeChecker
            .isSelected()) {
            m_minChildNodeSizeChecker.doClick();
        }
        if (minChildNodeSize == TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED) {
            m_minChildNodeSizeSpinner.setValue(5);
        } else {
            m_minChildNodeSizeSpinner.setValue(minChildNodeSize);
        }

//        String rootCol = cfg.getHardCodedRootColumn();
//        if (hasOrdinaryColumnsInInput) {
//            DataTableSpec attrSpec = getCurrentAttributeSpec();
//            m_hardCodedRootColumnChecker.setEnabled(true);
//            // select root attribute (may be null!), then sync states
//            m_hardCodedRootColumnBox.update(attrSpec, rootCol);
//            String nowSelected = m_hardCodedRootColumnBox.getSelectedColumn();
//            boolean isRootValid = nowSelected.equals(rootCol);
//            if (isRootValid != m_hardCodedRootColumnChecker.isSelected()) {
//                m_hardCodedRootColumnChecker.doClick();
//            }
//        } else {
//            // no appropriate root attribute
//            // --> clear checkbox, disable selection
//            m_hardCodedRootColumnBox.update(NO_VALID_INPUT_SPEC, "");
//            if (m_hardCodedRootColumnChecker.isSelected()) {
//                m_hardCodedRootColumnChecker.doClick();
//            }
//            m_hardCodedRootColumnChecker.setEnabled(false);
//        }

        // Forest Options

//        double colFrac = cfg.getColumnFractionLinearValue();
//        int colAbsolute = cfg.getColumnAbsoluteValue();
//        boolean useDifferentAttributesAtEachNode = cfg.isUseDifferentAttributesAtEachNode();
//        ColumnSamplingMode columnFraction = cfg.getColumnSamplingMode();
//        switch (columnFraction) {
//            case None:
//                m_columnFractionNoneButton.doClick();
//                useDifferentAttributesAtEachNode = false;
//                colFrac = 1.0;
//                break;
//            case Linear:
//                m_columnFractionLinearButton.doClick();
//                break;
//            case Absolute:
//                m_columnFractionAbsoluteButton.doClick();
//                break;
//            case SquareRoot:
//                m_columnFractionSqrtButton.doClick();
//                colFrac = 1.0;
//                break;
//        }
//        m_columnFractionLinearTreeSpinner.setValue(colFrac);
//        m_columnFractionAbsoluteTreeSpinner.setValue(colAbsolute);
//        if (useDifferentAttributesAtEachNode) {
//            m_columnUseDifferentSetOfAttributesForNodes.doClick();
//        } else {
//            m_columnUseSameSetOfAttributesForNodes.doClick();
//        }
//
//
//        Long seed = cfg.getSeed();
//        if (m_seedChecker.isSelected() != (seed != null)) {
//            m_seedChecker.doClick();
//        }
//        m_seedTextField.setText(Long.toString(seed != null ? seed : System.currentTimeMillis()));


        // Other
        m_lastTableSpec = inSpec;
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
            cfg.setIncludeAllColumns(false);
            cfg.setIncludeColumns(null);
        } else {
            assert m_useOrdinaryColumnsRadio.isSelected();
            boolean useAll = m_includeColumnsFilterPanel.isKeepAllSelected();
            if (useAll) {
                cfg.setIncludeAllColumns(true);
            } else {
                cfg.setIncludeAllColumns(false);
                Set<String> incls = m_includeColumnsFilterPanel.getIncludedColumnSet();
                if (incls.size() == 0) {
                    throw new InvalidSettingsException("No learn columns selected");
                }
                String[] includeCols = incls.toArray(new String[incls.size()]);
                cfg.setIncludeColumns(includeCols);
            }
        }
        int hiliteCount = m_enableHiliteChecker.isSelected() ? (Integer)m_hiliteCountSpinner.getValue() : -1;
        cfg.setNrHilitePatterns(hiliteCount);
        cfg.setIgnoreColumnsWithoutDomain(m_ignoreColumnsWithoutDomainChecker.isSelected());
//        cfg.setSaveTargetDistributionInNodes(false);
//        cfg.setIgnoreColumnsWithoutDomain(m_ignoreColumnsWithoutDomainChecker.isSelected());
//        cfg.setSaveTargetDistributionInNodes(m_saveTargetDistributionInNodesChecker.isSelected());

        // Tree Options

        cfg.setSplitCriterion(SplitCriterion.Gini);
//        cfg.setUseAverageSplitPoints(m_useAverageSplitPointsChecker.isSelected());
        cfg.setUseAverageSplitPoints(true);

        int maxLevel =
                m_maxLevelChecker.isSelected() ? (Integer)m_maxLevelSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE;
        cfg.setMaxLevels(maxLevel);

        int minNodeSize =
                m_minNodeSizeChecker.isSelected() ? (Integer)m_minNodeSizeSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED;

        int minChildNodeSize =
                m_minChildNodeSizeChecker.isSelected() ? (Integer)m_minChildNodeSizeSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED;
        cfg.setMinSizes(minNodeSize, minChildNodeSize);

//        String hardCodedRootCol =
//                m_hardCodedRootColumnChecker.isSelected() ? m_hardCodedRootColumnBox.getSelectedColumn() : null;
//        cfg.setHardCodedRootColumn(hardCodedRootCol);
        cfg.setHardCodedRootColumn(null);


        // Forest Options



        ColumnSamplingMode cf = ColumnSamplingMode.None;
        double columnFrac = 1.0;
        int columnAbsolute = TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE;
//        boolean isUseDifferentAttributesAtEachNode = m_columnUseDifferentSetOfAttributesForNodes.isSelected();
//        if (m_columnFractionNoneButton.isSelected()) {
//            cf = ColumnSamplingMode.None;
//            isUseDifferentAttributesAtEachNode = false;
//        } else if (m_columnFractionLinearButton.isSelected()) {
//            cf = ColumnSamplingMode.Linear;
//            columnFrac = (Double)m_columnFractionLinearTreeSpinner.getValue();
//        } else if (m_columnFractionAbsoluteButton.isSelected()) {
//            cf = ColumnSamplingMode.Absolute;
//            columnAbsolute = (Integer)m_columnFractionAbsoluteTreeSpinner.getValue();
//        } else if (m_columnFractionSqrtButton.isSelected()) {
//            cf = ColumnSamplingMode.SquareRoot;
//        } else {
//            throw new InvalidSettingsException("No column selection policy selected");
//        }
        cfg.setColumnSamplingMode(cf);
        cfg.setColumnFractionLinearValue(columnFrac);
        cfg.setColumnAbsoluteValue(columnAbsolute);
//        cfg.setUseDifferentAttributesAtEachNode(isUseDifferentAttributesAtEachNode);
        cfg.setUseDifferentAttributesAtEachNode(false);

//        Long seed;
//        if (m_seedChecker.isSelected()) {
//            final String seedText = m_seedTextField.getText();
//            try {
//                seed = Long.valueOf(seedText);
//            } catch (Exception e) {
//                throw new InvalidSettingsException("Unable to parse seed \"" + seedText + "\"", e);
//            }
//        } else {
//            seed = null;
//        }
//        cfg.setSeed(seed);

    }

    /**
     * Get table spec excluding the currently selected target column.
     *
     * @return table spec with learn attributes
     */
        DataTableSpec getCurrentAttributeSpec() {
        if (m_lastTableSpec == null) {
            throw new IllegalStateException("Not to be called during load");
        }
        return removeColumn(m_lastTableSpec, m_targetColumnBox.getSelectedColumn());
    }

    private static DataTableSpec removeColumn(final DataTableSpec spec, final String col) {
        ColumnRearranger r = new ColumnRearranger(spec);
        r.remove(col);
        return r.createSpec();
    }

    /**
     * @param item
     */
    private void newTargetSelected(final DataColumnSpec item) {
        String col = m_targetColumnBox.getSelectedColumn();
        if (m_lastTableSpec == null || col == null) {
            return;
        }
        DataTableSpec filtered = getCurrentAttributeSpec();
        Set<String> prevIn = m_includeColumnsFilterPanel.getIncludedColumnSet();
        m_includeColumnsFilterPanel.update(filtered, false, prevIn);
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : m_changeListenerList) {
            l.stateChanged(e);
        }
    }

}

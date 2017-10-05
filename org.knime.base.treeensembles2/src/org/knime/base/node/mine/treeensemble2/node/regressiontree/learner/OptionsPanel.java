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
package org.knime.base.node.mine.treeensemble2.node.regressiontree.learner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.ColumnSamplingMode;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
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
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

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

//    private final ColumnFilterPanel m_includeColumnsFilterPanel;
    private final DataColumnSpecFilterPanel m_includeColumnsFilterPanel2;

    private final JCheckBox m_ignoreColumnsWithoutDomainChecker;

    private final JCheckBox m_enableHiliteChecker;

    private final JSpinner m_hiliteCountSpinner;



    // Tree Options

    private final JCheckBox m_maxLevelChecker;

    private final JSpinner m_maxLevelSpinner;

//    private final JCheckBox m_useAverageSplitPointsChecker;

    private final JCheckBox m_useBinaryNominalSplitsCheckBox;

    private final JComboBox<MissingValueHandling> m_missingValueHandlingComboBox;

    private final JCheckBox m_minNodeSizeChecker;

    private final JSpinner m_minNodeSizeSpinner;

    private final JCheckBox m_minChildNodeSizeChecker;

    private final JSpinner m_minChildNodeSizeSpinner;


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
            new DataValueColumnFilter(BitVectorValue.class, ByteVectorValue.class, DoubleVectorValue.class));
//        m_includeColumnsFilterPanel = new ColumnFilterPanel(true, NominalValue.class, DoubleValue.class);
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
//                m_includeColumnsFilterPanel.setEnabled(!isFP);
                m_includeColumnsFilterPanel2.setEnabled(!isFP);
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

        m_useBinaryNominalSplitsCheckBox = new JCheckBox("Use binary splits for nominal attributes");

        m_missingValueHandlingComboBox = new JComboBox<>(MissingValueHandling.values());

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
        gbc.gridwidth = 2;
        add(m_useBinaryNominalSplitsCheckBox, gbc);

        gbc.gridwidth = 2;
        gbc.gridy += 1;
        add(new JLabel("Missing value handling"), gbc);
        gbc.gridx += 1;
        gbc.weightx = 0.0;
        add(m_missingValueHandlingComboBox, gbc);

        gbc.gridwidth = 1;
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
        boolean hasFPColumnInInput =
            inSpec.containsCompatibleType(BitVectorValue.class) || inSpec.containsCompatibleType(ByteVectorValue.class)
                || inSpec.containsCompatibleType(DoubleVectorValue.class);
        m_targetColumnBox.update(inSpec, cfg.getTargetColumn());
        DataTableSpec attSpec = removeColumn(inSpec, m_targetColumnBox.getSelectedColumn());
        String fpColumn = cfg.getFingerprintColumn();
        m_useOrdinaryColumnsRadio.setEnabled(true);
        m_useFingerprintColumnRadio.setEnabled(true);
        m_useOrdinaryColumnsRadio.doClick(); // default, fix later
        if (hasOrdinaryColumnsInInput) {
            m_includeColumnsFilterPanel2.loadConfiguration(cfg.getColumnFilterConfig(), attSpec);
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

        int hiliteCount = cfg.getNrHilitePatterns();
        if (hiliteCount > 0) {
            m_enableHiliteChecker.setSelected(true);
            m_hiliteCountSpinner.setValue(hiliteCount);
        } else {
            m_enableHiliteChecker.setSelected(false);
            m_hiliteCountSpinner.setValue(2000);
        }


        // Tree Options


//        m_useAverageSplitPointsChecker.setSelected(cfg.isUseAverageSplitPoints());

        m_useBinaryNominalSplitsCheckBox.setSelected(cfg.isUseBinaryNominalSplits());

        m_missingValueHandlingComboBox.setSelectedItem(cfg.getMissingValueHandling());

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
            m_minNodeSizeSpinner.setValue(1);
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
//            cfg.setIncludeAllColumns(false);
        } else {
            assert m_useOrdinaryColumnsRadio.isSelected();
//            boolean useAll = m_includeColumnsFilterPanel.isKeepAllSelected();
//            if (useAll) {
//                cfg.setIncludeAllColumns(true);
//            } else {
//                cfg.setIncludeAllColumns(false);
//                Set<String> incls = m_includeColumnsFilterPanel.getIncludedColumnSet();
            Set<String> incls = m_includeColumnsFilterPanel2.getIncludedNamesAsSet();
                if (incls.size() == 0) {
                    throw new InvalidSettingsException("No learn columns selected");
                }
//            }
        }
        m_includeColumnsFilterPanel2.saveConfiguration(cfg.getColumnFilterConfig());
        int hiliteCount = m_enableHiliteChecker.isSelected() ? (Integer)m_hiliteCountSpinner.getValue() : -1;
        cfg.setNrHilitePatterns(hiliteCount);
        cfg.setIgnoreColumnsWithoutDomain(m_ignoreColumnsWithoutDomainChecker.isSelected());
//        cfg.setSaveTargetDistributionInNodes(false);
//        cfg.setIgnoreColumnsWithoutDomain(m_ignoreColumnsWithoutDomainChecker.isSelected());
//        cfg.setSaveTargetDistributionInNodes(m_saveTargetDistributionInNodesChecker.isSelected());

        // Tree Options

        cfg.setSplitCriterion(SplitCriterion.Gini);
//        cfg.setUseAverageSplitPoints(m_useAverageSplitPointsChecker.isSelected());
        cfg.setUseBinaryNominalSplits(m_useBinaryNominalSplitsCheckBox.isSelected());

        final MissingValueHandling missValHandling = (MissingValueHandling)m_missingValueHandlingComboBox.getSelectedItem();
        if (missValHandling == MissingValueHandling.Surrogate && !m_useBinaryNominalSplitsCheckBox.isSelected()) {
            throw new InvalidSettingsException("Surrogate missing value handling only works if binary nominal splits are enabled");
        }
        cfg.setMissingValueHandling(missValHandling);

        cfg.setUseAverageSplitPoints(true);

        int maxLevel = m_maxLevelChecker.isSelected() ? (Integer)m_maxLevelSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE;
        cfg.setMaxLevels(maxLevel);

        int minNodeSize = m_minNodeSizeChecker.isSelected() ? (Integer)m_minNodeSizeSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED;

        int minChildNodeSize = m_minChildNodeSizeChecker.isSelected() ? (Integer)m_minChildNodeSizeSpinner.getValue()
                    : TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED;
        cfg.setMinSizes(minNodeSize, minChildNodeSize);

        cfg.setHardCodedRootColumn(null);


        // Forest Options



        ColumnSamplingMode cf = ColumnSamplingMode.None;
        double columnFrac = 1.0;
        int columnAbsolute = TreeEnsembleLearnerConfiguration.DEF_COLUMN_ABSOLUTE;
        cfg.setColumnSamplingMode(cf);
        cfg.setColumnFractionLinearValue(columnFrac);
        cfg.setColumnAbsoluteValue(columnAbsolute);
        cfg.setUseDifferentAttributesAtEachNode(false);

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

    @SuppressWarnings("null")
    private static String getMissingColSpecName(final DataTableSpec spec, final String[] includedNames,
        final String[] excludedNames) {
        ColumnRearranger r = new ColumnRearranger(spec);
        // remove columns we know from the include list
        for (String colName : includedNames) {
            if (spec.containsName(colName)) {
                r.remove(colName);
            }
        }
        // remove columns we know from the exclude list
        for (String colName : excludedNames) {
            if (spec.containsName(colName)) {
                r.remove(colName);
            }
        }
        DataTableSpec tableSpecWithMissing = r.createSpec();
        DataColumnSpec formerTargetSpec = null;
        // find the remaining compatible column
        // this must be the former target because all other compatible columns
        // were either in the include or exclude list
        for (DataColumnSpec colSpec : tableSpecWithMissing) {
            DataType colType = colSpec.getType();
            if (colType.isCompatible(NominalValue.class) || colType.isCompatible(DoubleValue.class)) {
                formerTargetSpec = colSpec;
                break;
            }
        }
        assert formerTargetSpec != null : "The former target spec is no longer part of the table, please check.";
        return formerTargetSpec.getName();
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
        //        Set<String> prevIn = m_includeColumnsFilterPanel.getIncludedColumnSet();
        //        m_includeColumnsFilterPanel.update(filtered, false, prevIn);
        Set<String> prevIn = m_includeColumnsFilterPanel2.getIncludedNamesAsSet();
        String[] prevInArray = prevIn.toArray(new String[prevIn.size()]);
        Set<String> prevEx = m_includeColumnsFilterPanel2.getExcludedNamesAsSet();
        String[] prevExArray = prevEx.toArray(new String[prevEx.size()]);
        DataColumnSpecFilterConfiguration conf = TreeEnsembleLearnerConfiguration.createColSpecFilterConfig();
        m_includeColumnsFilterPanel2.saveConfiguration(conf);
        EnforceOption prevEnforceOption =
            conf.isEnforceInclusion() ? EnforceOption.EnforceInclusion : EnforceOption.EnforceExclusion;
        String[] prevExWithFormerTarget = Arrays.copyOf(prevExArray, prevEx.size() + 1);
        prevExWithFormerTarget[prevEx.size()] = getMissingColSpecName(filtered, prevInArray, prevExArray);
        conf.loadDefaults(prevInArray, prevExWithFormerTarget, prevEnforceOption);
        m_includeColumnsFilterPanel2.loadConfiguration(conf, filtered);


        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : m_changeListenerList) {
            l.stateChanged(e);
        }
    }

}

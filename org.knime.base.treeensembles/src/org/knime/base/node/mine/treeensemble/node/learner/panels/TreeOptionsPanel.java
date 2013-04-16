/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 9, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.learner.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeOptionsPanel extends JPanel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(TreeOptionsPanel.class);

    private final JComboBox m_splitCriterionsBox;

    private final JCheckBox m_useAverageSplitPointsChecker;

    private final JCheckBox m_maxLevelChecker;

    private final JSpinner m_maxLevelSpinner;

    private final JCheckBox m_minNodeSizeChecker;

    private final JSpinner m_minNodeSizeSpinner;

    private final JCheckBox m_minChildNodeSizeChecker;

    private final JSpinner m_minChildNodeSizeSpinner;

    private final JCheckBox m_hardCodedRootColumnChecker;

    private final ColumnSelectionComboxBox m_hardCodedRootColumnBox;

    private final AttributeSelectionPanel m_attributePanel;

    /**
     *  */
    @SuppressWarnings("unchecked")
    public TreeOptionsPanel(final AttributeSelectionPanel attributePanel) {
        super(new GridBagLayout());
        m_attributePanel = attributePanel;
        m_attributePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                newTargetSelected();
            }
        });
        m_splitCriterionsBox = new JComboBox(SplitCriterion.values());
        m_useAverageSplitPointsChecker =
            new JCheckBox(
            "Use mid point splits (only for numeric attributes)");
        m_maxLevelSpinner = new JSpinner(
                new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
        m_maxLevelChecker =
            new JCheckBox("Limit number of levels (tree depth)");
        m_maxLevelChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_maxLevelSpinner.setEnabled(m_maxLevelChecker.isSelected());
            }
        });
        m_maxLevelChecker.doClick();

        m_minNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_minNodeSizeChecker = new JCheckBox("Minimum split node size");
        m_minNodeSizeChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_minNodeSizeChecker.isSelected();
                m_minNodeSizeSpinner.setEnabled(s);
            }
        });
        m_minNodeSizeChecker.doClick();
        m_minChildNodeSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_minChildNodeSizeChecker = new JCheckBox("Minimum child node size");
        m_minChildNodeSizeChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean s = m_minChildNodeSizeChecker.isSelected();
                m_minChildNodeSizeSpinner.setEnabled(s);
            }
        });
        m_minChildNodeSizeChecker.doClick();
        m_hardCodedRootColumnBox =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class,
                    DoubleValue.class);
        m_hardCodedRootColumnChecker =
            new JCheckBox("Use fixed root attribute");
        m_hardCodedRootColumnChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_hardCodedRootColumnBox
                .setEnabled(m_hardCodedRootColumnChecker.isSelected());
            }
        });
        m_hardCodedRootColumnChecker.doClick();
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

        if (!m_attributePanel.isRegression()) {
            // regression doesn't know about gini, info gain, etc.
            gbc.weightx = 0.0;
            add(new JLabel("Split Criterion"), gbc);
            gbc.gridx += 1;
            gbc.weightx = 1.0;
            add(m_splitCriterionsBox, gbc);
            gbc.gridy += 1;
        }

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2;
        add(m_useAverageSplitPointsChecker, gbc);
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

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_hardCodedRootColumnChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_hardCodedRootColumnBox, gbc);

    }

    public void loadSettingsFrom(final DataTableSpec inSpec,
            final TreeEnsembleLearnerConfiguration cfg)
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
        m_splitCriterionsBox.setSelectedItem(cfg.getSplitCriterion());
        m_useAverageSplitPointsChecker.setSelected(
                cfg.isUseAverageSplitPoints());
        int maxLevel = cfg.getMaxLevels();
        if ((maxLevel != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE)
                != m_maxLevelChecker.isSelected()) {
            m_maxLevelChecker.doClick();
        }
        if (maxLevel == TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            m_maxLevelSpinner.setValue(10);
        } else {
            m_maxLevelSpinner.setValue(maxLevel);
        }

        int minNodeSize = cfg.getMinNodeSize();
        if ((minNodeSize != TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED)
                != m_minNodeSizeChecker.isSelected()) {
            m_minNodeSizeChecker.doClick();
        }
        if (minNodeSize == TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED) {
            m_minNodeSizeSpinner.setValue(1);
        } else {
            m_minNodeSizeSpinner.setValue(minNodeSize);
        }
        int minChildNodeSize = cfg.getMinChildSize();
        if ((minChildNodeSize != TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED)
                != m_minChildNodeSizeChecker.isSelected()) {
            m_minChildNodeSizeChecker.doClick();
        }
        if (minChildNodeSize == TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED) {
            m_minChildNodeSizeSpinner.setValue(1);
        } else {
            m_minChildNodeSizeSpinner.setValue(minChildNodeSize);
        }
        String rootCol = cfg.getHardCodedRootColumn();
        if (hasOrdinaryColumnsInInput) {
            DataTableSpec attSpec = m_attributePanel.getCurrentAttributeSpec();
            m_hardCodedRootColumnChecker.setEnabled(true);
            // select root attribute (may be null!), then sync states
            m_hardCodedRootColumnBox.update(attSpec, rootCol);
            String nowSelected = m_hardCodedRootColumnBox.getSelectedColumn();
            boolean isRootValid = nowSelected.equals(rootCol);
            if (isRootValid != m_hardCodedRootColumnChecker.isSelected()) {
                m_hardCodedRootColumnChecker.doClick();
            }
        } else {
            // no appropriate root attribute
            // --> clear checkbox, disable selection
            m_hardCodedRootColumnBox.update(
                    AttributeSelectionPanel.NO_VALID_INPUT_SPEC, "");
            if (m_hardCodedRootColumnChecker.isSelected()) {
                m_hardCodedRootColumnChecker.doClick();
            }
            m_hardCodedRootColumnChecker.setEnabled(false);
        }
    }

    /**
     */
    private void newTargetSelected() {
        DataTableSpec filtered = m_attributePanel.getCurrentAttributeSpec();
        String prevSelected = m_hardCodedRootColumnBox.getSelectedColumn();

        try {
            m_hardCodedRootColumnBox.update(filtered, prevSelected);
        } catch (NotConfigurableException nfe) {
            LOGGER.coding("Unable to update column list upon target "
                    + "selection change", nfe);
        }
    }

    public void saveSettings(final TreeEnsembleLearnerConfiguration cfg)
    throws InvalidSettingsException {
        cfg.setSplitCriterion((SplitCriterion)m_splitCriterionsBox
                .getSelectedItem());
        cfg.setUseAverageSplitPoints(m_useAverageSplitPointsChecker
                .isSelected());
        int maxLevel = m_maxLevelChecker.isSelected()
        ? (Integer)m_maxLevelSpinner.getValue()
                : TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE;
        cfg.setMaxLevels(maxLevel);

        int minNodeSize = m_minNodeSizeChecker.isSelected()
        ? (Integer)m_minNodeSizeSpinner.getValue()
                : TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED;

        int minChildNodeSize = m_minChildNodeSizeChecker.isSelected()
                ? (Integer)m_minChildNodeSizeSpinner.getValue()
                        : TreeEnsembleLearnerConfiguration.MIN_CHILD_SIZE_UNDEFINED;
        cfg.setMinSizes(minNodeSize, minChildNodeSize);

        String hardCodedRootCol = m_hardCodedRootColumnChecker.isSelected()
        ? m_hardCodedRootColumnBox.getSelectedColumn() : null;
        cfg.setHardCodedRootColumn(hardCodedRootCol);
    }

}

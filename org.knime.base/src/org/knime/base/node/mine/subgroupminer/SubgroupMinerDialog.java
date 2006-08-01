/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   25.10.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.data.bitvector.BitVectorValue;
import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSetTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodedialog.DialogComponent;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnSelection;
import org.knime.core.node.defaultnodedialog.DialogComponentComboBox;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * The dialog for the subgroup miner node. Provides possibilities to adjust the
 * input column for the bitvectors, the minimum support, the desired type of
 * frequent itemsets (free, closed or maximal), the maximal itemset length, how
 * the output table should be sorted and the underlying algorithm.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubgroupMinerDialog extends NodeDialogPane {
    private DialogComponentColumnSelection m_bitVectorColumnComp;

    private DialogComponentNumber m_minSupportComp;

    private DialogComponentComboBox m_itemSetTypeComp;

    private DialogComponentNumber m_itemSetLengthComp;

    private DialogComponentComboBox m_sortByComp;

    private DialogComponentComboBox m_dataStructComp;

    private JCheckBox m_associationRules;

    private DialogComponentNumber m_confidence;

    private List<DialogComponent> m_dialogComponents;

    private JPanel m_panel;

    /**
     * Constructs the dialog for the subgroup miner node by adding the needed
     * default dialog components.
     */
    public SubgroupMinerDialog() {
        super();
        m_dialogComponents = new ArrayList<DialogComponent>();
        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
        m_panel.setPreferredSize(new Dimension(300, 400));
        super.addTab("Default Options", m_panel);

        m_bitVectorColumnComp = new DialogComponentColumnSelection(
                SubgroupMinerModel.CFG_BITVECTOR_COL,
                "Column containing the bit vectors", 0, BitVectorValue.class);
        addDialogComponent(m_bitVectorColumnComp);

        m_minSupportComp = new DialogComponentNumber(
                SubgroupMinerModel.CFG_MIN_SUPPORT, "Minimum support (0-1)",
                0.0d, 1.0, SubgroupMinerModel.DEFAULT_MIN_SUPPORT, 0.1);
        addDialogComponent(m_minSupportComp);

        m_itemSetTypeComp = new DialogComponentComboBox(
                SubgroupMinerModel.CFG_ITEMSET_TYPE, "Item Set Type",
                FrequentItemSet.Type.asStringList());
        addDialogComponent(m_itemSetTypeComp);

        m_itemSetLengthComp = new DialogComponentNumber(
                SubgroupMinerModel.CFG_MAX_ITEMSET_LENGTH,
                "Maximal Itemset Length:", 1, Integer.MAX_VALUE,
                SubgroupMinerModel.DEFAULT_MAX_ITEMSET_LENGTH);
        addDialogComponent(m_itemSetLengthComp);

        m_sortByComp = new DialogComponentComboBox(
                SubgroupMinerModel.CFG_SORT_BY, "Sort output table by: ",
                FrequentItemSetTable.Sorter.asStringList());
        addDialogComponent(m_sortByComp);

        JPanel associationPanel = new JPanel();
        associationPanel.setLayout(new BoxLayout(associationPanel,
                BoxLayout.X_AXIS));
        associationPanel.add(new JLabel("Output association rules"));
        m_associationRules = new JCheckBox();
        m_associationRules.setSelected(false);
        m_associationRules.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent arg0) {
                m_confidence.setEnabled(m_associationRules.isSelected());
                m_sortByComp.setEnabled(!m_associationRules.isSelected());
            }
        });
        associationPanel.add(m_associationRules);
        m_panel.add(associationPanel);

        m_confidence = new DialogComponentNumber(
                SubgroupMinerModel.CFG_CONFIDENCE, "Minimum Confidence:", 0.01,
                1.0, SubgroupMinerModel.DEFAULT_CONFIDENCE, 0.1);
        m_confidence.setEnabled(false);
        addDialogComponent(m_confidence);

        m_dataStructComp = new DialogComponentComboBox(
                SubgroupMinerModel.CFG_UNDERLYING_STRUCT,
                "Underlying data structure: ",
                AprioriAlgorithmFactory.AlgorithmDataStructure.asStringList());
        addDialogComponent(m_dataStructComp);

    }

    /**
     * Adds a dialog component to this dialog pane.
     * 
     * @param diaC the dialog component that should be added
     */
    public void addDialogComponent(final DialogComponent diaC) {
        m_dialogComponents.add(diaC);
        m_panel.add(diaC);
    }

    /**
     * @see org.knime.core.node.NodeDialogPane#loadSettingsFrom(
     *      NodeSettingsRO, org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            for (DialogComponent comp : m_dialogComponents) {
                comp.loadSettingsFrom(settings, specs);

            }
            m_associationRules.setSelected(settings
                    .getBoolean(SubgroupMinerModel.CFG_ASSOCIATION_RULES));
        } catch (InvalidSettingsException ise) {
            // do nothing here, since it is the dialog
        }
    }

    /**
     * @see org.knime.core.node.NodeDialogPane#saveSettingsTo(
     *      NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }
        settings.addBoolean(SubgroupMinerModel.CFG_ASSOCIATION_RULES,
                m_associationRules.isSelected());
    }
}

/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   25.10.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSetTable;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog for the subgroup miner node. Provides possibilities to adjust the
 * input column for the bitvectors, the minimum support, the desired type of
 * frequent itemsets (free, closed or maximal), the maximal itemset length, how
 * the output table should be sorted and the underlying algorithm.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubgroupMinerDialog extends DefaultNodeSettingsPane {
    
    private final DialogComponentColumnNameSelection m_bitVectorColumnComp;

    private final DialogComponentNumber m_minSupportComp;

    private final DialogComponentStringSelection m_itemSetTypeComp;

    private final DialogComponentNumber m_itemSetLengthComp;

    private final DialogComponentStringSelection m_sortByComp;

    private final DialogComponentStringSelection m_dataStructComp;

    private final DialogComponentBoolean m_associationRules;

    private final DialogComponentNumber m_confidence;


    /**
     * Constructs the dialog for the subgroup miner node by adding the needed
     * default dialog components.
     */
    @SuppressWarnings("unchecked")
    public SubgroupMinerDialog() {
        super();
        
        m_bitVectorColumnComp = new DialogComponentColumnNameSelection(
                createBitVectorColumnModel(),
                "Column containing the bit vectors", 0, BitVectorValue.class);
        
        m_minSupportComp = new DialogComponentNumber(
                createMinSupportModel(),
                "Minimum support (0-1)", 0.1);
        
        m_itemSetTypeComp = new DialogComponentStringSelection(
                createItemSetTypeModel(), "Item Set Type",
                FrequentItemSet.Type.asStringList());

        m_itemSetLengthComp = new DialogComponentNumber(
                createItemsetLengthModel(),
                "Maximal Itemset Length:", 1);
        
        m_sortByComp = new DialogComponentStringSelection(
                createSortByModel(), "Sort output table by: ",
                FrequentItemSetTable.Sorter.asStringList());
        

        m_dataStructComp = new DialogComponentStringSelection(
                createAlgorithmModel(),
                "Underlying data structure: ",
                AprioriAlgorithmFactory.AlgorithmDataStructure.asStringList());
        

        // models
        final SettingsModelDoubleBounded confidenceModel 
            = createConfidenceModel();
        final SettingsModelBoolean assocRuleFlag 
            = createAssociationRuleFlagModel();
        
        // components 
        m_confidence = new DialogComponentNumber(
                confidenceModel, "Minimum Confidence:", 0.1);
        

        m_associationRules = new DialogComponentBoolean(
                assocRuleFlag, "Output association rules");
        
        // listener
        assocRuleFlag.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                confidenceModel.setEnabled(assocRuleFlag.getBooleanValue());
            }
        });
        
        
        // adding to panel
        createNewGroup("Itemset Mining");
        addDialogComponent(m_bitVectorColumnComp);
        addDialogComponent(m_minSupportComp);
        addDialogComponent(m_dataStructComp);
        
        createNewGroup("Output");
        addDialogComponent(m_itemSetTypeComp);
        addDialogComponent(m_itemSetLengthComp);
        addDialogComponent(m_sortByComp);
        
        createNewGroup("Association Rules");
        addDialogComponent(m_associationRules);
        addDialogComponent(m_confidence);



    }

    /**
     * 
     * @return settings model for the bitvector column
     */
    static SettingsModelString createBitVectorColumnModel() {
        return new SettingsModelString(
                SubgroupMinerModel.CFG_BITVECTOR_COL, "");
    }
    
    /**
     * 
     * @return settings model for the minimum support
     */
    static SettingsModelDoubleBounded createMinSupportModel() {
        return new SettingsModelDoubleBounded(
                SubgroupMinerModel.CFG_MIN_SUPPORT, 
                SubgroupMinerModel.DEFAULT_MIN_SUPPORT, 0.0, 1.0);
    }
    
    /**
     * 
     * @return settings model for the item set type
     */
    static SettingsModelString createItemSetTypeModel() {
        return new SettingsModelString(SubgroupMinerModel.CFG_ITEMSET_TYPE, 
                FrequentItemSet.Type.CLOSED.name());
    }
    
    /**
     * 
     * @return settings model for the itemset length
     */
    static SettingsModelIntegerBounded createItemsetLengthModel() {
        return new SettingsModelIntegerBounded(
                SubgroupMinerModel.CFG_MAX_ITEMSET_LENGTH,
                SubgroupMinerModel.DEFAULT_MAX_ITEMSET_LENGTH,
                1, Integer.MAX_VALUE);
    }
    
    /**
     * 
     * @return settings model for the sort by method
     */
    static SettingsModelString createSortByModel() {
        return new SettingsModelString(
                SubgroupMinerModel.CFG_SORT_BY,
                FrequentItemSetTable.Sorter.NONE.name());
    }
    
    /**
     * 
     * @return settings model for the association rule creation flag
     */
    static SettingsModelBoolean createAssociationRuleFlagModel() {
        return new SettingsModelBoolean(
                SubgroupMinerModel.CFG_ASSOCIATION_RULES, false);
    }
    
    /**
     * 
     * @return settings model for the confidence 
     */
    static SettingsModelDoubleBounded createConfidenceModel() {
        SettingsModelDoubleBounded model = new SettingsModelDoubleBounded(
                SubgroupMinerModel.CFG_CONFIDENCE,
                SubgroupMinerModel.DEFAULT_CONFIDENCE, 0.0, 1.0);
        model.setEnabled(false);
        return model;
    }
    
    /**
     * 
     * @return settings model for the underlying algorithm
     */
    static SettingsModelString createAlgorithmModel() {
        return new SettingsModelString(
                SubgroupMinerModel.CFG_UNDERLYING_STRUCT, 
                AprioriAlgorithmFactory.AlgorithmDataStructure.ARRAY.name());
    }
}

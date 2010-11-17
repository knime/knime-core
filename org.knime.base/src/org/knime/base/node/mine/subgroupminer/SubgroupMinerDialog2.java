/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.knime.core.data.collection.CollectionDataValue;
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
 * @author Iris Adae, University of Konstanz
 */
public class SubgroupMinerDialog2 extends DefaultNodeSettingsPane {

    private final DialogComponentColumnNameSelection m_transactionCols;

    private final DialogComponentNumber m_minSupportComp;

    private final DialogComponentStringSelection m_itemSetTypeComp;

    private final DialogComponentNumber m_itemSetLengthComp;

    private final DialogComponentStringSelection m_dataStructComp;

    private final DialogComponentBoolean m_associationRules;

    private final DialogComponentNumber m_confidence;


    /**
     * Constructs the dialog for the subgroup miner node by adding the needed
     * default dialog components.
     */
    @SuppressWarnings("unchecked")
    public SubgroupMinerDialog2() {
        super();

        m_transactionCols = new DialogComponentColumnNameSelection(
              createBitVectorColumnModel(),
              "Column containing transactions",
              0, BitVectorValue.class, CollectionDataValue.class);

        m_minSupportComp = new DialogComponentNumber(
                createMinSupportModel(),
                "Minimum support (0-1)", 0.1);

        m_itemSetTypeComp = new DialogComponentStringSelection(
                createItemSetTypeModel(), "Itemset type",
                FrequentItemSet.Type.asStringList());

        m_itemSetLengthComp = new DialogComponentNumber(
                createItemsetLengthModel(),
                "Maximal itemset length:", 1);

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
                confidenceModel, "Minimum confidence:", 0.1, 8);


        m_associationRules = new DialogComponentBoolean(
                assocRuleFlag, "Output association rules");

        // listener
        assocRuleFlag.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                confidenceModel.setEnabled(assocRuleFlag.getBooleanValue());
            }
        });


        // adding to panel
        createNewGroup(" Itemset Mining ");
        addDialogComponent(m_transactionCols);
        addDialogComponent(m_minSupportComp);
        addDialogComponent(m_dataStructComp);

        createNewGroup(" Output ");
        addDialogComponent(m_itemSetTypeComp);
        addDialogComponent(m_itemSetLengthComp);

        createNewGroup(" Association Rules ");
        addDialogComponent(m_associationRules);
        addDialogComponent(m_confidence);
    }

    /**
     *
     * @return settings model for the transaction column
     */
    static SettingsModelString createBitVectorColumnModel() {
        return new SettingsModelString(
                SubgroupMinerModel2.CFG_TRANSACTION_COL, "");
    }

    /**
     *
     * @return settings model for the minimum support
     */
    static SettingsModelDoubleBounded createMinSupportModel() {
        return new SettingsModelDoubleBounded(
                SubgroupMinerModel2.CFG_MIN_SUPPORT,
                SubgroupMinerModel2.DEFAULT_MIN_SUPPORT, 0.0, 1.0);
    }

    /**
     *
     * @return settings model for the item set type
     */
    static SettingsModelString createItemSetTypeModel() {
        return new SettingsModelString(SubgroupMinerModel2.CFG_ITEMSET_TYPE,
                FrequentItemSet.Type.CLOSED.name());
    }

    /**
     *
     * @return settings model for the itemset length
     */
    static SettingsModelIntegerBounded createItemsetLengthModel() {
        return new SettingsModelIntegerBounded(
                SubgroupMinerModel2.CFG_MAX_ITEMSET_LENGTH,
                SubgroupMinerModel2.DEFAULT_MAX_ITEMSET_LENGTH,
                1, Integer.MAX_VALUE);
    }

    /**
     *
     * @return settings model for the association rule creation flag
     */
    static SettingsModelBoolean createAssociationRuleFlagModel() {
        return new SettingsModelBoolean(
                SubgroupMinerModel2.CFG_ASSOCIATION_RULES, false);
    }

    /**
     *
     * @return settings model for the confidence
     */
    static SettingsModelDoubleBounded createConfidenceModel() {
        SettingsModelDoubleBounded model = new SettingsModelDoubleBounded(
                SubgroupMinerModel2.CFG_CONFIDENCE,
                SubgroupMinerModel2.DEFAULT_CONFIDENCE, 0.0, 1.0);
        model.setEnabled(false);
        return model;
    }

    /**
     *
     * @return settings model for the underlying algorithm
     */
    static SettingsModelString createAlgorithmModel() {
        return new SettingsModelString(
                SubgroupMinerModel2.CFG_UNDERLYING_STRUCT,
                AprioriAlgorithmFactory.AlgorithmDataStructure.ARRAY.name());
    }
}

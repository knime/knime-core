/*
 * ------------------------------------------------------------------
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
 *   13.07.2006 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * @author Fabian Dill, University of Konstanz
 */
public class FrequentItemSetModel {

    // private static final NodeLogger LOGGER = NodeLogger
    // .getLogger(FrequentItemSetModel.class);

    // constants for the model
    private static final String TYPE = "modelType";

    private static final String ITEMSET_MODEL = "itemsets";

    private static final String ITEMSET = "itemSet";

    private static final String ITEM = "item";

    private static final String SUPPORT_ABS = "support(absolute)";

    private static final String ITEMSET_SIZE = "itemSetSize";

    private static final String NUMBER_OF_ITEMSETS = "numberItemsets";

    private static final String NAME_MAPPING = "nameMapping";

    private Collection<FrequentItemSet> m_itemSets;

    private List<String> m_nameMapping;

    private int m_itemSetCounter;

    private int m_itemCounter;

    private int m_dbsize;

    private List<FrequentItemSet> m_alwaysFrequent 
        = new ArrayList<FrequentItemSet>();

    /**
     * @param model the model containing information about the itemsets
     * @throws InvalidSettingsException if the model is not valid
     */
    public void loadFromModelContent(final ModelContentRO model)
            throws InvalidSettingsException {
        // if (!model.getString(TYPE, "").equals(ITEMSET_MODEL)) {
        // throw new IllegalArgumentException("Model input is not "
        // + "a frequent itemset model!");
        // }
        m_itemSets = new ArrayList<FrequentItemSet>();
        ModelContentRO itemsetsModel = model.getModelContent(ITEMSET_MODEL);
        String[] nameMapping = itemsetsModel.getStringArray(NAME_MAPPING);
        if (nameMapping.length > 0) {
            m_nameMapping = Arrays.asList(nameMapping);
        }
        int nrItemSets = itemsetsModel.getInt(NUMBER_OF_ITEMSETS);
        for (int i = 0; i < nrItemSets; i++) {
            ModelContentRO itemSet = itemsetsModel.getModelContent(ITEMSET + i);
            List<Integer> items = new ArrayList<Integer>();
            double support = itemSet.getDouble(SUPPORT_ABS);
            int nrOfItems = itemSet.getInt(ITEMSET_SIZE);
            for (int j = 0; j < nrOfItems; j++) {
                int pos;
                if (m_nameMapping == null) {
                    pos = j;
                } else {
                    String name = itemSet.getString(ITEM + j);
                    pos = m_nameMapping.indexOf(name);
                }
                items.add(pos);
            }
            FrequentItemSet freqSet = new FrequentItemSet(items, support);
            m_itemSets.add(freqSet);
        }
    }

    /**
     * Saves the itemsets to the model.
     * 
     * @param model the model the itemsets are saved to
     */
    public void saveToModelContent(final ModelContentWO model) {
        ModelContentWO itemSetsModel = model.addModelContent(ITEMSET_MODEL);
        model.addString(TYPE, ITEMSET_MODEL);
        String[] mappingArray = new String[0];
        if (m_nameMapping != null) {
            mappingArray = new String[m_nameMapping.size()];
            m_nameMapping.toArray(mappingArray);
        }
        itemSetsModel.addStringArray(NAME_MAPPING, mappingArray);

        m_itemSetCounter = 0;
        for (FrequentItemSet set : m_itemSets) {
            if (set.getSupport() == 1.0) {
                m_alwaysFrequent.add(set);
            } else {
                ModelContentWO itemSetModel = itemSetsModel
                        .addModelContent(ITEMSET + m_itemSetCounter++);
                m_itemCounter = 0;
                saveItemSetTo(itemSetModel, set);
            }

        }
        if (m_alwaysFrequent.size() > 0) {
            ModelContentWO alwaysFrequentModel = itemSetsModel
                    .addModelContent(ITEMSET + m_itemSetCounter++);
            m_itemCounter = 0;
            for (FrequentItemSet set : m_alwaysFrequent) {
                saveItemSetTo(alwaysFrequentModel, set);
            }
            alwaysFrequentModel.addInt(ITEMSET_SIZE, m_alwaysFrequent.size());
        }
        itemSetsModel.addInt(NUMBER_OF_ITEMSETS, m_itemSetCounter);
    }

    private void saveItemSetTo(final ModelContentWO itemSetModel,
            final FrequentItemSet set) {
        itemSetModel.addDouble(SUPPORT_ABS, set.getSupport());
        itemSetModel.addInt(ITEMSET_SIZE, set.getItems().size());
        for (Integer itemId : set.getItems()) {
            // for every item look at the referring column name
            String itemName;
            if (m_nameMapping != null && m_nameMapping.size() > itemId) {
                itemName = m_nameMapping.get(itemId);
            } else {
                itemName = "item" + itemId;
            }
            itemSetModel.addString(ITEM + m_itemCounter++, itemName);
        }
    }

    /**
     * @param nameMapping the mapping from index to name
     */
    public void setNameMapping(final List<String> nameMapping) {
        m_nameMapping = nameMapping;
    }

    /**
     * @return the integer name mapping
     */
    public List<String> getNameMapping() {
        return m_nameMapping;
    }

    /**
     * @return the total number of transactions
     */
    public int getDBSize() {
        return m_dbsize;
    }

    /**
     * @param dbSize the toal number of transactions
     */
    public void setDBSize(final int dbSize) {
        m_dbsize = dbSize;
    }

    /**
     * @return the loaded itemsets
     */
    public Collection<FrequentItemSet> getItemSets() {
        return m_itemSets;
    }

    /**
     * @param frequentItemSets the frequent itemsets to store
     */
    public void setFrequentItemsets(
            final Collection<FrequentItemSet> frequentItemSets) {
        m_itemSets = frequentItemSets;
    }
}

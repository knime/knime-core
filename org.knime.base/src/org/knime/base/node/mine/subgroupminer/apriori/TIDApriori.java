/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   10.12.2005 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.TIDFrequentItemSet;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * The TIDApriori algorithm is actually an Eclat implementation, since it
 * realizes a depth first search. First of all, the frequent items are
 * determined and stored with the transaction ids. Then, in a depth-first-search
 * manner, the items are combined to larger itemsets by taking the next item
 * from the frequent ones and join their transaction ids until the support is
 * less than the minimum support.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TIDApriori implements AprioriAlgorithm {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TIDApriori.class);

    private double m_minSupport;

    private int m_maxLength;

    private int m_dbsize;

    private List<TIDItem> m_frequentItems;

    private List<TIDItem> m_alwaysFrequentItems = new ArrayList<TIDItem>();

    private List<TIDItemSet> m_repository;

    private TIDPrefixTreeNode m_prefixTree;
    
    private int m_idCounter = 0;

    /**
     * Identify those items which occure in a sufficient, that is the minimum
     * support, number of transactions and stores them with the ids of the
     * transactions they appear in. At the end the always frequent items, which
     * occur in every transaction are filtered.
     * 
     * @param transactions the database containing the transactions as BitSets
     * @param exec the execution monitor
     * @throws CanceledExecutionException if user cancels execution
     */
    public void findFrequentItems(final List<BitVectorValue> transactions,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        m_frequentItems = new ArrayList<TIDItem>();
        int transactionNr = 0;
        for (BitVectorValue transaction : transactions) {
            double progress = transactionNr / (double)m_dbsize; 
            exec.setProgress(progress,
                    "detecting frequent items. Transaction nr: "
                            + transactionNr);
            exec.checkCanceled();
            // this type cast is save since the maximum length was checked in 
            // SubgroupMinerModel#preprocess
            for (int item = (int)transaction.nextSetBit(0); item >= 0; 
                item = (int)transaction.nextSetBit(item + 1)) {
                /*
                 * iterate over every set bit, but!!! if: counterSoFar (what we
                 * got yet) + (dbsize - transaction#) (what is still possible) <
                 * minSupport -> kick, delete and destroy it!!! else count++
                 */
                // if set not already contains it add transaction id (counter ==
                // size!)
                // but if dbsize - transactionNr > minSupport
                if (!m_frequentItems.contains(new TIDItem(item))) {
                    // System.out.println(m_frequentItems + " does not contain "
                    // + item);
//                    if ((transactions.size()- transactionNr)>= m_minSupport) {
                        // System.out.println(" possible: " +
                        // (transactions.size() - transactionNr) + " >= " +
                        // m_minSupport);
                        TIDItem tidItem = new TIDItem(item);
                        tidItem.addTID(transactionNr);
                        m_frequentItems.add(tidItem);
                        // added item to m_frequentItems
//                    }
                } else {
                    // item should already be in m_frequentItems);
                    // find it and add this transaction id to it
                    for (int j = 0; j < m_frequentItems.size(); j++) {
                        if (m_frequentItems.get(j).equals(new TIDItem(item))) {
                            // check if it still could become frequent
//                            int counterSoFar = m_frequentItems.get(j)
//                                    .getSupport();
//                            if (counterSoFar + (transactions.size() 
//                                    - transactionNr) >= m_minSupport) {
                                TIDItem freqItem = m_frequentItems.get(j);
                                freqItem.addTID(transactionNr);
                                m_frequentItems.set(j, freqItem);
                                break;
//                            } else {
                                // kick, delete and destroy it:
//                                m_frequentItems.remove(j);
//                                break;
//                            }
                        }
                    }
                }
            }
            transactionNr++;
            /*-------------------one iteration----------------------*/
        }
        List<TIDItem> candidateFrequent = new ArrayList<TIDItem>();
        candidateFrequent.addAll(m_frequentItems);
        for (TIDItem i : candidateFrequent) {
            if (i.getSupport() < m_minSupport) {
                m_frequentItems.remove(i);
            }
        }
        Collections.sort(m_frequentItems);
        // LOGGER.debug("frequent items: " + m_frequentItems);
    }

    /*
     * Filters the always frequent items which occur in every transaction.
     */
    private void filterAlwaysFrequentItems() {
        m_alwaysFrequentItems = new ArrayList<TIDItem>();
        for (TIDItem i : m_frequentItems) {
            // LOGGER.debug("freq item: " + i);
            // LOGGER.debug(i.getSupport() + " == " + m_dbsize);
            if (i.getSupport() == m_dbsize) {
                m_alwaysFrequentItems.add(i);
            }
        }
        m_frequentItems.removeAll(m_alwaysFrequentItems);
    }

    private void addToClosedRepository(final TIDItemSet i) {
        if (m_repository == null) {
            m_repository = new ArrayList<TIDItemSet>();
        }
        boolean insert = true;
        for (TIDItemSet s : m_repository) {
            // condition for maximal -> it must not be a superset, that's it
            /*
             * if(s.isSuperSetOf(i)){ insert = false; break; }
             */
            // condition for closed
            if (s.isSuperSetOf(i) && i.getSupport() == s.getSupport()) {
                insert = false;
                break;
            }
        }
        if (insert) {
            m_repository.add(i);
        }
    }

    private void findFrequentItemsDepthFirst(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        TIDItemSet emptySet = TIDItemSet.createEmptyTIDItemSet(
                "" + m_idCounter++, m_dbsize);
        m_prefixTree = new TIDPrefixTreeNode(emptySet);
        expandDepthFirstTree(m_prefixTree, 0, exec);
    }

    private void expandDepthFirstTree(final TIDPrefixTreeNode node,
            final int highestId, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (node.getItemSet().getItems().size() > m_maxLength) {
            return;
        }
        for (int i = highestId; i < m_frequentItems.size(); i++) {
            TIDItemSet newSet = node.getItemSet();
            newSet.addItem(m_frequentItems.get(i));
            if (newSet.getSupport() >= m_minSupport) {
                TIDPrefixTreeNode child = new TIDPrefixTreeNode(newSet);
                node.addChild(child);
                exec.checkCanceled();
                exec.setMessage("item: " + i + " level: "
                        + newSet.getItems().size());
                expandDepthFirstTree(child, i + 1, exec);
            }
        }
        // if all children of one node are processed, the set can be added to
        // the repository
        // this is also done for free sets (in case of association rules output 
        // the repository will be used later on
        addToClosedRepository(node.getItemSet());
    }

    /**
     * {@inheritDoc}
     */
    public void findFrequentItemSets(final List<BitVectorValue> transactions,
            final double minSupport, final int maxDepth,
            final FrequentItemSet.Type type, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_minSupport = minSupport;
        m_maxLength = maxDepth;
        m_dbsize = transactions.size();

        LOGGER.debug("dbsize: " + m_dbsize);

        findFrequentItems(transactions, exec);
        LOGGER.debug("found " + m_frequentItems.size() + " frequent items");
        filterAlwaysFrequentItems();
        findFrequentItemsDepthFirst(exec);
    }

    /**
     * {@inheritDoc}
     */
    public List<FrequentItemSet> getFrequentItemSets(
            final FrequentItemSet.Type type) {
        List<FrequentItemSet> freqSets = new ArrayList<FrequentItemSet>();
        List<Integer> tids = new ArrayList<Integer>();
        for (int i = 0; i < m_dbsize; i++) {
            tids.add(i);
        }
        for (TIDItem i : m_alwaysFrequentItems) {
            List<Integer> id = new ArrayList<Integer>();
            id.add(i.getId());
            TIDFrequentItemSet freqSet = new TIDFrequentItemSet(
                    "" + m_idCounter++,
                    id, 1.0,
                    tids);
            freqSets.add(freqSet);
        }
        if (type.equals(FrequentItemSet.Type.FREE)) {
            getFrequentItemSets(m_prefixTree, freqSets);
        } else if (type.equals(FrequentItemSet.Type.CLOSED)) {
            freqSets.addAll(getClosedItemSets());
        } else if (type.equals(FrequentItemSet.Type.MAXIMAL)) {
            freqSets.addAll(getMaximalItemSets());
        }
        return freqSets;
    }

    private List<FrequentItemSet> getClosedItemSets() {
        List<FrequentItemSet> freqSets = new ArrayList<FrequentItemSet>();
        for (TIDItemSet i : m_repository) {
            FrequentItemSet s = i.toFrequentItemSet();
            if (s != null) {
                s.setClosed(true);
                freqSets.add(s);
            }
        }
        return freqSets;
    }

    private List<FrequentItemSet> getMaximalItemSets() {
        List<FrequentItemSet> maximalItemsets 
            = new ArrayList<FrequentItemSet>();
        List<FrequentItemSet> closedItemsets = getClosedItemSets();
        for (FrequentItemSet outer : closedItemsets) {
            boolean isMaximal = true;
            for (FrequentItemSet inner : closedItemsets) {
                if (!outer.equals(inner) && outer.isSubsetOf(inner)) {
                    isMaximal = false;
                    break;
                }
            }
            if (isMaximal) {
                maximalItemsets.add(outer);
            }
        }
        return maximalItemsets;
    }

    private void getFrequentItemSets(final TIDPrefixTreeNode node,
            final List<FrequentItemSet> list) {
        TIDItemSet itemSet = node.getItemSet();
        // LOGGER.debug("node:" + node);
        if (itemSet.getSupport() >= m_minSupport) {
            FrequentItemSet frequentSet = itemSet.toFrequentItemSet();
            if (frequentSet != null) {
                list.add(frequentSet);
            }
            if (node.getChildren() != null) {
                for (TIDPrefixTreeNode child : node.getChildren()) {
                    getFrequentItemSets(child, list);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<AssociationRule> getAssociationRules(final double confidence) {
        List<FrequentItemSet> frequentItemSets = getFrequentItemSets(
                    FrequentItemSet.Type.CLOSED);
        List<AssociationRule> associationRules 
            = new ArrayList<AssociationRule>();
        // handle always frequent items seperately
        List<Integer> alwaysFrequentIds = new ArrayList<Integer>();
        for (TIDItem item : m_alwaysFrequentItems) {
            alwaysFrequentIds.add(item.getId());
        }
        for (TIDItem item : m_alwaysFrequentItems) {
            // create for each item an association
            // rule with the rest of them in them in the antecendent
            // support = dbsize, confidence = 1
            List<Integer> rest = new ArrayList<Integer>(alwaysFrequentIds);
            rest.remove(new Integer(item.getId()));
//            AssociationRule rule = new AssociationRule(item.getId(), rest, 1,
//                    m_dbsize);
            List<Integer>itemList = new ArrayList<Integer>();
            itemList.add(item.getId());
            AssociationRule rule = new AssociationRule(
                    new FrequentItemSet("" + m_idCounter++, rest, 1.0), 
                    new FrequentItemSet("" + m_idCounter++, itemList, 1.0),
                    1.0, 1.0);
            associationRules.add(rule);
        }
        // for each itemset
        for (FrequentItemSet s : frequentItemSets) {
            if (s.getItems().size() > 1) {
                // for each item
                for (Integer i : s.getItems()) {
                    // create the set without the item
                    List<Integer> sWithoutI = new ArrayList<Integer>(s
                            .getItems());
                    sWithoutI.remove(i);
                    // create an empty TIDItemSet
                    TIDItemSet itemSet = TIDItemSet
                            .createEmptyTIDItemSet("" + m_idCounter, m_dbsize);
                    for (Integer item : sWithoutI) {
                        int index = m_frequentItems.indexOf(new TIDItem(item));
                        TIDItem tidItem = m_frequentItems.get(index);
                        itemSet.addItem(tidItem);
                    }
                    // LOGGER.debug("newly created itemset: " + itemSet);
                    // LOGGER.debug("s " + s);
                    // LOGGER.debug("s' support: " + itemSet.getSupport() + "
                    // s': " + itemSet);
                    double newSupport = itemSet.getSupport();
                    double oldSupport = s.getSupport();
                    double c = oldSupport / newSupport;
                    if (c >= confidence) {
//                        AssociationRule rule = new AssociationRule(i,
//                                sWithoutI, c, s.getSupport());
                        List<Integer>iList = new ArrayList<Integer>();
                        iList.add(i);
                        int index = m_frequentItems.indexOf(new TIDItem(i));
                        TIDItem tidItem = m_frequentItems.get(index);
                        if (tidItem == null) {
                            // TODO: what if ???
                        }
                        AssociationRule rule = new AssociationRule(
                                new FrequentItemSet(
                                        "" + m_idCounter++, 
                                        sWithoutI, newSupport),
                                new FrequentItemSet(
                                        "" + m_idCounter++,
                                        iList, 
                                        // TODO: support of single item
                                        tidItem.getSupport()),
                                s.getSupport(), c);
                        associationRules.add(rule);
                    }
                }
            }
        }
        return associationRules;
    }
}

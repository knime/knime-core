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
 *   12.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet.Type;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * The array apriori uses the
 * {@link ArrayPrefixTreeNode}
 * data structure to find frequent itemsets. Based on these it constructs a
 * prefix tree. In a prefix tree each child of an item has the path in the tree
 * to that item in common. The path is its prefix. The transactions are
 * processed, for each level once, by going to the node corresponding to first
 * item in the transaction, and process the rest of the transaction for that
 * node. Thus, there is no candidate generation.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ArrayApriori implements AprioriAlgorithm {
    // private static final NodeLogger logger =
    // NodeLogger.getLogger(ArrayApriori.class);

    private double m_minSupport;

    private int m_builtLevel;

    private boolean m_childCreated;

    private int m_dbsize;

    private int m_transactionNr;

    private ArrayPrefixTreeNode m_root;

    private int m_bitSetLength;

    private int[] m_mapping;

    private int[] m_backwardMapping;

    private int m_compressedLength;

    private List<Integer> m_alwaysFrequentItems;
    
    private int m_idCounter;

    /**
     * Creates an ArrayApriori instance with the bitset length, corresponding to
     * the number of items.
     * 
     * @param bitSetLength the number of items
     * @param dbsize the number of transactions
     */
    public ArrayApriori(final int bitSetLength, final int dbsize) {
        m_bitSetLength = bitSetLength;
        m_dbsize = dbsize;
        m_idCounter = 0;
    }

    /**
     * 
     * @param minSupport the minimum support
     */
    public void setMinSupport(final double minSupport) {
        m_minSupport = minSupport;
    }

    /**
     * First of all it starts to identify those items which are frequent at all.
     * Then it creates a mapping, where the whole transaction length (all items)
     * are mapped to the array position of only the frequent ones. Thus, the
     * algorithm works with the mostly much shorter array of frequent items
     * only.
     * 
     * @param transactions the database as bitsets
     */
    public void findFrequentItems(final List<BitVectorValue> transactions) {
        int[] items = new int[m_bitSetLength + 1];
        m_mapping = new int[m_bitSetLength + 1];

        List<Integer> frequentItems = new ArrayList<Integer>();
        for (BitVectorValue s : transactions) {
            // this type cast is save because the maximum length is checked in 
            // SubgroupMinerNodeModel#preprocess
            for (int i = (int)s.nextSetBit(0); i >= 0; 
                i = (int)s.nextSetBit(i + 1)) {
                // simply increment the position
                // that is probably faster than checking whether it might be
                // frequent
                items[i]++;
            }
        }
        int listPos = 0;
        for (int i = 0; i < items.length; i++) {
            if (((double)items[i] / (double)m_dbsize) >= m_minSupport) {
                frequentItems.add(i);
                m_mapping[i] = listPos++;
            } else {
                m_mapping[i] = -1;
            }
        }
        m_compressedLength = frequentItems.size();
        m_backwardMapping = new int[m_compressedLength];
        for (int i = 0; i < m_compressedLength; i++) {
            m_backwardMapping[i] = frequentItems.get(i);
        }
        filterAlwaysFrequentItems(items);
    }

    private void filterAlwaysFrequentItems(final int[] items) {
        m_alwaysFrequentItems = new ArrayList<Integer>();
        // for all items in m_frequentItems
        for (int i = 0; i < items.length; i++) {
            // find those, where the support == m_dbsize
            // since these items are always frequent (mining them is not
            // informative)
            if (items[i] == m_dbsize) {
                // store the id
                m_alwaysFrequentItems.add(i);
                m_mapping[i] = -1;
            } else if (m_mapping[i] > 0) {
                // logger.debug(m_backwardMapping[m_mapping[i]] + " ");
            }
        }
        // and then add it to the output
    }

    /**
     * Finds the frequent itemsets by going down the tree until the current
     * build level is reached, there it counts those items which are present in
     * the transaction. This implies, that it can count only those items, for
     * which a path is present in the tree, that is, which have frequent
     * predecessors. When the counting is finished, new children are created for
     * those itemsets, which might become frequent in the next level, that is,
     * itemsets with one item more.
     * 
     * {@inheritDoc}
     */
    public void findFrequentItemSets(final List<BitVectorValue> transactions,
            final double minSupport, final int maxDepth,
            final FrequentItemSet.Type type, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_minSupport = minSupport;
        m_dbsize = transactions.size();

        findFrequentItems(transactions);

        m_root = new ArrayPrefixTreeNode(m_compressedLength, null, -1);
        m_builtLevel = 0;
        do {
            m_transactionNr = 0;
            for (BitVectorValue s : transactions) {
                exec.checkCanceled();
                if (s.cardinality() == 0) {
                    continue;
                }
                count(s, m_root, 0, 0);
                m_transactionNr++;
            }
            m_childCreated = false;
            createChildren(m_root, 0, 0, exec);
            m_builtLevel++;
            exec.setProgress((1.0 - (1.0 / m_builtLevel)), "building level: "
                    + m_builtLevel);
        } while (m_childCreated && m_builtLevel < maxDepth);
    }

    private void count(final BitVectorValue transaction,
            final ArrayPrefixTreeNode node, final int item, final int level) {
        // this type cast is save since the maximum length was checked in 
        // SubgroupMinerModel#preprocess
        for (int i = (int)transaction.nextSetBit(item); i >= 0; 
                i = (int)transaction.nextSetBit(i + 1)) {
            if (m_mapping[i] < 0) {
                // this means that this item is not frequent at all!
                continue;
            }
            if (level == m_builtLevel) {
                // count
                node.increment(m_mapping[i]);
            } else if (node.getChild(m_mapping[i]) != null
                    && m_mapping[i] < m_compressedLength - 1) {
                count(transaction, node.getChild(m_mapping[i]), i + 1,
                        level + 1);
            }
        }
    }

    private void createChildren(final ArrayPrefixTreeNode node, final int item,
            final int level, final ExecutionMonitor exec) 
        throws CanceledExecutionException {
        if (node == null) {
            return;
        }
        for (int i = item; i < node.getLength() - 1; i++) {
            // bugfix 1160
            exec.checkCanceled();
            if (level == m_builtLevel) {
                // create children
                if (((double)node.getCounterFor(i) / (double)m_dbsize) 
                        >= m_minSupport) {
                    /*
                     * Get the parent and the children of that parent ->
                     * parent.getChildren(node.getParentIndex()) if either they
                     * are null or the counter for i is < minSupport don't add
                     * to candidates
                     */
                    boolean createChild = true;
                    ArrayPrefixTreeNode parent = node.getParent();
                    if (parent != null) {
                        ArrayPrefixTreeNode parentsChild = parent.getChild(i);
                        if (parentsChild == null) {
                            // logger.debug("parents child == null");
                        } else {
                            boolean hasFrequentSubset = false;
                            for (int j = i; j < node.getLength(); j++) {
                                if (((double)parentsChild.getCounterFor(j) 
                                        / (double)m_dbsize) 
                                        >= m_minSupport) {
                                    hasFrequentSubset = true;
                                    break;
                                }
                            }
                            if (!hasFrequentSubset) {
                                // logger.debug("##########early
                                // pruning##########");
                                // logger.debug("for item: " + i + " and node "
                                // + node
                                // + " and level " + level
                                // + " and parentsChild: " + parentsChild);
                                createChild = false;
                            }
                        }
                        // }else{
                        // logger.debug("no pruning possible for item " + i + "
                        // and node " + node);
                        // }
                    }
                    if (createChild) {
                        node.createChildAt(i);
                        m_childCreated = true;
                    }
                }
            } else {
                createChildren(node.getChild(i), i + 1, level + 1, exec);
            }
        }
    }

    /*
     * new idea for mining association rules: getFrequentitemsets -> have to do
     * the mapping again create possible candidates: for each item i in the set
     * s create the set without this item s' go down the tree for both s and s'
     * compute the confidence with getCounterFor(last item in s) /
     * getCounterFor(last item in s') if confidence is large enough - create
     * association rule (i, s', counterFor(s), confidence) store it
     */
    /**
     * {@inheritDoc}
     */
    public List<AssociationRule> getAssociationRules(final double confidence) {
        List<FrequentItemSet> frequentItemSets = getFrequentItemSets(
                FrequentItemSet.Type.CLOSED);
        List<AssociationRule> associationRules 
            = new ArrayList<AssociationRule>();
        /*
         * handle always frequent items seperately: since they are always
         * frequent each association rule of the itemset -> item must have
         * confidence = 1 and support = dbsize go once through the list and
         * create an association rule for every item x, like
         * {alwaysFrequentItems\x}-> x
         */
        for (Integer i : m_alwaysFrequentItems) {
            List<Integer> withoutI = new ArrayList<Integer>(
                    m_alwaysFrequentItems);
            withoutI.remove(i);
            List<Integer>iList = new ArrayList<Integer>(1);
            iList.add(i);
//            AssociationRule rule = new AssociationRule(
//                    i, withoutI, 1, m_dbsize);
            AssociationRule rule = new AssociationRule(
                    new FrequentItemSet("" + m_idCounter++, withoutI, 1.0),
                    new FrequentItemSet("" + m_idCounter++,
                            iList, 1.0),
                    1.0, 1.0, 1.0);
            associationRules.add(rule);
        }
        // for each itemset s in frequentitemsets
        for (FrequentItemSet s : frequentItemSets) {
            if (s.getItems().size() > 1) {
                double supportS = s.getSupport();
                for (Iterator<Integer> iterator = s.iterator(); iterator
                        .hasNext();) {
                    Integer i = iterator.next();
                    List<Integer> sWithoutI = new ArrayList<Integer>(s
                            .getItems());
                    sWithoutI.remove(i);
                    // now go down the tree for both s and s'
                    double newSupport = getSupportFor(sWithoutI);
                    // logger.debug("support(s'): " + newSupport);
                    double c = supportS / newSupport;
                    if (c >= confidence) {
                        // create association rule (i, s', counterFor(s),
                        // confidence)
//                        AssociationRule rule = new AssociationRule(i,
//                                sWithoutI, c, supportS);
                        List<Integer>iList = new ArrayList<Integer>();
                        iList.add(i);
                        AssociationRule rule = new AssociationRule(
                                new FrequentItemSet(
                                        "" + m_idCounter++, 
                                        sWithoutI, newSupport),
                                new FrequentItemSet(
                                        "" + m_idCounter++, iList, 
                                        getSupportFor(iList)),
                                        s.getSupport(), c, c / getSupportFor(iList)
                                );
                        associationRules.add(rule);
                        // logger.debug("found association rule: " + rule);
                    }
                }
            }
        }
        return associationRules;
    }

    private double getSupportFor(final List<Integer> itemset) {
        ArrayPrefixTreeNode child = m_root;
        double support = 0;
        for (Integer item : itemset) {
            support = child.getCounterFor(m_mapping[item]);
            child = child.getChild(m_mapping[item]);
        }
        return support / m_dbsize;
    }

    /**
     * {@inheritDoc}
     */
    public List<FrequentItemSet> getFrequentItemSets(final Type type) {
        List<FrequentItemSet> list = new ArrayList<FrequentItemSet>();
        for (Integer i : m_alwaysFrequentItems) {
            List<Integer> id = new ArrayList<Integer>();
            id.add(i);
            FrequentItemSet set = new FrequentItemSet(
                    "" + m_idCounter++, id, 1);
            list.add(set);
        }
        FrequentItemSet initialSet = new FrequentItemSet("" + m_idCounter++);
        getFrequentItemSets(m_root, list, initialSet, 0);
        if (type.equals(FrequentItemSet.Type.CLOSED)) {
            List<FrequentItemSet> resultList = filterClosedItemsets(list);
            return resultList;
        }
        /*
         * nothing else then check for every set in the closed itemsets whether
         * it is a superset or not -> delete the subsets
         */
        if (type.equals(FrequentItemSet.Type.MAXIMAL)) {
            List<FrequentItemSet> resultList = filterClosedItemsets(list);
            return filterMaximalItemsets(resultList);
        }
        return list;
    }

    private List<FrequentItemSet> filterMaximalItemsets(
            final List<FrequentItemSet> closedItemsets) {
        List<FrequentItemSet> maximalItemsets 
            = new ArrayList<FrequentItemSet>();
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

    private void getFrequentItemSets(final ArrayPrefixTreeNode root,
            final List<FrequentItemSet> list, final FrequentItemSet currSet,
            final int item) {
        if (root == null) {
            return;
        }
        for (int i = item; i < root.getLength(); i++) {
            if (((double)root.getCounterFor(i) / (double)m_dbsize) 
                    >= m_minSupport) {
                FrequentItemSet newSet = new FrequentItemSet(
                        "" + m_idCounter++,
                        currSet.getItems(), ((double)root.getCounterFor(i) 
                                / (double)m_dbsize));
                newSet.add(m_backwardMapping[i]);
                list.add(newSet);
                getFrequentItemSets(root.getChild(i), list, newSet, i + 1);
            }
        }
    }

    private List<FrequentItemSet> filterClosedItemsets(
            final List<FrequentItemSet> completeList) {
        Collections.sort(completeList, new Comparator<FrequentItemSet>() {
            public int compare(final FrequentItemSet s1,
                    final FrequentItemSet s2) {
                if (s1.getSupport() == s2.getSupport()) {
                    return (new Integer(s1.getItems().size()).compareTo(s2
                            .getItems().size()));
                }
                return (new Double(s1.getSupport()).compareTo(s2.getSupport()));
            }
        });
        FrequentItemSet[] array = new FrequentItemSet[completeList.size()];
        completeList.toArray(array);
        for (int outer = 0; outer < array.length; outer++) {
            FrequentItemSet underSuspicion = array[outer];
            underSuspicion.setClosed(true);
            for (int inner = outer + 1; inner < array.length; inner++) {
                FrequentItemSet next = array[inner];
                if (next.getSupport() == underSuspicion.getSupport()) {
                    if (underSuspicion.isSubsetOf(next)) {
                        underSuspicion.setClosed(false);
                        break;
                    }
                }
            }
        }
        List<FrequentItemSet> closedList = new LinkedList<FrequentItemSet>();
        for (int i = 0; i < array.length; i++) {
            if (array[i].isClosed()) {
                closedList.add(array[i]);
            }
        }
        return closedList;
    }
}

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
 *   11.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.TIDFrequentItemSet;

/**
 * The TIDItemSet contains of some TIDItems and a BitSet with their common
 * transaction ids. The BitSet is of the length of the total number of
 * transactions with the bit set, if the items in the set are present in the
 * transaction where the id equals the position in the bitset. When an item is
 * added, the transaction ids are intersected.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class TIDItemSet {
    private List<TIDItem> m_items;

    private BitSet m_commonTIDs;
    
    private final int m_dbsize;
    
    private final String m_id;
    

    /*
     * Creates and empty TIDItemSet and with no items. Attention: the bitset is
     * empty, that means adding an item to this set will result in an itemset
     * with support = 0, since the common transaction ids are intersected. Use
     * createEmptyTIDItemSet instead.
     * 
     */
    private TIDItemSet(final String id, final int length) {
        m_items = new ArrayList<TIDItem>();
        m_commonTIDs = new BitSet();
        m_dbsize = length;
        m_id = id;
    }

    /**
     * Creates an empty TIDItemSet with no items but, since it is an empty set,
     * present in all transactions.
     * 
     * @param length the number of transactions
     * @return an empty TIDItemSet with no items but present in all transactions
     */
    public static TIDItemSet createEmptyTIDItemSet(final String id, 
            final int length) {
        TIDItemSet empty = new TIDItemSet(id, length);
        BitSet all = new BitSet(length);
        all.set(0, length);
        empty.m_commonTIDs = all;
        return empty;
    }
    
    public String getId() {
        return m_id;
    }

    /**
     * Adds an item to the set and thereby intersecting the transaction ids.
     * 
     * @param i the item to add
     */
    public void addItem(final TIDItem i) {
        if (!m_items.contains(i)) {
            m_items.add(i);
            m_commonTIDs.and(i.getTransactionIDs());
        }
    }

    /**
     * 
     * @return the items in this set
     */
    public List<TIDItem> getItems() {
        return new ArrayList<TIDItem>(m_items);
    }

    /**
     * Returns the support of this set, which is equal to the number of
     * transactions the items in this set appear together in.
     * 
     * @return the support of this set
     */
    public double getSupport() {
        if (m_items.size() > 0) {
            return (double)m_commonTIDs.cardinality() / (double)m_dbsize;
        } else {
            return 0.0;
        }
    }

    /**
     * Return the transaction ids in which the items in this set appear together
     * as a bitset where the bit is set if the items are present in this
     * transaction. The position of the bit refers to the transaction id.
     * 
     * @return the transaction ids in which the items in this set are present
     *         together
     */
    public BitSet getCommonTIDs() {
        return m_commonTIDs;
    }

    /**
     * Returns the list of tids as a integer list.
     * 
     * @return the list of tids as a integer list.
     */
    public List<Integer> getTIDs() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = m_commonTIDs.nextSetBit(0); i >= 0; i = m_commonTIDs
                .nextSetBit(i + 1)) {
            list.add(i);
        }
        return list;
    }

    /**
     * 
     * @param s the set to test
     * @return <code>true</code>, if this set contains all items of the other
     *         set, <code>false</code> otherwise
     */
    public boolean isSuperSetOf(final TIDItemSet s) {
        return m_items.containsAll(s.getItems());
    }

    /**
     * @return this set as a FrequentItemSet, with the ids as integer and the
     *         support
     */
    public FrequentItemSet toFrequentItemSet() {
        List<Integer> ids = new ArrayList<Integer>();
        for (TIDItem i : m_items) {
            ids.add(i.getId());
        }
        if (ids.isEmpty()) {
            return null;
        }
        return new TIDFrequentItemSet(m_id, ids, getSupport(), getTIDs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TIDItemSet clone() {
        TIDItemSet newItem = new TIDItemSet(m_id, m_dbsize);
        List<TIDItem> items = new ArrayList<TIDItem>(m_items);
        BitSet tids = new BitSet();
        for (int i = m_commonTIDs.nextSetBit(0); i >= 0; i = m_commonTIDs
                .nextSetBit(i + 1)) {
            tids.set(i);
        }
        newItem.m_items = items;
        newItem.m_commonTIDs = tids;
        return newItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        TIDItemSet theOther = (TIDItemSet)o;
        return this.m_items.equals(theOther.getItems())
                && this.m_commonTIDs.equals(theOther.getCommonTIDs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_items.hashCode() + m_commonTIDs.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(items: " + m_items.toString() + " tids: "
                + m_commonTIDs.toString() + ")";
    }
}

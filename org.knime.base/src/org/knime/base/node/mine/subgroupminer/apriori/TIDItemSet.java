/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
     * @param id the current item set ID
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
    
    /**
     * @return item set ID as string
     */
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
        // we do not have to take the itemset into account but only the 
        // commonTIDs (amount of items in this node)
        // if no items are in this node the cardinality - and the support - 
        // will be 0.
        // The root node is a node with no items but all TIDs in the commonTIDs
        return (double)m_commonTIDs.cardinality() / (double)m_dbsize;
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

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

import java.util.BitSet;

/**
 * The TIDItem consists of an id and a BitSet, where each bit corresponds to a
 * transaction id and is set, if this item is present in the transaction.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TIDItem implements Comparable<TIDItem> {

    private final int m_id;

    private BitSet m_transactionIDs;

    /**
     * Creates an TIDItem with this id and present in no transaction.
     * 
     * @param id the id of this item
     */
    public TIDItem(final int id) {
        m_id = id;
        m_transactionIDs = new BitSet();
    }

    /**
     * 
     * @return - the id of this item.
     */
    public int getId() {
        return m_id;
    }

    /**
     * Adds a transaction id to this item, thus, the item has to be present in
     * this transaction.
     * 
     * @param tid the transaction id of the transaction this item is present in
     */
    public void addTID(final int tid) {
        m_transactionIDs.set(tid);
    }

    /**
     * The support of this item, which is the number of transaction it is
     * present in.
     * 
     * @return the support of this item, which is the numer of transactions it
     *         is present in
     */
    public int getSupport() {
        return m_transactionIDs.cardinality();
    }

    /**
     * The transaction ids as a bitset, where the bit is set if the item is
     * present in the corresponding transaction.
     * 
     * @return the ids of the transactions this item appears in
     */
    public BitSet getTransactionIDs() {
        return m_transactionIDs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        TIDItem theOther = (TIDItem)o;
        if (m_id == theOther.getId()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TIDItem clone() {
        TIDItem newItem = new TIDItem(new Integer(m_id));
        for (int tid = m_transactionIDs.nextSetBit(0); tid >= 0; 
            tid = m_transactionIDs.nextSetBit(tid + 1)) {
            newItem.addTID(tid);
        }
        return newItem;
    }

    /**
     * @param theOther the object to compare to
     * @return -1 if this is smaller, 0 if it is equal to the other and +1 if
     *         this is greater
     */
    public int compareTo(final TIDItem theOther) {
        if (m_id < theOther.getId()) {
            return -1;
        } else if (m_id > theOther.getId()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "id:  " + m_id + " " + m_transactionIDs.toString();
    }
}

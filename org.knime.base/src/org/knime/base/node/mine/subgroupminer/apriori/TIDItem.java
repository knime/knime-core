/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
    @Override
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

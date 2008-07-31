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
 *   29.10.2005 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A FrequentItemSet represents items that occur together in a number of
 * transactions. The items are represented with integers as their id. This
 * number directly corresponds to the support. Frequent itemsets can either be
 * free, closed or maximal. Free sets have no other constraint then the minimum
 * support. Closed itemsets have no superset with the same support and maximal
 * itemsets have no frequent superset at all.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FrequentItemSet implements Iterable<Integer> {
    /**
     * The type of the frequent itemset. Either free, closed or maximal. Free
     * sets have no other constraint then the minimum support. Closed itemsets
     * have no superset with the same support and maximal itemsets have no
     * frequent superset at all.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public static enum Type {
        /** free. */
        FREE,
        /** closed. */
        CLOSED,
        /** maximal. */
        MAXIMAL;

        /**
         * Returns the enum fields as a String list of their names.
         * 
         * @return the enum fields as a String list of their names
         */
        public static List<String> asStringList() {
            Enum[] values = values();
            List<String> list = new ArrayList<String>();
            for (int i = 0; i < values.length; i++) {
                list.add(values[i].name());
            }
            return list;
        }
    }

    private List<Integer> m_items;

    private double m_support;

    private boolean m_isClosed;

    private boolean m_isMaximal;

    /**
     * Creates an empty frequent itemset with no items, support = 0 and neither
     * closed nor maximal.
     * 
     */
    public FrequentItemSet() {
        m_items = new LinkedList<Integer>();
        m_support = 0;
        m_isClosed = false;
        m_isMaximal = false;
    }

    /**
     * Creates frequent itemset with the passed items, support = 0 and neither
     * closed nor maximal.
     * 
     * @param items the items constituting this set
     */
    public FrequentItemSet(final List<Integer> items) {
        m_items = new LinkedList<Integer>(items);
        m_support = 0;
        m_isClosed = false;
        m_isMaximal = false;
    }

    /**
     * Creates a fequent itemset with the passed items and the given support.
     * Neither closed nor maximal.
     * 
     * @param items the items constituting this set
     * @param support the support of this itemset
     */
    public FrequentItemSet(final List<Integer> items, final double support) {
        m_items = new LinkedList<Integer>(items);
        m_support = support;
        m_isClosed = false;
        m_isMaximal = false;
    }

    /**
     * Creates a frequent itemset with the passed items, the given support and
     * whether it is closed or maximal. If both, closed and maximal are
     * <code>false</code> it is considered to be free.
     * 
     * @param items the items consituting this itemset.
     * @param support the support of this itemset.
     * @param isClosed <code>true</code>, if this itemset is closed,
     *            <code>false</code> otherwise
     * @param isMaximal <code>true</code> if this itemset is maximal,
     *            <code>false</code> otherwise
     */
    public FrequentItemSet(final List<Integer> items, final double support,
            final boolean isClosed, final boolean isMaximal) {
        m_items = new LinkedList<Integer>(items);
        m_support = support;
        m_isClosed = isClosed;
        m_isMaximal = isMaximal;
    }

    /**
     * Returns <code>true</code>, if this itemset is a subset of the passed
     * one, that is, if the passed one contains all items of this set.
     * 
     * @param s2 the frequent itemset to test on
     * @return <code>true</code>, if this is a subset of the passed one,
     *         <code>false</code> otherwise
     */
    public boolean isSubsetOf(final FrequentItemSet s2) {
        return s2.getItems().containsAll(m_items);
    }

    /**
     * Adds the passed item to the set.
     * 
     * @param item the item to add to this set
     */
    public void add(final Integer item) {
        m_items.add(item);
    }

    /**
     * 
     * @return an iterator over the items
     */
    public Iterator<Integer> iterator() {
        return m_items.iterator();
    }

    /**
     * Returns a copy of the items in this set.
     * 
     * @return a copy of the items in this set
     */
    public List<Integer> getItems() {
        return new LinkedList<Integer>(m_items);
    }

    /**
     * Adds a set of items.
     * 
     * @param items the items to add to this set
     */
    public void setItems(final List<Integer> items) {
        this.m_items = items;
    }

    /**
     * 
     * @return <code>true</code>, if this itemset is closed,
     *         <code>false</code> otherwise
     */
    public boolean isClosed() {
        return m_isClosed;
    }

    /**
     * Sets whether this set is closed or not.
     * 
     * @param isClosed <code>true</code>, if this set is closed,
     *            <code>false</code> otherwise
     */
    public void setClosed(final boolean isClosed) {
        this.m_isClosed = isClosed;
    }

    /**
     * 
     * @return <code>true</code>, if this set is maximal, <code>false</code>
     *         otherwise
     */
    public boolean isMaximal() {
        return m_isMaximal;
    }

    /**
     * Sets whether this set is maximal or not.
     * 
     * @param isMaximal whether this frequent item set is maximal or not
     */
    public void setMaximal(final boolean isMaximal) {
        this.m_isMaximal = isMaximal;
    }

    /**
     * @return the support of this set
     */
    public double getSupport() {
        return m_support;
    }

    /**
     * Sets the support of this set.
     * 
     * @param support the support of this set
     */
    public void setSupport(final double support) {
        this.m_support = support;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("support: " + m_support);
        buff.append(" isClosed: " + m_isClosed);
        buff.append(" items: {");
        for (Integer i : m_items) {
            buff.append(" " + i);
        }
        buff.append("}");
        return buff.toString();
    }
}

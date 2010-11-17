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
            Enum<Type>[] values = values();
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

    private final String m_id;

    /**
     * Creates an empty frequent itemset with no items, support = 0 and neither
     * closed nor maximal.
     * @param id - the id of the itemset
     */
    public FrequentItemSet(final String id) {
        this(id, new LinkedList<Integer>(), 0, false, false);
    }

    /**
     * Creates frequent itemset with the passed items, support = 0 and neither
     * closed nor maximal.
     *
     * @param id the id of this itemset
     * @param items the items constituting this set
     */
    public FrequentItemSet(final String id, final List<Integer> items) {
        this(id, items, 0, false, false);
    }

    /**
     * Creates a fequent itemset with the passed items and the given support.
     * Neither closed nor maximal.
     *
     * @param id the id of this itemset
     * @param items the items constituting this set
     * @param support the support of this itemset
     */
    public FrequentItemSet(final String id,
            final List<Integer> items, final double support) {
        this(id, items, support, false, false);
    }

    /**
     * Creates a frequent itemset with the passed items, the given support and
     * whether it is closed or maximal. If both, closed and maximal are
     * <code>false</code> it is considered to be free.
     *
     * @param id the id of this itemset
     * @param items the items consituting this itemset.
     * @param support the support of this itemset.
     * @param isClosed <code>true</code>, if this itemset is closed,
     *            <code>false</code> otherwise
     * @param isMaximal <code>true</code> if this itemset is maximal,
     *            <code>false</code> otherwise
     */
    public FrequentItemSet(final String id, final List<Integer> items,
            final double support,
            final boolean isClosed, final boolean isMaximal) {
        m_items = new LinkedList<Integer>(items);
        m_support = support;
        m_isClosed = isClosed;
        m_isMaximal = isMaximal;
        m_id = id;
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
     *
     * @return the id of this itemset
     */
    public String getId() {
        return m_id;
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
    @Override
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

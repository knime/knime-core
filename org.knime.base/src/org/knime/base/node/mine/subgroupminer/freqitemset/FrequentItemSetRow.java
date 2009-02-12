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
 *   29.10.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;


/**
 * This class implements one row of a FrequentItemSetTable. Beside the normal
 * DataRow functionality it provides information about the length of the
 * frequent item set, the support of it, whether it is closed, resp. maximal or
 * not. Each item in the set is realised as a StringValue (the name of the
 * item).
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class FrequentItemSetRow implements DataRow {
    private final RowKey m_key; // id

    private final DataCell[] m_cells; // check if super- or subset

    private final int m_length;

    private final int m_setLength;

    private final double m_support;

    private final boolean m_isClosed;

    private final boolean m_isMaximal;

    /**
     * Creates a FrequentItemSetRow from the passed arguments.
     * 
     * @param key the unique row key
     * @param items the names of the items in the set as strings
     * @param length the length of the item set (which might be different from
     *            the length of the row!)
     * @param support the support of the item set
     * @param isClosed <code>true</code> if the item set is closed,
     *            <code>false</code> otherwise
     * @param isMaximal <code>true</code> if the set is maximal,
     *            <code>false</code> otherwise
     */
    public FrequentItemSetRow(final RowKey key, final List<String> items,
            final int length, final double support, final boolean isClosed,
            final boolean isMaximal) {
        m_key = key;
        m_length = length;
        int pos = 0;
        m_cells = new DataCell[m_length + 1];
        m_support = support;
        m_isClosed = isClosed;
        m_isMaximal = isMaximal;

        m_cells[pos++] = new DoubleCell(support);
        m_setLength = items.size();
        for (Iterator<String> it = items.iterator(); it.hasNext();) {
            if (pos >= m_length + 1) {
                break;
            }
            m_cells[pos++] = new StringCell(it.next());
        }
        // fill the rest of the cell with an empty string
        for (int i = pos; i < m_length + 1; i++) {
            m_cells[pos++] = DataType.getMissingCell();
        }
    }

    /**
     * Extracts the itemset names from a Default row, which was a former itemset
     * row.
     * 
     * @param row a former frequent itemset row
     * @return the names of the itemset of a former itemset row
     */
    public static Set<String> extractItemNamesFrom(final DefaultRow row) {
        Set<String> names = new HashSet<String>();
        for (int i = 1; i < row.getNumCells(); i++) {
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                return names;
            }
            names.add(((StringValue)cell).getStringValue());
        }
        return names;
    }

    /**
     * Returns the support of a former frequent itemset row.
     * 
     * @param row a former frequent itemset row
     * @return the support of a former frequent itemset row
     */
    public static double getSupportFrom(final DefaultRow row) {
        return ((DoubleValue)row.getCell(0)).getDoubleValue();
    }

    /**
     * Creates a FrequentItemSetRow from the passed arguments. It is assumed
     * that this item set is neither closed nor maximal: these values will be
     * set to <code>false</code>.
     * 
     * @param key the unique row key
     * @param items the names of the items in the set as strings
     * @param length the length of the item set (which might be different from
     *            the length of the row!)
     * @param support the support of the item set
     */
    public FrequentItemSetRow(final RowKey key, final List<String> items,
            final int length, final double support) {
        this(key, items, length, support, false, false);
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_length + 1;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        return m_cells[index];
    }

    /**
     * Returns the support of the represented frequent itemset.
     * 
     * @return the support of the represented frequent itemset
     */
    public double getSupport() {
        return m_support;
    }

    /**
     * Returns the number of items in the represented frequent itemset.
     * 
     * @return the number of items in the represented frequent itemset
     */
    public int getSetLength() {
        return m_setLength;
    }

    /**
     * Returns the items as a set of Strings.
     * 
     * @return the items as a set of Strings
     */
    public Set<String> asSet() {
        Set<String> itemSet = new HashSet<String>();
        for (int i = 1; i < m_cells.length; i++) {
            itemSet.add(((StringValue)m_cells[i]).getStringValue());
        }
        return itemSet;
    }

    /**
     * Returns whether the represented itemset is closed or not.
     * 
     * @return whether the represented itemset is closed or not
     */
    public boolean isClosed() {
        return m_isClosed;
    }

    /**
     * @return whether the represented frequent itemset is maximal or not
     */
    public boolean isMaximal() {
        return m_isMaximal;
    }
}

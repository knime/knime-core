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

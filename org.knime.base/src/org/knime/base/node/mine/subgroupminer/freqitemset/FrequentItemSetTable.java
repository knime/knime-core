/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;


/**
 * A FrequentItemSetTable adds a sorting functionality to the normal DataTable.
 * This can not be done by the SorterNode, since the rows of a
 * FrequentItemSetTable are of variable length ( the rest is filled with empty
 * cells) and the user is able to sort by itemset length.
 *
 * @deprecated
 * @author Fabian Dill, University of Konstanz
 */
@Deprecated
public class FrequentItemSetTable implements DataTable {
    /** the feature the table should be sorted by. */
    public static enum Sorter {
        /** no sorting. */
        NONE,
        /** sort by itemset length. */
        ITEM_SET_LENGTH,
        /** sort by support. */
        SUPPORT;

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

    private final List<FrequentItemSetRow> m_rows;

    private final DataTableSpec m_spec;

    private static final Map<FrequentItemSetTable.Sorter,
        Comparator<FrequentItemSetRow>> COMPARATORS;

    static {
        COMPARATORS = new HashMap<FrequentItemSetTable.Sorter,
            Comparator<FrequentItemSetRow>>();
        COMPARATORS.put(FrequentItemSetTable.Sorter.SUPPORT,
                new Comparator<FrequentItemSetRow>() {
                    @Override
                    public int compare(final FrequentItemSetRow row1,
                            final FrequentItemSetRow row2) {
                        return ((Double)row2.getSupport()).compareTo(row1
                                .getSupport());
                    }
                });
        COMPARATORS.put(FrequentItemSetTable.Sorter.ITEM_SET_LENGTH,
                new Comparator<FrequentItemSetRow>() {
                    @Override
                    public int compare(final FrequentItemSetRow row1,
                            final FrequentItemSetRow row2) {
                        return ((Integer)row2.getSetLength()).compareTo(row1
                                .getSetLength());
                    }
                });
    }

    /**
     * Creates a FrequentItemSetTable out of the passed row array and the given
     * DataTableSpec.
     *
     * @param rows the rows of this table
     * @param spec the DataTableSpec of this Table
     */
    public FrequentItemSetTable(final FrequentItemSetRow[] rows,
            final DataTableSpec spec) {
        if (rows == null) {
            throw new NullPointerException("Data must not be null");
        }
        checkTableConsistency(rows, spec);
        m_rows = new ArrayList<FrequentItemSetRow>(Arrays.asList(rows));
        m_spec = spec;
    }

    /**
     * Creates a FrequentItemSetTable out of the passed row list and the given
     * DataTableSpec.
     *
     * @param rowList the rows of this table
     * @param spec the DataTableSpec of this table
     */
    public FrequentItemSetTable(final List<FrequentItemSetRow> rowList,
            final DataTableSpec spec) {
        DataRow[] rowArray = new DataRow[rowList.size()];
        rowList.toArray(rowArray);
        checkTableConsistency(rowArray, spec);
        m_rows = rowList;
        m_spec = spec;
    }

    private void checkTableConsistency(final DataRow[] rows,
            final DataTableSpec spec) {
        // check if spec fits the data
        for (int r = 0; r < rows.length; r++) {
            DataRow row = rows[r];
            if (row.getNumCells() != spec.getNumColumns()) {
                throw new IllegalArgumentException(
                        "The number of cells in row " + r + ": "
                                + +spec.getNumColumns()
                                + "must be the same as defined in the Spec: "
                                + row.getNumCells());
            }
            for (int c = 0; c < row.getNumCells(); c++) {
                DataType columnType = spec.getColumnSpec(c).getType();
                DataType cellType = row.getCell(c).getType();
                if (!columnType.isASuperTypeOf(cellType)) {
                    throw new IllegalArgumentException(
                            "Runtime class of object \""
                                    + row.getCell(c).toString()
                                    + "\" at index (" + r + "; " + c + ") is "
                                    + cellType
                                    + " and does not comply with its supposed "
                                    + "superclass " + columnType + "!");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowIterator iterator() {
        return new FrequentItemSetRowIterator();
    }

    /**
     * Sorts the table by the given sorter (support or itemset length).
     *
     * @param sorter defines by which feature the table should be sorted
     */
    public void sortBy(final Sorter sorter) {
        if (sorter.equals(Sorter.NONE)) {
            return;
        }
        if (COMPARATORS.get(sorter) != null) {
            Collections.sort(m_rows, COMPARATORS.get(sorter));
        }
    }

    private class FrequentItemSetRowIterator extends RowIterator {
        private Iterator<FrequentItemSetRow> m_iterator;

        /**
         * Creates an instance of the iterator over the table.
         *
         */
        public FrequentItemSetRowIterator() {
            m_iterator = m_rows.iterator();
        }

        @Override
        public boolean hasNext() {
            return m_iterator.hasNext();
        }

        @Override
        public DataRow next() {
            return m_iterator.next();
        }
    }
}

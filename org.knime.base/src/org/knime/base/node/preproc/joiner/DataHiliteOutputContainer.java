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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   08.03.2010 (hofer): created
 */
package org.knime.base.node.preproc.joiner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * This is a facade for a {@link DataContainer} which is used to create the
 * final joined table.
 *
 * @author Heiko Hofer
 */
class DataHiliteOutputContainer {
    private BufferedDataContainer m_dc;
    private DataTableSpec m_spec;

    private boolean m_enableHiLite;

    private HashMap<RowKey, Set<RowKey>> m_leftRowKeyMap;

    private HashMap<RowKey, Set<RowKey>> m_rightRowKeyMap;

    private BufferedDataTable m_leftTable;

    private int[] m_leftSurvivors;

    private int[] m_rightSurvivors;

    private CloseableRowIterator m_leftIter;

    private DataRow m_left;
    private int m_leftIndex;
    private DataRow m_leftMissing;


    private JoinedRowKeyFactory m_rowKeyFactory;




    /**
     * @param spec The spec of the data container used for storing all added
     *              tables.
     * @param enableHiLite If HiLite is enabled.
     * @param leftTable The left input table.
     * @param leftSurvivors The survivors of the left input table.
     * @param rightSurvivors The survivors of the right input table.
     * @param rowKeyFactory Used for creating the row ids of the joined rows.
     */
    DataHiliteOutputContainer(final DataTableSpec spec,
            final boolean enableHiLite, final BufferedDataTable leftTable,
            final int[] leftSurvivors, final int[] rightSurvivors,
            final JoinedRowKeyFactory rowKeyFactory) {
        m_spec = spec;
        m_enableHiLite = enableHiLite;
        if (enableHiLite) {
            m_leftRowKeyMap = new HashMap<RowKey, Set<RowKey>>();
            m_rightRowKeyMap = new HashMap<RowKey, Set<RowKey>>();
        }
        m_leftTable = leftTable;

        m_leftSurvivors = leftSurvivors;
        m_rightSurvivors = rightSurvivors;

        m_leftIter = leftTable.iterator();

        if (m_leftIter.hasNext()) {
            m_left = m_leftIter.next();
        }

        m_leftIndex = 0;

        m_rowKeyFactory = rowKeyFactory;

        m_leftMissing = new Missing(leftTable.getSpec().getNumColumns());

    }

    /**
     * @return the rowKeyMap
     */
    HashMap<RowKey, Set<RowKey>> getLeftRowKeyMap() {
        return m_leftRowKeyMap;
    }

    /**
     * @return the rowKeyMap
     */
    HashMap<RowKey, Set<RowKey>> getRightRowKeyMap() {
        return m_rightRowKeyMap;
    }

    /** Add the given row to m_dc. */
    private void addRow(final DataRow row) {
        // get left input row
        DataRow left = null;
        int leftIndex = OutputDataRow.getLeftIndex(row);
        if (leftIndex >= 0) {
            if (m_leftIndex > leftIndex) {
                m_leftIter.close();
                m_leftIter = m_leftTable.iterator();
                m_left = m_leftIter.next();
                m_leftIndex = 0;
            }

            while (m_leftIndex < leftIndex) {
                m_left = m_leftIter.next();
                m_leftIndex++;
            }
            left = m_left;
        } else {
            left = m_leftMissing;
        }
        // the first elements of the given row are equal to the elements
        // of the right row
        DataRow right = row;

        // Build joined row
        DataCell[] cells =
                new DataCell[m_leftSurvivors.length + m_rightSurvivors.length];
        int c = 0;
        for (int i = 0; i < m_leftSurvivors.length; i++) {
            cells[c] = left.getCell(m_leftSurvivors[i]);
            c++;
        }
        for (int i = 0; i < m_rightSurvivors.length; i++) {
            cells[c] = right.getCell(i);
            c++;
        }
        RowKey joinedKey =
                m_rowKeyFactory.createJoinedKey(left.getKey(),
                        OutputDataRow.getRightKey(row));
        DataRow joinedRow = new DefaultRow(joinedKey, cells);

        m_dc.addRowToTable(joinedRow);
        if (m_enableHiLite) {
            // Remember RowKeys for HiLiting
            if (null != left.getKey()) {
                Set<RowKey> keySet = m_leftRowKeyMap.get(left.getKey());
                if (null == keySet) {
                    keySet = new HashSet<RowKey>();
                    m_leftRowKeyMap.put(left.getKey(), keySet);
                }
                keySet.add(joinedKey);
            }
            if (null != right.getKey()) {
                Set<RowKey> keySet = m_rightRowKeyMap.get(right.getKey());
                if (null == keySet) {
                    keySet = new HashSet<RowKey>();
                    m_rightRowKeyMap.put(right.getKey(), keySet);
                }
                keySet.add(joinedKey);
            }
        }
    }

    /**
     * Close container.
     */
    public void close() {
        m_dc.close();
    }

    /**
     * An InputDataRow with solely missing data cells, needed for left and right
     * outer join.
     *
     * @author Heiko Hofer
     */
    static class Missing implements DataRow {
        private DataCell[] m_cells;

        /**
         * @param numCells The number of cells in the {@link DataRow}
         */
        public Missing(final int numCells) {
            m_cells = new DataCell[numCells];
            for (int i = 0; i < numCells; i++) {
                m_cells[i] = DataType.getMissingCell();
            }
        }

        /**
         * {@inheritDoc}
         */
        public RowKey getKey() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public DataCell getCell(final int index) {
            return m_cells[index];
        }

        /**
         * {@inheritDoc}
         */
        public int getNumCells() {
            return m_cells.length;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<DataCell> iterator() {
            return Arrays.asList(m_cells).iterator();
        }
    }

    /**
     * Adds the rows of the given table to the container. The rows are
     * supposed to be created with the factory methods in OutputDataRow.
     *
     * @param table The table with the rows that should be added.
     * @param exec The execution context
     */
    void addTableAndFilterDuplicates(final SortedTable table,
            final ExecutionContext exec) {
        if (null == m_dc) {
            m_dc = exec.createDataContainer(m_spec);
        }
        if (null == table) {
            return;
        }
        double progress = 0;
        double inc = 1.0 / table.getRowCount();

        Comparator<DataRow> joinComp = OutputDataRow.createRowComparator();
        Iterator<DataRow> iter = table.iterator();
        if (!iter.hasNext()) {
            return;
        }
        DataRow prev = iter.next();
        progress += inc;
        exec.setProgress(progress);
        addRow(prev);
        while (iter.hasNext()) {
            DataRow next = iter.next();
            progress += inc;
            exec.setProgress(progress);
            // There might be equal rows in the case of match any option.
            if (joinComp.compare(prev, next) != 0) {
                prev = next;
                addRow(prev);
            }
        }
    }

    /** Return the table.
     * @return the table
     */
    BufferedDataTable getTable() {
        return m_dc.getTable();
    }
}

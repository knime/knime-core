/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   25.11.2009 (Heiko Hofer): created
 */
package org.knime.core.data.join;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.join.JoinTableSettings.SpecialJoinColumn;

import gnu.trove.strategy.HashingStrategy;

/**
 * A join tuple is a combination of join column values of a {@link DataRow}. By hashing join tuples, we can quickly find
 * data rows with identical values in certain columns.
 *
 * <h1>Usage</h1> Extract a join tuple from a data row using {@link JoinTuple#get(JoinTableSettings, DataRow)}. Pass the
 * join tuple and the row to {@link HashIndex#addHashRow(DataCell[], DataRow, long)} to index that row. Index one input
 * table of the join this way. Then iterate over the other input table, looking up join partners for each row using
 * {@link HashIndex#joinSingleRow(DataRow, long)}. For the {@link HybridHashJoin}, an additional partitioning layer is
 * used, that first partitions the rows in a table and creates a hash index for every partition.
 *
 * <h1>Internals</h1> For the conjunctive case ({@link JoinSpecification#isConjunctive()}), rows are matched by
 * comparing all their join clauses (they are interpreted as one large conjunction). In this case
 * {@link #hashConjunctive()} can be used to create a hash code that maps to the same value for two rows that match in
 * every join clause. <br/>
 * A hash map comparing join tuples according to this criterion can be created using {@link #hashConjunctive()} hashing
 * strategy. <br/>
 * <br/>
 *
 * In the disjunctive case, rows are matched by comparing each of their conjunctive clauses (they are interpreted as the
 * disjunction of n conjunctions). To get a hash code that maps two rows with the same values in the i-th clause to the
 * same value, use {@link #disjunctiveHashCode(DataCell[], int)}. A hash map that indexes rows according to the values
 * in their i-th clause columns can be created using the {@link #hashDisjunctiveClause(int)} hashing strategy.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 * @see HashIndex
 */
@SuppressWarnings("javadoc")
public interface JoinTuple {

    /**
     *
     * @param tableSettings specifies one side of the join clauses and where to find them in the data row
     * @param row the row to extract the join column values from
     * @return null if any of the values is missing. Otherwise, the column values of the row in the order they are
     *         referenced in {@link JoinTableSettings#getJoinClauses()}. The array may contain multiple references to
     *         the same data cell, if the corresponding column (or special join column) appears multiple times in the
     *         join clauses. For instance, for A=X && A=Z this method would return [A, A] for the left table side.
     */
    public static DataCell[] get(final JoinTableSettings tableSettings, final DataRow row) {
        int[] joinClauseColumns = tableSettings.getJoinClauseColumns();
        DataCell[] cells = new DataCell[joinClauseColumns.length];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = joinClauseColumns[i] == SpecialJoinColumn.ROW_KEY.m_columnIndexIndicator
                ? new StringCell(row.getKey().getString()) : row.getCell(joinClauseColumns[i]);
            if (cells[i].isMissing()) {
                return null;
            }
        }
        return cells;
    }

    /**
     * @param cells the join column values (as extracted by {@link #get(JoinTableSettings, DataRow)}).
     * @return a hash code such that two join tuples with identical values will have the same hash code
     */
    public static int conjunctiveHashCode(final DataCell[] cells) {
        return Arrays.hashCode(cells);
    }

    /**
     * @param cells the join column values (as extracted by {@link #get(JoinTableSettings, DataRow)}).
     * @param i compare rows by looking at the i-th conjunction in the disjunctive normal form defining the join clauses
     * @return a hash code such that two join tuples with identical values in the i-th clause will have the same hash
     *         code.
     */
    public static int disjunctiveHashCode(final DataCell[] cells, final int i) {
        // TODO replace this with DataCell strides to allow for join predicates in disjunctive normal form.
        return cells[i].hashCode();
    }

    /**
     * @return strategy that maps rows to the same bucket if they have the same value in all their join clauses
     */
    @SuppressWarnings("serial")
    public static HashingStrategy<DataCell[]> hashConjunctive() {
        return new HashingStrategy<DataCell[]>() {

            @Override
            public int computeHashCode(final DataCell[] joinClauseSides) {
                return Arrays.hashCode(joinClauseSides);
            }

            @Override
            public boolean equals(final DataCell[] o1, final DataCell[] o2) {
                for (int i = 0; i < o1.length; i++) {

                    // TODO the hybrid hash join discards rows with missing cells, so maybe one could remove this check.
                    if (o1[i].isMissing() || o2[i].isMissing()) {
                        return false;
                    }
                    // compare the data cells
                    if (!o1[i].equals(o2[i])) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * @param i compare rows by looking at the i-th conjunction in the disjunctive normal form defining the join clauses
     * @return strategy that maps rows to the same bucket if they have the same values in the i-th conjunctive clause
     */
    @SuppressWarnings("serial")
    public static HashingStrategy<DataCell[]> hashDisjunctiveClause(final int i) {

        return new HashingStrategy<DataCell[]>() {

            @Override
            public int computeHashCode(final DataCell[] joinClauseSides) {
                // TODO replace this with DataCell strides to allow for join predicates in disjunctive normal form.
                return joinClauseSides[i].hashCode();
            }

            @Override
            public boolean equals(final DataCell[] o1, final DataCell[] o2) {
                // TODO the hybrid hash join discards rows with missing cells, so maybe one could remove this check.
                if (o1[i].isMissing() || o2[i].isMissing()) {
                    return false;
                }
                // compare the data cells
                return o1[i].equals(o2[i]);
            }
        };
    }

}


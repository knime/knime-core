/**
 *
 */
/**
 * A join tuple is a combination of join column values of a {@link DataRow}. By hashing join tuples, we can quickly find
 * data rows with identical values in certain columns.
 *
 * <h1>Usage</h1> Extract a join tuple from a data row using {@link JoinTableSettings#get(DataRow)}. Pass the
 * join tuple and the row to {@link HashIndex#addHashRow(DataCell[], DataRow, long)} to index that row. Index one input
 * table of the join this way. Then iterate over the other input table, looking up join partners for each row using
 * {@link HashIndex#joinSingleRow(DataRow, long)}. For the {@link HybridHashJoin}, an additional partitioning layer is
 * used, that first partitions the rows in a table and creates a hash index for every partition.
 *
 * <h1>Internals</h1> For the conjunctive case ({@link JoinSpecification#isConjunctive()}), rows are matched by
 * comparing all their join clauses (they are interpreted as one large conjunction). In this case
 * {@link HashIndex#hashConjunctive()} can be used to create a hash code that maps to the same value for two rows that match in
 * every join clause. <br/>
 * A hash map comparing join tuples according to this criterion can be created using {@link HashIndex#hashConjunctive()} hashing
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
 * If to-be-joined data is too big to join in memory, the join will be split in several passes. In this case, the
 * handling of unmatched rows from the table (left/right) that is used as probe input has to be deferred until the last
 * pass over the probe input has been completed.
 *
 * @param side for which input table to set
 * @param defer whether to enable deferred collection of unmatched rows
 */
package org.knime.core.data.join.implementation;

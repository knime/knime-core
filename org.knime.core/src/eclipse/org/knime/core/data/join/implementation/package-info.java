/**
 * This package contains classes that implement join logic.
 * <ul>
 * <li>{@link org.knime.core.data.join.implementation.JoinImplementation} provides a base for implementing different
 * join algorithms, providing utility functions like HiLiting.</li>
 * <li>{@link org.knime.core.data.join.implementation.BlockHashJoin} is a simple join implementation that uses a nested
 * loop join that falls back to multiple passes over the larger table if heap space does not suffice to completely index
 * the smaller table.</li>
 * <li>{@link org.knime.core.data.join.implementation.HashIndex} is a utility class to index a table for fast lookup of
 * rows according to the values in their join columns.</li>
 * <li>{@link org.knime.core.data.join.implementation.OrderedRow} is a utility class for relating rows to ordering
 * information, usually their offset in the table they came from.</li>
 * <li>{@link org.knime.core.data.join.implementation.JoinerFactory} a functional interface that defines the constructor
 * of a join implementation.</li>
 * </ul>
 *
 * <h2>Join example</h2>
 *
 * To give an idea how the code works together, a small example is given. The values of the join columns of a row are
 * extracted using {@link JoinTableSettings#getJoinTuple(DataRow)}. Pass the join tuple and the row to
 * {@link HashIndex#addHashRow(DataCell[], DataRow, long)} to index that row. Index one input table of the join this
 * way. Then iterate over the other input table, looking up join partners for each row using
 * {@link HashIndex#joinSingleRow(DataRow, long)}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
package org.knime.core.data.join.implementation;

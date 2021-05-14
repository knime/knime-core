/**
 * This package contains logic to collect the results, i.e., matching row pairs and individual unmatched rows, from a
 * join algorithm.
 *
 * <ul>
 * <li>{@link org.knime.core.data.join.results.JoinResult} defines the methods for objects that are designed to collect
 * the output of a join algorithm, e.g., adding matches and unmatched rows to the results. It defines two interfaces for
 * join output results in different formats: {@link org.knime.core.data.join.results.JoinResult.OutputCombined} for
 * collecting all join outputs in a single table and {@link org.knime.core.data.join.results.JoinResult.OutputSplit} for
 * collecting matches and unmatched rows in separate output tables. The user collects the final results by calling
 * {@link org.knime.core.data.join.results.JoinResult#getResults()}, obtaining either combined or split output.</li>
 * <li>{@link org.knime.core.data.join.results.JoinContainer} is a base implementation for
 * {@link org.knime.core.data.join.results.JoinResult}. It provides utility methods, e.g., to detect and reject
 * duplicate matches.</li>
 * <li>{@link org.knime.core.data.join.results.Unsorted} is a {@link org.knime.core.data.join.results.JoinContainer} for
 * results that do not require the output to be in any specific order, which helps performance.</li>
 * <li>{@link org.knime.core.data.join.results.LeftRightSorted} is a
 * {@link org.knime.core.data.join.results.JoinContainer} that outputs results (either combined or split into separate
 * tables) such that the rows are sorted according to their original offsets in the left or right input tables.</li>
 * <li>{@link org.knime.core.data.join.results.UnmatchedRowCollector} is an interface for adding logic to the processing
 * of unmatched rows. In some cases those can be output directly, in other cases they need to be processed at a later
 * point in time, in which case a {@link org.knime.core.data.join.results.DeferredUnmatchedRowCollector} is used.</li>
 * <li>{@link org.knime.core.data.join.results.RowHandler} is a functional interface to define callbacks on rows, e.g.,
 * a callback that defines what to do with unmatched rows. This is convenient to reference logic for left/right
 * unmatched rows, because which (left/right) table is used as hash input and which is used as probe input depends on
 * their relative sizes. In case the callback performs a long operation, the
 * {@link org.knime.core.data.join.results.RowHandlerCancelable} can be used to check for cancellation by the user.</li>
 * </ul>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 */
package org.knime.core.data.join.results;

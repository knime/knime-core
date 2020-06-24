/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 9, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.util.Arrays;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinImplementation.JoinProgressMonitor;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.results.JoinResults;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests all combinations of
 * - join types: inner, outer, anti
 * - output orders: arbitrary, probe-hash, left-right
 * - execution modes: in-memory, partially in-memory, on disk
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@RunWith(Theories.class)
public class HybridHashJoinTest extends JoinTest {

    /**
     * Produce both matches and unmatched rows in both input tables.
     * Include one non-join column from each table.
     */
    abstract static class SmallInput extends JoinTestInput {

        // left table
        DataRow leftA = defaultRow("A,A,1,2"), // left outer
                leftB = defaultRow("B,B,3,4"), // inner
                leftX = defaultRow("X,X,100,101"); // inner

        // right table
        DataRow rightX = defaultRow("X,X,102,103"), // inner
                rightB = defaultRow("B,B,5,6"), // inner
                rightC = defaultRow("C,?,7,8"), // right outer
                rightD = defaultRow("D,D,9,10"); // right outer

        DataRow leftAProjected = defaultRow("A,1"),
                leftBProjected = defaultRow("B,3"),
                leftXProjected = defaultRow("X,100");

        DataRow rightXProjected = defaultRow("X,103"),
                rightBProjected = defaultRow("B,6"),
                rightCProjected = defaultRow("C,8"),
                rightDProjected = defaultRow("D,10");

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", leftA, leftB, leftX);
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", rightX, rightB, rightC, rightD);
        }

        // results
        DataRow innerX = defaultRow("X+X,100,103");
        DataRow innerB = defaultRow("B+B,3,6");
        DataRow leftOuterA = defaultRow("A+?,1,?");
        DataRow leftOuterB = defaultRow("B+?,3,?");
        DataRow leftOuterX = defaultRow("X+?,100,?");
        DataRow rightOuterX = defaultRow("?+X,?,103");
        DataRow rightOuterB = defaultRow("?+B,?,6");
        DataRow rightOuterC = defaultRow("?+C,?,8");
        DataRow rightOuterD = defaultRow("?+D,?,10");

        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new String[]{"Join Column"};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch(order) {
                case ARBITRARY:
                    switch(mode) {
                        case INNER: return new DataRow[] {innerB, innerX};
                        case LEFT_OUTER: return new DataRow[] {innerX, leftOuterA, innerB};
                        case RIGHT_OUTER: return new DataRow[] {innerX, innerB, rightOuterC, rightOuterD};
                        case FULL_OUTER: return new DataRow[] {innerX, rightOuterC, leftOuterA, innerB, rightOuterD};
                        case LEFT_ANTI: return new DataRow[] {leftOuterA};
                        case RIGHT_ANTI: return new DataRow[] {rightOuterD, rightOuterC};
                        case FULL_ANTI: return new DataRow[] {rightOuterD, rightOuterC, leftOuterA};
                        case EMPTY: return new DataRow[] {};
                        default: throw new IllegalStateException();
                    }
                case DETERMINISTIC:
                    switch(mode) {
                        // innerX comes first because the right table dictates major row order (because it is larger
                        // and thus used as probe table)
                        case INNER: return new DataRow[] {innerX, innerB};
                        case LEFT_OUTER: return new DataRow[] {innerX, innerB, leftOuterA};
                        case RIGHT_OUTER: return new DataRow[] {innerX,innerB, rightOuterC, rightOuterD};
                        case FULL_OUTER: return new DataRow[] {innerX, innerB,leftOuterA, rightOuterC, rightOuterD};
                        case LEFT_ANTI: return new DataRow[]{ leftOuterA};
                        case RIGHT_ANTI: return new DataRow[] {rightOuterC, rightOuterD};
                        case FULL_ANTI: return new DataRow[] {leftOuterA, rightOuterC, rightOuterD};
                        case EMPTY: return new DataRow[] {};
                        default: throw new IllegalStateException();
                    }
                case LEFT_RIGHT: switch(mode) {
                    // innerB comes first because the left table dictates the major row order
                    case INNER: return new DataRow[] {innerB, innerX};
                    case LEFT_OUTER: return new DataRow[] {innerB, innerX, leftOuterA};
                    case RIGHT_OUTER: return new DataRow[] {innerB,innerX, rightOuterC, rightOuterD};
                    case FULL_OUTER: return new DataRow[] {innerB, innerX,leftOuterA, rightOuterC, rightOuterD};
                    case LEFT_ANTI: return new DataRow[]{ leftOuterA};
                    case RIGHT_ANTI: return new DataRow[] {rightOuterC, rightOuterD};
                    case FULL_ANTI: return new DataRow[] {leftOuterA, rightOuterC, rightOuterD};
                    case EMPTY: return new DataRow[] {};
                    default: throw new IllegalStateException();
                }
                default: throw new IllegalStateException();
            }
        }

        @Override
        DataRow[] leftOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT: return new DataRow[]{leftAProjected};
                default: throw new IllegalStateException();
            }
        }

        @Override
        DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY: return new DataRow[]{rightDProjected, rightCProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT: return new DataRow[]{rightCProjected, rightDProjected};
                default: throw new IllegalStateException();
            }
        }

        @Override public String toString() { return "SmallInput"; }

    }

    /**
     * Test two small input tables that produce innner, left outer, and right outer results.
     */
    @DataPoint
    public static final JoinTestInput allResultTypes = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new String[]{"Join Column"};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }
    };

    /**
     * Test self join.
     */
    @DataPoint
    public static final JoinTestInput selfJoin = new SmallInput() {
        BufferedDataTable table = JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", leftA, leftB, leftX);
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new String[]{"Join Column"};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override BufferedDataTable left() { return table; }
        @Override BufferedDataTable right() { return table; }

        @Override
        DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (mode) {
                case INNER:
                case LEFT_OUTER:
                case RIGHT_OUTER:
                case FULL_OUTER:
                    return new DataRow[]{JoinTestInput.defaultRow("A+A,1,2"), JoinTestInput.defaultRow("B+B,3,4"),
                        JoinTestInput.defaultRow("X+X,100,101")};
                case EMPTY:
                case LEFT_ANTI:
                case RIGHT_ANTI:
                case FULL_ANTI:
                    return new DataRow[0];
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        DataRow[] leftOuter(final OutputRowOrder order) { return new DataRow[0]; }

        @Override
        DataRow[] rightOuter(final OutputRowOrder order) { return new DataRow[0]; }

        @Override public String toString() { return "SmallInput"; }

    };

    /**
     * Produces the same output as {@link SmallInput}, but by comparing the row key to "Join Column"
     */
    @DataPoint
    public static final JoinTestInput mixRowKeyWithNormalColumn = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

    };

    /**
     * Produces the same output as {@link SmallInput} but by comparing the row keys instead of the join columns.
     */
    @DataPoint
    public static final JoinTestInput joinOnRowKeys = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }
    };

    /**
     * Test that duplicate join clauses (e.g., A=A && A=A) work as expected.
     */
    @DataPoint
    public static final JoinTestInput joinOnRedundantColumns = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column", "Join Column",JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column", "Join Column",JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

    };

    /**
     * Test match any with a single clause - gives same result as conjunctive with single clause.
     */
    @DataPoint
    public static final JoinTestInput singleColumnMatchAny = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder)
            throws InvalidSettingsException {

            JoinTableSettings leftSettings =
                JoinTableSettings.left(joinMode.m_retainLeftUnmatched, m_leftJoinColumns, m_leftIncludeColumns, left());
            JoinTableSettings rightSettings = JoinTableSettings.right(joinMode.m_retainRightUnmatched,
                m_rightJoinColumns, m_rightIncludeColumns, right());


            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*"))
                .mergeJoinColumns(false)
                // match any
                .conjunctive(false)
                .outputRowOrder(outputRowOrder).rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

    };

    /**
     * Test match any with a clause that doesn't change results (because it produces no matches).
     */
    @DataPoint
    public static final JoinTestInput nonAdditionalMatchAny = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column"};
            m_rightJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Nonjoin1"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder)
            throws InvalidSettingsException {

            JoinTableSettings leftSettings =
                JoinTableSettings.left(joinMode.m_retainLeftUnmatched, m_leftJoinColumns, m_leftIncludeColumns, left());
            JoinTableSettings rightSettings = JoinTableSettings.right(joinMode.m_retainRightUnmatched,
                m_rightJoinColumns, m_rightIncludeColumns, right());


            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*"))
                .mergeJoinColumns(false)
                // match any
                .conjunctive(false)
                .outputRowOrder(outputRowOrder).rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

    };

    /**
     * Test match any with a single clause - gives same result as conjunctive with single clause.
     */
    @DataPoint
    public static final JoinTestInput redundantMatchAny = new SmallInput() {
        @Override void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder)
            throws InvalidSettingsException {

            JoinTableSettings leftSettings =
                JoinTableSettings.left(joinMode.m_retainLeftUnmatched, m_leftJoinColumns, m_leftIncludeColumns, left());
            JoinTableSettings rightSettings = JoinTableSettings.right(joinMode.m_retainRightUnmatched,
                m_rightJoinColumns, m_rightIncludeColumns, right());


            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*"))
                .mergeJoinColumns(false)
                // match any
                .conjunctive(false)
                .outputRowOrder(outputRowOrder).rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

    };

    /**
     * Test that joining on row keys and mixing a special column with a normal column does not throw any errors.
     * Also covers the case where no matches are produced at all.
     */
    @DataPoint
    public static final JoinTestInput emptyJoinOnRowKeys = new SmallInput() {
        @Override void configureJoin() {
            m_leftJoinColumns =
                new Object[]{JoinTableSettings.SpecialJoinColumn.ROW_KEY, JoinTableSettings.SpecialJoinColumn.ROW_KEY};
            m_rightJoinColumns = new Object[]{"Nonjoin1", "Nonjoin2"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    switch(mode) {
                        case EMPTY:
                        case INNER: return new DataRow[0];
                        case LEFT_ANTI:
                        case LEFT_OUTER: return new DataRow[] {leftOuterA, leftOuterB, leftOuterX};
                        case RIGHT_ANTI:
                        case RIGHT_OUTER: return new DataRow[] {rightOuterX, rightOuterB, rightOuterC, rightOuterD};
                        case FULL_ANTI:
                        case FULL_OUTER: return new DataRow[] {leftOuterA, leftOuterB, leftOuterX, rightOuterX, rightOuterB, rightOuterC, rightOuterD};
                        default: throw new IllegalStateException();
                    }
                default: throw new IllegalStateException();
            }

        }

        /** empty join -> all left rows are unmatched */
        @Override
        DataRow[] leftOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY: return new DataRow[]{leftAProjected,leftXProjected,leftBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT: return new DataRow[]{leftAProjected,leftBProjected,leftXProjected};
                default: throw new IllegalStateException();
            }
        }

        /** empty join -> all right rows are unmatched */
        @Override
        DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY: return new DataRow[]{rightDProjected, rightXProjected, rightCProjected, rightBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT: return new DataRow[]{rightXProjected, rightBProjected, rightCProjected, rightDProjected};
                default: throw new IllegalStateException();
            }
        }

    };

    /**
     * Test the case where one table contains no rows at all.
     */
    @DataPoint
    public static final JoinTestInput emptyHashTable = new SmallInput() {
        @Override void configureJoin() {
            m_leftJoinColumns = new String[]{"Join Column"};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", new DataRow[0]);
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", rightX, rightB, rightC, rightD);
        }

        @Override DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    switch(mode) {
                        case EMPTY:
                        case LEFT_ANTI:
                        case LEFT_OUTER:
                        case INNER: return new DataRow[0];
                        case RIGHT_ANTI:
                        case RIGHT_OUTER:
                        case FULL_ANTI:
                        case FULL_OUTER: return new DataRow[] {rightOuterX, rightOuterB, rightOuterC, rightOuterD};
                        default: throw new IllegalStateException();
                    }
                default: throw new IllegalStateException();
            }

        }

        /** empty join -> all left rows are unmatched */
        @Override
        DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        /** empty join -> all right rows are unmatched */
        @Override
        DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY: return new DataRow[]{rightDProjected, rightXProjected, rightCProjected, rightBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT: return new DataRow[]{rightXProjected, rightBProjected, rightCProjected, rightDProjected};
                default: throw new IllegalStateException();
            }
        }

    };

    /**
     * Just a single inner join result row.
     */
    @DataPoint
    public static JoinTestInput singleInnerJoin = new JoinTestInput() {

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", "Left B,B,3,4");
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", "Right B,B,5,6");
        }

        @Override void configureJoin() {
            m_leftJoinColumns = new String[]{"Join Column"};
            m_rightJoinColumns = new String[]{"Join Column"};
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {

            DataRow innerA = defaultRow("Left B+Right B,3,6");

            switch (mode) {
                case INNER:
                case LEFT_OUTER:
                case RIGHT_OUTER:
                case FULL_OUTER:
                    return new DataRow[]{innerA};
                case LEFT_ANTI:
                case RIGHT_ANTI:
                case FULL_ANTI:
                case EMPTY:
                    return new DataRow[]{};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override public String toString() { return "single row"; }

        @Override
        DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        @Override
        DataRow[] rightOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

    };

    /**
     * @param input the left and right input table
     * @param joinMode which results to retain
     * @param order output row order
     * @param executionMode in-memory, partial on disk, on disk
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Theory
    public void getSingleTable(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO remove
        assumeThat(input, is(not(nonAdditionalMatchAny)));
        assumeThat(input, is(not(redundantMatchAny)));
        assumeThat(input, is(not(singleColumnMatchAny)));

//        assumeThat(joinMode, is(JoinMode.INNER));
//        assumeThat(order, is(OutputOrder.LEGACY));
        assumeThat(executionMode, is(Execution.IN_MEMORY));

//        assumeThat(joinMode, is(JoinMode.LEFT_OUTER));
//        assumeThat(order, is(OutputOrder.ARBITRARY));
//        assumeThat(executionMode, is(Execution.ON_DISK));
        System.out.println(String.format("Single Table %-15s %-15s %-15s", joinMode, order.m_rowOrder, executionMode));

        // create the joiner
        JoinSpecification joinSpec = input.getJoinSpecification(joinMode, order.m_rowOrder);
        JoinImplementation implementation =
            JoinerFactory.JoinAlgorithm.HYBRID_HASH.getFactory().create(joinSpec, JoinTestInput.EXEC);
        implementation.setMaxOpenFiles(4);

        HybridHashJoin hybridHash = (HybridHashJoin)implementation;
        JoinProgressMonitor mon = hybridHash.m_progress;
        mon.setDesiredPartitionsOnDisk(executionMode.m_desiredPartitionsOnDisk);

        // do the join
        BufferedDataTable result = hybridHash.join().getSingleTable();

        validateExecutionMode(joinMode, executionMode, mon);

        // compare to expected results
        DataRow[] expected = input.ordered(joinMode, order.m_rowOrder);
        order.m_validator.accept(result, expected);

    }

    /**
     * @param input the left and right input table
     * @param joinMode which results to retain
     * @param order output row order
     * @param executionMode in-memory, partial on disk, on disk
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Theory
    public void separateOutput(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO remove
        assumeThat(input, is(not(nonAdditionalMatchAny)));
        assumeThat(input, is(not(redundantMatchAny)));
        assumeThat(input, is(not(singleColumnMatchAny)));
//
//        assumeThat(input, is(mixRowKeyWithNormalColumn));
//        assumeThat(joinMode, is(JoinMode.INNER));
//        assumeThat(order, is(OutputOrder.LEGACY));
        assumeThat(executionMode, is(Execution.IN_MEMORY));
        System.out.println(String.format(">>>> Separate Outputs %-15s %-15s %-15s", joinMode,  order.m_rowOrder, executionMode));

        // create the joiner
        JoinSpecification joinSpec = input.getJoinSpecification(joinMode, order.m_rowOrder);

        // a small number of allowed open files is needed to test flushing to disk on small inputs
        // e.g., if there are only 3 rows, at most 3 partition pairs will be flushed to disk
        JoinImplementation implementation = JoinerFactory.JoinAlgorithm.HYBRID_HASH.getFactory()
                .create(joinSpec, JoinTestInput.EXEC)
                .setMaxOpenFiles(4);

        HybridHashJoin hybridHash = (HybridHashJoin)implementation;
        JoinProgressMonitor mon = hybridHash.m_progress;
        mon.setDesiredPartitionsOnDisk(executionMode.m_desiredPartitionsOnDisk);

        // do the join
        JoinResults results = hybridHash.join();
        validateExecutionMode(joinMode, executionMode, mon);

        if(joinMode.m_retainMatches) {
            DataRow[] expectedMatches = input.ordered(JoinMode.INNER, order.m_rowOrder);
            BufferedDataTable actual = results.getMatches();
            order.m_validator.accept(actual, expectedMatches);
        }

        if(joinMode.m_retainLeftUnmatched) {
            // validate left unmatched rows by comparing the produced left unmatched rows with the expected join result
            // for a left antijoin (only left unmatched rows)
            DataRow[] expectedLeft = input.leftOuter(order.m_rowOrder);
            BufferedDataTable actual = results.getLeftOuter();
            order.m_validator.accept(actual, expectedLeft);
        }

        if (joinMode.m_retainRightUnmatched) {
            // validate right unmatched rows
            DataRow[] expectedRight = input.rightOuter(order.m_rowOrder);
            BufferedDataTable actual = results.getRightOuter();
            order.m_validator.accept(actual, expectedRight);
        }

    }

    /**
     * Checks that in-memory executions do not use disk (etc.) by comparing the execution statistics reported by the
     * {@link JoinProgressMonitor} to the desired execution mode.
     *
     * @param joinMode to check whether the result contains any results at all
     * @param executionMode to check whether in-memory or disk should be used
     * @param mon provides execution information about the hybrid hash join
     */
    private static void validateExecutionMode(final JoinMode joinMode, final Execution executionMode,
        final JoinProgressMonitor mon) {
        switch (executionMode) {
            case IN_MEMORY:
                // if in-memory execution is to be tested, then no partitions are to be swapped to disk
                // this also tests the execution of this test case (maybe not enough heap is available to join in memory)
                assertTrue(mon.getProbeRowsProcessedInMemory() > 0 ^ joinMode == JoinMode.EMPTY);
                assertTrue(mon.getProbeRowsProcessedFromDisk() == 0);
                break;
            case PARTIAL_IN_MEMORY:
                // if we're testing the low memory case, at least some buckets must be written to disk
                // (except there is no output at all to be produced, then zero partitions should be flushed to disk)
                {
                    String message = "No rows in memory in partial in-memory execution. "
                        + Arrays.toString(mon.m_hashBucketSizes);
                    assertTrue(message, mon.getProbeRowsProcessedInMemory() > 0 ^ joinMode == JoinMode.EMPTY);
                }
                {
                    long nonEmptyProbeDiskBuckets = Arrays.stream(mon.m_probeBucketSizes).filter(l -> l > 0).count();
                    String message = "No rows processed from disk in partial in-memory execution. "
                        + Arrays.toString(mon.m_hashBucketSizes);
                    assertTrue(message, mon.getProbeRowsProcessedFromDisk() > 0
                        ^ (joinMode == JoinMode.EMPTY || nonEmptyProbeDiskBuckets == 0));
                }
                break;
            case ON_DISK:
                //                assertTrue("Processed " + mon.getProbeRowsProcessedInMemory() + " rows in memory in disk-based execution mode.", mon.getProbeRowsProcessedInMemory() == 0);
                //                assertTrue(String.format("%s partitions on disk in disk-based execution mode", mon.getNumPartitionsOnDisk()), mon.getNumPartitionsOnDisk() > 0 ^ mon.getProbeRowsProcessedInMemory() == 0);
                //                assertTrue(
                //                    String.format(
                //                        "%s rows processed from disk in disk based execution mode. Rows processed in memory: %s",
                //                        mon.getProbeRowsProcessedFromDisk(), mon.getProbeRowsProcessedInMemory()),
                //                    mon.getProbeRowsProcessedFromDisk() > 0 ^ joinMode == JoinMode.EMPTY);
        }
    }


}

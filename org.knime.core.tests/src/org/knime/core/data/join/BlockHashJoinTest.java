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
import static org.junit.Assume.assumeThat;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinImplementation.JoinProgressMonitor;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.results.JoinContainer;
import org.knime.core.data.join.results.LeftRightSortedJoinContainer;
import org.knime.core.data.join.results.UnorderedJoinContainer;
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
public class BlockHashJoinTest extends JoinTest {

    // TODO test with true, but that would require test data with annotated offsets
    @DataPoints
    public static boolean[] doExtractRowOffsets = new boolean[] {false};

    /**
     * TODO match any not yet implemented
     */
    @DataPoints
    public static JoinTestInput[] inputs = JoinTestInput.CONJUNCTIVE;

    /**
     * @param input the left and right input table
     * @param joinMode which results to retain
     * @param order output row order
     * @param executionMode
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Theory
    public void getSingleTable(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO fast sort not yet supported
        assumeThat(order, is(not(OutputOrder.PROBE_HASH)));
        // consider in memory and partial in memory (on disk doesn't make a difference compared to partial in memory)
        assumeThat(executionMode, is(not(Execution.ON_DISK)));

        // create the joiner
        JoinSpecification joinSpec = input.getJoinSpecification(joinMode, order.m_rowOrder);

        // only to get the progress monitor
        HybridHashJoin hybrid = (HybridHashJoin)JoinerFactory.JoinAlgorithm.HYBRID_HASH.getFactory().
                create(joinSpec, JoinTestInput.EXEC);
        JoinProgressMonitor mon = hybrid.m_progress;
        mon.m_assumeMemoryLow = executionMode != Execution.IN_MEMORY;

        // test data would need to be in auxiliary format with annotated row offsets.
        // only relevant when splitting the join problem in several independent join problems.
        boolean extractRowOffsets = false;
        BlockHashJoin blockHashJoin = new BlockHashJoin(JoinTestInput.EXEC, mon, joinSpec, extractRowOffsets);

        JoinContainer results = order.m_rowOrder == OutputRowOrder.ARBITRARY
            ? new UnorderedJoinContainer(joinSpec, JoinTestInput.EXEC, false, false)
            : new LeftRightSortedJoinContainer(joinSpec, JoinTestInput.EXEC, false, false);

        // do the join
        blockHashJoin.join(results);
        BufferedDataTable result = results.getSingleTable();

        // compare to expected results
        DataRow[] expected = input.ordered(joinMode, order.m_rowOrder);
        order.m_validator.accept(result, expected);

    }

    /**
     * @param input the left and right input table
     * @param joinMode which results to retain
     * @param order output row order
     * @param executionMode
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Theory
    public void separateOutput(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO fast sort not yet supported
        assumeThat(order, is(not(OutputOrder.PROBE_HASH)));
        // consider in memory and partial in memory (on disk doesn't make a difference compared to partial in memory)
        assumeThat(executionMode, is(not(Execution.ON_DISK)));

        System.out.println(String.format("Single Table %-15s %-15s %-15s", joinMode, order.m_rowOrder, executionMode));

        // create the joiner
        JoinSpecification joinSpec = input.getJoinSpecification(joinMode, order.m_rowOrder);

        // only to get the progress monitor
        HybridHashJoin hybrid = (HybridHashJoin)JoinerFactory.JoinAlgorithm.HYBRID_HASH.getFactory().
                create(joinSpec, JoinTestInput.EXEC);
        JoinProgressMonitor mon = hybrid.m_progress;
        mon.m_assumeMemoryLow = executionMode != Execution.IN_MEMORY;

        // test data would need to be in auxiliary format with annotated row offsets.
        // only relevant when splitting the join problem in several independent join problems.
        boolean extractRowOffsets = false;
        BlockHashJoin blockHashJoin = new BlockHashJoin(JoinTestInput.EXEC, mon, joinSpec, extractRowOffsets);

        JoinContainer results = order.m_rowOrder == OutputRowOrder.ARBITRARY
            ? new UnorderedJoinContainer(joinSpec, JoinTestInput.EXEC, extractRowOffsets, extractRowOffsets)
            : new LeftRightSortedJoinContainer(joinSpec, JoinTestInput.EXEC, extractRowOffsets, extractRowOffsets);

        // do the join
        blockHashJoin.join(results);

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


}

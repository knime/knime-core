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

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinImplementation.JoinProgressMonitor;
import org.knime.core.data.join.results.JoinResults.OutputSplit;
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
     * TODO match any not yet implemented
     */
    @DataPoints
    public static JoinTestInput[] inputs = JoinTestInput.CONJUNCTIVE;

    /**
     * @param input the left and right input table
     * @param joinMode which results to retain
     * @param order output row order
     * @param executionMode in-memory, partial on disk, on disk
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Theory
    public void testJoinOutputCombined(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO fast sort not yet supported
        assumeThat(order, is(not(OutputOrder.PROBE_HASH)));

        // TODO full hybrid hash join not yet supported
        assumeThat(executionMode, is(not(Execution.PARTIAL_IN_MEMORY)));

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
        BufferedDataTable result = hybridHash.joinOutputCombined().getTable();

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
    public void testJoinOutputSplit(final JoinTestInput input, final JoinMode joinMode, final OutputOrder order,
        final Execution executionMode) throws CanceledExecutionException, InvalidSettingsException {

        // TODO fast sort not yet supported
        assumeThat(order, is(not(OutputOrder.PROBE_HASH)));

        // TODO full hybrid hash join not yet supported
        assumeThat(executionMode, is(not(Execution.PARTIAL_IN_MEMORY)));

//        assumeThat(input, is(mixRowKeyWithNormalColumn));
//        assumeThat(joinMode, is(JoinMode.INNER));
//        assumeThat(order, is(OutputOrder.LEGACY));
//        assumeThat(executionMode, is(Execution.IN_MEMORY));
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
        OutputSplit results = hybridHash.joinOutputSplit();
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

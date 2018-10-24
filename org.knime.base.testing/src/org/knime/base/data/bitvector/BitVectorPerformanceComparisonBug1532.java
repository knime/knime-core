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
 */
package org.knime.base.data.bitvector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;
import org.knime.core.data.vector.bitvector.BitVectorUtil;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class BitVectorPerformanceComparisonBug1532 {
    @Test
    public void testCompareCreate() throws Throwable {
        int size = 500000;
        int length = 2048;
        // load classes before
        assertNotNull(BitVectorCell.TYPE);
        assertNotNull(DenseBitVectorCell.TYPE);
        assertNotNull(DenseBitVectorCellFactory.class);
        final long seed = System.currentTimeMillis();
        NodeLogger.getLogger(BitVectorPerformanceComparisonBug1532.class).info("Using seed " + seed);
        long time = System.currentTimeMillis();
        createNewDenseBitVectorCells(size, length, seed);
        long timeForNew = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        createOldBitVectorCells(size, length, seed);
        long timeForOld = System.currentTimeMillis() - time;
        NodeLogger.getLogger(BitVectorPerformanceComparisonBug1532.class).info(
            "bit vector generation old: " + timeForOld);
        NodeLogger.getLogger(BitVectorPerformanceComparisonBug1532.class).info(
            "bit vector generation new: " + timeForNew);
        assertTrue("Creation of new bit vector cells takes longer than " +
        		"generating old (java) bit vectors", timeForNew < timeForOld);
    }

    @Test
    public void testCompareTanimotoTime() throws Throwable {
        final int nRuns = 100;
        final int size = 128;
        long sumOfRuntimesNew = 0;
        long sumOfRuntimesOld = 0;

        for (int runId = 0; runId < nRuns; runId++) {
            final int length = 16384 + new Random(runId).nextInt(16384);
            DenseBitVectorCell[] newCells = createNewDenseBitVectorCells(size, length, runId);
            BitVectorCell[] oldCells = createOldBitVectorCells(size, length, runId);
            double[] newValues = new double[size * (size - 1) / 2];
            double[] oldValues = new double[size * (size - 1) / 2];
            int point = 0;
            long time = System.nanoTime();
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    newValues[point++] = getNewTanimoto(newCells[i], newCells[j]);
                }
            }
            long timeForNew = System.nanoTime() - time;
            point = 0;
            time = System.nanoTime();
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    oldValues[point++] = getOldTanimoto(oldCells[i], oldCells[j]);
                }
            }
            long timeForOld = System.nanoTime() - time;
            NodeLogger.getLogger(BitVectorPerformanceComparisonBug1532.class).info(
                "tanimoto calculation old: " + timeForOld);
            NodeLogger.getLogger(BitVectorPerformanceComparisonBug1532.class).info(
                "tanimoto calculation new: " + timeForNew);

            sumOfRuntimesNew += timeForNew;
            sumOfRuntimesOld += timeForOld;
        }

        assertTrue("Tanimoto calculation of new bit vector cells takes much "
            + "longer than calculation on old (java) bit vectors: " + sumOfRuntimesNew + "ns vs. " + sumOfRuntimesOld
            + "ns", sumOfRuntimesNew < 1.2 * sumOfRuntimesOld);
    }


    private double getOldTanimoto(final BitVectorCell c1,
            final BitVectorCell c2) {
        BitSet set1 = c1.getBitSet();
        BitSet set2 = c2.getBitSet();
        BitSet intersection = (BitSet)set1.clone();
        intersection.and(set2);
        int nominator = intersection.cardinality();
        int denominator  = set1.cardinality()
            + set2.cardinality() - nominator;
        if (denominator > 0) {
            return 1.0 - nominator / (double)denominator;
        } else {
            return 1.0;
        }
    }

    private double getNewTanimoto(final DenseBitVectorCell b1,
            final DenseBitVectorCell b2) {
        long nominator = BitVectorUtil.cardinalityOfIntersection(b1, b2);
        long denominator  = b1.cardinality() + b2.cardinality() - nominator;
        if (denominator > 0) {
            return 1.0 - nominator / (double)denominator;
        } else {
            return 1.0;
        }
    }

    private BitVectorCell[] createOldBitVectorCells(
            final int count, final long vectorLength, final long seed) {
        Random r = new Random(seed);
        BitVectorCell[] result = new BitVectorCell[count];
        int hexLength = (int)(vectorLength / 4);
        StringBuilder b = new StringBuilder(hexLength);
        for (int i = 0; i < count; i++) {
            b.setLength(0);
            for (int j = 0; j < hexLength; j++) {
                b.append(HEX_CHARS[r.nextInt(16)]);
            }
            result[i] = new BitVectorCell(b.toString());
        }
        return result;
    }

    private DenseBitVectorCell[] createNewDenseBitVectorCells(
            final int count, final long vectorLength, final long seed) {
        Random r = new Random(seed);
        DenseBitVectorCell[] result = new DenseBitVectorCell[count];
        int hexLength = (int)(vectorLength / 4);
        StringBuilder b = new StringBuilder(hexLength);
        for (int i = 0; i < count; i++) {
            b.setLength(0);
            for (int j = 0; j < hexLength; j++) {
                b.append(HEX_CHARS[r.nextInt(16)]);
            }
            result[i] = new DenseBitVectorCellFactory(
                    b.toString()).createDataCell();
        }
        return result;
    }

    private static final char[] HEX_CHARS = new char[]{
        '0', '1', '2','3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'};
}

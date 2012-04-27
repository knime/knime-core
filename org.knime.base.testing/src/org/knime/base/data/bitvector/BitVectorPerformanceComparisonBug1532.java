/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 22, 2008 (wiswedel): created
 */
package org.knime.base.data.bitvector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

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
        System.out.println("Using seed " + seed);
        long time = System.currentTimeMillis();
        createNewDenseBitVectorCells(size, length, seed);
        long timeForNew = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        createOldBitVectorCells(size, length, seed);
        long timeForOld = System.currentTimeMillis() - time;
        System.out.println("bit vector generation old: " + timeForOld);
        System.out.println("bit vector generation new: " + timeForNew);
        assertTrue("Creation of new bit vector cells takes longer than " +
        		"generating old (java) bit vectors", timeForNew < timeForOld);
    }

    @Test
    public void testCompareTanimotoResult() throws Throwable {
        final long seed = System.currentTimeMillis();
        int size = 5000;
        // NOTE (BW), 22 Oct 2008:
        // If you set the length to 2048 the test case seems to succeed
        // more often than with a random length (as below)
        int length = 1000 + new Random(seed).nextInt(2000);
        System.out.println("Using seed " + seed);
        DenseBitVectorCell[] newCells = createNewDenseBitVectorCells(
                size, length, seed);
        BitVectorCell[] oldCells = createOldBitVectorCells(size, length, seed);
        double[] newValues = new double[size * (size - 1) / 2];
        double[] oldValues = new double[size * (size - 1) / 2];
        long time = System.currentTimeMillis();
        int point = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                newValues[point++] = getNewTanimoto(newCells[i], newCells[j]);
            }
        }
        long timeForNew = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        point = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                oldValues[point++] = getOldTanimoto(oldCells[i], oldCells[j]);
            }
        }
        long timeForOld = System.currentTimeMillis() - time;
        System.out.println("tanimoto calculation old: " + timeForOld);
        System.out.println("tanimoto calculation new: " + timeForNew);
        assertTrue(Arrays.equals(newValues, oldValues));
    }

    // @Test
    // This is not a real bug and do not have an idea how to resolve
    // this issue. It is recorded in Bugzilla, thus this test is currently
    // disabled.
    public void testCompareTanimotoTime() throws Throwable {
        final long seed = System.currentTimeMillis();
        int size = 5000;
        // NOTE (BW), 22 Oct 2008:
        // If you set the length to 2048 the test case seems to succeed
        // more often than with a random length (as below)
        int length = 1000 + new Random(seed).nextInt(2000);
        System.out.println("Using seed " + seed);
        DenseBitVectorCell[] newCells = createNewDenseBitVectorCells(
                size, length, seed);
        BitVectorCell[] oldCells = createOldBitVectorCells(size, length, seed);
        double[] newValues = new double[size * (size - 1) / 2];
        double[] oldValues = new double[size * (size - 1) / 2];
        long time = System.currentTimeMillis();
        int point = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                newValues[point++] = getNewTanimoto(newCells[i], newCells[j]);
            }
        }
        long timeForNew = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        point = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                oldValues[point++] = getOldTanimoto(oldCells[i], oldCells[j]);
            }
        }
        long timeForOld = System.currentTimeMillis() - time;
        System.out.println("tanimoto calculation old: " + timeForOld);
        System.out.println("tanimoto calculation new: " + timeForNew);
        assertTrue("Tanimoto calculation of new bit vector cells takes much " +
                "longer than calculation on old (java) bit vectors",
                timeForNew < 1.2 * timeForOld);
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
        long nominator = DenseBitVectorCellFactory.and(b1, b2).cardinality();
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

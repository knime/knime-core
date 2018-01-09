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
package org.knime.core.data.vector.bytevector;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.hamcrest.core.Is;
import org.knime.core.data.vector.bitvector.DenseBitVector;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests the {@link DenseBitVector} class.
 *
 * @author ohl, University of Konstanz
 */
public class DenseByteVectorTest extends TestCase {

    /**
     * Tests the constructor that takes a byte array for initializing the
     * counts.
     */
    public void testByteConstructor() {
        DenseByteVector bv = new DenseByteVector(new byte[0]);
        assertTrue(bv.length() == 0);
        try {
            bv.get(0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception expected
        }

        byte[] init = new byte[20];
        for (int b = 0; b < 20; b++) {
            init[b] = (byte)(b * 20);
        }
        bv = new DenseByteVector(init);
        assertTrue(bv.length() == 20);
        assertTrue(bv.get(0) == 0);
        assertEquals(bv.toString(),
                "{0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, "
                        + "4, 24, 44, 64, 84, 104, 124}");

        DenseByteVector bv2 = new DenseByteVector(bv.getAllCountsAsBytes());

        assertEquals(bv, bv2);
        assertEquals(bv2, bv);

        byte[] counts = bv2.getAllCountsAsBytes();
        assertTrue(init.length == counts.length);
        for (int i = 0; i < counts.length; i++) {
            assertTrue(init[i] == counts[i]);
        }

    }

    /**
     * Makes sure vectors of length zero work.
     */
    public void testLengthZero() {
        DenseByteVector bvZ1 = new DenseByteVector(0);
        DenseByteVector bvZ2 = new DenseByteVector(0);
        DenseByteVector bv = new DenseByteVector(10);

        bv.set(1, 3);
        bv.set(7, 8);
        bv.set(9, 66);

        DenseByteVector result;

        // equals with zero length
        assertTrue(bvZ1.equals(bvZ2));
        assertTrue(bvZ1.equals(bvZ1));
        assertFalse(bv.equals(bvZ1));
        assertFalse(bvZ2.equals(bv));

        // ADD with zero length
        result = bvZ1.add(bvZ2, true);
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 0);
        assertTrue(result.nextCountIndex(0) == -1);
        assertTrue(result.nextZeroIndex(0) == -1);

        result = bvZ1.add(bvZ2, false);
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 0);
        assertTrue(result.nextCountIndex(0) == -1);
        assertTrue(result.nextZeroIndex(0) == -1);

        result = bv.add(bvZ1, true);
        assertTrue(!result.isEmpty());
        assertTrue(result.length() == bv.length());
        assertTrue(result.sumOfAllCounts() == bv.sumOfAllCounts());

        result = bvZ1.add(bv, true);
        assertTrue(!result.isEmpty());
        assertTrue(result.length() == bv.length());
        assertTrue(result.sumOfAllCounts() == bv.sumOfAllCounts());

        // concatenate with zero length
        result = bvZ1.concatenate(bvZ2);
        assertTrue(result.length() == 0);
        assertTrue(result.isEmpty());

        result = bv.concatenate(bvZ1);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        result = bvZ1.concatenate(bv);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        // cardinality
        assertTrue(bvZ1.cardinality() == 0);

        // sumOfAllCounts
        assertTrue(bvZ1.sumOfAllCounts() == 0);

        // toString on zero length shouldn't fail.
        bvZ1.toString();
        assertTrue(bvZ1.getAllCounts().length == 0);
        assertTrue(bvZ1.getAllCountsAsBytes().length == 0);
        bvZ1.hashCode();
    }

    /**
     * tests set, clear and get functionality.
     */
    public void testSetFillClearGet() {
        DenseByteVector bv0 = new DenseByteVector(0);
        assertTrue(bv0.getAllCountsAsBytes().length == 0);
        DenseByteVector bv129 = new DenseByteVector(129);
        assertTrue(bv129.getAllCountsAsBytes().length == 129);

        bv129.set(3, 17);
        bv129.set(5, 128);
        bv129.set(7, 255);
        bv129.set(100, 100);
        bv129.set(128, 128);

        assertTrue(bv129.cardinality() == 5);
        assertTrue(bv129.get(3) == 17);
        assertTrue(bv129.get(5) == 128);
        assertTrue(bv129.get(7) == 255);
        assertTrue(bv129.get(100) == 100);
        assertTrue(bv129.get(128) == 128);
        assertTrue(bv129.sumOfAllCounts() == 628);

        bv129.clear(3);
        bv129.clear(5);
        bv129.clear(7);
        bv129.clear(100);
        bv129.clear(128);
        assertTrue(bv129.cardinality() == 0);
        assertTrue(bv129.get(3) == 0);
        assertTrue(bv129.get(5) == 0);
        assertTrue(bv129.get(7) == 0);
        assertTrue(bv129.get(100) == 0);
        assertTrue(bv129.get(128) == 0);
        assertTrue(bv129.isEmpty());
        bv129.clear(4);
        assertTrue(bv129.isEmpty());
        bv129.clear(128);
        assertTrue(bv129.isEmpty());
        bv129.set(127, 0);
        assertTrue(bv129.isEmpty());

        bv129.fill(0, 129, 255);
        assertTrue(bv129.cardinality() == 129);
        assertTrue(bv129.sumOfAllCounts() == 129 * 255);

        bv129.fill(100, 110, 10);
        assertTrue(bv129.cardinality() == 129);
        assertTrue(bv129.sumOfAllCounts() == 119 * 255 + 100);

        bv129.fill(0, 10, 0);
        assertTrue(bv129.cardinality() == 119);
        assertTrue(bv129.sumOfAllCounts() == 109 * 255 + 100);

        try {
            bv129.set(-1, 0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }
        try {
            bv129.set(129, 0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }
        try {
            bv129.set(129, -1);
            fail();
        } catch (IllegalArgumentException iae) {
            // exception is required
        }
        try {
            bv129.set(129, 256);
            fail();
        } catch (IllegalArgumentException iae) {
            // exception is required
        }
        try {
            bv0.set(0, 0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }
        try {
            bv0.set(1, 0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }

    }

    /**
     * Checks set and clear of a range
     */
    public void testSetClearGetRange() {
        DenseBitVector bv10 = new DenseBitVector(10);
        DenseBitVector bv128 = new DenseBitVector(128);
        DenseBitVector bv135 = new DenseBitVector(135);
        bv10.set(0, 9);
        assertTrue(bv10.getAllBits()[0] == 0x1FFL);
        bv10.set(3, 6, false);
        assertTrue(bv10.getAllBits()[0] == 0x1C7L);
        bv10.clear(0, 10);
        assertTrue(bv10.getAllBits()[0] == 0x0L);
        bv10.isEmpty();

        bv128.set(37, 64);
        assertTrue(bv128.getAllBits()[0] == 0xFFFFFFE000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x0L);
        bv128.set(64, 100);
        assertTrue(bv128.getAllBits()[0] == 0xFFFFFFE000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x0000000FFFFFFFFFL);
        bv128.clear(0, 120);
        assertTrue(bv128.isEmpty());
        assertTrue(bv128.getAllBits()[0] == 0x0L);
        assertTrue(bv128.getAllBits()[1] == 0x0L);

        bv128.set(64, 128);
        assertTrue(bv128.getAllBits()[0] == 0x0L);
        assertTrue(bv128.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);

        bv128.set(63, 65);
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);

        bv128.set(0);
        bv128.clear(0, 128);
        assertTrue(bv128.isEmpty());
        assertTrue(bv128.getAllBits()[0] == 0x0L);
        assertTrue(bv128.getAllBits()[1] == 0x0L);

        bv128.set(63, 65);
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x1L);

        bv128.set(18, 18); // must not change anything!
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x1L);
        bv128.set(64, 64, false);
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x1L);
        bv128.clear(64, 64);
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x1L);
        bv128.clear(18, 18);
        assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv128.getAllBits()[1] == 0x1L);

        bv135.set(64, 128);
        assertTrue(bv135.getAllBits()[0] == 0x0L);
        assertTrue(bv135.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);
        assertTrue(bv135.getAllBits()[2] == 0x0L);
        bv135.set(64, 128, false);
        assertTrue(bv135.isEmpty());
        assertTrue(bv135.getAllBits()[0] == 0x0L);
        assertTrue(bv135.getAllBits()[1] == 0x0L);
        assertTrue(bv135.getAllBits()[2] == 0x0L);

        bv135.set(0, 135, false);
        assertTrue(bv135.isEmpty());
        bv135.set(60, 125);
        assertTrue(bv135.cardinality() == 65);
        bv135.set(55, 130);
        assertTrue(bv135.cardinality() == 75);
        bv135.set(30, 40);
        assertTrue(bv135.cardinality() == 85);
        bv135.set(25, 35);
        assertTrue(bv135.cardinality() == 90);
        bv135.set(127, 132);
        assertTrue(bv135.cardinality() == 92);
        bv135.set(133, 135);
        assertTrue(bv135.cardinality() == 94);

        try {
            bv135.set(-1, 18);
            fail();
        } catch (ArrayIndexOutOfBoundsException ohoh) {
            // required exception
        }
        try {
            bv135.set(0, 136);
            fail();
        } catch (ArrayIndexOutOfBoundsException ohoh) {
            // required exception
        }
        try {
            bv135.set(0, -1);
            fail();
        } catch (IllegalArgumentException ohoh) {
            // required exception
        }
        try {
            bv135.set(18, 17);
            fail();
        } catch (IllegalArgumentException ohoh) {
            // required exception
        }
        try {
            bv135.set(136, 18);
            fail();
        } catch (IllegalArgumentException ohoh) {
            // required exception
        }

        try {
            bv135.set(-128, 18);
            fail();
        } catch (ArrayIndexOutOfBoundsException ohoh) {
            // required exception
        }

    }

    /**
     * Tests the nextSetBit and nextClearBit methods.
     */
    public void nextSetClearBit() {

        DenseBitVector bv10 = new DenseBitVector(10);

        bv10.set(3);
        bv10.set(8);

        // must not blow
        assertTrue(bv10.nextSetBit(10) == -1);
        assertTrue(bv10.nextSetBit(598) == -1);
        assertTrue(bv10.nextClearBit(10) == -1);
        assertTrue(bv10.nextClearBit(598) == -1);

        assertTrue(bv10.nextSetBit(0) == 3);
        assertTrue(bv10.nextSetBit(3) == 3);
        assertTrue(bv10.nextSetBit(4) == 8);
        assertTrue(bv10.nextSetBit(8) == 8);
        assertTrue(bv10.nextSetBit(9) == -1);

        bv10.set(4);
        bv10.set(5);
        assertTrue(bv10.nextClearBit(0) == 0);
        assertTrue(bv10.nextClearBit(2) == 2);
        assertTrue(bv10.nextClearBit(3) == 6);
        assertTrue(bv10.nextClearBit(6) == 6);
        assertTrue(bv10.nextClearBit(7) == 7);
        assertTrue(bv10.nextClearBit(8) == 9);

        DenseBitVector bv64 = new DenseBitVector(64);
        assertTrue(bv10.nextClearBit(0) == 0);
        assertTrue(bv10.nextClearBit(62) == 62);
        assertTrue(bv10.nextClearBit(63) == 63);
        assertTrue(bv10.nextClearBit(64) == -1);
        assertTrue(bv10.nextSetBit(8) == -1);
        assertTrue(bv10.nextSetBit(62) == -1);
        assertTrue(bv10.nextSetBit(63) == -1);
        assertTrue(bv10.nextSetBit(64) == -1);

        bv64.set(0, 64);
        assertTrue(bv10.nextClearBit(0) == -1);
        assertTrue(bv10.nextClearBit(62) == -1);
        assertTrue(bv10.nextClearBit(63) == -1);
        assertTrue(bv10.nextSetBit(8) == 8);
        assertTrue(bv10.nextSetBit(62) == 62);
        assertTrue(bv10.nextSetBit(63) == 63);
        assertTrue(bv10.nextSetBit(64) == -1);

        DenseBitVector bv120 = new DenseBitVector(120);
        assertTrue(bv120.nextClearBit(0) == 0);
        assertTrue(bv120.nextClearBit(62) == 62);
        assertTrue(bv120.nextClearBit(63) == 63);
        assertTrue(bv120.nextClearBit(64) == 64);
        assertTrue(bv120.nextClearBit(65) == 65);
        assertTrue(bv120.nextClearBit(118) == 118);
        assertTrue(bv120.nextClearBit(119) == 119);
        assertTrue(bv120.nextClearBit(120) == -1);
        assertTrue(bv120.nextSetBit(8) == -1);
        assertTrue(bv120.nextSetBit(62) == -1);
        assertTrue(bv120.nextSetBit(63) == -1);
        assertTrue(bv120.nextSetBit(64) == -1);
        assertTrue(bv120.nextSetBit(118) == -1);
        assertTrue(bv120.nextSetBit(119) == -1);
        assertTrue(bv120.nextSetBit(120) == -1);

        bv120.set(0, 120);
        assertTrue(bv120.nextSetBit(0) == 0);
        assertTrue(bv120.nextSetBit(62) == 62);
        assertTrue(bv120.nextSetBit(63) == 63);
        assertTrue(bv120.nextSetBit(64) == 64);
        assertTrue(bv120.nextSetBit(65) == 65);
        assertTrue(bv120.nextSetBit(118) == 118);
        assertTrue(bv120.nextSetBit(119) == 119);
        assertTrue(bv120.nextSetBit(120) == -1);
        assertTrue(bv120.nextClearBit(8) == -1);
        assertTrue(bv120.nextClearBit(62) == -1);
        assertTrue(bv120.nextClearBit(63) == -1);
        assertTrue(bv120.nextClearBit(64) == -1);
        assertTrue(bv120.nextClearBit(118) == -1);
        assertTrue(bv120.nextClearBit(119) == -1);
        assertTrue(bv120.nextClearBit(120) == -1);

    }

    /**
     * Tests the AND operation of two vectors and the cardinality method.
     */
    public void testAnd_Cardinality() {

        int l = 64;
        while (true) {
            DenseBitVector a = new DenseBitVector(l);
            DenseBitVector b = new DenseBitVector(l);
            DenseBitVector result;

            result = a.and(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.and(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            a.set(0, l);
            result = a.and(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.and(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            b.set(0, l);
            result = a.and(b);
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.and(a);
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            l += 65;
            if (l > 130) {
                // do this for length 64 and 129.
                break;
            }

        }

        // different length
        DenseBitVector a = new DenseBitVector(8);
        DenseBitVector b = new DenseBitVector(16);
        DenseBitVector result = a.and(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        a.set(0, 8);
        b.set(0, 16);
        result = a.and(b);
        assertTrue(result.getAllBits()[0] == 0xFF);
        assertTrue(result.length() == 16);

    }

    /**
     * Tests the OR operation on bit vectors.
     */
    public void testOR() {
        int l = 64;
        while (true) {
            DenseBitVector a = new DenseBitVector(l);
            DenseBitVector b = new DenseBitVector(l);
            DenseBitVector result;

            result = a.or(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.or(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            a.set(0, l);
            result = a.or(b);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.or(a);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            b.set(0, l);
            result = a.or(b);
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.or(a);
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            l += 65;
            if (l > 130) {
                // do this for length 64 and 129.
                break;
            }

        }

        // different length
        DenseBitVector a = new DenseBitVector(8);
        DenseBitVector b = new DenseBitVector(16);
        DenseBitVector result = a.or(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        a.set(0, 8);
        b.set(0, 16);
        result = a.or(b);
        assertTrue(result.getAllBits()[0] == 0xFFFF);
        assertTrue(result.length() == 16);

    }

    /**
     * Tests bit inversion.
     */
    public void testInvert() {

        DenseBitVector bv = new DenseBitVector(0);
        DenseBitVector result = bv.invert();
        assertTrue(result.isEmpty());
        assertTrue(result.getAllBits().length == 0);

        bv = new DenseBitVector(63);
        result = bv.invert();
        assertTrue(!result.isEmpty());
        assertTrue(result.length() == 63);
        assertTrue(result.getAllBits()[0] == 0x7FFFFFFFFFFFFFFFL);

        bv = new DenseBitVector(new long[]{-1L}, 63);
        result = bv.invert();
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 63);
        assertTrue(result.getAllBits()[0] == 0x0000000000000000L);

        bv = new DenseBitVector(new long[]{-1L}, 64);
        result = bv.invert();
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 64);
        assertTrue(result.getAllBits()[0] == 0x0000000000000000L);

        bv =
                new DenseBitVector(new long[]{0x00000000FFFFFFFFL,
                        0xFFFFFFFF00000000L}, 30);
        result = bv.invert();
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 30);
        assertTrue(result.getAllBits()[0] == 0x0000000000000000L);

    }

    /**
     * Tests the XOR.
     */
    public void testXOR() {
        int l = 64;
        while (true) {
            DenseBitVector a = new DenseBitVector(l);
            DenseBitVector b = new DenseBitVector(l);
            DenseBitVector result;

            result = a.xor(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.xor(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            a.set(0, l);
            result = a.xor(b);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.xor(a);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            b.set(0, l);
            result = a.xor(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.xor(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            l += 65;
            if (l > 130) {
                // do this for length 64 and 129.
                break;
            }

        }

        // different length
        DenseBitVector a = new DenseBitVector(8);
        DenseBitVector b = new DenseBitVector(16);
        DenseBitVector result = a.xor(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        a.set(0, 8);
        b.set(0, 16);
        result = a.xor(b);
        assertTrue(result.getAllBits()[0] == 0xFF00);
        assertTrue(result.length() == 16);

    }

    /**
     * Tests concatenate. What else?!?
     */
    public void testConcatenate() {
        DenseBitVector bv0 = new DenseBitVector(0);
        DenseBitVector bv40 = new DenseBitVector(new long[]{0x5500AA00FFL}, 40);
        DenseBitVector bv24 = new DenseBitVector(new long[]{0xFF00AA}, 24);
        DenseBitVector bv64 =
                new DenseBitVector(new long[]{0xFF0000FF00FF00FFL}, 64);

        DenseBitVector result = bv0.concatenate(bv64);
        assertTrue(result.length() == 64);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits()[0] == 0xFF0000FF00FF00FFL);

        result = bv64.concatenate(bv0);
        assertTrue(result.length() == 64);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits()[0] == 0xFF0000FF00FF00FFL);

        result = bv0.concatenate(bv64).concatenate(bv40).concatenate(bv24);
        assertTrue(result.length() == 128);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits().length == 2);
        assertTrue(result.getAllBits()[0] == 0xFF0000FF00FF00FFL);
        assertTrue(result.getAllBits()[1] == 0xFF00AA5500AA00FFL);

        result = bv0.concatenate(bv40).concatenate(bv64).concatenate(bv24);
        assertTrue(result.length() == 128);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits().length == 2);
        assertTrue(result.getAllBits()[0] == 0xFF00FF5500AA00FFL);
        assertTrue(result.getAllBits()[1] == 0xFF00AAFF0000FF00L);

        result = bv0.concatenate(bv40).concatenate(bv64).concatenate(bv40);
        assertTrue(result.length() == 144);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits().length == 3);
        assertTrue(result.getAllBits()[0] == 0xFF00FF5500AA00FFL);
        assertTrue(result.getAllBits()[1] == 0xAA00FFFF0000FF00L);
        assertTrue(result.getAllBits()[2] == 0x5500L);

        result = bv0.concatenate(bv40).concatenate(bv40).concatenate(bv40);
        assertTrue(result.length() == 120);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits().length == 2);
        assertTrue(result.getAllBits()[0] == 0xAA00FF5500AA00FFL);
        assertTrue(result.getAllBits()[1] == 0x005500AA00FF5500L);

        result = bv64.concatenate(bv64);
        assertTrue(result.length() == 128);
        assertTrue(!result.isEmpty());
        assertTrue(result.getAllBits().length == 2);
        assertTrue(result.getAllBits()[0] == 0xFF0000FF00FF00FFL);
        assertTrue(result.getAllBits()[1] == 0xFF0000FF00FF00FFL);

    }

    /**
     * Guess what it does.
     */
    public void testIntersects() {
        DenseBitVector bv50 = new DenseBitVector(50);
        DenseBitVector bv130 = new DenseBitVector(130);
        DenseBitVector bv256 = new DenseBitVector(256);

        assertTrue(!bv50.intersects(bv130));
        assertTrue(!bv130.intersects(bv50));
        assertTrue(!bv50.intersects(bv256));
        assertTrue(!bv256.intersects(bv50));
        assertTrue(!bv130.intersects(bv256));
        assertTrue(!bv256.intersects(bv130));
        assertTrue(!bv50.intersects(bv50));
        assertTrue(!bv130.intersects(bv130));
        assertTrue(!bv256.intersects(bv256));

        bv50.set(30);
        bv130.set(29);
        bv130.set(31);
        bv130.set(100);
        assertTrue(!bv50.intersects(bv130));
        assertTrue(!bv130.intersects(bv50));
        assertTrue(bv130.intersects(bv130));

        bv130.set(129);
        bv256.set(129);
        assertTrue(bv130.intersects(bv256));
        assertTrue(bv256.intersects(bv130));
        assertTrue(bv256.intersects(bv256));

        bv130.clear(129);
        assertTrue(!bv130.intersects(bv256));
        assertTrue(!bv256.intersects(bv130));
    }

    /**
     * Checks whether the {@link DenseByteVectorCellFactory} works as expected.
     */
    public void testFactory() {
        DenseByteVectorCellFactory fac = new DenseByteVectorCellFactory(5);
        fac.setValue(0, 0);
        fac.setValue(1, 1);
        fac.setValue(2, 160);
        fac.setValue(3, 254);
        fac.setValue(4, 255);

        DenseByteVectorCell cell = fac.createDataCell();
        assertThat("Unexpected value at index 0", cell.get(0), is(0));
        assertThat("Unexpected value at index 1", cell.get(1), is(1));
        assertThat("Unexpected value at index 2", cell.get(2), is(160));
        assertThat("Unexpected value at index 3", cell.get(3), is(254));
        assertThat("Unexpected value at index 4", cell.get(4), is(255));

        try {
            fac.setValue(0, -1);
            Assert.fail("Expected IllegalArgumentException for negative int value");
        } catch (IllegalArgumentException ex) {
            // OK
        }


        try {
            fac.setValue(0, 256);
            Assert.fail("Expected IllegalArgumentException for to large int value");
        } catch (IllegalArgumentException ex) {
            // OK
        }
    }


    /**
     * Checks that the hashCode of dense and sparse byte vectors are identical.
     *
     * @throws Exception if an error occurs
     */
    public void testHashCode() throws Exception {
        checkHashCode(new byte[0]);
        checkHashCode(new byte[] { 31 });
        checkHashCode(new byte[] { 31, 5, 7, 8, 9 });
        checkHashCode(new byte[] { 0, 0, 7, 0, 0 });
        checkHashCode(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
    }

    private void checkHashCode(final byte[] counts) {
        long[] indices = new long[counts.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        SparseByteVector sparse = new SparseByteVector(counts.length, indices, counts);
        DenseByteVector dense = new DenseByteVector(counts);

        assertThat("Hashcode for " + Arrays.toString(counts) + " not equal", sparse.hashCode(), Is.is(dense.hashCode()));
    }
}

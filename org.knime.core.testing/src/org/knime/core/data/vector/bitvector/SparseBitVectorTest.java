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
package org.knime.core.data.vector.bitvector;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SparseBitVectorTest extends TestCase {

    /**
     * Tests the constructor that takes a long array for initializing the bits.
     */
    public void testLongConstructor() {
        SparseBitVector bv = new SparseBitVector(0, new long[0]);
        assertTrue(bv.isEmpty());
        assertTrue(bv.length() == 0);

        bv = new SparseBitVector(87, new long[0]);
        assertTrue(bv.isEmpty());
        assertTrue(bv.length() == 87);

        bv = new SparseBitVector(64, new long[]{0, 7, 8, 63});
        assertTrue(!bv.isEmpty());
        assertTrue(bv.length() == 64);
        assertTrue(bv.getAllOneIndices().length == 4);
        assertTrue(bv.getAllOneIndices()[0] == 0);
        assertTrue(bv.getAllOneIndices()[1] == 7);
        assertTrue(bv.getAllOneIndices()[2] == 8);
        assertTrue(bv.getAllOneIndices()[3] == 63);

        try {
            bv = new SparseBitVector(128, new long[]{12, -3});
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }
        try {
            bv = new SparseBitVector(16, new long[17]);
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }
        try {
            bv = new SparseBitVector(16, new long[]{5, 6, 4});
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }
        try {
            bv = new SparseBitVector(1, new long[]{1});
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }

    }

    /**
     * Makes sure vectors of length zero work.
     */
    public void testLengthZero() {
        SparseBitVector bvZ1 = new SparseBitVector(0);
        SparseBitVector bvZ2 = new SparseBitVector(0);
        SparseBitVector bv = new SparseBitVector(10);

        bv.set(1);
        bv.set(7);
        bv.set(9);
        bv.toString();
        SparseBitVector result;

        // equals with zero length
        assertTrue(bvZ1.equals(bvZ2));
        assertTrue(bvZ1.equals(bvZ1));
        assertFalse(bv.equals(bvZ1));
        assertFalse(bvZ2.equals(bv));

        // AND with zero length
        result = bvZ1.and(bvZ2);
        assertTrue(result.isEmpty());
        assertTrue(result.length() == 0);
        assertTrue(result.nextClearBit(0) == -1);
        assertTrue(result.nextSetBit(0) == -1);

        result = bv.and(bvZ1);
        assertTrue(result.length() == bv.length());
        assertTrue(result.isEmpty());

        result = bvZ1.and(bv);
        assertTrue(result.length() == bv.length());
        assertTrue(result.isEmpty());

        // OR with zero length
        result = bvZ1.or(bvZ2);
        assertTrue(result.length() == 0);
        assertTrue(result.isEmpty());
        assertTrue(result.nextClearBit(0) == -1);
        assertTrue(result.nextSetBit(0) == -1);

        result = bv.or(bvZ1);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        result = bvZ1.or(bv);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        // XOR with zero length
        result = bvZ1.xor(bvZ2);
        assertTrue(result.length() == 0);
        assertTrue(result.isEmpty());
        assertTrue(result.nextClearBit(0) == -1);
        assertTrue(result.nextSetBit(0) == -1);

        result = bv.xor(bvZ1);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        result = bvZ1.xor(bv);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        // concatenate with zero length
        result = bvZ1.concatenate(bvZ2);
        assertTrue(result.length() == 0);
        assertTrue(result.isEmpty());
        assertTrue(result.nextClearBit(0) == -1);
        assertTrue(result.nextSetBit(0) == -1);

        result = bv.concatenate(bvZ1);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        result = bvZ1.concatenate(bv);
        assertTrue(result.length() == bv.length());
        assertTrue(result.equals(bv));

        // intersect with zero length
        assertFalse(bvZ2.intersects(bv));
        assertFalse(bv.intersects(bvZ1));
        assertFalse(bvZ1.intersects(bvZ2));

        // cardinality
        assertTrue(bvZ1.cardinality() == 0);

        // toString on zero length shouldn't fail.
        bvZ1.toString();
        assertTrue(bvZ1.getAllOneIndices().length == 0);
        bvZ1.hashCode();
    }

    /**
     * tests set, clear and get functionality.
     */
    public void testSetClearGet() {
        SparseBitVector bv0 = new SparseBitVector(0);
        SparseBitVector bv10 = new SparseBitVector(10, 2);

        bv10.set(3);
        bv10.set(1);
        bv10.set(9);
        assertEquals(3, bv10.cardinality());
        assertTrue(bv10.get(1));
        assertTrue(bv10.get(3));
        assertTrue(bv10.get(9));

        bv10.set(3);
        bv10.set(1);
        bv10.set(9);
        assertEquals(3, bv10.cardinality());
        assertTrue(bv10.get(1));
        assertTrue(bv10.get(3));
        assertTrue(bv10.get(9));

        bv10.clear(0);
        assertTrue(bv10.get(1));
        assertTrue(bv10.get(3));
        assertTrue(bv10.get(9));

        bv10.clear(3);
        assertEquals(2, bv10.cardinality());
        assertTrue(bv10.get(1));
        assertFalse(bv10.get(3));
        assertTrue(bv10.get(9));

        bv10.clear(9);
        assertEquals(1, bv10.cardinality());
        assertTrue(bv10.get(1));
        assertFalse(bv10.get(3));
        assertFalse(bv10.get(9));

        bv10.clear(9);
        assertEquals(1, bv10.cardinality());
        assertTrue(bv10.get(1));
        assertFalse(bv10.get(3));
        assertFalse(bv10.get(9));

        bv10.clear(1);
        assertEquals(0, bv10.cardinality());
        assertTrue(bv10.isEmpty());
        assertFalse(bv10.get(1));
        assertFalse(bv10.get(3));
        assertFalse(bv10.get(9));


        bv10.set(3);
        bv10.set(1);
        bv10.set(9);
        bv10.set(0);
        assertEquals(4, bv10.cardinality());
        assertEquals("{length=10, set bits=0, 1, 3, 9}", bv10.toString());
        bv10.set(2);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 9}", bv10.toString());
        bv10.set(4);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 9}", bv10.toString());
        bv10.set(5);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 5, 9}", bv10.toString());
        bv10.set(6);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 5, 6, 9}", bv10.toString());
        bv10.set(7);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 5, 6, 7, 9}", bv10.toString());
        bv10.set(8);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 5, 6, 7, 8, 9}", bv10.toString());

        bv10.clear(9);
        assertEquals("{length=10, set bits=0, 1, 2, 3, 4, 5, 6, 7, 8}", bv10.toString());
        bv10.clear(0);
        assertEquals("{length=10, set bits=1, 2, 3, 4, 5, 6, 7, 8}", bv10.toString());
        bv10.clear(5);
        assertEquals("{length=10, set bits=1, 2, 3, 4, 6, 7, 8}", bv10.toString());
        bv10.clear(4);
        assertEquals("{length=10, set bits=1, 2, 3, 6, 7, 8}", bv10.toString());
        bv10.clear(8);
        assertEquals("{length=10, set bits=1, 2, 3, 6, 7}", bv10.toString());
        bv10.set(9);
        assertEquals("{length=10, set bits=1, 2, 3, 6, 7, 9}", bv10.toString());
        assertEquals(6, bv10.cardinality());
        bv10.set(5);
        assertEquals("{length=10, set bits=1, 2, 3, 5, 6, 7, 9}", bv10.toString());
        assertEquals(7, bv10.cardinality());
        bv10.set(4);
        assertEquals("{length=10, set bits=1, 2, 3, 4, 5, 6, 7, 9}", bv10.toString());
        assertEquals(8, bv10.cardinality());

        try {
            bv10.set(-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }
        try {
            bv10.set(10);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }
        try {
            bv10.set(67);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }

        try {
            bv0.set(0);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }

        try {
            bv0.set(1);
            fail();
        } catch (ArrayIndexOutOfBoundsException aioob) {
            // exception is required
        }

    }

    /**
     * Checks set and clear of a range
     */
    public void testSetClearGetRange() {
        // SparseBitVector bv10 = new SparseBitVector(10);
        // SparseBitVector bv128 = new SparseBitVector(128);
        // SparseBitVector bv135 = new SparseBitVector(135);
        // bv10.set(0, 9);
        // assertTrue(bv10.getAllBits()[0] == 0x1FFL);
        // bv10.set(3, 6, false);
        // assertTrue(bv10.getAllBits()[0] == 0x1C7L);
        // bv10.clear(0, 10);
        // assertTrue(bv10.getAllBits()[0] == 0x0L);
        // bv10.isEmpty();
        //
        // bv128.set(37, 64);
        // assertTrue(bv128.getAllBits()[0] == 0xFFFFFFE000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x0L);
        // bv128.set(64, 100);
        // assertTrue(bv128.getAllBits()[0] == 0xFFFFFFE000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x0000000FFFFFFFFFL);
        // bv128.clear(0, 120);
        // assertTrue(bv128.isEmpty());
        // assertTrue(bv128.getAllBits()[0] == 0x0L);
        // assertTrue(bv128.getAllBits()[1] == 0x0L);
        //
        // bv128.set(64, 128);
        // assertTrue(bv128.getAllBits()[0] == 0x0L);
        // assertTrue(bv128.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);
        //
        // bv128.set(63, 65);
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);
        //
        // bv128.set(0);
        // bv128.clear(0, 128);
        // assertTrue(bv128.isEmpty());
        // assertTrue(bv128.getAllBits()[0] == 0x0L);
        // assertTrue(bv128.getAllBits()[1] == 0x0L);
        //
        // bv128.set(63, 65);
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x1L);
        //
        // bv128.set(18, 18); // must not change anything!
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x1L);
        // bv128.set(64, 64, false);
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x1L);
        // bv128.clear(64, 64);
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x1L);
        // bv128.clear(18, 18);
        // assertTrue(bv128.getAllBits()[0] == 0x8000000000000000L);
        // assertTrue(bv128.getAllBits()[1] == 0x1L);
        //
        // bv135.set(64, 128);
        // assertTrue(bv135.getAllBits()[0] == 0x0L);
        // assertTrue(bv135.getAllBits()[1] == 0xFFFFFFFFFFFFFFFFL);
        // assertTrue(bv135.getAllBits()[2] == 0x0L);
        // bv135.set(64, 128, false);
        // assertTrue(bv135.isEmpty());
        // assertTrue(bv135.getAllBits()[0] == 0x0L);
        // assertTrue(bv135.getAllBits()[1] == 0x0L);
        // assertTrue(bv135.getAllBits()[2] == 0x0L);
        //
        // bv135.set(0, 135, false);
        // assertTrue(bv135.isEmpty());
        // bv135.set(60, 125);
        // assertTrue(bv135.cardinality() == 65);
        // bv135.set(55, 130);
        // assertTrue(bv135.cardinality() == 75);
        // bv135.set(30, 40);
        // assertTrue(bv135.cardinality() == 85);
        // bv135.set(25, 35);
        // assertTrue(bv135.cardinality() == 90);
        // bv135.set(127, 132);
        // assertTrue(bv135.cardinality() == 92);
        // bv135.set(133, 135);
        // assertTrue(bv135.cardinality() == 94);
        //
        // try {
        // bv135.set(-1, 18);
        // fail();
        // } catch (ArrayIndexOutOfBoundsException ohoh) {
        // // required exception
        // }
        // try {
        // bv135.set(0, 136);
        // fail();
        // } catch (ArrayIndexOutOfBoundsException ohoh) {
        // // required exception
        // }
        // try {
        // bv135.set(0, -1);
        // fail();
        // } catch (IllegalArgumentException ohoh) {
        // // required exception
        // }
        // try {
        // bv135.set(18, 17);
        // fail();
        // } catch (IllegalArgumentException ohoh) {
        // // required exception
        // }
        // try {
        // bv135.set(136, 18);
        // fail();
        // } catch (IllegalArgumentException ohoh) {
        // // required exception
        // }
        //
        // try {
        // bv135.set(-128, 18);
        // fail();
        // } catch (ArrayIndexOutOfBoundsException ohoh) {
        // // required exception
        // }
        //
    }

    /**
     * Tests the nextSetBit and nextClearBit methods.
     */
    public void nextSetClearBit() {

        SparseBitVector bv10 = new SparseBitVector(10);

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

        SparseBitVector bv64 = new SparseBitVector(64);
        assertTrue(bv64.nextClearBit(0) == 0);
        assertTrue(bv64.nextClearBit(62) == 62);
        assertTrue(bv64.nextClearBit(63) == 63);
        assertTrue(bv64.nextClearBit(64) == -1);
        assertTrue(bv64.nextSetBit(8) == -1);
        assertTrue(bv64.nextSetBit(62) == -1);
        assertTrue(bv64.nextSetBit(63) == -1);
        assertTrue(bv64.nextSetBit(64) == -1);

        long[] init = new long[64];
        for (int i = 0; i < 64; i++) {
            init[i] = i;
        }
        bv64 = new SparseBitVector(64, init);
        assertTrue(bv64.nextClearBit(0) == -1);
        assertTrue(bv64.nextClearBit(62) == -1);
        assertTrue(bv64.nextClearBit(63) == -1);
        assertTrue(bv64.nextSetBit(8) == 8);
        assertTrue(bv64.nextSetBit(62) == 62);
        assertTrue(bv64.nextSetBit(63) == 63);
        assertTrue(bv64.nextSetBit(64) == -1);

        SparseBitVector bv120 = new SparseBitVector(120);
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

        long[] init120 = new long[120];
        for (int i = 0; i < 120; i++) {
            init120[i] = i;
        }
        bv120 = new SparseBitVector(120, init120);
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
            SparseBitVector a = new SparseBitVector(l);
            SparseBitVector b = new SparseBitVector(l);
            SparseBitVector result;

            result = a.and(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.and(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                a.set(i);
            }
            result = a.and(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.and(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                b.set(i);
            }
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
        SparseBitVector a = new SparseBitVector(8);
        SparseBitVector b = new SparseBitVector(16);
        SparseBitVector result = a.and(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        for (int i = 0; i < 8; i++) {
            a.set(i);
        }
        for (int i = 0; i < 16; i++) {
            b.set(i);
        }
        result = a.and(b);
        assertTrue(result.cardinality() == 8);
        assertEquals("{length=16, set bits=0, 1, 2, 3, 4, 5, 6, 7}", result.toString());
        assertTrue(!result.isEmpty());
        assertTrue(result.length() == 16);

    }

    /**
     * Tests the OR operation on bit vectors.
     */
    public void testOR() {
        int l = 64;
        while (true) {
            SparseBitVector a = new SparseBitVector(l);
            SparseBitVector b = new SparseBitVector(l);
            SparseBitVector result;

            result = a.or(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.or(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                a.set(i);
            }
            result = a.or(b);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.or(a);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                b.set(i);
            }
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
        SparseBitVector a = new SparseBitVector(8);
        SparseBitVector b = new SparseBitVector(16);
        SparseBitVector result = a.or(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        for (int i = 0; i < 8; i++) {
            a.set(i);
        }
        for (int i = 0; i < 16; i++) {
            b.set(i);
        }
        result = a.or(b);
        assertTrue(result.cardinality() == 16);
        assertTrue(result.length() == 16);

    }

    /**
     * Tests bit inversion.
     */
    public void testInvert() {

        // SparseBitVector bv = new SparseBitVector(0);
        // SparseBitVector result = bv.invert();
        // assertTrue(result.isEmpty());
        // assertTrue(result.getAllBits().length == 0);
        //
        // bv = new SparseBitVector(63);
        // result = bv.invert();
        // assertTrue(!result.isEmpty());
        // assertTrue(result.length() == 63);
        // assertTrue(result.getAllBits()[0] == 0x7FFFFFFFFFFFFFFFL);
        //
        // bv = new SparseBitVector(new long[]{-1L}, 63);
        // result = bv.invert();
        // assertTrue(result.isEmpty());
        // assertTrue(result.length() == 63);
        // assertTrue(result.getAllBits()[0] == 0x0000000000000000L);
        //
        // bv = new SparseBitVector(new long[]{-1L}, 64);
        // result = bv.invert();
        // assertTrue(result.isEmpty());
        // assertTrue(result.length() == 64);
        // assertTrue(result.getAllBits()[0] == 0x0000000000000000L);
        //
        // bv =
        // new SparseBitVector(new long[]{0x00000000FFFFFFFFL,
        // 0xFFFFFFFF00000000L}, 30);
        // result = bv.invert();
        // assertTrue(result.isEmpty());
        // assertTrue(result.length() == 30);
        // assertTrue(result.getAllBits()[0] == 0x0000000000000000L);

    }

    /**
     * Tests the XOR.
     */
    public void testXOR() {
        int l = 64;
        while (true) {
            SparseBitVector a = new SparseBitVector(l);
            SparseBitVector b = new SparseBitVector(l);
            SparseBitVector result;

            result = a.xor(b);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);
            result = b.xor(a);
            assertTrue(result.isEmpty());
            assertTrue(result.cardinality() == 0);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                a.set(i);
            }
            result = a.xor(b);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);
            result = b.xor(a);
            assertTrue(!result.isEmpty());
            assertTrue(result.cardinality() == l);
            assertTrue(result.length() == l);

            for (int i = 0; i < l; i++) {
                b.set(i);
            }
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
        SparseBitVector a = new SparseBitVector(8);
        SparseBitVector b = new SparseBitVector(16);
        SparseBitVector result = a.xor(b);

        assertTrue(result.length() == 16);
        assertTrue(result.isEmpty());

        for (int i = 0; i < 8; i++) {
            a.set(i);
        }
        for (int i = 0; i < 16; i++) {
            b.set(i);
        }
        result = a.xor(b);
        assertTrue(result.cardinality() == 8);
        assertTrue(result.length() == 16);

    }

    /**
     * Blah.
     */
    public void testGetAllOneIndices() {
        // make sure we get a copy
        SparseBitVector bv = new SparseBitVector(10);
        bv.set(4);
        long[] idx = bv.getAllOneIndices();
        assertTrue(idx[0] == 4);
        idx[0] = 3;
        assertTrue(bv.get(4));
        assertFalse(bv.get(3));
        SparseBitVector bv2 = new SparseBitVector(10, idx);
        assertTrue(bv2.get(3));
        assertFalse(bv2.get(4));
        assertTrue(idx[0] == 3);
        idx[0] = 8;
        assertTrue(bv2.get(3));
        assertFalse(bv2.get(4));

    }

    /**
     * Tests concatenate. What else?!?
     */
    public void testConcatenate() {
        long[] thousand =
                new long[]{0, 3, 5, 7, 9, 20, 22, 24, 26, 28, 225, 255, 555,
                        666, 777, 888, 999}; // 17 ones
        long[] hundred =
                new long[]{11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 30, 40, 50,
                        60, 70, 80, 90}; // 17 ones
        long[] ten = new long[]{2, 5, 7, 8}; // 4 ones

        SparseBitVector bv0 = new SparseBitVector(0);
        SparseBitVector e50 = new SparseBitVector(50);
        SparseBitVector bv10 = new SparseBitVector(10, ten);
        SparseBitVector bv100 = new SparseBitVector(100, hundred);
        SparseBitVector bv1000 = new SparseBitVector(1000, thousand);

        SparseBitVector result = bv0.concatenate(bv100);
        assertTrue(result.length() == 100);
        assertTrue(!result.isEmpty());
        assertTrue(result.cardinality() == 17);
        assertTrue(Arrays.equals(hundred, result.getAllOneIndices()));

        result = bv100.concatenate(bv0);
        assertTrue(result.length() == 100);
        assertTrue(!result.isEmpty());
        assertTrue(result.cardinality() == 17);
        assertTrue(Arrays.equals(hundred, result.getAllOneIndices()));

        result = bv0.concatenate(bv100).concatenate(bv10).concatenate(bv100);
        assertTrue(result.length() == 210);
        assertTrue(result.cardinality() == 38);
        assertTrue(!result.isEmpty());
        assertTrue(Arrays.equals(new long[]{11, 12, 13, 14, 15, 16, 17, 18, 19,
                20, 30, 40, 50, 60, 70, 80, 90, 102, 105, 107, 108, 121, 122,
                123, 124, 125, 126, 127, 128, 129, 130, 140, 150, 160, 170,
                180, 190, 200}, result.getAllOneIndices()));

        result = bv100.concatenate(bv0).concatenate(bv1000);
        assertTrue(result.length() == 1100);
        assertTrue(result.cardinality() == 34);
        assertTrue(!result.isEmpty());
        assertTrue(Arrays.equals(new long[]{11, 12, 13, 14, 15, 16, 17, 18, 19,
                20, 30, 40, 50, 60, 70, 80, 90, 100, 103, 105, 107, 109, 120,
                122, 124, 126, 128, 325, 355, 655, 766, 877, 988, 1099}, result
                .getAllOneIndices()));

        result = bv10.concatenate(e50);
        assertTrue(result.length() == 60);
        assertTrue(result.cardinality() == 4);
        assertTrue(!result.isEmpty());
        assertTrue(Arrays.equals(ten, result.getAllOneIndices()));

        result = e50.concatenate(bv10);
        assertTrue(result.length() == 60);
        assertTrue(result.cardinality() == 4);
        assertTrue(!result.isEmpty());
        assertTrue(Arrays.equals(new long[]{52, 55, 57, 58}, result
                .getAllOneIndices()));

    }

    /**
     * Guess what it does.
     */
    public void testIntersects() {
        SparseBitVector bv50 = new SparseBitVector(50);
        SparseBitVector bv130 = new SparseBitVector(130);
        SparseBitVector bv256 = new SparseBitVector(256);

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

    public void testToBinaryString() {
        SparseBitVector bv = new SparseBitVector("1F03");
        assertEquals(bv.toBinaryString(), "0001111100000011");
    }

    public void testToHexString() {
        SparseBitVector bv = new SparseBitVector("1F03");
        assertEquals("1F03", bv.toHexString());

        bv = new SparseBitVector("1F0329384abedf7cA7FC29FF0");
        assertEquals("1F0329384ABEDF7CA7FC29FF0", bv.toHexString());

        bv = new SparseBitVector("");
        assertEquals("", bv.toHexString());

        bv = new SparseBitVector(3);
        bv.set(0);
        bv.set(2);
        assertEquals("5", bv.toHexString());

        bv = new SparseBitVector(13L);
        bv.set(8);
        bv.set(3);
        assertEquals("0108", bv.toHexString());

        bv = new SparseBitVector("FFF");
        assertEquals("FFF", bv.toHexString());

        bv = new SparseBitVector("FFFF8888EEEEFFFF");
        assertEquals("FFFF8888EEEEFFFF", bv.toHexString());

        bv = new SparseBitVector("1FFFF8888EEEEFFFF");
        assertEquals("1FFFF8888EEEEFFFF", bv.toHexString());

        String hex = "E02008440A840140480081012400304040200432829D00102440110020265B20082C811354080040D80046A65806080" +
                "502004031204000812483400000898010";
        bv = new SparseBitVector(hex);
        assertEquals(hex, bv.toHexString());
        
        bv = new SparseBitVector(3);
        assertEquals("0", bv.toHexString());
        
        bv = new SparseBitVector("0");
        assertEquals("0", bv.toHexString());
        
        bv = new SparseBitVector(4);
        assertEquals("0", bv.toHexString());
        
        bv = new SparseBitVector(8);
        assertEquals("00", bv.toHexString());
    }
    
    public void testSubsequence(){
    	try {
    		new SparseBitVector("10f0").subSequence(-1, 0);
    		fail("Exception Expected");			
		} catch (IllegalArgumentException e) {
		}
    	
    	try {
    		new SparseBitVector("10f0").subSequence(0, -2);
    		fail("Exception Expected");			
		} catch (IllegalArgumentException e) {
		}
    	
       	try {
    		new SparseBitVector("10f0").subSequence(0, 17);
    		fail("Exception Expected");			
		} catch (IllegalArgumentException e) {
		}
       	
       	try {
    		new SparseBitVector("10f0").subSequence(5, 4);
    		fail("Exception Expected");			
		} catch (IllegalArgumentException e) {
		}
      	assertEquals("", new SparseBitVector("").subSequence(0, 0).toHexString());
      	assertEquals("", new SparseBitVector("10f0").subSequence(0, 0).toHexString());
       	assertEquals("F0", new SparseBitVector("10f0").subSequence(0, 8).toHexString());
       	assertEquals("0F0", new SparseBitVector("10f0").subSequence(0, 12).toHexString());
       	assertEquals("043", new SparseBitVector("10f0").subSequence(6, 16).toHexString());
       	assertEquals("3", new SparseBitVector("10f0").subSequence(4, 6).toHexString());
    }
    
    public void testBug5077(){
    	SparseBitVector sparseBit = new SparseBitVector("1000000000000000001");
    	SparseBitVector sparseBit2 = new SparseBitVector("101");
    	
    	SparseBitVector next = new SparseBitVector("1000000000000000101");
		
    	assertEquals(next.toHexString(), sparseBit2.or(sparseBit).toHexString());
    	assertEquals(next.toHexString(), sparseBit.or(sparseBit2).toHexString());
		
		assertEquals("0000000000000000001", sparseBit.and(sparseBit2).toHexString());
		assertEquals("0000000000000000001", sparseBit2.and(sparseBit).toHexString());
		
		assertEquals("1000000000000000100", sparseBit.xor(sparseBit2).toHexString());
		assertEquals("1000000000000000100", sparseBit2.xor(sparseBit).toHexString());
    }


    /**
     * tests toString
     */
    public void testToString() {
        int length = 500000;
        SparseBitVector bv = new SparseBitVector(length);
        bv.set(18);
        bv.set(7645);
        bv.set(700);
        bv.set(381966);
        assertEquals("{length=" + length + ", set bits=18, 700, 7645, 381966}", bv.toString());

    }
}

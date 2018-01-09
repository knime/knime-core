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

import static org.junit.Assert.assertThat;

import org.hamcrest.core.Is;

import junit.framework.TestCase;

/**
 * Tests the {@link DenseBitVector} class.
 *
 * @author ohl, University of Konstanz
 */
public class DenseBitVectorTest extends TestCase {

    /**
     * Tests the constructor that takes a long array for initializing the bits.
     */
    public void testLongConstructor() {
        DenseBitVector bv = new DenseBitVector(new long[0], 0);
        assertTrue(bv.isEmpty());
        assertTrue(bv.length() == 0);

        bv = new DenseBitVector(new long[1], 64);
        assertTrue(bv.isEmpty());
        assertTrue(bv.length() == 64);
        assertTrue(bv.getAllBits()[0] == 0x0L);

        bv = new DenseBitVector(new long[]{0x00FF00FF00FF00FFL}, 32);
        assertTrue(!bv.isEmpty());
        assertTrue(bv.length() == 32);
        assertTrue(bv.getAllBits()[0] == 0x0000000000FF00FFL);

        bv = new DenseBitVector(new long[]{0xFF00FF00FF00FF00L}, 64);
        assertTrue(!bv.isEmpty());
        assertTrue(bv.length() == 64);
        assertTrue(bv.getAllBits()[0] == 0xFF00FF00FF00FF00L);

        bv =
                new DenseBitVector(new long[]{0x00000000FFFFFFFFL,
                        0xFFFFFFFF00000000L}, 30);
        assertTrue(!bv.isEmpty());
        assertTrue(bv.length() == 30);
        assertTrue(bv.getAllBits()[0] == 0x3FFFFFFFL);

        try {
            bv = new DenseBitVector(new long[0], 128);
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }
        try {
            bv = new DenseBitVector(new long[17], 64 * 17 + 1);
            fail();
        } catch (IllegalArgumentException iae) {
            // this exception must fly
        }

    }

    public void testHexConstructor() {
        DenseBitVector bv = new DenseBitVector("1FE");
        assertTrue(bv.getAllBits()[0] == 0x1FEL);
        bv = new DenseBitVector("1FE0345efd027eabCef9d3");
        assertTrue(bv.getAllBits()[0] == 0x5efd027eabCef9d3L);
        assertTrue(bv.getAllBits()[1] == 0x1FE034L);
    }

    public void testInfocomHexNumber() {
        String hex =
                "35BF7308B4797C2D67D1F8E8FCD03B65E78B19"
                        + "D2EE113EFAD239ACFCD952F3E000";
        String bin =
                "001101011011111101110011000010001011010001111001011111000010"
                        + "11010110011111010001111110001110100011111100110100"
                        + "00001110110110010111100111100010110001100111010010"
                        + "11101110000100010011111011111010110100100011100110"
                        + "10110011111100110110010101001011110011111000000000"
                        + "0000";
        assertTrue(hex.length() * 4 == bin.length());

        DenseBitVector hexBV = new DenseBitVector(hex);
        // make sure the vector has the expected length
        assertTrue(hexBV.length() == hex.length() * 4);

        // make sure each bit matches the corresponding bit in the bin vector
        for (int i = 0; i < hexBV.length(); i++) {
            char strBit = bin.charAt(bin.length() - 1 - i);
            if (hexBV.get(i)) {
                assertTrue(strBit == '1');
            } else {
                assertTrue(strBit == '0');
            }
        }

        assertEquals(hexBV.toBinaryString(), bin);

    }

    /**
     * Makes sure vectors of length zero work.
     */
    public void testLengthZero() {
        DenseBitVector bvZ1 = new DenseBitVector(0);
        DenseBitVector bvZ2 = new DenseBitVector(0);
        DenseBitVector bv = new DenseBitVector(10);

        bv.set(1);
        bv.set(7);
        bv.set(9);

        DenseBitVector result;

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

        // invert zero length vector
        result = bvZ1.invert();
        assertTrue(result.length() == 0);

        // intersect with zero length
        assertFalse(bvZ2.intersects(bv));
        assertFalse(bv.intersects(bvZ1));
        assertFalse(bvZ1.intersects(bvZ2));

        // cardinality
        assertTrue(bvZ1.cardinality() == 0);

        // toString on zero length shouldn't fail.
        bvZ1.toString();
        bvZ1.dumpBits();
        assertTrue(bvZ1.getAllBits().length == 0);
        bvZ1.hashCode();
    }

    /**
     * tests set, clear and get functionality.
     */
    public void testSetClearGet() {
        DenseBitVector bv0 = new DenseBitVector(0);
        assertTrue(bv0.getAllBits().length == 0);
        DenseBitVector bv10 = new DenseBitVector(10);
        assertTrue(bv10.getAllBits().length == 1);
        DenseBitVector bv64 = new DenseBitVector(64);
        assertTrue(bv64.getAllBits().length == 1);
        DenseBitVector bv65 = new DenseBitVector(65);
        assertTrue(bv65.getAllBits().length == 2);
        DenseBitVector bv129 = new DenseBitVector(129);
        assertTrue(bv129.getAllBits().length == 3);
        DenseBitVector bv4098 = new DenseBitVector(4098);
        assertTrue(bv4098.getAllBits().length == 65);

        bv10.set(3);
        bv10.set(5);
        bv10.set(7);
        assertTrue(bv10.cardinality() == 3);
        assertTrue(bv10.get(3));
        assertTrue(bv10.get(5));
        assertTrue(bv10.get(7));
        // 0x 0000 0000 0000 00+(10101000)
        assertTrue(bv10.getAllBits()[0] == 0xA8);

        bv10.clear(3);
        bv10.clear(5);
        bv10.clear(7);
        assertTrue(bv10.cardinality() == 0);
        assertFalse(bv10.get(3));
        assertFalse(bv10.get(5));
        assertFalse(bv10.get(7));
        assertTrue(bv10.isEmpty());
        assertTrue(bv10.getAllBits()[0] == 0);
        bv10.clear(4);
        assertTrue(bv10.getAllBits()[0] == 0);
        bv10.clear(5);
        assertTrue(bv10.getAllBits()[0] == 0);
        bv10.clear(6);
        assertTrue(bv10.getAllBits()[0] == 0);
        assertTrue(bv10.isEmpty());

        bv10.set(3);
        bv10.set(5);
        bv10.set(7);
        bv10.set(3, false);
        bv10.set(5, false);
        bv10.set(7, false);
        assertTrue(bv10.isEmpty());
        assertTrue(bv10.getAllBits()[0] == 0);

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

        bv64.set(63);
        assertTrue(bv64.get(63));
        assertTrue(bv64.getAllBits()[0] == 0x8000000000000000L);
        bv64.clear(63);
        assertFalse(bv64.get(63));
        assertTrue(bv64.getAllBits()[0] == 0x0L);

        bv64.set(0);
        assertTrue(bv64.get(0));
        bv64.set(1);
        assertTrue(bv64.get(1));
        bv64.set(61);
        assertTrue(bv64.get(61));
        bv64.set(62);
        assertTrue(bv64.get(62));
        bv64.set(63);
        assertTrue(bv64.get(63));
        assertTrue(bv64.getAllBits()[0] == 0xE000000000000003L);
        assertTrue(bv64.cardinality() == 5);

        // 65bit vector
        bv65.set(63);
        assertTrue(bv65.get(63));
        assertTrue(bv65.getAllBits()[0] == 0x8000000000000000L);
        assertTrue(bv65.getAllBits()[1] == 0x0L);
        assertTrue(bv65.cardinality() == 1);
        bv65.clear(63);
        assertFalse(bv65.get(63));
        assertTrue(bv65.getAllBits()[0] == 0x0L);
        assertTrue(bv65.getAllBits()[1] == 0x0L);
        assertTrue(bv65.cardinality() == 0);
        bv65.set(64);
        assertTrue(bv65.get(64));
        assertTrue(bv65.getAllBits()[0] == 0x0L);
        assertTrue(bv65.getAllBits()[1] == 0x1L);
        assertTrue(bv65.cardinality() == 1);
        bv65.clear(64);
        assertFalse(bv65.get(64));
        assertTrue(bv65.isEmpty());
        assertTrue(bv65.getAllBits()[0] == 0x0L);
        assertTrue(bv65.getAllBits()[1] == 0x0L);
        assertTrue(bv65.cardinality() == 0);
        bv65.set(0);
        assertTrue(bv65.get(0));
        bv65.set(1);
        assertTrue(bv65.get(1));
        bv65.set(61);
        assertTrue(bv65.get(61));
        bv65.set(62);
        assertTrue(bv65.get(62));
        bv65.set(63);
        assertTrue(bv65.get(63));
        bv65.set(64);
        assertTrue(bv65.get(64));
        assertTrue(bv65.getAllBits()[0] == 0xE000000000000003L);
        assertTrue(bv65.getAllBits()[1] == 0x1L);
        assertTrue(bv65.cardinality() == 6);

        // 129bit vector
        bv129.set(127);
        assertTrue(bv129.get(127));
        assertTrue(bv129.getAllBits()[0] == 0x0L);
        assertTrue(bv129.getAllBits()[1] == 0x8000000000000000L);
        assertTrue(bv129.getAllBits()[2] == 0x0L);
        bv129.clear(127);
        assertFalse(bv129.get(127));
        assertTrue(bv129.isEmpty());
        assertTrue(bv129.getAllBits()[0] == 0x0L);
        assertTrue(bv129.getAllBits()[1] == 0x0L);
        assertTrue(bv129.getAllBits()[2] == 0x0L);

        bv129.set(128);
        assertTrue(bv129.get(128));
        assertTrue(bv129.getAllBits()[0] == 0x0L);
        assertTrue(bv129.getAllBits()[1] == 0x0L);
        assertTrue(bv129.getAllBits()[2] == 0x1L);
        bv129.clear(128);
        assertFalse(bv129.get(128));
        assertTrue(bv129.isEmpty());
        assertTrue(bv129.getAllBits()[0] == 0x0L);
        assertTrue(bv129.getAllBits()[1] == 0x0L);
        assertTrue(bv129.getAllBits()[2] == 0x0L);
        bv129.clear(128);
        assertFalse(bv129.get(128));
        assertTrue(bv129.isEmpty());
        assertTrue(bv129.getAllBits()[0] == 0x0L);
        assertTrue(bv129.getAllBits()[1] == 0x0L);
        assertTrue(bv129.getAllBits()[2] == 0x0L);
        bv129.set(0);
        assertTrue(bv129.get(0));
        bv129.set(1);
        assertTrue(bv129.get(1));
        bv129.set(63);
        assertTrue(bv129.get(63));
        bv129.set(64);
        assertTrue(bv129.get(64));
        bv129.set(65);
        assertTrue(bv129.get(65));
        bv129.set(66);
        assertTrue(bv129.get(66));
        bv129.set(126);
        assertTrue(bv129.get(126));
        bv129.set(127);
        assertTrue(bv129.get(127));
        bv129.set(128);
        assertTrue(bv129.get(128));
        assertTrue(bv129.getAllBits()[0] == 0x8000000000000003L);
        assertTrue(bv129.getAllBits()[1] == 0xC000000000000007L);
        assertTrue(bv129.getAllBits()[2] == 0x1L);

        bv129.set(0, 129);
        long[] bits = bv129.getAllBits();
        assertTrue(bits[0] == 0xFFFFFFFFFFFFFFFFL);
        assertTrue(bits[1] == 0xFFFFFFFFFFFFFFFFL);
        assertTrue(bv129.cardinality() == 129);
        assertTrue(bv129.get(3));
        // make sure we got a copy
        bits[0] = 0L;
        assertTrue(bv129.cardinality() == 129);
        assertTrue(bv129.get(3));

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
    public void testNextSetClearBit() {

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
        assertTrue(bv64.nextClearBit(0) == 0);
        assertTrue(bv64.nextClearBit(62) == 62);
        assertTrue(bv64.nextClearBit(63) == 63);
        assertTrue(bv64.nextClearBit(64) == -1);
        assertTrue(bv64.nextSetBit(8) == -1);
        assertTrue(bv64.nextSetBit(62) == -1);
        assertTrue(bv64.nextSetBit(63) == -1);
        assertTrue(bv64.nextSetBit(64) == -1);

        bv64.set(0, 64);
        assertTrue(bv64.nextClearBit(0) == -1);
        assertTrue(bv64.nextClearBit(62) == -1);
        assertTrue(bv64.nextClearBit(63) == -1);
        assertTrue(bv64.nextSetBit(8) == 8);
        assertTrue(bv64.nextSetBit(62) == 62);
        assertTrue(bv64.nextSetBit(63) == 63);
        assertTrue(bv64.nextSetBit(64) == -1);

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
     * tests toString
     */
    public void testToString() {
        int length = 500000;
        DenseBitVector bv = new DenseBitVector(length);
        bv.set(18);
        bv.set(7645);
        bv.set(700);
        bv.set(381966);
        assertEquals("{length=" + length + ", set bits=18, 700, 7645, 381966}", bv.toString());

    }

    public void testSubsequence(){
    	try {
    		new DenseBitVector("10f0").subSequence(-1, 0);
    		fail("Exception Expected");
		} catch (IllegalArgumentException e) {
		}

    	try {
    		new DenseBitVector("10f0").subSequence(0, -2);
    		fail("Exception Expected");
		} catch (IllegalArgumentException e) {
		}

       	try {
    		new DenseBitVector("10f0").subSequence(0, 17);
    		fail("Exception Expected");
		} catch (IllegalArgumentException e) {
		}

       	try {
    		new DenseBitVector("10f0").subSequence(5, 4);
    		fail("Exception Expected");
		} catch (IllegalArgumentException e) {
		}

      	assertEquals("", new DenseBitVector("").subSequence(0, 0).toHexString());
      	assertEquals("", new DenseBitVector("10f0").subSequence(0, 0).toHexString());
       	assertEquals("F0", new DenseBitVector("10f0").subSequence(0, 8).toHexString());
       	assertEquals("0F0", new DenseBitVector("10f0").subSequence(0, 12).toHexString());
       	assertEquals("043", new DenseBitVector("10f0").subSequence(6, 16).toHexString());
       	assertEquals("3", new DenseBitVector("10f0").subSequence(4, 6).toHexString());
    }

    public void testBug5077(){
    	DenseBitVector denseBit = new DenseBitVector("1000000000000000001");
		DenseBitVector denseBit2 = new DenseBitVector("101");

		assertEquals("1000000000000000101", denseBit.or(denseBit2).toHexString());
		assertEquals("1000000000000000101", denseBit2.or(denseBit).toHexString());

		assertEquals("0000000000000000001", denseBit.and(denseBit2).toHexString());
		assertEquals("0000000000000000001", denseBit2.and(denseBit).toHexString());

		assertEquals("1000000000000000100", denseBit.xor(denseBit2).toHexString());
		assertEquals("1000000000000000100", denseBit2.xor(denseBit).toHexString());
    }

    public void testToBinaryString() {
        DenseBitVector bv = new DenseBitVector("1F03");
        assertEquals(bv.toBinaryString(), "0001111100000011");
    }

    public void testToHexString() {
        DenseBitVector bv = new DenseBitVector("1F03");
        assertEquals("1F03", bv.toHexString());

        bv = new DenseBitVector("1F0329384abedf7cA7FC29FF0");
        assertEquals("1F0329384ABEDF7CA7FC29FF0", bv.toHexString());

        bv = new DenseBitVector("");
        assertEquals("", bv.toHexString());

        bv = new DenseBitVector(3);
        bv.set(0);
        bv.set(2);
        assertEquals("5", bv.toHexString());

        bv = new DenseBitVector(13L);
        bv.set(8);
        bv.set(3);
        assertEquals("0108", bv.toHexString());

        bv = new DenseBitVector("FFF");
        assertEquals("FFF", bv.toHexString());

        bv = new DenseBitVector("FFFF8888EEEEFFFF");
        assertEquals("FFFF8888EEEEFFFF", bv.toHexString());

        bv = new DenseBitVector("1FFFF8888EEEEFFFF");
        assertEquals("1FFFF8888EEEEFFFF", bv.toHexString());

        String hex = "E02008440A840140480081012400304040200432829D00102440110020265B20082C811354080040D80046A65806080" +
        		"502004031204000812483400000898010";
        bv = new DenseBitVector(hex);
        assertEquals(hex, bv.toHexString());
    }

    /**
     * Checks that the hashCode of dense and sparse bit vectors are identical.
     *
     * @throws Exception if an error occurs
     */
    public void testHashCode() throws Exception {
        checkHashCode("0");
        checkHashCode("1");
        checkHashCode("101");
        checkHashCode("01101");
        checkHashCode("110011001100110011001100110011001"); // 65 bits
        checkHashCode("000000000000000000001000000000000"); // 65 bits
        checkHashCode("0000000000000000000000000000000001000000000000000000000000000000000");
    }

    private void checkHashCode(final String bits) {
        SparseBitVector sparse = new SparseBitVector(bits);
        DenseBitVector dense = new DenseBitVector(bits);

        assertThat("Hashcode for " + bits + " not equal", sparse.hashCode(), Is.is(dense.hashCode()));
    }
}

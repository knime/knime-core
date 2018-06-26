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

import java.math.BigInteger;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;
import org.knime.core.util.Pair;

public class BitVectorUtilTest {
    private static final Random RANDOM = new Random(42);

    private static final BitVectorValue EMPTY_VECTOR = new DenseBitVectorCellFactory("").createDataCell();

    private static final TestVectorFactory DENSE_VECTOR_FACTORY = new TestVectorFactory() {

        @Override
        public Pair<BitVectorValue, BitVectorValue> createVectorPair(final String first, final String second) {
            return Pair.<BitVectorValue, BitVectorValue> create(new DenseBitVectorCellFactory(first).createDataCell(),
                new DenseBitVectorCellFactory(second).createDataCell());
        }
    };

    private static final TestVectorFactory SPARSE_VECTOR_FACTORY = new TestVectorFactory() {

        @Override
        public Pair<BitVectorValue, BitVectorValue> createVectorPair(final String first, final String second) {
            return Pair.<BitVectorValue, BitVectorValue> create(new SparseBitVectorCellFactory(first).createDataCell(),
                new SparseBitVectorCellFactory(second).createDataCell());
        }
    };

    private static final TestVectorFactory MIXED_VECTOR_FACTORY = new TestVectorFactory() {

        @Override
        public Pair<BitVectorValue, BitVectorValue> createVectorPair(final String first, final String second) {
            return RANDOM.nextBoolean() ? Pair.<BitVectorValue, BitVectorValue> create(new DenseBitVectorCellFactory(
                first).createDataCell(), new SparseBitVectorCellFactory(second).createDataCell()) : Pair
                .<BitVectorValue, BitVectorValue> create(new SparseBitVectorCellFactory(first).createDataCell(),
                    new DenseBitVectorCellFactory(second).createDataCell());
        }
    };

    @Test(expected = NullPointerException.class)
    public void testCardinalityOfIntersectionThrowsNullpointer() {
        BitVectorUtil.cardinalityOfIntersection(null, EMPTY_VECTOR);
    }

    @Test(expected = NullPointerException.class)
    public void testCardinalityOfIntersectionThrowsNullpointer2() {
        BitVectorUtil.cardinalityOfIntersection(EMPTY_VECTOR, null);
    }

    @Test
    public void testCardinalityOfIntersectionOnlyDense() {
        Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(EMPTY_VECTOR, EMPTY_VECTOR));

        assertCardinalityOfIntersection(DENSE_VECTOR_FACTORY);
    }

    @Test
    public void testCardinalityOfIntersectionOnlySparse() {
        Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(EMPTY_VECTOR, EMPTY_VECTOR));

        assertCardinalityOfIntersection(SPARSE_VECTOR_FACTORY);
    }

    @Test
    public void testCardinalityOfIntersectionMixed() {
        Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(EMPTY_VECTOR, EMPTY_VECTOR));

        assertCardinalityOfIntersection(MIXED_VECTOR_FACTORY);
    }

    @Test(expected = NullPointerException.class)
    public void testCardinalityOfRelativeComplementThrowsNullpointer() {
        BitVectorUtil.cardinalityOfRelativeComplement(EMPTY_VECTOR, null);
    }

    @Test(expected = NullPointerException.class)
    public void testCardinalityOfRelativeComplementThrowsNullpointer2() {
        BitVectorUtil.cardinalityOfRelativeComplement(null, EMPTY_VECTOR);
    }

    @Test
    public void testCardinalityOfRelativeComplementOnlyDense() {
        assertCardinalityOfRelativeComplement(DENSE_VECTOR_FACTORY);
    }

    @Test
    public void testCardinalityOfRelativeComplementOnlySparse() {
        assertCardinalityOfRelativeComplement(SPARSE_VECTOR_FACTORY);
    }

    @Test
    public void testCardinalityOfRelativeComplementMixed() {
        assertCardinalityOfRelativeComplement(MIXED_VECTOR_FACTORY);
    }

    // Tests for bug http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5077
    @Test
    public void testBitVectorUtilXor() {
        assertCardinalityOfXor(MIXED_VECTOR_FACTORY);
        assertCardinalityOfXor(SPARSE_VECTOR_FACTORY);
        assertCardinalityOfXor(DENSE_VECTOR_FACTORY);
    }

    @Test
    public void testBitVectorUtilOr() {
        assertCardinalityOfOr(MIXED_VECTOR_FACTORY);
        assertCardinalityOfOr(SPARSE_VECTOR_FACTORY);
        assertCardinalityOfOr(DENSE_VECTOR_FACTORY);
    }

    @Test
    public void testBitVectorUtilAnd() {
        assertCardinalityOfAnd(MIXED_VECTOR_FACTORY);
        assertCardinalityOfAnd(SPARSE_VECTOR_FACTORY);
        assertCardinalityOfAnd(DENSE_VECTOR_FACTORY);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilOrThrowsNullpointer() {
        BitVectorUtil.or(null, EMPTY_VECTOR);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilOrThrowsNullpointer2() {
        BitVectorUtil.or(EMPTY_VECTOR, null);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilXorThrowsNullpointer() {
        BitVectorUtil.xor(null, EMPTY_VECTOR);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilXorThrowsNullpointer2() {
        BitVectorUtil.xor(EMPTY_VECTOR, null);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilAndThrowsNullpointer() {
        BitVectorUtil.and(null, EMPTY_VECTOR);
    }

    @Test(expected = NullPointerException.class)
    public void testBitVectorUtilAndThrowsNullpointer2() {
        BitVectorUtil.and(EMPTY_VECTOR, null);
    }

    private static void assertCardinalityOfAnd(final TestVectorFactory vectorFactory) {
        for (int i = 1; i < 1000; i++) {
            String first = new BigInteger(RANDOM.nextInt(800), RANDOM).toString(16);

            String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5 : 150), RANDOM).toString(16);
            Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory.createVectorPair(first, second);

            BitVectorValue a = createVectorPair.getFirst();

            BitVectorValue b = createVectorPair.getSecond();

            DenseBitVectorCell expected = DenseBitVectorCellFactory.and(a, b);

            BitVectorValue actual = BitVectorUtil.and(a, b);

            if (vectorFactory == SPARSE_VECTOR_FACTORY || vectorFactory == MIXED_VECTOR_FACTORY) {
                Assert.assertTrue("Not an instance of Sparce Vectors", actual instanceof SparseBitVectorCell);
            } else {
                Assert.assertTrue("Not an instance of Dense Vectors", actual instanceof DenseBitVectorCell);
            }

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), expected.toHexString(),
                actual.toHexString());
        }
    }

    private static void assertCardinalityOfXor(final TestVectorFactory vectorFactory) {
        for (int i = 1; i < 1000; i++) {
            String first = new BigInteger(RANDOM.nextInt(800), RANDOM).toString(16);

            String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5 : 150), RANDOM).toString(16);
            Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory.createVectorPair(first, second);

            BitVectorValue a = createVectorPair.getFirst();

            BitVectorValue b = createVectorPair.getSecond();

            DenseBitVectorCell expected = DenseBitVectorCellFactory.xor(a, b);

            BitVectorValue actual = BitVectorUtil.xor(a, b);

            if (vectorFactory == SPARSE_VECTOR_FACTORY) {
                Assert.assertTrue("Not an instance of Sparce Vectors", actual instanceof SparseBitVectorCell);
            } else {
                Assert.assertTrue("Not an instance of Dense Vectors", actual instanceof DenseBitVectorCell);
            }

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), expected.toHexString(),
                actual.toHexString());
        }
    }

    private static void assertCardinalityOfOr(final TestVectorFactory vectorFactory) {
        for (int i = 1; i < 1000; i++) {
            String first = new BigInteger(RANDOM.nextInt(800), RANDOM).toString(16);

            String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5 : 150), RANDOM).toString(16);
            Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory.createVectorPair(first, second);

            BitVectorValue a = createVectorPair.getFirst();

            BitVectorValue b = createVectorPair.getSecond();

            DenseBitVectorCell expected = DenseBitVectorCellFactory.or(a, b);

            BitVectorValue actual = BitVectorUtil.or(a, b);

            if (vectorFactory == SPARSE_VECTOR_FACTORY) {
                Assert.assertTrue("Not an instance of Sparce Vectors", actual instanceof SparseBitVectorCell);
            } else {
                Assert.assertTrue("Not an instance of Dense Vectors", actual instanceof DenseBitVectorCell);
            }

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), expected.toHexString(),
                actual.toHexString());
        }
    }

    private static void assertCardinalityOfIntersection(final TestVectorFactory vectorFactory) {
        for (int i = 1; i < 1000; i++) {
            String first = new BigInteger(RANDOM.nextInt(800), RANDOM).toString(16);

            String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5 : 150), RANDOM).toString(16);
            Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory.createVectorPair(first, second);

            BitVectorValue a = createVectorPair.getFirst();

            BitVectorValue b = createVectorPair.getSecond();

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), BitVectorUtil.and(a, b)
                .cardinality(), BitVectorUtil.cardinalityOfIntersection(a, b));

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), BitVectorUtil.and(a, b)
                .cardinality(), BitVectorUtil.cardinalityOfIntersection(b, a));
        }
    }

    private static void assertCardinalityOfRelativeComplement(final TestVectorFactory vectorFactory) {
        Assert.assertEquals(0, BitVectorUtil.cardinalityOfRelativeComplement(EMPTY_VECTOR, EMPTY_VECTOR));

        for (int i = 1; i < 1000; i++) {
            String first = new BigInteger(RANDOM.nextInt(2000), RANDOM).toString(16);

            String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5 : 150), RANDOM).toString(16);

            DenseBitVector aWithoutB = createRelativeComplement(first, second);

            DenseBitVector bWithoutA = createRelativeComplement(second, first);

            Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory.createVectorPair(first, second);

            BitVectorValue a = createVectorPair.getFirst();

            BitVectorValue b = createVectorPair.getSecond();

            Assert.assertEquals("Broken add: " + a.toHexString() + "|" + b.toHexString(), aWithoutB.cardinality(),
                BitVectorUtil.cardinalityOfRelativeComplement(a, b));

            Assert.assertEquals("Broken add: " + b.toHexString() + "|" + a.toHexString(), bWithoutA.cardinality(),
                BitVectorUtil.cardinalityOfRelativeComplement(b, a));
        }
    }

    private interface TestVectorFactory {
        Pair<BitVectorValue, BitVectorValue> createVectorPair(String first, String second);
    }

    /**
     * Creates a reference bit vector for the tests using the equivalence: A \ B = A cut not B (When using bit vectors
     * and assuming same length)
     */
    private static DenseBitVector createRelativeComplement(final String first, final String second) {
        DenseBitVector firstVector = new DenseBitVector(first);
        DenseBitVector invertedVector = new DenseBitVector(second).invert();
        if (invertedVector.length() < firstVector.length()) {
            // add ones to the start of the inverted vector
            DenseBitVector filledWithOnes = createVectorWithOnes(firstVector.length() - invertedVector.length());

            invertedVector = invertedVector.concatenate(filledWithOnes);
        }

        return firstVector.and(invertedVector);
    }

    private static DenseBitVector createVectorWithOnes(final long l) {
        DenseBitVector denseBitVector = new DenseBitVector(l);
        denseBitVector.set(0, l);
        return denseBitVector;
    }
}

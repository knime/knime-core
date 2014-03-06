package org.knime.core.data.vector.bitvector;

import java.math.BigInteger;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;
import org.knime.core.util.Pair;

public class BitVectorUtilTest {
	private static final Random RANDOM = new Random(42);
	private static final BitVectorValue EMPTY_VECTOR = new DenseBitVectorCellFactory(
			"").createDataCell();
	private TestVectorFactory DENSE_VECTOR_FACTORY = new TestVectorFactory() {

		@Override
		public Pair<BitVectorValue, BitVectorValue> createVectorPair(
				String first, String second) {
			return Pair.<BitVectorValue, BitVectorValue> create(
					new DenseBitVectorCellFactory(first).createDataCell(),
					new DenseBitVectorCellFactory(second).createDataCell());
		}
	};

	private TestVectorFactory SPARSE_VECTOR_FACTORY = new TestVectorFactory() {

		@Override
		public Pair<BitVectorValue, BitVectorValue> createVectorPair(
				String first, String second) {
			return Pair.<BitVectorValue, BitVectorValue> create(
					new SparseBitVectorCellFactory(first).createDataCell(),
					new SparseBitVectorCellFactory(second).createDataCell());
		}
	};

	private TestVectorFactory MIXED_VECTOR_FACTORY = new TestVectorFactory() {

		@Override
		public Pair<BitVectorValue, BitVectorValue> createVectorPair(
				String first, String second) {
			return RANDOM.nextBoolean() ? Pair
					.<BitVectorValue, BitVectorValue> create(
							new DenseBitVectorCellFactory(first)
									.createDataCell(),
							new SparseBitVectorCellFactory(second)
									.createDataCell()) : Pair
					.<BitVectorValue, BitVectorValue> create(
							new SparseBitVectorCellFactory(first)
									.createDataCell(),
							new DenseBitVectorCellFactory(second)
									.createDataCell());
		}
	};

	// Tests for bug http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5058
	// @Test
	// public void testBug5058() {
	// BitVectorValue denseBitVector = new DenseBitVectorCellFactory("020f")
	// .createDataCell();
	// BitVectorValue sparseBitVector = new SparseBitVectorCellFactory("0304")
	// .createDataCell();
	//
	// Assert.assertEquals("0204",
	// BitVectorUtil.and(denseBitVector, sparseBitVector)
	// .toHexString());
	// }

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
		Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(
				EMPTY_VECTOR, EMPTY_VECTOR));

		assertCardinalityOfIntersection(DENSE_VECTOR_FACTORY);
	}

	@Test
	public void testCardinalityOfIntersectionOnlySparse() {
		Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(
				EMPTY_VECTOR, EMPTY_VECTOR));

		assertCardinalityOfIntersection(SPARSE_VECTOR_FACTORY);
	}

	@Test
	public void testCardinalityOfIntersectionMixed() {
		Assert.assertEquals(0, BitVectorUtil.cardinalityOfIntersection(
				EMPTY_VECTOR, EMPTY_VECTOR));

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

	private static void assertCardinalityOfIntersection(
			TestVectorFactory vectorFactory) {
		for (int i = 1; i < 1000; i++) {
			String first = new BigInteger(RANDOM.nextInt(800), RANDOM)
					.toString(16);

			String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5
					: 150), RANDOM).toString(16);
			Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory
					.createVectorPair(first, second);

			BitVectorValue a = createVectorPair.getFirst();

			BitVectorValue b = createVectorPair.getSecond();

			Assert.assertEquals(
					"Broken add: " + a.toHexString() + "|" + b.toHexString(),
					BitVectorUtil.and(a, b).cardinality(),
					BitVectorUtil.cardinalityOfIntersection(a, b));

			Assert.assertEquals(
					"Broken add: " + a.toHexString() + "|" + b.toHexString(),
					BitVectorUtil.and(a, b).cardinality(),
					BitVectorUtil.cardinalityOfIntersection(b, a));
		}
	}

	private static void assertCardinalityOfRelativeComplement(
			TestVectorFactory vectorFactory) {
		Assert.assertEquals(0, BitVectorUtil.cardinalityOfRelativeComplement(
				EMPTY_VECTOR, EMPTY_VECTOR));

		for (int i = 1; i < 1000; i++) {
			String first = new BigInteger(RANDOM.nextInt(2000), RANDOM)
					.toString(16);

			String second = new BigInteger(RANDOM.nextInt(i % 5 == 0 ? i * 5
					: 150), RANDOM).toString(16);

			DenseBitVector aWithoutB = createRelativeComplement(first, second);

			DenseBitVector bWithoutA = createRelativeComplement(second, first);

			Pair<BitVectorValue, BitVectorValue> createVectorPair = vectorFactory
					.createVectorPair(first, second);

			BitVectorValue a = createVectorPair.getFirst();

			BitVectorValue b = createVectorPair.getSecond();

			Assert.assertEquals(
					"Broken add: " + a.toHexString() + "|" + b.toHexString(),
					aWithoutB.cardinality(),
					BitVectorUtil.cardinalityOfRelativeComplement(a, b));

			Assert.assertEquals(
					"Broken add: " + b.toHexString() + "|" + a.toHexString(),
					bWithoutA.cardinality(),
					BitVectorUtil.cardinalityOfRelativeComplement(b, a));
		}
	}

	@Test
	public void test() {

		Pair<BitVectorValue, BitVectorValue> createVectorPair = SPARSE_VECTOR_FACTORY
				.createVectorPair("5C", "1A");
		System.out.println("!!"
				+ BitVectorUtil.cardinalityOfRelativeComplement(
						createVectorPair.getFirst(),
						createVectorPair.getSecond()));
	}

	private interface TestVectorFactory {
		Pair<BitVectorValue, BitVectorValue> createVectorPair(String first,
				String second);
	}

	/**
	 * Creates a reference bit vector for the tests using the equivalence: A \ B
	 * = A cut not B (When using bit vectors and assuming same length)
	 */
	private static DenseBitVector createRelativeComplement(String first,
			String second) {
		DenseBitVector firstVector = new DenseBitVector(first);
		DenseBitVector invertedVector = new DenseBitVector(second).invert();
		if (invertedVector.length() < firstVector.length()) {
			// add ones to the start of the inverted vector
			DenseBitVector filledWithOnes = createVectorWithOnes(firstVector
					.length() - invertedVector.length());

			invertedVector = invertedVector.concatenate(filledWithOnes);
		}

		return firstVector.and(invertedVector);
	}

	private static DenseBitVector createVectorWithOnes(long l) {
		DenseBitVector denseBitVector = new DenseBitVector(l);
		denseBitVector.set(0, l);
		return denseBitVector;
	}
}

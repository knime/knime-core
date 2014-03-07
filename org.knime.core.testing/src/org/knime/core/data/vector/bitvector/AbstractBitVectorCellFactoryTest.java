package org.knime.core.data.vector.bitvector;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractBitVectorCellFactoryTest {

	private static final int AMOUNT_OF_BITS = 200;
	private static final Random RANDOM = new Random(42);

	private static final String FIRST_DEFAULT = "020f";

	private static final String SECOND_DEFAULT = "0304";

	private static final String AND_RESULT = "0204";
	private static final String OR_RESULT = "030F";
	private static final String XOR_RESULT = "010B";

	@Test
	public void testSameTypeAnd() {

		BitVectorValue and = doAnd(createBitVector(FIRST_DEFAULT),
				createBitVector(SECOND_DEFAULT));

		Assert.assertEquals(AND_RESULT, and.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			BitVectorValue sparseBV = createBitVector(first);

			BitVectorValue denseBV = createBitVector(second);

			BitVectorValue reference = createReferenceAnd(first, second);

			Assert.assertEquals("Broken at: " + sparseBV.toHexString()
					+ " <-> " + denseBV.toHexString(), reference,
					doAnd(sparseBV, denseBV));

			Assert.assertEquals("Broken at: " + sparseBV.toHexString()
					+ " <-> " + denseBV.toHexString(), reference,
					doAnd(denseBV, sparseBV));
		}
	}

	@Test
	public void testSameTypeOr() {

		BitVectorValue or = doOr(createBitVector(FIRST_DEFAULT),
				createBitVector(SECOND_DEFAULT));

		Assert.assertEquals(OR_RESULT, or.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			BitVectorValue sparseBV = createBitVector(first);

			BitVectorValue denseBV = createBitVector(second);

			BitVectorValue reference = createReferenceOr(first, second);

			Assert.assertEquals("Broken at: " + sparseBV.toHexString() + "|"
					+ denseBV.toHexString(), reference, doOr(sparseBV, denseBV));

			Assert.assertEquals("Broken at: " + sparseBV.toHexString() + "|"
					+ denseBV.toHexString(), reference, doOr(denseBV, sparseBV));
		}
	}

	@Test
	public void testSameTypeXor() {
		BitVectorValue xor = doXor(createBitVector(FIRST_DEFAULT),
				createBitVector(SECOND_DEFAULT));

		Assert.assertEquals(XOR_RESULT, xor.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			BitVectorValue a = createBitVector(first);

			BitVectorValue b = createBitVector(second);

			BitVectorValue reference = createReferenceXor(first, second);

			Assert.assertEquals(
					"Broken at: " + a.toHexString() + "|" + b.toHexString(),
					reference, doXor(a, b));

			Assert.assertEquals(
					"Broken at: " + a.toHexString() + "|" + b.toHexString(),
					reference, doXor(b, a));
		}
	}

	// Tests for bug http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5058
	@Test
	public void testBug5058And() {

		BitVectorValue and = doAnd(
				new DenseBitVectorCellFactory(FIRST_DEFAULT).createDataCell(),
				new SparseBitVectorCellFactory(SECOND_DEFAULT).createDataCell());

		Assert.assertEquals(AND_RESULT, and.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			BitVectorValue sparseBV = new DenseBitVectorCellFactory(first)
					.createDataCell();

			BitVectorValue denseBV = new SparseBitVectorCellFactory(second)
					.createDataCell();

			BitVectorValue reference = createReferenceAnd(first, second);

			Assert.assertEquals("Broken at: " + sparseBV.toHexString()
					+ " <-> " + denseBV.toHexString(), reference,
					doAnd(sparseBV, denseBV));

			Assert.assertEquals("Broken at: " + sparseBV.toHexString()
					+ " <-> " + denseBV.toHexString(), reference,
					doAnd(denseBV, sparseBV));
		}
	}
	
	// Tests for bug http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5058
	@Test
	public void testBug5058Or() {

		BitVectorValue or = doOr(
				new DenseBitVectorCellFactory(FIRST_DEFAULT).createDataCell(),
				new SparseBitVectorCellFactory(SECOND_DEFAULT).createDataCell());

		Assert.assertEquals(OR_RESULT, or.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			BitVectorValue sparseBV = new DenseBitVectorCellFactory(first)
					.createDataCell();

			BitVectorValue denseBV = new SparseBitVectorCellFactory(second)
					.createDataCell();

			BitVectorValue reference = createReferenceOr(first, second);

			Assert.assertEquals("Broken at: " + sparseBV.toHexString() + "|"
					+ denseBV.toHexString(), reference, doOr(sparseBV, denseBV));

			Assert.assertEquals("Broken at: " + sparseBV.toHexString() + "|"
					+ denseBV.toHexString(), reference, doOr(denseBV, sparseBV));
		}
	}

	// Tests for bug http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5058
	@Test
	public void testBug5058Xor() {
		BitVectorValue xor = doXor(
				new DenseBitVectorCellFactory(FIRST_DEFAULT).createDataCell(),
				new SparseBitVectorCellFactory(SECOND_DEFAULT).createDataCell());

		Assert.assertEquals(XOR_RESULT, xor.toHexString());

		for (int i = 1; i < 2000; i++) {
			String first = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);
			String second = new BigInteger(RANDOM.nextInt(AMOUNT_OF_BITS),
					RANDOM).toString(16);

			DenseBitVectorCell a = new DenseBitVectorCellFactory(first)
					.createDataCell();

			BitVectorValue b = new SparseBitVectorCellFactory(second)
					.createDataCell();

			BitVectorValue reference = createReferenceXor(first, second);

			Assert.assertEquals(
					"Broken at: " + a.toHexString() + "|" + b.toHexString(),
					reference, doXor(a, b));

			Assert.assertEquals(
					"Broken at: " + a.toHexString() + "|" + b.toHexString(),
					reference, doXor(b, a));
		}
	}

	abstract BitVectorValue createBitVector(String content);

	abstract BitVectorValue createReferenceAnd(String first, String second);

	abstract BitVectorValue createReferenceOr(String first, String second);

	abstract BitVectorValue createReferenceXor(String first, String second);

	abstract BitVectorValue doAnd(BitVectorValue first, BitVectorValue second);

	abstract BitVectorValue doOr(BitVectorValue first, BitVectorValue second);

	abstract BitVectorValue doXor(BitVectorValue first, BitVectorValue second);
}

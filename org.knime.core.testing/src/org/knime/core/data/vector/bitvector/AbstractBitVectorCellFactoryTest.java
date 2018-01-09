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

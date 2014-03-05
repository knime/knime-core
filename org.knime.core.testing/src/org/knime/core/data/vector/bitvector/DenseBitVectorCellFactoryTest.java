package org.knime.core.data.vector.bitvector;

public class DenseBitVectorCellFactoryTest extends
		AbstractBitVectorCellFactoryTest {
	


	@Override
	BitVectorValue doAnd(BitVectorValue first, BitVectorValue second) {
		return DenseBitVectorCellFactory.and(first, second);
	}

	@Override
	BitVectorValue createReferenceAnd(String first, String second) {
		return new DenseBitVectorCellFactory(
				new DenseBitVector(first).and(new DenseBitVector(second)))
				.createDataCell();
	}

	@Override
	BitVectorValue doOr(BitVectorValue first, BitVectorValue second) {
		return DenseBitVectorCellFactory.or(first, second);
	}

	@Override
	BitVectorValue createReferenceOr(String first, String second) {
		return new DenseBitVectorCellFactory(
				new DenseBitVector(first).or(new DenseBitVector(second)))
				.createDataCell();
	}

	@Override
	BitVectorValue doXor(BitVectorValue first, BitVectorValue second) {
		return DenseBitVectorCellFactory.xor(first, second);
	}

	@Override
	BitVectorValue createReferenceXor(String first, String second) {
		return new DenseBitVectorCellFactory(
				new DenseBitVector(first).xor(new DenseBitVector(second)).toHexString())
				.createDataCell();
	}

	@Override
	BitVectorValue createBitVector(String content) {
		return new DenseBitVectorCellFactory(
				new DenseBitVector(content)).createDataCell();
	}
}

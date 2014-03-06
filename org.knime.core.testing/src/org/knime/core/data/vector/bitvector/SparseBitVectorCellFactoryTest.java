package org.knime.core.data.vector.bitvector;

public class SparseBitVectorCellFactoryTest extends
		AbstractBitVectorCellFactoryTest {

	@Override
	BitVectorValue doAnd(BitVectorValue first, BitVectorValue second) {
		return SparseBitVectorCellFactory.and(first, second);
	}

	@Override
	BitVectorValue createReferenceAnd(String first, String second) {
		return new SparseBitVectorCellFactory(
				new SparseBitVector(first).and(new SparseBitVector(second)))
				.createDataCell();
	}

	@Override
	BitVectorValue doOr(BitVectorValue first, BitVectorValue second) {
		return SparseBitVectorCellFactory.or(first, second);
	}

	@Override
	BitVectorValue createReferenceOr(String first, String second) {
		return new SparseBitVectorCellFactory(
				new SparseBitVector(first).or(new SparseBitVector(second)))
				.createDataCell();
	}

	@Override
	BitVectorValue doXor(BitVectorValue first, BitVectorValue second) {
		return SparseBitVectorCellFactory.xor(first, second);
	}

	@Override
	BitVectorValue createReferenceXor(String first, String second) {
		return new SparseBitVectorCellFactory(
				new SparseBitVector(first).xor(new SparseBitVector(second)).toHexString())
				.createDataCell();
	}
	
	@Override
	BitVectorValue createBitVector(String content) {
		return new SparseBitVectorCellFactory(
				new SparseBitVector(content)).createDataCell();
	}
}

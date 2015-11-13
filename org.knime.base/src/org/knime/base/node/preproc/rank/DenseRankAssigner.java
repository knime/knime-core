package org.knime.base.node.preproc.rank;

import org.knime.core.data.DataRow;

/**
* Assigns ranks in dense mode,
* that rows with the same values receive the same rank,
* the next different row receives a rank increased by 1
*
* @author Adrian Nembach, KNIME GmbH Konstanz
*/
class DenseRankAssigner implements RankAssigner {
	private long m_currRank;
	private DataCellTuple m_currVals;
    private int[] m_rankColIndices;

	public DenseRankAssigner(final int[] rankColIndices) {
		m_rankColIndices = rankColIndices;
		m_currVals = new DataCellTuple(m_rankColIndices);
		m_currRank = 0;
	}

	@Override
    public long getRank(final DataRow row) {
	    DataCellTuple rowVals = new DataCellTuple(row, m_rankColIndices);
		if (!rowVals.equals(m_currVals)) {
			m_currRank++;
			m_currVals = rowVals;
		}

		return m_currRank;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_currVals = new DataCellTuple(m_rankColIndices);
        m_currRank = 0;
    }

}

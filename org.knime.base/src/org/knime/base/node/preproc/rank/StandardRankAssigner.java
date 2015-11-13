package org.knime.base.node.preproc.rank;

import org.knime.core.data.DataRow;

/**
* This assigns ranks in the standard mode,
* that means rows with the same values in the ranking columns receive the same rank
* but the next row with different columns receives a rank increased by the number of
* the rows that had the same value before
*
* @author Adrian Nembach, KNIME GmbH Konstanz
*/
class StandardRankAssigner implements RankAssigner {

	private long m_currRank;
	private long m_rankCounter;
	private DataCellTuple m_currVals;
    private int[] m_rankColIndices;

	public StandardRankAssigner(final int[] rankColIndices) {
		m_rankColIndices = rankColIndices;
		m_currVals = new DataCellTuple(m_rankColIndices);
		m_currRank = 0;
		m_rankCounter = 0;
	}

	@Override
    public long getRank(final DataRow row) {
	    DataCellTuple rowVals = new DataCellTuple(row, m_rankColIndices);
		m_rankCounter++;
    	if (!rowVals.equals(m_currVals)) {
    		m_currRank = m_rankCounter;
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
        m_rankCounter = 0;
    }

}

package org.knime.base.node.preproc.rank;

import org.knime.core.data.DataRow;

/**
* Assigns ranks in ordinal mode,
* that means every row receives a unique rank
*
* @author Adrian Nembach, KNIME GmbH Konstanz
*/
class OrdinalRankAssigner implements RankAssigner {

	private long m_currRank;

	public OrdinalRankAssigner() {
		m_currRank = 0;
	}

	@Override
	public long getRank(final DataRow row) {
		return ++m_currRank;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_currRank = 0;
    }

}

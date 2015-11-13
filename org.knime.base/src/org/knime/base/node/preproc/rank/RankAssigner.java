package org.knime.base.node.preproc.rank;

import org.knime.core.data.DataRow;

/**
* Interface for RankAssigners that assign a rank to DataRows
*
* @author Adrian Nembach, KNIME GmbH Konstanz
*/
interface RankAssigner {

	public long getRank(DataRow row);

	public void reset();

}

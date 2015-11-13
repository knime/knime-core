/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.preproc.rank;

import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * This factory produces RankCells for the RankNode.
 * For this factory to function properly it must not be used in a concurrent setup.
 *
 * @author Adrian Nembach, KNIME GmbH Konstanz
 */
class RankCellFactory extends SingleCellFactory {

    private int[] m_groupColIndices;

    private int[] m_rankColIndices;

    private String m_rankMode;

    private boolean m_rankAsLong;

    private HashMap<DataCellTuple, RankAssigner> m_groupHashTable;

    public RankCellFactory(final DataColumnSpec newColSpec, final int[] groupColIndices, final int[] rankColIndices,
        final String rankMode, final boolean rankAsLong, final int initialHashtableCapacity) {
        super(newColSpec);
        m_groupColIndices = groupColIndices;
        m_rankColIndices = rankColIndices;
        m_rankMode = rankMode;
        m_rankAsLong = rankAsLong;
        m_groupHashTable = new HashMap<DataCellTuple, RankAssigner>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        // create group identification
        DataCellTuple rowVals = new DataCellTuple(row, m_groupColIndices);
        // get RankAssigner for corresponding group
        RankAssigner rankAssigner = m_groupHashTable.get(rowVals);
        DataCell rankCell = null;
        // check if RankAssigner is registered for group
        if (rankAssigner == null) {
            // create new RankAssigner and register it for this new group
            rankAssigner = createRankAssigner(m_rankMode, m_rankColIndices);
            m_groupHashTable.put(rowVals, rankAssigner);

            // create RankCell
            if (m_rankAsLong) {
                rankCell = new LongCell(rankAssigner.getRank(row));
            } else {
                rankCell = new IntCell((int)rankAssigner.getRank(row));
            }
        } else {
            // create RankCell
            if (m_rankAsLong) {
                rankCell = new LongCell(rankAssigner.getRank(row));
            } else {
                rankCell = new IntCell((int) rankAssigner.getRank(row));
            }
        }
        return rankCell;
    }

    private RankAssigner createRankAssigner(final String rankMode, final int[] rankColIndices) {
        RankAssigner rankAssigner = null;

        // Create corresponding RankAssigner or throw an exception if there is no such mode
        switch (rankMode) {
            case "Standard":
                rankAssigner = new StandardRankAssigner(rankColIndices);
                break;
            case "Dense":
                rankAssigner = new DenseRankAssigner(rankColIndices);
                break;
            case "Ordinal":
                rankAssigner = new OrdinalRankAssigner();
                break;
            default:
                throw new IllegalArgumentException("The rank mode \"" + rankMode + "\" does not exist.");
        }
        return rankAssigner;

    }

}

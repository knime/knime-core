/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   29.03.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships.test;

import java.util.BitSet;

import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;

/**
 * This class provides access to the index positions of the records in the current branch
 * for the respective column (defined by the provided column index)
 *
 * @author Adrian Nembach, KNIME.com
 */
public class ColMem implements ColumnMemberships {

    /**
     * Contains the indices for the current node in column order
     */
    private final BitSet m_sorted;

    private final IDataIndexManager m_indexManager;

    private final RootDataMem m_root;

    private final int m_colIdx;

    private int m_iterator = 0;



    ColMem(final BitSet sorted, final IDataIndexManager indexManager, final RootDataMem root, final int colIdx) {
        m_sorted = sorted;
        m_indexManager = indexManager;
        m_root = root;
        m_colIdx = colIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return m_sorted.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() {
        if (m_iterator == -1) {
            m_iterator = m_sorted.nextSetBit(0);
        } else {
            m_iterator = m_sorted.nextSetBit(m_iterator + 1);
        }
        return m_iterator != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextIndexFrom(final int indexInColumn) {
        m_iterator = m_sorted.nextSetBit(indexInColumn);
        return m_iterator != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight() {
        return m_root.getRowWeight(m_indexManager.getOriginalPosition(m_colIdx, m_iterator));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex() {
        return m_indexManager.getOriginalPosition(m_colIdx, m_iterator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInColumn() {
        return m_iterator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInDataMemberships() {
        // in this case original index and index in data memberships are the same
        return getOriginalIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_iterator = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goToLast() {
        m_iterator = m_sorted.length() - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() {
        m_iterator = m_sorted.previousSetBit(m_iterator - 1);
        return m_iterator != -1;
    }

}

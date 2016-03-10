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
 *   11.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import java.util.BitSet;

/**
 *
 * @author Adrian Nembach
 */
public class DescendantColumnMemberships implements ColumnMemberships {

    private final IntArrayColumnMemberships m_root;
    private final BitSet m_includedIndices;

    private int m_internalIndex = -1;

    public DescendantColumnMemberships(final IntArrayColumnMemberships root, final BitSet includedIndices) {
        m_root = root;
        m_includedIndices = includedIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return m_includedIndices.cardinality();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() {
        int nextSetIndex = m_includedIndices.nextSetBit(m_internalIndex + 1);
        if (nextSetIndex < 0) {
            return false;
        }
        m_internalIndex = nextSetIndex;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextIndexFrom(final int indexInColumn) {
        int startIndex = m_internalIndex < 0 ? 0 : m_internalIndex;
        for (int i = m_includedIndices.nextSetBit(startIndex); i >= 0; i = m_includedIndices.nextSetBit(i+1)) {
            if (m_root.descendantGetIndexInColumn(i) >= indexInColumn) {
                m_internalIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight() {
        // TODO Auto-generated method stub
        return m_root.descendantGetRowWeight(m_internalIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex() {
        // TODO Auto-generated method stub
        return m_root.descendantGetIndexInOriginal(m_internalIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInColumn() {
        // TODO Auto-generated method stub
        return m_root.descendantGetIndexInColumn(m_internalIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInDataMemberships() {
        // TODO Auto-generated method stub
        return m_root.descendantGetIndexInDataMemberships(m_internalIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_internalIndex = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goToLast() {
        m_internalIndex = m_includedIndices.length() - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() {
        if (m_internalIndex > 0) {
            m_internalIndex--;
            return true;
        }
        m_internalIndex = -1;
        return false;
    }

}

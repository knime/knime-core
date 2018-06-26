/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.04.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.Arrays;
import java.util.BitSet;

import org.knime.base.node.mine.treeensemble2.data.memberships.ColumnMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;

/**
 * This class acts as both DataMemberships and ColumnMemberships to allow the testing of the different column types
 * without having to rely on the actual DataMemberships (which could contain bugs of their own).
 * <br> <br>
 * This class exists for the sole purpose of testing, and should not be used for anything else
 *
 * @author Adrian Nembach, KNIME.com
 */
class MockDataColMem implements ColumnMemberships, DataMemberships {

    private final int[] m_originalIndices;
    private final int[] m_columnIndices;
    private final double[] m_weights;

    private int m_iterator;

    public MockDataColMem(final int[] originalIndices, final int[] columnIndices, final double[] weights) {
        m_originalIndices = originalIndices;
        m_columnIndices = columnIndices;
        m_weights = weights;
        m_iterator = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getRowWeights() {
        return m_weights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnMemberships getColumnMemberships(final int index) {
        if (index == 0) {
            return this;
        }
        throw new IllegalArgumentException("This class is only intended for the use with a single column");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndices() {
        return m_originalIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataMemberships createChildMemberships(final BitSet inChild) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight(final int index) {
        return m_weights[index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex(final int index) {
        return m_originalIndices[index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_originalIndices.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCountInRoot() {
        return getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() {
        if (m_iterator + 1 < m_originalIndices.length) {
            m_iterator++;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextIndexFrom(final int indexInColumn) {
        final int idx = Arrays.binarySearch(m_columnIndices, indexInColumn);
        if (idx < m_columnIndices.length) {
            m_iterator = idx;
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight() {
        return getRowWeight(m_iterator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex() {
        return getOriginalIndex(m_iterator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInColumn() {
        return m_columnIndices[m_iterator];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInDataMemberships() {
        return m_iterator;
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
        m_iterator = m_originalIndices.length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() {
        if (m_iterator - 1 >= 0) {
            m_iterator--;
            return true;
        }
        return false;
    }

}

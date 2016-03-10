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
 *   30.11.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

/**
 * Holds the indices of the rows included in the current sample and current branch for a column.
 *
 * @author Adrian Nembach
 */
public class IntArrayColumnMemberships implements ColumnMemberships {

    // This class has an internal index that is not necessarily the same as the internal index in the corresponding
    // DataMemberships object!

    private int[] m_indexInColumn;

    private int[] m_indexInDataMemberships;

    private DataMemberships m_dataMemberships;

    private int m_internalIndex = -1;

    /**
     * @param indexInColumn The indices of the contained instances in the column
     * @param indexInDataMemberships The indices of the contained instances in <b>dataMemberships</b>
     * @param dataMemberships The DataMemberships object that holds this ColumnMemberships object.
     */
    public IntArrayColumnMemberships(final int[] indexInColumn, final int[] indexInDataMemberships,
        final DataMemberships dataMemberships) {
        m_indexInColumn = indexInColumn;
        m_dataMemberships = dataMemberships;
        m_indexInDataMemberships = indexInDataMemberships;
    }

    int descendantGetIndexInColumn(final int internalIndex) {
        return m_indexInColumn[internalIndex];
    }

    double descendantGetRowWeight(final int internalIndex) {
        return m_dataMemberships.getRowWeight(m_indexInDataMemberships[internalIndex]);
    }

    int descendantGetIndexInDataMemberships(final int internalIndex) {
        return m_indexInDataMemberships[internalIndex];
    }

    int descendantGetIndexInOriginal(final int internalIndex) {
        return m_dataMemberships.getOriginalIndex(m_indexInDataMemberships[internalIndex]);
    }

    int getIndexInColumn(final int dataMembershipsIndex) {
        return m_indexInColumn[dataMembershipsIndex];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() {
        if (m_indexInColumn.length > 0 && m_internalIndex < m_indexInColumn.length - 1) {
            m_internalIndex++;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextIndexFrom(final int indexInColumn) {
        final int startIndex = m_internalIndex < 0 ? 0 : m_internalIndex;
        for (int i = startIndex; i < m_indexInColumn.length; i++) {
            if (m_indexInColumn[i] >= indexInColumn) {
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
        if (m_internalIndex < 0) {
            throw new IllegalStateException(
                "DataMemberships#next() must be called at least once before this Method is called.");
        }
        return m_dataMemberships.getRowWeight(m_indexInDataMemberships[m_internalIndex]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex() {
        if (m_internalIndex < 0) {
            throw new IllegalStateException(
                "DataMemberships#next() must be called at least once before this Method is called.");
        }
        return m_dataMemberships.getOriginalIndex(m_indexInDataMemberships[m_internalIndex]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInColumn() {
        if (m_internalIndex < 0) {
            throw new IllegalStateException(
                "DataMemberships#next() must be called at least once before this Method is called.");
        }
        return m_indexInColumn[m_internalIndex];
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
    public int size() {
        return m_indexInColumn.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndexInDataMemberships() {
        return m_indexInDataMemberships[m_internalIndex];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goToLast() {
        m_internalIndex = m_indexInColumn.length - 1;
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

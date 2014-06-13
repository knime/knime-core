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
 *   04.06.2014 (koetter): created
 */
package org.knime.base.data.bitvector;

import org.knime.base.node.preproc.filter.row.rowfilter.StringCompareRowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.vector.bitvector.BitVectorType;

/**
 *
 * @author Tobias Koetter
 * @since 2.10
 */
public class MultiString2BitVectorCellFactory extends BitVectorCellFactory {

    private StringCompareRowFilter m_filter;
    private final int[] m_colIndices;
    private final BitVectorType m_type;
    private int m_numberOfSetBits;
    private int m_numberOfNotSetBits;
    private final boolean m_setMatching;

    /**
     * @param type the {@link BitVectorType} to use
     * @param colSpec the column spec of the column containing the bitvectors
     * @param strPattern the pattern that is matched against the string
     *            representation of the data cell
     * @param caseSensitive if true a case sensitive match is performed,
     *            otherwise characters of different case match, too.
     * @param hasWildcards if true, '*' and '?' is interpreted as wildcard
     *            matching any character sequence or any character respectively.
     *            If false, '*' and '?' are treated as regular characters and
     *            match '*' and '?' in the value.
     * @param isRegExpr if true, the pattern argument is treated as regular
     *            expression. Can't be true when the hasWildcard argument is
     *            true
     * @param setMatching <code>true</code> if the bit for matching columns should be set otherwise the bits for not
     * matching columns are set
     * @param colIndices the indices of the columns to include
     */
    public MultiString2BitVectorCellFactory(final BitVectorType type, final DataColumnSpec colSpec,
        final boolean caseSensitive, final boolean hasWildcards, final boolean isRegExpr, final boolean setMatching,
        final String strPattern, final int[] colIndices) {
        super(colSpec);
        m_type = type;
        m_setMatching = setMatching;
        m_colIndices = colIndices;
        m_filter =
                new StringCompareRowFilter(strPattern, "", true, false, caseSensitive, hasWildcards, isRegExpr);
        m_numberOfSetBits = 0;
        m_numberOfNotSetBits = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSetBits() {
        return m_numberOfSetBits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfNotSetBits() {
        return m_numberOfNotSetBits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasSuccessful() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLastErrorMessage() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        incrementNrOfRows();
        org.knime.core.data.vector.bitvector.BitVectorCellFactory<? extends DataCell> factory =
                m_type.getCellFactory(m_colIndices.length);
        int bitIdx = 0;
        for (int idx : m_colIndices) {
            final DataCell cell = row.getCell(idx);
            final boolean set;
            if (cell.isMissing()) {
                set = false;
            } else {
                set = m_setMatching == m_filter.matches(cell);
            }
            if (set) {
                factory.set(bitIdx);
                m_numberOfSetBits++;
            } else {
                m_numberOfNotSetBits++;
            }
            bitIdx++;
        }
        return factory.createDataCell();
    }

}

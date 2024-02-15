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
 *   15 Feb 2024 (pietzsch): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.DataRowRowRead;
import org.knime.core.data.v2.RowRead;

/**
 * A {@link RowRead} that forwards to a {@link DataRow}.
 * <p>
 * The underlying {@code DataRow} can be changed via {@link #setDataRow}.
 *
 * @since 5.4
 * @author Tobias Pietzsch
 */
public final class DataRowRead implements RowRead, DataRowRowRead {

    private DataRow m_row;

    /**
     * Let this {@code RowRead} delegate to the given {@code row}.
     *
     * @param row the row to delegate to
     * @return {@code this}, for convenience
     */
    public DataRowRead setDelegate(final DataRow row) {
        m_row = row;
        return this;
    }

    @Override
    public DataRow materializeDataRow() {
        return m_row;
    }

    @Override
    public int getNumColumns() {
        return m_row.getNumCells();
    }

    @Override
    public RowKeyValue getRowKey() {
        return m_row.getKey();
    }

    @Override
    public <D extends DataValue> D getValue(final int index) {
        @SuppressWarnings("unchecked")
        final D cell = (D)m_row.getCell(index);
        return cell;
    }

    @Override
    public boolean isMissing(final int index) {
        return m_row.getCell(index).isMissing();
    }
}

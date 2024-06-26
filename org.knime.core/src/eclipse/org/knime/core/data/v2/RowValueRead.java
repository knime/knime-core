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
 *   Sep 10, 2020 (dietzc): created
 */
package org.knime.core.data.v2;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 * Read access to a row.
 *
 * @author Christian Dietz
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface RowValueRead {

    /**
     * @return number of columns.
     */
    int getNumColumns();

    /**
     * Get a {@link DataValue} at a given position.
     * <b>NOTE:</b> The DataValues are transient and may return a different value if the {@link RowCursor} producing
     * this RowValueRead is {@link RowCursor#forward() forwarded}. If you need to store the underlying value, access them
     * via the corresponding access methods that the concrete DataValue interface provides.
     *
     * @param <D> type of the {@link DataValue}
     * @param index the column index
     *
     * @return the {@link DataValue} at column index or <source>null</source> if {@link DataValue} is not available, for
     *         example if the column has been filtered out. In case {@link #isMissing(int)} returns
     *         <source>true</source> value of returned {@link DataValue} is not deterministic.
     */
    <D extends DataValue> D getValue(int index);

    /**
     * If <code>true</code>, calls to {@link #getValue(int)} are non-deterministic.
     *
     * @param index column index
     * @return <code>true</code> if value at index is missing
     */
    boolean isMissing(int index);

    /**
     * Materializes the value in the column with the given index as {@link DataCell} via
     * {@link DataValue#materializeDataCell()}.  If the value is missing, {@link DataType#getMissingCell()} is returned.
     *
     * @param index column index
     * @return materialized data cell
     * @since 5.3
     */
    default DataCell getAsDataCell(final int index) {
        return isMissing(index) ? DataType.getMissingCell() : getValue(index).materializeDataCell();
    }
}

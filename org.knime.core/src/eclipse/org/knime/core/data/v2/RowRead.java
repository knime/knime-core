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

import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.DefaultRow;

/**
 * Read access to a data row.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface RowRead extends RowValueRead {

    /**
     * Adapter factory method to create a {@link RowRead} which can be supplied by a sequence of {@link DataRow}s
     * which are made available through the given supplier.
     *
     * @param currentRowSupplier supplier for the current {@link DataRow}
     * @param numColumns number of columns of the rows
     * @return the created {@link RowRead}
     * @since 5.3
     */
    static RowRead suppliedBy(final Supplier<DataRow> currentRowSupplier, final int numColumns) {
        return new RowRead() { // NOSONAR

            @Override
            public int getNumColumns() {
                return numColumns;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <D extends DataValue> D getValue(final int index) {
                final var cell = getAsDataCell(index);
                return cell.isMissing() ? null : (D)cell;
            }

            @Override
            public boolean isMissing(final int index) {
                return getAsDataCell(index).isMissing();
            }

            @Override
            public RowKeyValue getRowKey() {
                return materializeDataRow().getKey();
            }

            @Override
            public DataCell getAsDataCell(final int index) {
                return materializeDataRow().getCell(index);
            }

            @Override
            public DataRow materializeDataRow() {
                return currentRowSupplier.get();
            }
        };
    }

    /**
     * Adapter factory method to create a {@link RowRead} representing a single {@link DataRow}.
     *
     * @param row the row to be adapted, may be {@code null}
     * @return the created {@link RowRead}, {@code null} if the given row is {@code null}
     * @since 5.3
     */
    static RowRead from(final DataRow row) {
        return row == null ? null : suppliedBy(() -> row, row.getNumCells());
    }

    /**
     * @return the {@link RowKeyReadValue}
     */
    RowKeyValue getRowKey();

    /**
     * Returns an immutable {@link DataRow} containing the contents of the current state of this {@link RowRead}.
     *
     * @return the immutable data row
     * @since 5.3
     */
    default DataRow materializeDataRow() {
        return new DefaultRow(getRowKey().getString(), IntStream.range(0, getNumColumns()) //
            .mapToObj(this::getAsDataCell) //
            .toArray(DataCell[]::new));
    }
}

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
 */
package org.knime.core.data.container;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.UnmaterializedCell;
import org.knime.core.data.UnmaterializedCell.UnmaterializedDataCellException;

/**
 * Implementation of partly materialized {@link DataRow}s with supports for blobs. A partly materialized DataRow is a
 * row in which certain cells can be of type {@link UnmaterializedCell}, throwing an
 * {@link UnmaterializedDataCellException} when accessed.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public class PartlyMaterializedBlobSupportRow extends BlobSupportDataRow {

    /**
     * Initializes a new partly materialized data row with support for blob cells. The new data row has a {@link RowKey}
     * and an array of {@link DataCell}s. The content of the argument array is copied.
     *
     * @param key a row key containing a row id
     * @param cells an array containing the actual data of this row
     */
    public PartlyMaterializedBlobSupportRow(final RowKey key, final DataCell[] cells) {
        super(key, cells);
    }

    /**
     * Initializes a new partly materialized data row with support for blob cells. The new data row has a new
     * {@link RowKey} and an array of {@link DataCell}s. The {@link DataCell}s of the argument data row is copied.
     *
     * @param key a row key containing a row id
     * @param oldRow container of the cells for the new row
     */
    public PartlyMaterializedBlobSupportRow(final RowKey key, final DataRow oldRow) {
        super(key, oldRow);
    }

    @Override
    public DataCell getCell(final int index) {
        final DataCell cell = super.getCell(index);
        if (cell instanceof UnmaterializedCell) {
            throw new UnmaterializedDataCellException();
        }
        return cell;
    }

    @Override
    public DataCell getRawCell(final int index) {
        final DataCell cell = super.getRawCell(index);
        if (cell instanceof UnmaterializedCell) {
            throw new UnmaterializedDataCellException();
        }
        return cell;
    }

    /**
     * Returns the {@link DataCell} at the provided index within this row. Returns the wrapper cell (if any). Won't
     * throw an exception if the cell is of type {@link UnmaterializedCell}.
     *
     * @param index the index of the cell to retrieve (indices start from 0)
     * @return the {@link DataCell} at the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public DataCell getRawCellUnsafe(final int index) {
        return super.getCell(index);
    }

}

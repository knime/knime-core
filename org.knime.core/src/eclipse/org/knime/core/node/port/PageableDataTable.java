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
package org.knime.core.node.port;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;

/**
 * A port objects that represents a data table where ranges of rows can be requested ('pageable'). Implementations must
 * also be able to provide a {@link DataTableSpec} - see {@link #getSpec()}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 3.8
 * @noimplement This interface is not intended to be implemented by clients. Pending API!
 * @noreference This interface is not intended to be referenced by clients. Pending API!
 */
public interface PageableDataTable extends DataTable, PortObject {

    /**
     * {@inheritDoc}
     */
    @Override
    default DataTableSpec getDataTableSpec() {
        return getSpec().getDataTableSpec();
    }

    /**
     * Creates a 'bounded' iterator with an offset and the number of rows to iterate. If
     * <code>(from + count) > totalRowCount</code> the returned iterator will iterate till the table end.
     *
     * @param from the index of the row to initialize the iterator with (minimum 0)
     * @param count the number of rows to iterate
     * @return the closable bounded iterator
     */
    CloseableRowIterator iterator(long from, long count);

    /**
     * Calculates the overall number of rows in the data table. Should not be called multiple times if not necessary
     * (i.e. cache the result).
     *
     * @return the total number of rows
     * @throws UnknownRowCountException if the number of rows could not be determined
     */
    long calcTotalRowCount() throws UnknownRowCountException;

    /**
     * {@inheritDoc}
     */
    @Override
    default RowIterator iterator() {
        long size;
        try {
            size = calcTotalRowCount();
        } catch (UnknownRowCountException ex) {
            size = Long.MAX_VALUE;
        }
        return iterator(0, size);
    }

    /**
     * {@inheritDoc}
     *
     * Narrows the return type to {@link HasDataTableSpec}.
     */
    @Override
    HasDataTableSpec getSpec();

    /**
     * Exception thrown when number of rows of a data table could not be determined.
     */
    public class UnknownRowCountException extends Exception {

        private static final long serialVersionUID = 2914638262455429737L;

        /**
         * see {@link Exception#Exception(String)}
         */
        public UnknownRowCountException(final String message) {
            super(message);
        }

        /**
         * see {@link Exception#Exception(String, Throwable)}
         */
        public UnknownRowCountException(final String message, final Throwable cause) {
            super(message, cause);
        }

    }
}

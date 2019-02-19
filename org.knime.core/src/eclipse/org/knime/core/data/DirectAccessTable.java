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
 *   18 Feb 2019 (albrecht): created
 */
package org.knime.core.data;

import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * General data interface in table structure with a fixed number of columns and random access to chunks of rows.
 *
 * <p>
 * Each <code>DirectAccessTable</code> is a read-only container of {@link DataRow} elements which are returned by the
 * {@link #getRows(long, int)} method. Each row must have the same number of {@link DataCell} elements (columns),
 * is read-only, and must provide a unique row identifier. A table also contains a {@link DataTableSpec} member
 * which provides information about the structure of the table. The {@link DataTableSpec} consists of
 * {@link DataColumnSpec}s which contain information about the column, e.g. name, type, and possible values etc.
 *
 * @author Christian Albrecht, Martin Horn, Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 *
 * @see DataTable
 * @see DataCell
 * @see DataRow
 *
 * @noreference This interface is not intended to be referenced by clients. Pending API
 * @noextend This interface is not intended to be extended by clients. Pending API
 * @noimplement This interface is not intended to be implemented by clients. Pending API
 */
public interface DirectAccessTable {

    /**
     * Returns the {@link DataTableSpec} object of this table which contains information about the structure of
     * this table.
     *
     * @return the DataTableSpec of this table
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns a contiguous list of {@link DataRow}s given a start index and a maximum length.
     *
     * @param start the start index of the rows to return from this table (inclusive)
     * @param length the maximum number of rows to return
     * @param exec an {@link ExecutionMonitor} to set progress and check for cancellation, can be null if no execution
     * monitor is available.
     * @return list of requested {@link DataRow}s. {@link List#size()} might return a smaller number than <br>
     * {@code length} if {@code (start + length > getRowCount())}. If {@code (start > tableSize)} the list will
     * be empty.
     *
     * @throws IndexOutOfBoundsException if {@code (start < 0 || length < 0 || start + length < 0)}
     * @throws CanceledExecutionException if execution was cancelled
     * @see DataRow
     */
    List<DataRow> getRows(long start, int length, ExecutionMonitor exec)
            throws IndexOutOfBoundsException, CanceledExecutionException;

    /**
     * Returns the number of rows held by this table, if this number is applicable.
     *
     * @return the number of rows
     * @throws UnknownRowCountException if the row count is not or not yet known
     */
    long getRowCount() throws UnknownRowCountException;

    /**
     * Exception which indicates that a table-like structure was queried for its row count, but this number is unknown
     * or not yet known to the callee.
     *
     * @author Christian Albrecht, Martin Horn, KNIME GmbH, Konstanz, Germany
     */
    public static class UnknownRowCountException extends Exception {

        private static final long serialVersionUID = -343712216070058553L;

        /**
         * Constructs a new {@link UnknownRowCountException} with {@code null} as its detail message.
         */
        public UnknownRowCountException() {
            super();
        }

        /**
         * Constructs a new {@link UnknownRowCountException} with the specified detail message.
         *
         * @param   message   the detail message. The detail message is saved for
         *          later retrieval by the {@link #getMessage()} method.
         */
        public UnknownRowCountException(final String message) {
            super(message);
        }

        /**
         * Constructs a new {@link UnknownRowCountException} with the specified cause and a detail
         * message of {@code (cause==null ? null : cause.toString())} (which
         * typically contains the class and detail message of {@code cause}).
         * This constructor is useful for {@link UnknownRowCountException} that are little more than
         * wrappers for other exceptions.
         *
         * @param  cause the cause (which is saved for later retrieval by the
         *         {@link #getCause()} method).  (A {@code null} value is
         *         permitted, and indicates that the cause is nonexistent or
         *         unknown.)
         */
        public UnknownRowCountException(final Throwable cause) {
            super(cause);
        }

        /**
         * Constructs a new {@link UnknownRowCountException} with the specified detail message and
         * cause.  <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
         * incorporated in this exception's detail message.
         *
         * @param  message the detail message (which is saved for later retrieval
         *         by the {@link #getMessage()} method).
         * @param  cause the cause (which is saved for later retrieval by the
         *         {@link #getCause()} method).  (A {@code null} value is
         *         permitted, and indicates that the cause is nonexistent or
         *         unknown.)
         */
        public UnknownRowCountException(final String message, final Throwable cause) {
            super(message, cause);
        }

    }

}

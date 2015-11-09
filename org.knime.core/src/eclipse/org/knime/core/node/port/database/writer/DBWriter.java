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
 *   05.11.2015 (koetter): created
 */
package org.knime.core.node.port.database.writer;

import java.util.Map;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 *
 * @author Tobias Koetter, KNIME.com
 * @since 3.1
 */
public interface DBWriter {

    /** Create connection to write into database. Assumes that the table exists and only appends data to
     * the existing table.
     * @param schema optional db schema
     * @param table name of table to write
     * @param data The data to write.
     * @param rowCount number of row of the table to write, -1 if unknown
     * @param exec Used the cancel writing.
     * @param sqlTypes A mapping from column name to SQL-type.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows written in one batch
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    default String writeData(final String schema, final String table, final RowInput data, final long rowCount,
        final ExecutionMonitor exec, final CredentialsProvider cp, final int batchSize) throws Exception {
        final String schemaTable = schema == null ? table : schema + "." + table;
        return writeData(schemaTable, data, rowCount, true, exec, null, cp, batchSize, false);
    }

    /** Create connection to write into database.
     * @param schema table schema name. Could be <code>null</code>.
     * @param table name of table to write
     * @param input the data table as as row input
     * @param rowCount number of row of the table to write, -1 if unknown
     * @param appendData if checked the data is appended to an existing table
     * @param exec Used the cancel writing.
     * @param sqlTypes A mapping from column name to SQL-type.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows written in one batch
     * @param insertNullForMissingCols <code>true</code> if <code>null</code> should be inserted for missing columns
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     * @deprecated use {@link #writeData(String, String, RowInput, long, ExecutionMonitor, CredentialsProvider, int)} instead
     */
    @Deprecated
    String writeData(String table, RowInput input, final long rowCount, boolean appendData, ExecutionMonitor exec,
        Map<String, String> sqlTypes, CredentialsProvider cp, int batchSize, boolean insertNullForMissingCols) throws Exception;

    /** Update rows in the given database table.
     * @param schema optional db schema
     * @param table name of table to write
     * @param data The data to write.
     * @param setColumns columns part of the SET clause
     * @param whereColumns columns part of the WHERE clause
     * @param updateStatus int array of length data#getRowCount; will be filled with
     *             update info from the database
     * @param exec Used the cancel writing.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows updated in one batch
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    String updateTable(final String schema, String table, BufferedDataTable data, String[] setColumns, String[] whereColumns,
        int[] updateStatus, ExecutionMonitor exec, CredentialsProvider cp, int batchSize)
            throws Exception;

    /** Delete rows from the given database table.
     * @param schema optional db schema
     * @param table name of table to write
     * @param data The data to write.
     * @param whereColumns columns part of the WHERE clause
     * @param deleteStatus int array of length data#getRowCount; will be filled with
     *             the number of rows effected
     * @param exec Used the cancel writing.
     * @param cp {@link CredentialsProvider} providing user/password
     * @param batchSize number of rows deleted in one batch
     * @return error string or null, if non
     * @throws Exception if connection could not be established
     */
    String deleteRows(final String schema, String table, BufferedDataTable data, String[] whereColumns, int[] deleteStatus,
        ExecutionMonitor exec, CredentialsProvider cp, int batchSize) throws Exception;

}
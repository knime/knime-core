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
 *   May 17, 2016 (budiyanto): created
 */
package org.knime.core.node.port.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ConnectionPendingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 *
 * @author Budi Yanto, KNIME.com
 * @since 3.2
 */
public class DatabaseHelper {

    private DatabaseConnectionSettings m_conn;

    /**
     * Creates a empty handle for a new connection.
     * @param conn a database connection object
     */
    protected DatabaseHelper(final DatabaseConnectionSettings conn) {
        if(conn == null) {
            throw new NullPointerException("conn must not be null");
        }
        m_conn = conn;
    }

    /**
     * @return the {@link DatabaseConnectionSettings}
     */
    protected DatabaseConnectionSettings getDatabaseConnectionSettings() {
        return m_conn;
    }

    /**
     * @param type
     * @param dbIdx
     * @param meta
     * @return the KNIME {@link DataType}
     * @throws SQLException
     */
    protected DataType getKNIMEType(final int type, final ResultSetMetaData meta, final int dbIdx) throws SQLException {
        DataType newType;
        switch (type) {
            // all types that can be interpreted as integer
            case Types.BIT:
            case Types.BOOLEAN:
                newType = BooleanCell.TYPE;
                break;
            // all types that can be interpreted as integer
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                newType = IntCell.TYPE;
                break;
             // all types that can be interpreted as long
            case Types.BIGINT:
                newType = LongCell.TYPE;
                break;
            // all types that can be interpreted as double
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.REAL:
                newType = DoubleCell.TYPE;
                break;
            // all types that can be interpreted as data-and-time
            case Types.TIME:
            case Types.DATE:
            case Types.TIMESTAMP:
                newType = DateAndTimeCell.TYPE;
                break;
            // all types that can be interpreted as binary object
            case Types.BLOB:
            case Types.BINARY:
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
                newType = BinaryObjectDataCell.TYPE;
                break;
            case Types.ARRAY:
                //by default we convert the array elements to string and return a list cell with string cells
                newType = ListCell.getCollectionType(StringCell.TYPE);
                break;
            // fallback string
            default:
                newType = StringCell.TYPE;
        }
        return newType;
    }

    /**
     * Creates <code>DataTableSpec</code> from the given <code>ResultSetMetaData</code>
     * @param meta the <code>ResultSetMetaData</code> used to create <code>DataTableSpec</code>
     * @return a {@link DataTableSpec} created from the given {@link ResultSetMetaData}
     * @throws SQLException
     */
    protected DataTableSpec createTableSpec(final ResultSetMetaData meta)
            throws SQLException {
        int cols = meta.getColumnCount();
        if (cols == 0) {
            return new DataTableSpec("database");
        }
        StatementManipulator manipulator = m_conn.getUtility().getStatementManipulator();
        DataTableSpec spec = null;
        for (int i = 0; i < cols; i++) {
            int dbIdx = i + 1;
            String name =  manipulator.unquoteColumn(meta.getColumnLabel(dbIdx));
            int type = meta.getColumnType(dbIdx);
            DataType newType = getKNIMEType(type, meta, dbIdx);
            if (spec == null) {
                spec = new DataTableSpec("database",
                        new DataColumnSpecCreator(name, newType).createSpec());
            } else {
                name = DataTableSpec.getUniqueColumnName(spec, name);
                spec = new DataTableSpec("database", spec,
                       new DataTableSpec(new DataColumnSpecCreator(
                               name, newType).createSpec()));
            }
        }
        return spec;
    }

    /**
     * @param conn {@link ConnectionPendingException}
     * @param table table name
     * @return the column type mapping
     * @throws SQLException
     */
    protected Map<Integer, Integer> getColumnTypes(final Connection conn, final String table) throws SQLException {
        Map<Integer, Integer> columnTypes = new HashMap<>();
        try (ResultSet metaDataRs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, null)) {
            while (metaDataRs.next()) {
                columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
            }
        }

        if (columnTypes.isEmpty()) {
            // e.g. PostgreSQL converts all table names to lower case by default
            try (ResultSet metaDataRs =
                conn.getMetaData().getColumns(conn.getCatalog(), null, table.toLowerCase(), null)) {
                while (metaDataRs.next()) {
                    columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
                }
            }
        }
        if (columnTypes.isEmpty()) {
            // e.g. Oracle converts all table names to upper case by default
            try (ResultSet metaDataRs =
                conn.getMetaData().getColumns(conn.getCatalog(), null, table.toUpperCase(), null)) {
                while (metaDataRs.next()) {
                    columnTypes.put(metaDataRs.getInt("ORDINAL_POSITION"), metaDataRs.getInt("DATA_TYPE"));
                }
            }
        }
        return columnTypes;
    }

    /**
     * Set given column value into SQL statement.
     * @param stmt statement used
     * @param dbIdx database index to update/write
     * @param cspec column spec to check type
     * @param cell the data cell to write into the statement
     * @param tz the {@link TimeZone} to use
     * @param columnTypes
     * @throws SQLException if the value can't be set
     */
    protected void fillStatement(final PreparedStatement stmt, final int dbIdx, final DataColumnSpec cspec,
        final DataCell cell, final TimeZone tz, final Map<Integer, Integer> columnTypes)
            throws SQLException {
        if (cspec.getType().isCompatible(BooleanValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BOOLEAN);
            } else {
                boolean bool = ((BooleanValue) cell).getBooleanValue();
                stmt.setBoolean(dbIdx, bool);
            }
        } else if (cspec.getType().isCompatible(IntValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.INTEGER);
            } else {
                int integer = ((IntValue) cell).getIntValue();
                stmt.setInt(dbIdx, integer);
            }
        } else if (cspec.getType().isCompatible(LongValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BIGINT);
            } else {
                long dbl = ((LongValue) cell).getLongValue();
                stmt.setLong(dbIdx, dbl);
            }
        } else if (cspec.getType().isCompatible(DoubleValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.DOUBLE);
            } else {
                double dbl = ((DoubleValue) cell).getDoubleValue();
                if (Double.isNaN(dbl)) {
                    stmt.setNull(dbIdx, Types.DOUBLE);
                } else {
                    stmt.setDouble(dbIdx, dbl);
                }
            }
        } else if (cspec.getType().isCompatible(DateAndTimeValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.DATE);
            } else {
                final DateAndTimeValue dateCell = (DateAndTimeValue) cell;
                final long corrDate = dateCell.getUTCTimeInMillis() - tz.getOffset(dateCell.getUTCTimeInMillis());
                if (!dateCell.hasTime() && !dateCell.hasMillis()) {
                    java.sql.Date date = new java.sql.Date(corrDate);
                    stmt.setDate(dbIdx, date);
                } else if (!dateCell.hasDate()) {
                    java.sql.Time time = new java.sql.Time(corrDate);
                    stmt.setTime(dbIdx, time);
                } else {
                    java.sql.Timestamp timestamp = new java.sql.Timestamp(corrDate);
                    stmt.setTimestamp(dbIdx, timestamp);
                }
            }
        } else if (cspec.getType().isCompatible(BinaryObjectDataValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.BLOB);
            } else {
                try {
                    BinaryObjectDataValue value = (BinaryObjectDataValue) cell;
                    InputStream is = value.openInputStream();
                    if (is == null) {
                        stmt.setNull(dbIdx, Types.BLOB);
                    } else {
                        try {
                            // to be compatible with JDBC 3.0, the length of the stream is restricted to max integer,
                            // which are ~2GB; with JDBC 4.0 longs are supported and the respective method can be called
                            stmt.setBinaryStream(dbIdx, is, (int) value.length());
                        } catch (SQLException ex) {
                            // if no supported (i.e. SQLite) set byte array
                            byte[] bytes = IOUtils.toByteArray(is);
                            stmt.setBytes(dbIdx, bytes);
                        }
                    }
                } catch (IOException ioe) {
                    stmt.setNull(dbIdx, Types.BLOB);
                }
            }
        } else if (cspec.getType().isCompatible(CollectionDataValue.class)) {
            fillArray(stmt, dbIdx, cell, tz);
        } else if ((columnTypes == null) || cspec.getType().isCompatible(StringValue.class)) {
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, Types.VARCHAR);
            } else {
                stmt.setString(dbIdx, cell.toString());
            }
        } else {
            Integer sqlType = columnTypes.get(dbIdx);
            if (sqlType == null) {
                sqlType = Types.VARCHAR;
            }
            if (cell.isMissing()) {
                stmt.setNull(dbIdx, sqlType);
            } else {
                stmt.setObject(dbIdx, cell.toString(), sqlType);
            }
        }
    }

    /**
     * @param stmt
     * @param dbIdx
     * @param cell
     * @param tz
     * @throws SQLException
     */
    protected void fillArray(final PreparedStatement stmt, final int dbIdx, final DataCell cell, final TimeZone tz)
        throws SQLException {
        if (cell.isMissing()) {
            stmt.setNull(dbIdx, Types.VARCHAR);
        } else {
            stmt.setString(dbIdx, cell.toString());
        }
    }

    /**
     * @param spec
     * @param sqlTypes
     * @param columnNamesForInsertStatement
     * @return the create table statement
     */
    protected String createTableStmt(final DataTableSpec spec,
            final Map<String, String> sqlTypes, final StringBuilder columnNamesForInsertStatement) {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i > 0) {
                buf.append(", ");
                columnNamesForInsertStatement.append(", ");
            }
            DataColumnSpec cspec = spec.getColumnSpec(i);
            String colName = cspec.getName();
            String column = replaceColumnName(colName);
            buf.append(column + " " + sqlTypes.get(colName));

            columnNamesForInsertStatement.append(column);
        }
        buf.append(")");
        columnNamesForInsertStatement.append(')');

        return buf.toString();
    }

    /**
     * @param oldName old column name
     * @return new column name
     */
    protected String replaceColumnName(final String oldName) {
        final String colName;
        if (!m_conn.getAllowSpacesInColumnNames()) {
            //TK_TODO: this might replace not only spaces!!!
            colName = oldName.replaceAll("[^a-zA-Z0-9]", "_");
        } else {
            colName = oldName;
        }
        //always call the quote method to also quote key words etc.
        return m_conn.getUtility().getStatementManipulator().quoteIdentifier(colName);
    }

    /**
     * @param whereColumns
     * @param setColumns
     * @param table
     * @return update statment
     */
    protected String createUpdateStatement(final String table, final String[] setColumns, final String[] whereColumns) {
     // create query connection object
        final StringBuilder query = new StringBuilder("UPDATE " + table + " SET");
        for (int i = 0; i < setColumns.length; i++) {
            if (i > 0) {
                query.append(",");
            }
            final String newColumnName = replaceColumnName(setColumns[i]);
            query.append(" " + newColumnName + " = ?");
        }
        query.append(" WHERE");
        for (int i = 0; i < whereColumns.length; i++) {
            if (i > 0) {
                query.append(" AND");
            }
            final String newColumnName = replaceColumnName(whereColumns[i]);
            query.append(" " + newColumnName + " = ?");
        }
        return query.toString();
    }

    /**
     * @param table
     * @param columnNames
     * @param mapping
     * @param insertNullForMissingCols
     * @return the insert statement
     */
    protected String createInsertStatment(final String table, final String columnNames, final int[] mapping,
        final boolean insertNullForMissingCols) {
        // // creates the wild card string based on the number of columns
        // this string it used every time an new row is inserted into the db
        final StringBuilder wildcard = new StringBuilder("(");
        boolean first = true;
        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] >= 0 || insertNullForMissingCols) {
                    //insert only a ? if the column is available in the input table or the insert null for missing
                    //columns option is enabled
                if (first) {
                    first = false;
                } else {
                    wildcard.append(", ");
                }
                wildcard.append("?");
            }
        }
        wildcard.append(")");
        // create table meta data with empty column information
        final String query = "INSERT INTO " + table + " " + columnNames + " VALUES " + wildcard;
        return query;
    }


}

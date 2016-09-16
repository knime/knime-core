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
 *   07.11.2015 (koetter): created
 */
package org.knime.core.node.port.database.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.util.FileUtil;

/**
 * RowIterator via a database ResultSet.
 * @since 3.1
 */
@SuppressWarnings("javadoc")
public class DBRowIteratorImpl extends RowIterator {

    protected final ResultSet m_result;

    private boolean m_hasExceptionReported = false;

    private long m_rowCounter = 0;

    /** FIXME: Some database (such as sqlite) do NOT support methods such as
     * <code>getAsciiStream</code> nor <code>getBinaryStream</code> and will fail with an
     * SQLException. To prevent this exception for each ResultSet's value,
     * this flag for each column indicated that this exception has been
     * thrown and we directly can access the value via <code>getString</code>.
     */
    private final boolean[] m_streamException;

    // fix for bug #4066
    final boolean m_rowIdsStartWithZero;
    // fix for bug #5991
    private final boolean m_useDbRowId;

    protected final DataTableSpec m_spec;

    protected final DatabaseConnectionSettings m_conn;

    protected final BinaryObjectCellFactory m_blobFactory;

    /**
     * @param spec {@link DataTableSpec}
     * @param conn {@link DatabaseConnectionSettings}
     * @param blobFactory {@link BinaryObjectCellFactory}
     * @param result {@link ResultSet}
     * @param useDbRowId <code>true</code> if the db row id should be used
     */
    protected DBRowIteratorImpl(final DataTableSpec spec, final DatabaseConnectionSettings conn,
        final BinaryObjectCellFactory blobFactory, final ResultSet result, final boolean useDbRowId) {
        this(spec, conn, blobFactory, result, useDbRowId, 0);
    }

    /**
     * @param spec {@link DataTableSpec}
     * @param conn {@link DatabaseConnectionSettings}
     * @param blobFactory {@link BinaryObjectCellFactory}
     * @param result {@link ResultSet}
     * @param useDbRowId <code>true</code> if the db row id should be used
     * @since 3.2
     */
    protected DBRowIteratorImpl(final DataTableSpec spec, final DatabaseConnectionSettings conn,
        final BinaryObjectCellFactory blobFactory, final ResultSet result, final boolean useDbRowId, final long startRowId) {
        m_spec = spec;
        m_conn = conn;
        m_blobFactory = blobFactory;
        m_result = result;
        m_streamException = new boolean[m_spec.getNumColumns()];
        m_rowIdsStartWithZero = m_conn.getRowIdsStartWithZero();
        m_useDbRowId = useDbRowId;
        m_rowCounter = startRowId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        boolean ret = false;
        try {
            ret = m_result.next();
        } catch (SQLException sql) {
            ret = false;
        }
        if (!ret) {
            try {
                m_result.close();
            } catch (SQLException ex) {
                Throwable cause = ExceptionUtils.getRootCause(ex);
                if (cause == null) {
                    cause = ex;
                }

                DBReaderImpl.LOGGER.error("SQL Exception while closing result set: " + ex.getMessage(), ex);
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        DataCell[] cells = new DataCell[m_spec.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            DataType type = m_spec.getColumnSpec(i).getType();
            int dbType = Types.NULL;
            final DataCell cell;
            try {
                dbType = m_result.getMetaData().getColumnType(i + 1);
                if (type.isCompatible(BooleanValue.class)) {
                    switch (dbType) {
                        // all types that can be interpreted as boolean
                        case Types.BIT:
                        case Types.BOOLEAN:
                            cell = readBoolean(i);
                            break;
                        default: cell = readBoolean(i);
                    }
                } else if (type.isCompatible(IntValue.class)) {
                    switch (dbType) {
                        // all types that can be interpreted as integer
                        case Types.TINYINT:
                            cell = readByte(i);
                            break;
                        case Types.SMALLINT:
                            cell = readShort(i);
                            break;
                        case Types.INTEGER:
                            cell = readInt(i);
                            break;
                        default: cell = readInt(i);
                    }
                } else if (type.isCompatible(LongValue.class)) {
                    switch (dbType) {
                        // all types that can be interpreted as long
                        case Types.BIGINT:
                            cell = readLong(i);
                            break;
                        default: cell = readLong(i);
                    }
                } else if (type.isCompatible(DoubleValue.class)) {
                    switch (dbType) {
                        // all types that can be interpreted as double
                        case Types.REAL:
                            cell = readFloat(i);
                            break;
                        default: cell = readDouble(i);
                    }
                } else if (type.isCompatible(DateAndTimeValue.class)) {
                    switch (dbType) {
                        case Types.DATE:
                            cell = readDate(i); break;
                        case Types.TIME:
                            cell = readTime(i); break;
                        case Types.TIMESTAMP:
                            cell = readTimestamp(i); break;
                        default: cell = readString(i);
                    }
                } else if (type.isCompatible(BinaryObjectDataValue.class)) {
                    switch (dbType) {
                        case Types.BLOB:
                            DataCell c = null;
                            try {
                                c = readBlob(i);
                            } catch (SQLException ex) {
                                // probably not supported (e.g. SQLite), therefore try another method
                                c = readBytesAsBLOB(i);
                            }
                            cell = c;
                            break;
                        case Types.LONGVARCHAR:
                        case Types.LONGNVARCHAR:
                            cell = readAsciiStream(i); break;
                        case Types.BINARY:
                        case Types.LONGVARBINARY:
                        case Types.VARBINARY:
                            cell = readBinaryStream(i); break;
                        default: cell = readString(i);
                    }
                } else {
                    switch (dbType) {
                        case Types.CLOB:
                            cell = readClob(i); break;
                        case Types.ARRAY:
                            cell = readArray(i); break;
                        case Types.CHAR:
                        case Types.VARCHAR:
                        case Types.LONGVARCHAR:
                            cell = readString(i); break;
                        case Types.VARBINARY:
                            cell = readBytesAsString(i); break;
                        case Types.REF:
                            cell = readRef(i); break;
                        case Types.NCHAR:
                        case Types.NVARCHAR:
                        case Types.LONGNVARCHAR:
                            cell = readNString(i); break;
                        case Types.NCLOB:
                            cell = readNClob(i); break;
                        case Types.DATALINK:
                            cell = readURL(i); break;
                        case Types.STRUCT:
                        case Types.JAVA_OBJECT:
                            cell = readObject(i); break;
                        default:
                            cell = readObject(i); break;

                    }
                }
                // finally set the new cell into the array of cells
                cells[i] = cell;
            } catch (SQLException sqle) {
                handlerException("SQL Exception reading Object of type \"" + dbType + "\": ", sqle);
                cells[i] = new MissingCell(sqle.getMessage());
            } catch (IOException ioe) {
                handlerException("I/O Exception reading Object of type \"" + dbType + "\": ", ioe);
                cells[i] = new MissingCell(ioe.getMessage());
            }
        }
        long rowId;
        try {
            rowId = m_result.getRow();
            // Bug 2729: ResultSet#getRow return 0 if there is no row id
            if (rowId <= 0 || !m_useDbRowId ) {
                // use row counter
                rowId = m_rowCounter;
            } else if (m_rowIdsStartWithZero) {
                rowId--; // first row in SQL always is 1, KNIME starts with 0
            }
        } catch (SQLException sqle) {
             // ignored: use m_rowCounter
            rowId = m_rowCounter;
        }
        m_rowCounter++;
        return new DefaultRow(RowKey.createRowKey(rowId), cells);
    }

    protected DataCell readClob(final int i)
            throws IOException, SQLException {
        Clob clob = m_result.getClob(i + 1);
        if (wasNull() || clob == null) {
            return DataType.getMissingCell();
        } else {
            try(Reader reader = clob.getCharacterStream();
                    StringWriter writer = new StringWriter();) {
                FileUtil.copy(reader, writer);
                reader.close();
                writer.close();
                return new StringCell(writer.toString());
            }
        }
    }

    protected DataCell readNClob(final int i)
            throws IOException, SQLException {
        NClob nclob = m_result.getNClob(i + 1);
        if (wasNull() || nclob == null) {
            return DataType.getMissingCell();
        } else {
            try(Reader reader = nclob.getCharacterStream();
                    StringWriter writer = new StringWriter();) {
                FileUtil.copy(reader, writer);
                reader.close();
                writer.close();
                return new StringCell(writer.toString());
            }
        }
    }

    protected DataCell readBlob(final int i) throws IOException, SQLException {
        if (m_streamException[i]) {
            return readString(i);
        }
        Blob blob = m_result.getBlob(i + 1);
        if (wasNull() || blob == null) {
            return DataType.getMissingCell();
        }
        try (InputStream is = blob.getBinaryStream();){
            if (wasNull() || is == null) {
                return DataType.getMissingCell();
            } else {
                return m_blobFactory.create(is);
            }
        } catch (SQLException sql) {
            m_streamException[i] = true;
            handlerException("Can't read from BLOB stream, trying to read string... ", sql);
            StringCell cell = (StringCell) readString(i);
            return m_blobFactory.create(cell.getStringValue().getBytes());
        }
    }

    protected DataCell readAsciiStream(final int i) throws IOException, SQLException {
        if (m_streamException[i]) {
            return readString(i);
        }
        try (InputStream is = m_result.getAsciiStream(i + 1);) {
            if (wasNull() || is == null) {
                return DataType.getMissingCell();
            } else {
                return m_blobFactory.create(is);
            }
        } catch (SQLException sql) {
            m_streamException[i] = true;
            handlerException("Can't read from ASCII stream, trying to read string... ", sql);
            StringCell cell = (StringCell) readString(i);
            return m_blobFactory.create(cell.getStringValue().getBytes());
        }
    }

    protected DataCell readBinaryStream(final int i) throws IOException, SQLException {
        if (m_streamException[i]) {
            return readString(i);
        }
        try (InputStream is = m_result.getBinaryStream(i + 1);) {
            if (wasNull() || is == null) {
                return DataType.getMissingCell();
            } else {
                return m_blobFactory.create(is);
            }
        } catch (SQLException sql) {
            m_streamException[i] = true;
            handlerException("Can't read from BINARY stream, trying to read string... ", sql);
            StringCell cell = (StringCell) readString(i);
            return m_blobFactory.create(cell.getStringValue().getBytes());
        }
    }

    protected DataCell readByte(final int i) throws SQLException {
        byte b = m_result.getByte(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new IntCell(b);
        }
    }

    protected DataCell readShort(final int i) throws SQLException {
        short s = m_result.getShort(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new IntCell(s);
        }
    }

    protected DataCell readInt(final int i) throws SQLException {
        int integer = m_result.getInt(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new IntCell(integer);
        }
    }

    protected DataCell readBoolean(final int i) throws SQLException {
        boolean b = m_result.getBoolean(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return (b ? BooleanCell.TRUE : BooleanCell.FALSE);
        }
    }

    protected DataCell readDouble(final int i) throws SQLException {
        double d = m_result.getDouble(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new DoubleCell(d);
        }
    }

    protected DataCell readFloat(final int i) throws SQLException {
        float f = m_result.getFloat(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new DoubleCell(f);
        }
    }

    protected DataCell readLong(final int i) throws SQLException {
        long l = m_result.getLong(i + 1);
        if (wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new LongCell(l);
        }
    }

    protected DataCell readString(final int i) throws SQLException {
        String s = m_result.getString(i + 1);
        if (wasNull() || s == null) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(s);
        }
    }

    protected DataCell readBytesAsBLOB(final int i) throws SQLException, IOException {
        byte[] bytes = m_result.getBytes(i + 1);
        if (wasNull() || bytes == null) {
            return DataType.getMissingCell();
        } else {
            return m_blobFactory.create(bytes);
        }
    }

    protected DataCell readBytesAsString(final int i) throws SQLException {
        byte[] bytes = m_result.getBytes(i + 1);
        if (wasNull() || bytes == null) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(new String(bytes));
        }
    }

    protected DataCell readBigDecimal(final int i) throws SQLException {
        BigDecimal bc = m_result.getBigDecimal(i + 1);
        if (wasNull() || bc == null) {
            return DataType.getMissingCell();
        } else {
            return new DoubleCell(bc.doubleValue());
        }
    }

    protected DataCell readNString(final int i) throws SQLException {
        String str = m_result.getNString(i + 1);
        if (wasNull() || str == null) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(str);
        }
    }

    protected DataCell readDate(final int i) throws SQLException {
        Date date = m_result.getDate(i + 1);
        if (wasNull() || date == null) {
            return DataType.getMissingCell();
        } else {
            return createDateCell(date, true, false, false);
        }
    }

    protected DataCell readTime(final int i) throws SQLException {
        Time time = m_result.getTime(i + 1);
        if (wasNull() || time == null) {
            return DataType.getMissingCell();
        } else {
            return createDateCell(time, false, true, true);
        }
    }

    protected DataCell readTimestamp(final int i) throws SQLException {
        Timestamp timestamp = m_result.getTimestamp(i + 1);
        if (wasNull() || timestamp == null) {
            return DataType.getMissingCell();
        } else {
            return createDateCell(timestamp, true, true, true);
        }
    }

    /**
     * @param date the date to convert
     * @param hasDate
     * @param hasTime
     * @param hasMillis
     * @return the {@link DateAndTimeCell}
     */
    protected DateAndTimeCell createDateCell(final java.util.Date date, final boolean hasDate,
        final boolean hasTime, final boolean hasMillis) {
        final long corrTime = date.getTime() + m_conn.getTimeZoneOffset(date.getTime());
        return new DateAndTimeCell(corrTime, hasDate, hasTime, hasMillis);
    }

    protected DataCell readArray(final int i) throws SQLException {
        final Array array = m_result.getArray(i + 1);
        if (wasNull() || array == null) {
            return DataType.getMissingCell();
        } else {
            final Object[] vals = (Object[])array.getArray();
            final Collection<DataCell>cells = new ArrayList<>(vals.length);
            for (Object val : vals) {
                cells.add(val == null ? DataType.getMissingCell() : new StringCell(val.toString()));
            }
            return CollectionCellFactory.createListCell(cells);
        }
    }

    protected DataCell readRef(final int i) throws SQLException {
        Ref ref = m_result.getRef(i + 1);
        if (wasNull() || ref == null) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(ref.getObject().toString());
        }
    }

    protected DataCell readURL(final int i) throws SQLException {
        URL url = m_result.getURL(i + 1);
        if (url == null || wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(url.toString());
        }
    }

    protected DataCell readObject(final int i) throws SQLException {
        Object o = m_result.getObject(i + 1);
        if (o == null || wasNull()) {
            return DataType.getMissingCell();
        } else {
            return new StringCell(o.toString());
        }
    }

    protected boolean wasNull() {
        try {
            return m_result.wasNull();
        } catch (SQLException sqle) {
            handlerException("SQL Exception: ", sqle);
            return true;
        }
    }

    protected void handlerException(final String msg, final Exception ex) {
        if (m_hasExceptionReported) {
            DBReaderImpl.LOGGER.debug(msg + ex.getMessage(), ex);
        } else {
            m_hasExceptionReported = true;
            Throwable cause = ExceptionUtils.getRootCause(ex);
            if (cause == null) {
                cause = ex;
            }

            DBReaderImpl.LOGGER.error(msg + cause.getMessage() + " - all further errors are suppressed "
                + "and reported on debug level only", ex);
        }
    }
}
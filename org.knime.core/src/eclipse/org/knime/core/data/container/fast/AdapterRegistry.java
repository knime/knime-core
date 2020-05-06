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
 *   May 9, 2020 (dietzc): created
 */
package org.knime.core.data.container.fast;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.table.column.ColumnType;
import org.knime.core.data.table.row.RowReadCursor;
import org.knime.core.data.table.row.RowWriteCursor;
import org.knime.core.data.table.type.DoubleType;
import org.knime.core.data.table.type.IntType;
import org.knime.core.data.table.type.StringType;
import org.knime.core.data.table.value.DoubleReadValue;
import org.knime.core.data.table.value.DoubleWriteValue;
import org.knime.core.data.table.value.IntReadValue;
import org.knime.core.data.table.value.IntWriteValue;
import org.knime.core.data.table.value.ReadValue;
import org.knime.core.data.table.value.StringReadValue;
import org.knime.core.data.table.value.StringWriteValue;
import org.knime.core.data.table.value.WriteValue;

/**
 * TODO
 *
 * @author Christian Dietz, KNIME GmbH
 */
class AdapterRegistry {

    private static final DataCell MISSING_CELL = DataType.getMissingCell();

    private static final Map<DataType, DataCellAdapter<?, ?>> ADAPTERS = new HashMap<>();

    // There is a 1-to-1 mapping between DataColumnSpec and ColumnType in FastTables for now.
    //
    static {
        ADAPTERS.put(DoubleCell.TYPE, new DoubleAdapter());
        ADAPTERS.put(IntCell.TYPE, new IntAdapter());
        ADAPTERS.put(StringCell.TYPE, new StringAdapter());
    }

    private AdapterRegistry() {
    }

    public static boolean hasAdapter(final DataType type) {
        return ADAPTERS.containsKey(type);
    }

    public static DataSpecAdapter createAdapter(final DataTableSpec spec, final boolean isRowKey) {
        final DataCellAdapter<?, ?>[] adapters = new DataCellAdapter[spec.getNumColumns() + (isRowKey ? 1 : 0)];
        final ColumnType<?, ?>[] columnTypes = new ColumnType[adapters.length];

        int offset = 0;
        if (isRowKey) {
            adapters[0] = ADAPTERS.get(StringCell.TYPE);
            columnTypes[0] = adapters[0].getColumnType();
            offset = 1;
        }

        for (int i = offset; i < adapters.length; i++) {
            adapters[i] = ADAPTERS.get(spec.getColumnSpec(i - offset).getType());
            columnTypes[i] = adapters[i].getColumnType();
        }

        return new DataSpecAdapter() {
            @Override
            public DataCellProducer[] createProducers(final RowReadCursor cursor) {
                final DataCellProducer[] producers = new DataCellProducer[adapters.length];
                for (int j = 0; j < adapters.length; j++) {
                    producers[j] = adapters[j].createProducer(cursor.get(j));
                }
                return producers;
            }

            @Override
            public DataCellConsumer[] createConsumers(final RowWriteCursor cursor) {
                final DataCellConsumer[] consumers = new DataCellConsumer[adapters.length];
                for (int j = 0; j < adapters.length; j++) {
                    consumers[j] = adapters[j].createConsumer(cursor.get(j));
                }
                return consumers;
            }

            @Override
            public ColumnType<?, ?>[] getColumnTypes() {
                return columnTypes;
            }

            @Override
            public boolean isRowKey() {
                return isRowKey;
            }
        };
    }

    interface DataSpecAdapter {

        DataCellProducer[] createProducers(RowReadCursor cursor);

        DataCellConsumer[] createConsumers(RowWriteCursor cursor);

        ColumnType<?, ?>[] getColumnTypes();

        boolean isRowKey();

    }

    // TODO introduce version?
    static interface DataCellAdapter<R extends ReadValue, W extends WriteValue> {

        DataCellConsumer createConsumer(W access);

        DataCellProducer createProducer(R access);

        ColumnType<?, ?> getColumnType();
    }

    static class IntAdapter implements DataCellAdapter<IntReadValue, IntWriteValue> {

        @Override
        public DataCellConsumer createConsumer(final IntWriteValue access) {
            return new IntWriteMapper(access);
        }

        @Override
        public DataCellProducer createProducer(final IntReadValue access) {
            return new IntReadMapper(access);
        }

        @Override
        public IntType getColumnType() {
            return IntType.INSTANCE;
        }

        private static final class IntWriteMapper implements DataCellConsumer {

            private final IntWriteValue m_access;

            private IntWriteMapper(final IntWriteValue access) {
                m_access = access;
            }

            @Override
            public void set(final DataCell cell) {
                if (cell.isMissing()) {
                    m_access.setMissing();
                } else {
                    m_access.setInt(((IntCell)cell).getIntValue());
                }
            }
        }

        private static final class IntReadMapper implements DataCellProducer {

            private final IntReadValue m_access;

            private IntReadMapper(final IntReadValue access) {
                m_access = access;
            }

            @Override
            public DataCell get() {
                if (m_access.isMissing()) {
                    return MISSING_CELL;
                }
                return new IntCell(m_access.getInt());
            }
        }
    }

    static class StringAdapter implements DataCellAdapter<StringReadValue, StringWriteValue> {

        @Override
        public DataCellConsumer createConsumer(final StringWriteValue access) {
            return new StringWriteMapper(access);
        }

        @Override
        public DataCellProducer createProducer(final StringReadValue access) {
            return new StringReadMapper(access);
        }

        @Override
        public StringType getColumnType() {
            return StringType.INSTANCE;
        }

        private static final class StringWriteMapper implements DataCellConsumer {

            private final StringWriteValue m_access;

            private StringWriteMapper(final StringWriteValue access) {
                m_access = access;
            }

            @Override
            public void set(final DataCell cell) {
                if (cell.isMissing()) {
                    m_access.setMissing();
                } else {
                    m_access.setStringValue(((StringCell)cell).getStringValue());
                }
            }
        }

        private static final class StringReadMapper implements DataCellProducer {

            private final StringReadValue m_access;

            private StringReadMapper(final StringReadValue access) {
                m_access = access;
            }

            @Override
            public DataCell get() {
                if (m_access.isMissing()) {
                    return MISSING_CELL;
                } else {
                    return new StringCell(m_access.getStringValue());
                }
            }
        }
    }

    static class DoubleAdapter implements DataCellAdapter<DoubleReadValue, DoubleWriteValue> {

        @Override
        public DataCellConsumer createConsumer(final DoubleWriteValue access) {
            return new DoubleWriteMapper(access);
        }

        @Override
        public DataCellProducer createProducer(final DoubleReadValue access) {
            return new DoubleReadMapper(access);
        }

        @Override
        public DoubleType getColumnType() {
            return DoubleType.INSTANCE;
        }

        private static final class DoubleWriteMapper implements DataCellConsumer {

            private final DoubleWriteValue m_access;

            private DoubleWriteMapper(final DoubleWriteValue access) {
                m_access = access;
            }

            @Override
            public void set(final DataCell cell) {
                if (cell.isMissing()) {
                    m_access.setMissing();
                } else {
                    m_access.setDouble(((DoubleCell)cell).getDoubleValue());
                }
            }
        }

        private static final class DoubleReadMapper implements DataCellProducer {

            private final DoubleReadValue m_access;

            private DoubleReadMapper(final DoubleReadValue access) {
                m_access = access;
            }

            @Override
            public DataCell get() {
                if (m_access.isMissing()) {
                    return MISSING_CELL;
                } else {
                    return new DoubleCell(m_access.getDouble());
                }
            }
        }
    }

}

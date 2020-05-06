package org.knime.core.data.container.fast;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.fast.AdapterRegistry.DataSpecAdapter;
import org.knime.core.data.table.ReadTable;
import org.knime.core.data.table.row.RowReadCursor;
import org.knime.core.data.table.value.StringReadValue;

class FastTableRowIterator extends CloseableRowIterator {

    private final RowReadCursor m_cursor;

    // TODO
    private final StringReadValue m_rowKeyValue;

    private final DataCellProducer[] m_suppliers;

    FastTableRowIterator(final ReadTable table, final DataSpecAdapter adapter, final boolean isRowKey) {
        m_cursor = table.newCursor();
        m_suppliers = adapter.createProducers(m_cursor);
        m_rowKeyValue = isRowKey ? m_cursor.get(0) : null;
    }

    @Override
    public boolean hasNext() {
        return m_cursor.canFwd();
    }

    @Override
    public DataRow next() {
        m_cursor.fwd();
        return new FastTableDataRow(m_rowKeyValue, m_suppliers);
    }

    @Override
    public void close() {
        try {
            m_cursor.close();
        } catch (Exception e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    static class FastTableDataRow implements DataRow {

        private StringReadValue m_rowKeyValue;

        private DataCellProducer[] m_producer;

        public FastTableDataRow(final StringReadValue rowKeyValue, final DataCellProducer[] producer) {
            m_rowKeyValue = rowKeyValue;
            m_producer = producer;
        }

        @Override
        public Iterator<DataCell> iterator() {
            return new Iterator<DataCell>() {
                int idx = 0;

                @Override
                public boolean hasNext() {
                    return idx < m_producer.length;
                }

                @Override
                public DataCell next() {
                    return getCell(idx++);
                }
            };
        }

        @Override
        public int getNumCells() {
            return m_producer.length;
        }

        @Override
        public RowKey getKey() {
            // TODO too expensive? Check per access... :-(
            if (m_rowKeyValue == null) {
                throw new IllegalStateException("RowKey requested, but not part of table. Implementation error!");
            }
            return new RowKey(m_rowKeyValue.getStringValue());
        }

        @Override
        public DataCell getCell(final int index) {
            return m_producer[index].get();
        }
    }
}
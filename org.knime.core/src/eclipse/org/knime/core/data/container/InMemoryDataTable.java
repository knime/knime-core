package org.knime.core.data.container;

import java.util.Collection;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.SizeAwareDataTable;

final class InMemoryDataTable implements SizeAwareDataTable {

    private final DataTableSpec m_spec;

    private final Collection<DataRow> m_buffer;

    /**
     * Creates a new in-memory row batch from a list of data rows. Ownership of the list is passed, i.e. modifications
     * after the batch is cursor'd or iterated on is not allowed.
     *
     * @param spec data table spec
     * @param buffer buffer of data rows
     */
    InMemoryDataTable(final DataTableSpec spec, final Collection<DataRow> buffer) {
        m_spec = spec;
        m_buffer = buffer;
    }

    @Override
    public long size() {
        return m_buffer.size();
    }

    @Override
    public RowCursor cursor() {
        return new IteratorAdapterRowCursor(m_spec, iterator());
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    @Override
    public RowIterator iterator() {
        return new IteratorAdapterRowIterator(m_buffer.iterator());
    }

}
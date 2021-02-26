package org.knime.core.data.v2.value.cell;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.access.ObjectAccess.ObjectSerializer;

/* {@link ObjectSerializer} for arbitrary {@link DataCell}s */
final class DataCellObjectSerializer implements ObjectSerializer<DataCell> {

    private final DataCellSerializerFactory m_factory;

    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    DataCellObjectSerializer(final DataCellSerializerFactory factory, final IWriteFileStoreHandler handler,
        final IDataRepository repository) {
        m_factory = factory;
        m_fsHandler = handler;
        m_dataRepository = repository;
    }

    @Override
    public DataCell deserialize(final DataInput input) throws IOException {
        try (DataCellDataInputDelegator stream = new DataCellDataInputDelegator(m_factory, m_dataRepository, input)) {
            return stream.readDataCell();
        }
    }

    @Override
    public void serialize(final DataCell cell, final DataOutput output) throws IOException {
        try (final DataCellDataOutputDelegator stream =
            new DataCellDataOutputDelegator(m_factory, m_fsHandler, output)) {
            stream.writeDataCell(cell);
        }
    }
}
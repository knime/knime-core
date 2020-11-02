package org.knime.core.data.v2.value.cell;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.access.ObjectAccess.ObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectSerializer;

final class DataCellAccessSpec implements ObjectAccessSpec<DataCell> {

    private final DataCellSerializerFactory m_factory;

    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    public DataCellAccessSpec(final DataCellSerializerFactory factory, final IWriteFileStoreHandler fsHandler,
        final IDataRepository repository) {
        m_factory = factory;
        m_fsHandler = fsHandler;
        m_dataRepository = repository;
    }

    @Override
    public ObjectSerializer<DataCell> getSerializer() {
        return new DataCellObjectSerializer(m_factory, m_fsHandler, m_dataRepository);
    }
}
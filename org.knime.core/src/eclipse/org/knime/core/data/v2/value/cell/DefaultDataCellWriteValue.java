package org.knime.core.data.v2.value.cell;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

final class DefaultDataCellWriteValue extends AbstractDataCellWriteValue {

    private final ObjectSerializer<DataCell> m_serializer;

    private final VarBinaryWriteAccess m_access;

    DefaultDataCellWriteValue(final VarBinaryWriteAccess access, final DataCellSerializerFactory factory,
        final IDataRepository repository, final IWriteFileStoreHandler fsHandler) {
        super(repository, fsHandler);
        m_access = access;
        m_serializer = (output, cell) -> {
            try (final DataCellDataOutputDelegator stream =
                new DataCellDataOutputDelegator(factory, fsHandler, output)) {
                stream.writeDataCell(cell);
            }
        };
    }

    @Override
    protected void setValueImpl(final DataCell cell) {
        m_access.setObject(cell, m_serializer);
    }
}
package org.knime.core.data.v2.value.cell;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

final class DefaultDataCellWriteValue implements WriteValue<DataCell> {

    private final ObjectSerializer<DataCell> m_serializer;

    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    private final VarBinaryWriteAccess m_access;

    private final DataCellSerializerFactory m_factory;

    DefaultDataCellWriteValue(final VarBinaryWriteAccess access, final DataCellSerializerFactory factory,
        final IDataRepository repository, final IWriteFileStoreHandler fsHandler) {
        m_access = access;
        m_fsHandler = fsHandler;
        m_dataRepository = repository;
        m_serializer = (output, cell) -> {
            try (final DataCellDataOutputDelegator stream =
                new DataCellDataOutputDelegator(factory, m_fsHandler, output)) {
                stream.writeDataCell(cell);
            }
        };

        m_factory = factory;
    }

    @Override
    public void setValue(final DataCell cell) {
        // legacy code for BlobDataCells. BlobDataCells are wrapped in FileStoreCells.
        if (cell instanceof BlobDataCell) {
            setValue(new BlobFileStoreCell((BlobDataCell)cell, // NO-SONAR
                m_fsHandler, // NO-SONAR
                m_factory)); // NO-SONAR
        } else {
            if (cell instanceof FileStoreCell) {
                handleFileStoreCell(cell);
            }
            m_access.setObject(cell, m_serializer);
        }
    }

    private void handleFileStoreCell(final DataCell actual) {
        final FileStoreCell fsCell = (FileStoreCell)actual;

        // handle loops
        if (mustBeFlushedPriorSave(fsCell)) {
            final FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
            final FileStoreKey[] fileStoreKeys = new FileStoreKey[fileStores.length];

            for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                fileStoreKeys[fileStoreIndex] = m_fsHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
            }

            // update file store keys without calling post-construct.
            try {
                FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository, false);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private boolean mustBeFlushedPriorSave(final FileStoreCell cell) {
        final FileStore[] fileStores = FileStoreUtil.getFileStores(cell);
        for (final FileStore fs : fileStores) {
            if (m_fsHandler.mustBeFlushedPriorSave(fs)) {
                return true;
            }
        }
        return false;
    }
}
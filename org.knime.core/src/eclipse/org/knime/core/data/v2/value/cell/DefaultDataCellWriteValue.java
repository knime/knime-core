package org.knime.core.data.v2.value.cell;

import java.io.DataOutput;
import java.io.IOException;
import java.util.function.BiConsumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.ByteArrayAccess.VarBinaryWriteAccess;

final class DefaultDataCellWriteValue implements WriteValue<DataCell> {

    private final BiConsumer<DataOutput, DataCell> m_serializer;

    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    private final VarBinaryWriteAccess m_access;

    DefaultDataCellWriteValue(final VarBinaryWriteAccess access, final DataCellSerializerFactory factory,
        final IDataRepository repository, final IWriteFileStoreHandler fsHandler) {
        m_access = access;
        m_fsHandler = fsHandler;
        m_dataRepository = repository;
        m_serializer = (output, cell) -> {
            try (final DataCellDataOutputDelegator stream =
                new DataCellDataOutputDelegator(factory, m_fsHandler, output)) {
                stream.writeDataCell(cell);
            } catch (final IOException ex) {
                throw new IllegalStateException("Error during serialization", ex);
            }
        };
    }

    @Override
    public void setValue(final DataCell cell) {
        if (cell instanceof FileStoreCell) {
            final FileStoreCell fsCell = (FileStoreCell)cell;

            // handle loops
            if (mustBeFlushedPriorSave(fsCell)) {
                try {
                    final FileStore[] fileStores = FileStoreUtil.getFileStores(fsCell);
                    final FileStoreKey[] fileStoreKeys = new FileStoreKey[fileStores.length];

                    for (int fileStoreIndex = 0; fileStoreIndex < fileStoreKeys.length; fileStoreIndex++) {
                        fileStoreKeys[fileStoreIndex] =
                            m_fsHandler.translateToLocal(fileStores[fileStoreIndex], fsCell);
                    }

                    // update file store keys without calling post-construct.
                    FileStoreUtil.retrieveFileStoreHandlersFrom(fsCell, fileStoreKeys, m_dataRepository, false);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        // NB: Missing Value checks is expected to happen before cell is actually written. See RowWriteAccess.
        m_access.setObject(cell, m_serializer);
    }

    // TODO why do we need to flush? problem with heap cache!
    private boolean mustBeFlushedPriorSave(final FileStoreCell cell) {
        final FileStore[] fileStores = FileStoreUtil.getFileStores(cell);
        for (FileStore fs : fileStores) {
            if (m_fsHandler.mustBeFlushedPriorSave(fs)) {
                return true;
            }
        }
        return false;
    }
}
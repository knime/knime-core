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
 */
package org.knime.core.data.v2.value.cell;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowIterator;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WrappedReadValue;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectSerializer;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 *
 * {@link ValueFactory} to write and read arbitrary {@link DataCell}s. The created {@link WrappedReadValue}s are special
 * compared to other {@link ReadValue}s in the sense that they are actually just suppliers of {@link DataCell}'s and
 * don't represent a {@link DataValue} themselves. Needs special casing in corresponding {@link RowIterator} and
 * {@link RowCursor} implementations.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DataCellValueFactory
    implements ValueFactory<ObjectReadAccess<DataCell>, ObjectWriteAccess<DataCell>> {

    private final DataCellSerializerFactory m_factory;

    private final IWriteFileStoreHandler m_fsHandler;

    private final IDataRepository m_dataRepository;

    private DataCellValueFactory(final DataCellSerializerFactory factory, final IWriteFileStoreHandler fileStoreHandler,
        final IDataRepository repository) {
        m_factory = factory;
        m_fsHandler = fileStoreHandler;
        m_dataRepository = repository;
    }

    /**
     * Create a {@link DataCellValueFactory} for writing.
     *
     * @param factory used to retrieve {@link DataCellSerializer}s
     * @param fileStoreHandler to deal with file stores.
     */
    public DataCellValueFactory(final DataCellSerializerFactory factory,
        final IWriteFileStoreHandler fileStoreHandler) {
        this(factory, fileStoreHandler, null);
    }

    /**
     * Create a {@link DataCellValueFactory} for reading.
     *
     * @param factory used to retrieve {@link DataCellSerializer}s
     * @param repository to deal with (potentially) written file stores.
     */
    public DataCellValueFactory(final DataCellSerializerFactory factory, final IDataRepository repository) {
        this(factory, null, repository);
    }

    @Override
    public ReadValue createReadValue(final ObjectReadAccess<DataCell> access) {
        return new DefaultDataCellReadValue(access);
    }

    @Override
    public WriteValue<? extends DataCell> createWriteValue(final ObjectWriteAccess<DataCell> access) {
        return new DefaultDataCellWriteValue(access);
    }

    @Override
    public AccessSpec<ObjectReadAccess<DataCell>, ObjectWriteAccess<DataCell>> getSpec() {
        return new DataCellAccessSpec(m_factory, m_fsHandler, m_dataRepository);
    }

    /* DataCellAccessSpec based on {@link ObjectAccessSpec}. */
    private static final class DataCellAccessSpec implements ObjectAccessSpec<DataCell> {

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

    /*
     * WriteValue accepting all DataCells
     */
    private static class DefaultDataCellWriteValue implements WriteValue<DataCell> {

        private final ObjectWriteAccess<DataCell> m_access;

        DefaultDataCellWriteValue(final ObjectWriteAccess<DataCell> access) {
            m_access = access;
        }

        @Override
        public void setValue(final DataCell cell) {
            // NB: Missing Value checks is expected to happen before cell is actually written. See RowWriteAccess.
            m_access.setObject(cell);
        }
    }

    /* {@link ObjectSerializer} for arbitrary {@link DataCell}s */
    private static final class DataCellObjectSerializer implements ObjectSerializer<DataCell> {

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
        public DataCell deserialize(final byte[] bytes) {
            try (final ByteArrayDataCellInput stream = new ByteArrayDataCellInput(m_factory, m_dataRepository, bytes)) {
                return stream.readDataCell();
            } catch (IOException ex) {
                // TODO logging
                throw new RuntimeException(ex);
            } catch (Exception ex1) {
                // TODO logging
                throw new RuntimeException(ex1);
            }
        }

        @Override
        public byte[] serialize(final DataCell cell) {
            try (final ByteArrayDataCellOutput stream = new ByteArrayDataCellOutput(m_factory, m_fsHandler)) {
                stream.writeDataCell(cell);
                return stream.toByteArray();
            } catch (IOException ex) {
                // TODO logging
                throw new RuntimeException(ex);
            } catch (Exception ex1) {
                // TODO logging
                throw new RuntimeException(ex1);
            }

        }
    }
}

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowIterator;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait.KeyType;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultStructDataTraits;

/**
 * {@link ValueFactory} to write and read arbitrary {@link DataCell}s using dictionary encoding. Needs special casing in
 * corresponding {@link RowIterator} and {@link RowCursor} implementations.
 *
 * Replaces {@link DataCellValueFactory} because dictionary encoding only adds very little overhead but brings a lot of
 * speedup in case e.g. BlobCells are repeated in the table.
 * Furthermore, the DataCellSerializer class name is now stored alongside the data as dictionary encoded string.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DictEncodedDataCellValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    private IWriteFileStoreHandler m_fsHandler;

    private IDataRepository m_dataRepository;

    private DataType m_type;

    /**
     * Empty framework constructor. Call {@link #initialize(IDataRepository, DataType)} after using this constructor.
     */
    public DictEncodedDataCellValueFactory() {
    }

    /**
     * Create a {@link DictEncodedDataCellValueFactory} for writing.
     *
     * @param fileStoreHandler to deal with file stores.
     * @param type type associated with cells
     */
    public DictEncodedDataCellValueFactory(final IWriteFileStoreHandler fileStoreHandler, final DataType type) {
        m_type = type;
        m_fsHandler = fileStoreHandler;
        m_dataRepository = fileStoreHandler.getDataRepository();
    }

    /**
     * Create a {@link DataCellValueFactory} for reading.
     *
     * @param repository to deal with (potentially) written file stores.
     * @param type type associated with cells
     */
    public void initialize(final IDataRepository repository, final DataType type) {
        m_type = type;
        m_fsHandler = null;
        m_dataRepository = repository;
    }

    /**
     * @return the type corresponding to this ValueFactory
     */
    public DataType getType() {
        return m_type;
    }

    @Override
    public DataSpec getSpec() {
        // we store the actual implementation class name next to the serialized binary data
        // to be able to fetch the appropriate deserializer from the registry
        return new StructDataSpec(VarBinaryDataSpec.INSTANCE, StringDataSpec.INSTANCE);
    }

    @Override
    public DataTraits getTraits() {
        // store the binary data as well as the class names using dictionary encoding
        return new DefaultStructDataTraits(//
            new DefaultDataTraits(new DictEncodingTrait()), //
            new DefaultDataTraits(new DictEncodingTrait(KeyType.BYTE_KEY)));
    }

    @Override
    public ReadValue createReadValue(final StructReadAccess access) {
        final ArrayList<Class<? extends DataValue>> types = new ArrayList<>(m_type.getValueClasses());
        types.add(ReadValue.class);
        final Class<?>[] array = types.toArray(new Class<?>[types.size()]);

        Class<? extends DataCell> cellClass = m_type.getCellClass();
        final ClassLoader loader;
        if (cellClass == null) {
            loader = DataCell.class.getClassLoader();
        } else {
            loader = cellClass.getClassLoader();
        }
        return (ReadValue)Proxy.newProxyInstance(loader, array,
            new DictEncodedDataCellInvocationHandler(access, m_dataRepository));
    }

    @Override
    public WriteValue<? extends DataCell> createWriteValue(final StructWriteAccess access) {
        return new DictEncodedDataCellWriteValue(access, m_dataRepository, m_fsHandler);
    }

    private static final class DictEncodedDataCellInvocationHandler implements InvocationHandler {

        private final Method m_getDataCell;

        private final StructReadAccess m_access;

        private final IDataRepository m_dataRepository;

        private DictEncodedDataCellInvocationHandler(final StructReadAccess access,
            final IDataRepository dataRepository) {
            m_access = access;
            m_dataRepository = dataRepository;
            try {
                m_getDataCell = ReadValue.class.getMethod("getDataCell");
            } catch (Exception ex) {//NOSONAR can't recover
                throw new IllegalStateException("Fatal: Proxy can't be setup.", ex);
            }
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final VarBinaryReadAccess binaryBlobAccess = m_access.getAccess(0);
            final StringReadAccess classNameAccess = m_access.getAccess(1);
            final var classNames = classNameAccess.getStringValue();
            ObjectDeserializer<DataCell> deserializer = input -> {
                try (DictEncodedDataCellDataInputDelegator stream =
                    new DictEncodedDataCellDataInputDelegator(m_dataRepository, input, classNames)) {
                    return stream.readDataCell();
                }
            };

            final DataCell cell = binaryBlobAccess.getObject(deserializer);
            if (method.equals(m_getDataCell)) {
                return cell;
            } else {
                return method.invoke(cell, args);
            }
        }
    }

}

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
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowIterator;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataCellSerializerFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.GenericObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 * {@link ValueFactory} to write and read arbitrary {@link DataCell}s. Needs special casing in corresponding
 * {@link RowIterator} and {@link RowCursor} implementations.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DataCellValueFactory
    implements ValueFactory<ObjectReadAccess<DataCell>, ObjectWriteAccess<DataCell>> {

    private DataCellSerializerFactory m_factory;

    private IWriteFileStoreHandler m_fsHandler;

    private IDataRepository m_dataRepository;

    private DataType m_type;

    /**
     * Empty framework constructor. Call {@link #initialize(DataCellSerializerFactory, IDataRepository, DataType)} after using
     * this constructor.
     */
    public DataCellValueFactory() {
    }

    /**
     * Create a {@link DataCellValueFactory} for writing.
     *
     * @param factory used to retrieve {@link DataCellSerializer}s
     * @param fileStoreHandler to deal with file stores.
     * @param type type associated with cells
     */
    public DataCellValueFactory(final DataCellSerializerFactory factory, final IWriteFileStoreHandler fileStoreHandler,
        final DataType type) {
        m_factory = factory;
        m_type = type;
        m_fsHandler = fileStoreHandler;
        m_dataRepository = fileStoreHandler.getDataRepository();
    }

    /**
     * Create a {@link DataCellValueFactory} for reading.
     *
     * @param factory used to retrieve {@link DataCellSerializer}s
     * @param repository to deal with (potentially) written file stores.
     * @param type type associated with cells
     */
    public void initialize(final DataCellSerializerFactory factory, final IDataRepository repository,
        final DataType type) {
        m_factory = factory;
        m_type = type;
        m_fsHandler = null;
        m_dataRepository = repository;
    }

    @Override
    public ReadValue createReadValue(final ObjectReadAccess<DataCell> access) {
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
        return (ReadValue)Proxy.newProxyInstance(loader, array, new DataCellInvocationHandler(access));
    }

    @Override
    public WriteValue<? extends DataCell> createWriteValue(final ObjectWriteAccess<DataCell> access) {
        return new DefaultDataCellWriteValue(access, m_fsHandler, m_dataRepository);
    }

    @Override
    public AccessSpec<ObjectReadAccess<DataCell>, ObjectWriteAccess<DataCell>> getSpec() {
        return new GenericObjectAccessSpec<>(new DataCellObjectSerializer(m_factory, m_fsHandler, m_dataRepository));
    }

    private final static class DataCellInvocationHandler implements InvocationHandler {

        private final Method m_getDataCell;

        private final ObjectReadAccess<DataCell> m_access;

        private DataCellInvocationHandler(final ObjectReadAccess<DataCell> access) {
            m_access = access;
            try {
                m_getDataCell = ReadValue.class.getMethod("getDataCell");
            } catch (Exception ex) {
                throw new IllegalStateException("Fatal: Proxy can't be setup.", ex);
            }
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final DataCell cell = m_access.getObject();
            if (method.equals(m_getDataCell)) {
                return cell;
            } else {
                return method.invoke(cell, args);
            }
        }
    }

}

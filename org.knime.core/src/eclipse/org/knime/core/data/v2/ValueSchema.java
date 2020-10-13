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
 *
 * History
 *   Sep 27, 2020 (dietzc): created
 */
package org.knime.core.data.v2;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.RowKey;
import org.knime.core.data.TableBackend;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.WriteAccess;
import org.knime.core.data.v2.value.BooleanValueFactory;
import org.knime.core.data.v2.value.CustomRowKeyValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.IntValueFactory;
import org.knime.core.data.v2.value.LongValueFactory;
import org.knime.core.data.v2.value.StringValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyValueFactory;
import org.knime.core.data.v2.value.cell.DataCellValueFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A ValueSchema wraps a {@link DataTableSpec} by mapping each {@link DataColumnSpec} via it's {@link DataType} to a
 * {@link ValueFactory}. {@link TableBackend} implementations leverage the {@link ValueFactory}s in turn as a canonical,
 * logical access layer, independent from it's physical implementation.
 *
 * NB: All value schemas will always have a {@link ValueFactory} producing a {@link RowKeyReadValue} at column index 0,
 * i.e. {@link ValueSchema#getNumColumns()} equals {@link DataTableSpec#getNumColumns()} + 1;
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
// TODO Bernd do we want to interface this?
public final class ValueSchema {

    private final DataTableSpec m_spec;

    private final ValueFactory<?, ?>[] m_factories;

    private final RowKeyType m_rowKeyType;

    private final DataCellSerializerFactory m_factory;

    ValueSchema(final DataTableSpec spec, //
        final ValueFactory<?, ?>[] factories, //
        final RowKeyType type, //
        final DataCellSerializerFactory factory) {
        m_spec = spec;
        m_rowKeyType = type;
        m_factories = factories;
        m_factory = factory;
    }

    /**
     * @return the rowKeyType
     */
    public final RowKeyType getRowKeyType() {
        return m_rowKeyType;
    }

    /**
     * @return the underlying {@link DataTableSpec}.
     */
    public final DataTableSpec getSourceSpec() {
        return m_spec;
    }

    /**
     * Number of columns always corresponds to {@link DataTableSpec#getNumColumns()} of source spec + 1. The additional
     * entry at colIndex = 0 is reserved for the RowKey column.
     *
     * @return the number of columns in this schema.
     */
    public final int getNumColumns() {
        return m_spec.getNumColumns() + 1;
    }

    /**
     * Get the {@link ValueFactory} at colIndex.
     *
     * @param colIndex the index of a column.
     * @return the {@link ValueFactory} at colIdx.
     */
    public final ValueFactory<ReadAccess, WriteAccess> getFactoryAt(final int colIndex) {
        @SuppressWarnings("unchecked")
        final ValueFactory<ReadAccess, WriteAccess> factory =
            (ValueFactory<ReadAccess, WriteAccess>)m_factories[colIndex];
        return factory;
    }

    /**
     * Get the {@link AccessSpec} at colIndex.
     *
     * @param colIndex the index of a column.
     * @return the {@link AccessSpec} at colIdx.
     */
    public final AccessSpec<?, ?> getAccessSpecAt(final int colIndex) {
        return m_factories[colIndex].getSpec();
    }

    /**
     * Creates a new {@link ValueSchema} based up-on the provided {@link DataTableSpec}.
     *
     * @param spec the data table spec to derive the {@link ValueSchema} from.
     * @param rowKeyType type of the {@link RowKey}
     * @param handler file-store handler
     * @return the value schema.
     */
    public final static ValueSchema create(final DataTableSpec spec, final RowKeyType rowKeyType,
        final IWriteFileStoreHandler handler) {

        final DataCellSerializerFactory factory = new DataCellSerializerFactory();

        final ValueFactory<?, ?>[] factories = new ValueFactory[spec.getNumColumns() + 1];
        factories[0] = getRowKeyFactory(rowKeyType);
        for (int i = factories.length; --i >= 1;) {
            factories[i] = findNativeValueFactory(spec.getColumnSpec(i - 1).getType());
            if (factories[i] == null) {
                factories[i] = new DataCellValueFactory(factory, handler);
            }
        }
        return new ValueSchema(spec, factories, rowKeyType, factory);
    }

    private final static ValueFactory<?, ?> findNativeValueFactory(final DataType type) {
        /* TODO extension point */
        if (type == DoubleCell.TYPE) {
            return DoubleValueFactory.INSTANCE;
        } else if (type == IntCell.TYPE) {
            return IntValueFactory.INSTANCE;
        } else if (type == LongCell.TYPE) {
            return LongValueFactory.INSTANCE;
        } else if (type == StringCell.TYPE) {
            return StringValueFactory.INSTANCE;
        } else if (type == BooleanCell.TYPE) {
            return BooleanValueFactory.INSTANCE;
        }
        return null;
    }

    /**
     * @param rowKeyType
     * @return value factory to support this rowKeyConfig
     */
    private static ValueFactory<ReadAccess, WriteAccess> getRowKeyFactory(final RowKeyType rowKeyType) {
        final ValueFactory<?, ?> factory;

        // TODO auto rowkey
        switch (rowKeyType) {
            case CUSTOM:
                factory = CustomRowKeyValueFactory.INSTANCE;
                break;
            case NOKEY:
                factory = VoidRowKeyValueFactory.INSTANCE;
                break;
            default:
                throw new IllegalArgumentException("Unknown RowKey configuration " + rowKeyType.name() + ".");

        }
        @SuppressWarnings("unchecked")
        final ValueFactory<ReadAccess, WriteAccess> cast = (ValueFactory<ReadAccess, WriteAccess>)factory;
        return cast;
    }

    /**
     * Serializer to save/load {@link ValueSchema}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany.
     * @since 4.3
     */
    public static final class Serializer {

        private static final String CFG_ROW_KEY_CONFIG = "row_key_config";

        private static final String CFG_KEY_MAPPED_DATA_TYPES = "mapped_data_types";

        private Serializer() {
        }

        /**
         * Saves a ValueSchema to the provided settings.
         *
         * @param schema the ValueSchema to save.
         * @param settings the settings to save the ValueSchema to.
         */
        public final static void save(final ValueSchema schema, final NodeSettingsWO settings) {

            // save row key config
            settings.addString(CFG_ROW_KEY_CONFIG, schema.m_rowKeyType.name());

            // we need to remember which settings we have saved via legacy data cell and
            // which via ValueFactories.
            final Set<DataType> mapped = new HashSet<>();
            final DataTableSpec sourceSpec = schema.getSourceSpec();
            for (int i = 0; i < sourceSpec.getNumColumns(); i++) {
                final ValueFactory<?, ?> factoryAt = schema.getFactoryAt(i + 1);
                if (!(factoryAt instanceof DataCellValueFactory)) {
                    mapped.add(sourceSpec.getColumnSpec(i).getType());
                }
            }
            settings.addDataTypeArray(CFG_KEY_MAPPED_DATA_TYPES, mapped.toArray(new DataType[mapped.size()]));

            // now store all info required to restore DataCellValueFactories
            schema.m_factory.saveTo(settings);
        }

        /**
         * Loads a ValueSchema from the given settings.
         *
         * @param source the source {@link DataTableSpec}.
         * @param repository the data repository to restore file store cells.
         * @param settings to save the value schema to.
         * @return the loaded {@link ValueSchema}.
         *
         * @throws InvalidSettingsException
         */
        public final static ValueSchema load(final DataTableSpec source, final IDataRepository repository,
            final NodeSettingsRO settings) throws InvalidSettingsException {

            // load all the things
            final RowKeyType rowKeyConfig = RowKeyType.valueOf(settings.getString(CFG_ROW_KEY_CONFIG));
            final DataType[] mappedTypes = settings.getDataTypeArray(CFG_KEY_MAPPED_DATA_TYPES);
            final Set<DataType> types = new HashSet<>();
            for (int i = 0; i < mappedTypes.length; i++) {
                types.add(mappedTypes[i]);
            }
            final DataCellSerializerFactory factory = new DataCellSerializerFactory();
            factory.loadFrom(settings);

            final ValueFactory<?, ?>[] factories = new ValueFactory[source.getNumColumns() + 1];

            factories[0] = getRowKeyFactory(rowKeyConfig);
            for (int i = 1; i < factories.length; i++) {
                final DataType type = source.getColumnSpec(i - 1).getType();
                if (types.contains(type)) {
                    factories[i] = findNativeValueFactory(type);
                } else {
                    factories[i] = new DataCellValueFactory(factory, repository);
                }
            }

            return new ValueSchema(source, factories, rowKeyConfig, factory);
        }
    }

}

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
 *   28 Mar 2025 (pietzsch): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.RowKeyType;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueFactoryUtils;
import org.knime.core.data.v2.schema.ValueSchema.ValueSchemaColumn;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.traits.LogicalTypeTrait;

/**
 * Utility class for creating, transforming, saving and loading of {@link DataTableValueSchema DataTableValueSchemas}.
 *
 * @author Tobias Pietzsch
 * @since 5.8
 * @noreference This class is not intended to be referenced by clients.
 */
public class DataTableValueSchemaUtils {

    /**
     * Creates a new {@link DataTableValueSchema} based up-on the provided {@link DataTableSpec}.
     *
     * @param spec the data table spec to derive the {@link ValueSchema} from.
     * @param rowKeyType type of the {@link RowKey}
     * @param fileStoreHandler file-store handler
     * @return the value schema.
     * @since 5.8
     */
    public static final DataTableValueSchema create(final DataTableSpec spec, final RowKeyType rowKeyType,
        final IWriteFileStoreHandler fileStoreHandler) {
        final var factories = new ValueFactory<?, ?>[spec.getNumColumns() + 1];
        factories[0] = ValueFactoryUtils.getRowKeyValueFactory(rowKeyType);
        for (int i = 1; i < factories.length; i++) {//NOSONAR
            var type = spec.getColumnSpec(i - 1).getType();
            factories[i] = ValueFactoryUtils.getValueFactory(type, fileStoreHandler);
        }
        return new DefaultDataTableValueSchema(spec, factories);
    }

    /**
     * Create a {@link DataTableValueSchema}.
     * <p>
     * {@code columns[0]} must be the RowKey. The {@code DataColumnSpec}s of the given {@code columns} must correspond
     * to the {@code DataColumnSpec}s of the {@code spec} (except {@code columns[0]} which has no
     * {@code DataColumnSpec}).
     *
     * @param spec a DataTableSpec matching columns
     * @param columns the columns of the schema
     * @return a new schema
     *
     * @throws IllegalArgumentException if the given spec and columns are not compatible
     */
    public static DataTableValueSchema create(final DataTableSpec spec, final ValueSchemaColumn... columns)
        throws IllegalArgumentException {
        return new DefaultDataTableValueSchema(spec, columns);
    }

    /**
     * Creates a new {@link ValueSchema} given the provided {@link DataTableSpec spec} and {@link ValueFactory
     * factories}.
     *
     * @param spec the data table spec that the {@link ValueSchema} should wrap
     * @param valueFactories one for the row key, and one for each column in spec
     * @return the value schema
     * @since 5.8
     */
    public static final DataTableValueSchema create(final DataTableSpec spec, final ValueFactory<?, ?>... valueFactories) {
        return new DefaultDataTableValueSchema(spec, valueFactories);
    }

    /**
     * Saves a {@code DataTableValueSchema} to the provided settings.
     *
     * @param schema the ValueSchema to save.
     * @param settings the settings to save the ValueSchema to.
     * @since 5.7
     */
    public static final void save(final DataTableValueSchema schema, final NodeSettingsWO settings) {
        if (schema instanceof SerializerFactoryValueSchema) {
            SerializerFactoryValueSchema.Serializer.save((SerializerFactoryValueSchema)schema, settings);
        } else if (schema instanceof DefaultDataTableValueSchema) {
            // nothing to save
        } else {
            throw new IllegalArgumentException("Unsupported schema type: " + schema.getClass());
        }
    }

    /**
     * Loads a {@code DataTableValueSchema} from the given settings.
     *
     * @param schema underlying schema
     * @param loadContext in which the schema is loaded
     * @return the loaded {@link ValueSchema}.
     *
     * @throws InvalidSettingsException if the settings in loadContext are invalid
     * @since 5.8
     */
    public static final DataTableValueSchema load(final ColumnarSchema schema, final ValueSchemaLoadContext loadContext)
        throws InvalidSettingsException {
        if (hasTypeTraits(schema)) {
            return createWithTypeTraits(schema, loadContext);
        } else {
            var source = loadContext.getTableSpec();
            var dataRepository = loadContext.getDataRepository();
            return SerializerFactoryValueSchema.Serializer.load(source, dataRepository, loadContext.getSettings());
        }
    }

    private static boolean hasTypeTraits(final ColumnarSchema schema) {
        return IntStream.range(0, schema.numColumns())//
            .mapToObj(schema::getTraits)//
            .allMatch(t -> t.hasTrait(LogicalTypeTrait.class));
    }

    private static DataTableValueSchema createWithTypeTraits(final ColumnarSchema schema, final ValueSchemaLoadContext loadContext) {
        var source = loadContext.getTableSpec();
        var dataRepository = loadContext.getDataRepository();
        int numDataColumns = source.getNumColumns();
        CheckUtils.checkArgument(numDataColumns + 1 == schema.numColumns(),
            "Expected %s columns in the schema but encountered %s.", numDataColumns + 1, schema.numColumns());
        final var factories = new ValueFactory<?, ?>[schema.numColumns()];
        Arrays.setAll(factories, i -> ValueFactoryUtils.loadValueFactory(schema.getTraits(i), dataRepository));
        return new DefaultDataTableValueSchema(source, factories);
    }

    /**
     * Indicates whether the provided schema was created prior to KNIME Analytics Platform 4.5.0.
     *
     * @param schema to check
     * @return true if the schema was created before KNIME AP 4.5.0
     * @since 5.8
     */
    public static boolean storesDataCellSerializersSeparately(final DataTableValueSchema schema) {
        if (schema instanceof SerializerFactoryValueSchema) {
            return true;
        } else if (schema instanceof DefaultDataTableValueSchema) {
            return false;
        } else {
            throw new IllegalArgumentException("Unsupported schema type: " + schema.getClass());
        }
    }
}

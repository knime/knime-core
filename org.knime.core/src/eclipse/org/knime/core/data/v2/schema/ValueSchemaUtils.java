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
 *   Oct 7, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.v2.schema.ValueSchema.ValueSchemaColumn;

/**
 * Utility class for creating and transforming {@link ValueSchema ValueSchemas}.
 *
 * @author Tobias Pietzsch
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ValueSchemaUtils {

    /**
     * Create a {@code ValueSchema} with the specified {@code columns}.
     *
     * @param columns the column specs
     * @return a ValueSchema with the specified columns
     * @since 5.7
     */
    public static final ValueSchema create(final ValueSchemaColumn... columns) {
        return new DefaultValueSchema(columns);
    }

    /**
     * Create a {@code ValueSchema} with the specified {@code columns}.
     *
     * @param columns the column specs
     * @return a ValueSchema with the specified columns
     * @since 5.7
     */
    public static final ValueSchema create(final Collection<ValueSchemaColumn> columns) {
        return create(columns.toArray(ValueSchemaColumn[]::new));
    }

    /**
     * Updates the {@link DataColumnSpec DataColumnSpecs} of the given {@code ValueSchema}.
     *
     * @param schema schema to update
     * @param domainMap the domains used for update.
     * @param metadataMap the columnar metadata used to update
     *
     * @return a new {@link ValueSchema}, with updated DataColumnSpecs
     * @since 5.7
     */
    public static final ValueSchema updateDataColumnSpecs(final ValueSchema schema,
        final Map<Integer, DataColumnDomain> domainMap, final Map<Integer, DataColumnMetaData[]> metadataMap) {

        final int numCols = schema.numColumns();
        final var updatedCols = new ValueSchemaColumn[numCols];
        for (int i = 0; i < numCols; i++) {//NOSONAR
            final ValueSchemaColumn column = schema.getColumn(i);
            final DataColumnDomain domain = domainMap.get(i);
            final DataColumnMetaData[] metadata = metadataMap.get(i);
            if (domain == null && metadata == null) {
                updatedCols[i] = column;
            } else {
                final DataColumnSpec colSpec = column.dataColumnSpec();
                final var creator = new DataColumnSpecCreator(colSpec);
                if (domain != null) {
                    creator.setDomain(domain);
                }

                for (final DataColumnMetaData element : metadata) {
                    creator.addMetaData(element, true);
                }
                updatedCols[i] = column.with(creator.createSpec());
            }
        }
        return new DefaultValueSchema(updatedCols);
    }

    /**
     * Create a new {@code ValueSchema} comprising only the specified columns.
     *
     * @param schema input schema
     * @param columnIndices indices of columns to select
     * @return a new {@code ValueSchema} comprising only the specified columns
     */
    public static ValueSchema selectColumns(final ValueSchema schema, final int... columnIndices) {
        var columns = new ValueSchemaColumn[columnIndices.length];
        Arrays.setAll(columns, i -> schema.getColumn(columnIndices[i]));
        return new DefaultValueSchema(columns);
    }

    /**
     * Checks whether a schema has a RowKey as column 0.
     *
     * @param schema to check
     * @return true if the schema has a RowID column
     */
    public static final boolean hasRowID(final ValueSchema schema) {
        return schema.numColumns() > 0 && schema.getColumn(0).isRowKey();
    }

    /**
     * Creates a new {@code DataTableSpec} matching the given {@code schema}.
     * <p>
     * The {@code schema} must satisfy the following constraints:
     * <ul>
     * <li>Column 0 (and only column 0) must be a RowKey column.</li>
     * <li>All columns (except column 0) must have unique column names.</li>
     * </ul>
     *
     * @param schema ValueSchema with RowKey in column 0
     * @return DataTableSpec for the schema
     *
     * @throws IllegalArgumentException if the provided schema does not satisfy the constraints
     * @since 5.7
     */
    public static DataTableSpec createDataTableSpec(final ValueSchema schema) throws IllegalArgumentException {
        return new DataTableSpec(dataColumnSpecs(schema));
    }

    /**
     * Extract {@code DataColumnSpec[]} from {@code schema}. This does not include the RowKey, i.e. the returned array
     * contains {@code schema.getColumn(1).dataColumnSpec()} at index 0. Note, that the {@code schema} must have RowKey
     * at column 0, otherwise an {@code IllegalArgumentException} is thrown!
     *
     * @param schema ValueSchema with RowKey in column 0
     * @return extracted DataColumnSpecs (not including the RowKey)
     *
     * @throws IllegalArgumentException if the provided schema does not have RowKey at column 0
     * @since 5.7
     */
    public static DataColumnSpec[] dataColumnSpecs(final ValueSchema schema) throws IllegalArgumentException {
        if (schema.numColumns() < 1 || !schema.getColumn(0).isRowKey()) {
            throw new IllegalArgumentException("expected schema with RowKey at column 0");
        }
        return Arrays.stream(schema.getColumns()) //
            .skip(1) // skip RowKey column
            .map(ValueSchemaColumn::dataColumnSpec) //
            .toArray(DataColumnSpec[]::new);
    }

    private ValueSchemaUtils() {
    }
}

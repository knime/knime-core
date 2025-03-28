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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.v2.schema.ValueSchema.ValueSchemaColumn;

/**
 * Utility class for creating, transforming, saving and loading of {@link DataTableValueSchema DataTableValueSchemas}.
 *
 * @author Tobias Pietzsch
 * @since 5.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class DataTableValueSchemaUtils {

    /**
     * Create a {@link DataTableValueSchema}.
     * <p>
     * The DataTypes of the given {@code columns} must correspond to the DataTypes of the {@code sourceSpec}.
     * {@code columns[0]} must be the RowKey.
     *
     * @param spec
     * @param columns
     * @return
     *
     * @throws IllegalArgumentException if the given DataTableSpec and columns are not compatible
     */
    public static DataTableValueSchema create(final DataTableSpec spec, final ValueSchemaColumn[] columns)
        throws IllegalArgumentException {
        return new DefaultDataTableValueSchema(spec, columns);
    }

    /**
     * Creates a new {@code DataTableSpec} matching the given {@code schema}. The {@code schema} must have RowKey at
     * column 0, otherwise an {@code IllegalArgumentException} is thrown!
     *
     * @param schema ValueSchema with RowKey in column 0
     * @return DataTableSpec for the schema
     *
     * @throws IllegalArgumentException if the provided schema does not have RowKey at column 0
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
}

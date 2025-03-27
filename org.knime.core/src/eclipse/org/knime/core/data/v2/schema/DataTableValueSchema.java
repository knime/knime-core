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
 *   7 Feb 2025 (pietzsch): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;

/**
 * TODO
 * The plan:
 * <p>
 * DataTableValueSchema extends ValueSchema and additionally has
 *    getSourceSpec()
 * (which is removed from ValueSchema).
 * <p>
 * Start plugging that into AbstractColumnarContainerTable (line 146).
 * See where we can get away with ValueSchema and where we actually need DataTableValueSchema.
 *
 * @author pietzsch
 * @since 5.5
 */
public interface DataTableValueSchema extends ValueSchema {

    /**
     * @return the underlying {@link DataTableSpec}.
     */
    DataTableSpec getSourceSpec();

    /**
     * TODO (TP) javadoc
     *
     * @param spec
     * @param schema
     * @return
     */
    // TODO (TP) TEMPORARY. exploring API options.
    static DataTableValueSchema create(final DataTableSpec spec, final ValueSchema schema) {
        // need to check for spec/schema compatibility here
        DataColumnSpec[] cols = dataColumnSpecs(schema);
        if (cols.length != spec.getNumColumns()) {
            throw new IllegalArgumentException(
                "schema has " + cols.length + " data columns, but spec has " + spec.getNumColumns());
        }
        for (int i = 0; i < cols.length; i++) {
            if (!cols[i].equals(spec.getColumnSpec(i))) {
                throw new IllegalArgumentException("column spec mismatch at column " + i + ": schema has " + cols[i]
                    + ", spec has " + spec.getColumnSpec(i));
            }
        }
        return new DefaultDataTableValueSchema(spec, schema.getColumns());
    }

    /**
     * Creates a new {@code DataTableValueSchema} with the {@code DataColumnSpec}s of the provided {@code schema}. The
     * {@code DataTableValueSchema} will have a {@code DataTableSpec} which uses the {@code DataColumnSpec}s of
     * {@code schema} and the name and properties of the provided {@code spec}.
     * <p>
     * TODO (TP) javadoc
     *
     * @param dataTableSpec
     * @param schema
     * @return
     * @throws IllegalArgumentException if the provided schema does not have RowKey at column 0
     */
    // TODO (TP) TEMPORARY. exploring API options.
    static DataTableValueSchema createWithInheritedMetadata(final DataTableSpec spec, final ValueSchema schema) {
        final DataTableSpec newSpec = new DataTableSpecCreator(spec) //
                .dropAllColumns() //
                .addColumns(dataColumnSpecs(schema)) //
                .createSpec();
        return new DefaultDataTableValueSchema(newSpec, schema.getColumns());
    }

    /**
     * Creates a new {@code DataTableSpec} with the name and properties of {@code spec} and the {@code DataColumnSpec}s of {@code schema}.
     * <p>
     * TODO (TP) javadoc
     *
     * @param dataTableSpec
     * @param schema
     * @return
     */
    // TODO (TP) TEMPORARY. exploring API options.
    static DataTableSpec createDataTableSpecWithInheritedMetadata(final DataTableSpec spec, final ValueSchema schema) {
        return new DataTableSpecCreator(spec) //
            .dropAllColumns() //
            .addColumns(dataColumnSpecs(schema)) //
            .createSpec();
    }

    /**
     * Creates a new {@code DataTableSpec} from {@code DataColumnSpec}s of {@code schema}.
     * <p>
     * TODO (TP) javadoc
     *
     * @param dataTableSpec
     * @param schema
     * @return
     * @throws IllegalArgumentException if the provided schema does not have RowKey at column 0
     */
    // TODO (TP) TEMPORARY. exploring API options.
    static DataTableSpec createDataTableSpec(final ValueSchema schema) {
        return new DataTableSpec(dataColumnSpecs(schema));
    }

    /**
     * Extract {@code DataColumnSpec[]} from {@code schema}. This does not include the RowKey, i.e. the returned array
     * contains {@code schema.getColumn(1).dataColumnSpec()} at index 0. Note, that the {@code schema} must have RowKey
     * at column 0, otherwise an {@code IllegalArgumentException} is thrown!
     *
     * @throws IllegalArgumentException if the provided schema does not have RowKey at column 0
     */
    private static DataColumnSpec[] dataColumnSpecs(final ValueSchema schema) {
        if (schema.numColumns() < 1 || !schema.getColumn(0).isRowKey()) {
            throw new IllegalArgumentException("expected schema with RowKey at column 0");
        }
        return Arrays.stream(schema.getColumns()) //
            .skip(1) // skip RowKey column
            .map(ValueSchemaColumn::dataColumnSpec) //
            .toArray(DataColumnSpec[]::new);
    }
}

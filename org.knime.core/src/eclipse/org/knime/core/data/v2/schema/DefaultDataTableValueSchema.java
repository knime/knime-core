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
 *   Oct 13, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;
import java.util.Objects;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.v2.RowKeyValueFactory;
import org.knime.core.data.v2.ValueFactory;

/**
 * Default implementation of {@link DataTableValueSchema}.
 *
 * @author Tobias Pietzsch
 */
sealed class DefaultDataTableValueSchema extends DefaultValueSchema implements DataTableValueSchema
    permits SerializerFactoryValueSchema {

    private final DataTableSpec m_sourceSpec;

    /**
     * Create a {@link DefaultDataTableValueSchema}.
     * <p>
     * {@code columns[0]} must be the RowKey. The {@code DataColumnSpec}s of the given {@code columns} must correspond
     * to the {@code DataColumnSpec}s of the {@code sourceSpec} (except {@code columns[0]} which has no
     * {@code DataColumnSpec}).
     *
     * @param sourceSpec
     * @param columns
     *
     * @throws IllegalArgumentException if the given DataTableSpec and columns are not compatible
     */
    DefaultDataTableValueSchema(final DataTableSpec sourceSpec, final ValueSchemaColumn[] columns)
        throws IllegalArgumentException {
        super(columns);
        if (sourceSpec.getNumColumns() + 1 != columns.length) {
            throw new IllegalArgumentException("Number of columns doesnn't match the sourceSpec.");
        }
        if (!columns[0].isRowKey()) {
            throw new IllegalArgumentException("Expecting RowKey as column 0.");
        }
        for (int i = 1; i < columns.length; ++i) {
            if (!sourceSpec.getColumnSpec(i - 1).equals(columns[i].dataColumnSpec())) {
                throw new IllegalArgumentException(
                    "sourceSpec and columns don't match at column " + i + "(index including RowKey at column 0)");
            }
        }
        m_sourceSpec = Objects.requireNonNull(sourceSpec);
    }

    /**
     * Create a {@link DefaultValueSchema}.
     * <p>
     * The given {@code factories} must correspond to the DataTypes of the {@code sourceSpec}. {@code factories[0]} must
     * be the RowKey.
     */
    DefaultDataTableValueSchema(final DataTableSpec sourceSpec, final ValueFactory<?, ?>[] factories) {
        super(createColumns(Objects.requireNonNull(sourceSpec), Objects.requireNonNull(factories)));
        m_sourceSpec = sourceSpec;
    }

    private static ValueSchemaColumn[] createColumns(final DataTableSpec sourceSpec,
        final ValueFactory<?, ?>[] factories) {
        if (sourceSpec.getNumColumns() + 1 != factories.length) {
            throw new IllegalArgumentException("Number of factories doesnn't match the sourceSpec.");
        }
        if (!(factories[0] instanceof RowKeyValueFactory)) {
            throw new IllegalArgumentException("Expecting a RowKeyValueFactory at factories[0]s.");
        }
        final ValueSchemaColumn[] columns = new ValueSchemaColumn[factories.length];
        Arrays.setAll(columns,
            i -> new ValueSchemaColumn(i == 0 ? null : sourceSpec.getColumnSpec(i - 1), factories[i]));
        return columns;
    }

    @Override
    public DataTableSpec getSourceSpec() {
        return m_sourceSpec;
    }
}

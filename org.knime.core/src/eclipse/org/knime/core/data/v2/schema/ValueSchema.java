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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.v2.RowKeyValueFactory;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueFactoryUtils;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTraits;

/**
 * A {@link ColumnarSchema} with {@link DataColumnSpec DataColumnSpecs} and {@link ValueFactory ValueFactories} for all
 * columns.
 * <p>
 * Compared to {@link DataTableValueSchema} (and {@link DataTableSpec}) there are less constraints on names or types of
 * columns in a {@link ValueSchema}. In particular, a {@code ValueSchema}
 * <ul>
 * <li>need not contain a RowKey column,</li>
 * <li>may contain more than one RowKey column,</li>
 * <li>may contain multiple columns with identical names.</li>
 * </ul>
 *
 * @author Tobias Pietzsch
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ValueSchema extends ColumnarSchema {

    /**
     * Obtain the {@link DataColumnSpec} of the column at a given index. (Returns {@code null}, if the column at
     * {@code index} is a RowKey).
     *
     * @param index colunm index
     * @return the DataColumnSpec of the column at the given index
     * @since 5.8
     */
    default DataColumnSpec getDataColumnSpec(final int index) {
        return getColumn(index).dataColumnSpec();
    }

    /**
     * Find a column by name.
     * <p>
     * Returns the index of the first column with the specified name.
     * Returns -1 if no such column is found.
     *
     * @param columnName name of the column
     * @return the index of the first column with the specified columnName
     * @since 5.8
     */
    default int findColumnIndex(final String columnName) {
        if (columnName != null) {
            for (int i = 0; i < numColumns(); ++i) {
                var spec = getDataColumnSpec(i);
                if (spec != null && spec.getName().equals(columnName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Get the {@link ValueFactory} at a given column {@code index}.
     *
     * @param <R> {@link ReadAccess} the returned {@link ValueFactory} expects
     * @param <W> {@link WriteAccess} the returned {@link ValueFactory} expects
     * @param index of the value factory to return
     * @return the {@link ValueFactory} at the provided index
     */
    default <R extends ReadAccess, W extends WriteAccess> ValueFactory<R, W> getValueFactory(final int index) {
        return getColumn(index).castValueFactory();
    }

    @Override
    default DataSpec getSpec(final int index) {
        return getColumn(index).dataSpec();
    }

    @Override
    default DataTraits getTraits(final int index) {
        return getColumn(index).dataTraits();
    }


    /**
     * Describes a single column in a {@link ValueSchema}.
     * <p>
     * A {@code ValueSchemaColumn} contains a {@link DataColumnSpec} with the logical type and name of the column, as
     * well as a {@link ValueFactory} and {@link DataTraits}. (If the {@link #valueFactory()} is a
     * {@code RowKeyValueFactory}, the {@link #dataColumnSpec()} is {@code null}.)
     * <p>
     * Compared to {@link DataTableValueSchema} (and {@link DataTableSpec}) there are less constraints on names or types
     * of columns in a {@link ValueSchema}. In particular, a {@code ValueSchema}
     * <ul>
     * <li>need not contain a RowKey column,</li>
     * <li>may contain more than one RowKey column,</li>
     * <li>may contain multiple columns with identical names.</li>
     * </ul>
     *
     * @param dataColumnSpec the DataColumnSpec of this column (or {@code null}, if this is a RowKey column)
     * @param valueFactory the ValueFactory of this column
     * @param dataTraits the DataTraits of this column
     * @since 5.8
     */
    public record ValueSchemaColumn( //
        DataColumnSpec dataColumnSpec, //
        ValueFactory<?, ?> valueFactory, //
        DataTraits dataTraits) {
        /**
         * Create a {@code ValueSchemaColumn}. The {@code dataColumnSpec} and {@code valueFactory} must be compatible.
         * (The {@code dataColumnSpec} for a {@code RowKeyValueFactory} must be {@code null}.)
         *
         * @param dataColumnSpec
         * @param valueFactory
         * @param dataTraits
         */
        public ValueSchemaColumn {
            if (dataColumnSpec == null) {
                if (!(valueFactory instanceof RowKeyValueFactory)) {
                    throw new IllegalArgumentException(
                        "if dataColumnSpec==null, then valueFactory must be a RowKeyValueFactory");
                }
            } else {
                if (!dataColumnSpec.getType().equals(ValueFactoryUtils.getDataTypeForValueFactory(valueFactory))) {
                    throw new IllegalArgumentException("dataColumnSpec and valueFactory don't match");
                }
            }
        }

        /**
         * Create a {@code ValueSchemaColumn}. The {@code dataColumnSpec} and {@code valueFactory} must be compatible.
         * (The {@code dataColumnSpec} for a {@code RowKeyValueFactory} must be {@code null}.)
         * <p>
         * DataTraits are created via {@link ValueFactoryUtils#getTraits(ValueFactory)}.
         *
         * @param dataColumnSpec the DataColumnSpec of this column
         * @param valueFactory the ValueFactory of this column
         */
        public ValueSchemaColumn(final DataColumnSpec dataColumnSpec, final ValueFactory<?, ?> valueFactory) {
            this(dataColumnSpec, valueFactory, ValueFactoryUtils.getTraits(valueFactory));
        }

        /**
         * Create a {@code ValueSchemaColumn}. The {@code dataType} and {@code valueFactory} must be compatible.
         * (Note that this method can not be used to create a RowKey column.)
         * <p>
         * DataTraits are created via {@link ValueFactoryUtils#getTraits(ValueFactory)}.
         *
         * @param name the column name
         * @param type the column type
         * @param valueFactory the ValueFactory of this column
         * @throws NullPointerException if either the column name or type is <code>null</code>
         */
        public ValueSchemaColumn(final String name, final DataType type, final ValueFactory<?, ?> valueFactory) {
            this(new DataColumnSpecCreator(name, type).createSpec(), valueFactory,
                ValueFactoryUtils.getTraits(valueFactory));
        }

        /**
         * Create a RowKey {@code ValueSchemaColumn}.
         *
         * @param valueFactory the RowKeyValueFactory
         */
        public ValueSchemaColumn(final RowKeyValueFactory<?, ?> valueFactory) {
            this(null, valueFactory, ValueFactoryUtils.getTraits(valueFactory));
        }

        /**
         * Get a ValueSchemaColumn with the given {@code DataColumnSpec} and otherwise the same properties as
         * {@code this} one.
         *
         * @param columnSpec the DataColumnSpec of the new column
         * @return a ValueSchemaColumn with the given DataColumnSpec
         */
        public ValueSchemaColumn with(final DataColumnSpec columnSpec) {
            return new ValueSchemaColumn(columnSpec, valueFactory, dataTraits);
        }

        /**
         * Get the {@code DataSpec} of this column.
         *
         * @return the DataSpec of this column
         */
        public DataSpec dataSpec() {
            return valueFactory.getSpec();
        }

        /**
         * Get the {@code ValueFactory} of this column.
         *
         * @param <R> {@link ReadAccess} the returned {@link ValueFactory} expects
         * @param <W> {@link WriteAccess} the returned {@link ValueFactory} expects
         * @return the ValueFactory of this column
         */
        @SuppressWarnings("unchecked")
        <R extends ReadAccess, W extends WriteAccess> ValueFactory<R, W> castValueFactory() {
            return (ValueFactory<R, W>)valueFactory();
        }

        /**
         * Returns {@code true} if this column is a RowKey.
         *
         * @return {@code true} if this column is a RowKey
         */
        public boolean isRowKey() {
            return dataColumnSpec == null && valueFactory instanceof RowKeyValueFactory;
        }

        /**
         * Returns {@code true}, if the {@link #dataColumnSpec()} of {@code this} and the given {@code column} have
         * {@link DataColumnSpec#equalStructure} (or are both {@code null}).
         *
         * @param column the column to compare with
         * @return true, if this and the given column have equal structure
         */
        public boolean equalStructure(final ValueSchemaColumn column) {
            if (dataColumnSpec == null) {
                return column.dataColumnSpec == null;
            } else {
                return dataColumnSpec.equalStructure(column.dataColumnSpec);
            }
        }
    }

    /**
     * Get the properties of the column at the given {@code index}.
     *
     * @param index column index
     * @return column properties
     * @since 5.8
     */
    ValueSchemaColumn getColumn(int index);

    /**
     * Get properties of all columns.
     *
     * @return column properties
     * @since 5.8
     */
    default ValueSchemaColumn[] getColumns() {
        final var columns = new ValueSchemaColumn[numColumns()];
        Arrays.setAll(columns, this::getColumn);
        return columns;
    }
}

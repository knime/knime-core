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
import org.knime.core.data.v2.RowKeyValueFactory;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.ValueFactoryUtils;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.traits.DataTraits;

/**
 * Decorates a DataTableSpec with ValueFactories for all columns (including the RowKey).
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ValueSchema extends ColumnarSchema {

    /**
     * TODO (TP) javadoc
     *
     * @param index colunm index (rowkey is 0)
     * @return
     * @since 5.5
     */
    DataColumnSpec getDataColumnSpec(int index);

    /**
     * TODO (TP) javadoc
     *
     * Find index of a (data) column by its name.
     * (The RowKey column doesn't have a name.)
     * <p>
     * Finds first column with matching name
     * <p>
     * Returns -1 if not found.
     *
     *
     * @param columnName name of the column.
     * @return
     * @since 5.5
     */
    // TODO (TP) Make a (lazy?) lookup map, like DataTableSpec does?
    //
    // TODO (TP) Probably this should fail if column names are not unique?
    //           So just lean on getSourceSpec() and add +1 (depending on whether hasRowKey)?
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
     * TODO (TP) javadoc
     *
     * Returns the {@link DataColumnSpec} of the column with the provided name.
     * This method returns {@code null} if the argument is {@code null}.
     *
     * @param columnName the column name to find the {@code DataColumnSpec} for
     * @return the column specification or {@code null} if not available
     * @since 5.5
     */
    default DataColumnSpec getColumnSpec(final String columnName) {
        int columnIndex = findColumnIndex(columnName);
        if (columnIndex == -1) {
            return null;
        }
        return getDataColumnSpec(columnIndex);
    }

    /**
     * @param <R> {@link ReadAccess} the returned {@link ValueFactory} expects
     * @param <W> {@link WriteAccess} the returned {@link ValueFactory} expects
     * @param index of the value factory to return
     * @return the {@link ValueFactory} at the provided index
     */
    <R extends ReadAccess, W extends WriteAccess> ValueFactory<R, W> getValueFactory(int index);

    /**
     * @since 5.5
     */
    public record ValueSchemaColumn(//
        DataColumnSpec dataColumnSpec, //
        ValueFactory<?, ?> valueFactory, //
        DataSpec dataSpec, //
        DataTraits dataTraits//
    ) {
        public ValueSchemaColumn(final DataColumnSpec dataColumnSpec, final ValueFactory<?, ?> valueFactory) {
            this(dataColumnSpec, valueFactory, valueFactory.getSpec(), ValueFactoryUtils.getTraits(valueFactory));
        }

        public ValueSchemaColumn with(final DataColumnSpec newDataColumnSpec) {
            return new ValueSchemaColumn(newDataColumnSpec, valueFactory, dataSpec, dataTraits);
        }

        @SuppressWarnings("unchecked")
        <R extends ReadAccess, W extends WriteAccess> ValueFactory<R, W> castValueFactory() {
            return (ValueFactory<R, W>)valueFactory();
        }

        public boolean isRowKey() {
            return dataColumnSpec == null && valueFactory instanceof RowKeyValueFactory;
        }

        public boolean isCompatibleWith(final ValueSchemaColumn column) {
            if (!valueFactory.getClass().equals(column.valueFactory.getClass())) {
                return false;
            }
            if (!dataSpec.equals(column.dataSpec)) {
                return false;
            }
            //
            // TODO (TP): what about dataTraits? Do they have to match too?
            //
            if (dataColumnSpec == null) {
                return column.dataColumnSpec == null;
            } else {
                return dataColumnSpec.isCompatibleWith(column.dataColumnSpec);
            }
        }

        public boolean equalStructure(final ValueSchemaColumn column) {
            if (!valueFactory.getClass().equals(column.valueFactory.getClass())) {
                return false;
            }
            if (!dataSpec.equals(column.dataSpec)) {
                return false;
            }
            //
            // TODO (TP): what about dataTraits? Do they have to match too?
            //
            if (dataColumnSpec == null) {
                return column.dataColumnSpec == null;
            } else {
                return dataColumnSpec.equalStructure(column.dataColumnSpec);
            }
        }
    }

    /**
     * TODO (TP): use this method to get info about a column, exclusively.
     *            Instead of:
     *              - getValueFactory
     *              - getSpecWithTraits
     *              - getTraits
     *              - getSpec
     *              - getDataColumnSpec
     *
     * @param index
     * @return
     * @since 5.5
     */
    ValueSchemaColumn getColumn(int index);

    /**
     * TODO (TP): decide contract: Does this method always return a copy or the internal array if it exists?
     *
     * @return
     * @since 5.5
     */
    default ValueSchemaColumn[] getColumns() {
        final var columns = new ValueSchemaColumn[numColumns()];
        Arrays.setAll(columns, this::getColumn);
        return columns;
    }
}

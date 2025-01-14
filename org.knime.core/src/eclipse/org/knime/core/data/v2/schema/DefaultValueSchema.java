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
 *   27 Mar 2025 (pietzsch): created
 */
package org.knime.core.data.v2.schema;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;

import com.google.common.collect.Iterators;

/**
 * Default implementation of {@link ValueSchema}.
 *
 * @author Tobias Pietzsch
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
sealed class DefaultValueSchema implements ValueSchema permits DefaultDataTableValueSchema {

    private final ValueSchemaColumn[] m_columns;

    DefaultValueSchema(final ValueSchemaColumn[] columns)
    {
        m_columns = Objects.requireNonNull(columns);
    }

    @Override
    public ValueSchemaColumn getColumn(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.format("Column index %d smaller than 0.", index));
        } else if (index >= numColumns()) {
            throw new IndexOutOfBoundsException(
                String.format("Column index %d greater than largest column index (%d).", index, numColumns() - 1));
        }
        return m_columns[index];
    }

    // -------- ColumnarSchema --------

    @Override
    public int numColumns() {
        return m_columns.length;
    }

    @Override
    public Stream<DataSpec> specStream() {
        return Arrays.stream(m_columns).map(ValueSchemaColumn::dataSpec);
    }

    @Override
    public Iterator<DataSpec> iterator() {
        return specStream().iterator();
    }

    @Override
    public int hashCode() {
        // NB: The following computes the same hashCode as
        //     Arrays.hashCode(specStream().toArray()) would,
        //     but avoids the array creation overhead.
        int result = 1;
        for (ValueSchemaColumn col : m_columns) {
            result = 31 * result + col.dataSpec().hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ColumnarSchema)) { // NOSONAR
            return false;
        }
        final ColumnarSchema other = (ColumnarSchema)obj;
        if (numColumns() != other.numColumns()) {
            return false;
        }
        return Iterators.elementsEqual(iterator(), other.iterator());
    }
}

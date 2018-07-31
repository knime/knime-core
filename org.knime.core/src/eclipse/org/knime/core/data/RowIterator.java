/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data;

import java.util.Iterator;
import java.util.function.Supplier;

import org.knime.core.node.util.CheckUtils;

/**
 * Classes extending this class iterate over the rows of a {@link DataTable}.
 * Each DataTable has its specific <code>RowIterator</code>, which returns
 * the rows one by one. A <code>RowIterator</code> must return the rows always
 * in the same order.
 *
 * <p>
 * Use RowIterators as follows:
 *
 * <pre>
 *     DataTable table = ...;
 *     for (RowIterator it = table.getRowIterator(); it.hasNext();) {
 *         DataRow row = it.next();
 *         ...
 *     }
 * </pre>
 *
 * <p>
 * or, if you don't need access to the iterator:
 *
 * <pre>
 *     DataTable table =...;
 *     for (DataRow row : table) {
 *       // access the row here
 *     }
 * </pre>
 *
 * <p>
 * Note, the difference of this class to a generic Iterator&lt;DataRow&gt; is
 * that it does not allow to remove elements.
 *
 * @see DataRow
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 */
public abstract class RowIterator implements Iterator<DataRow> {

    /**
     * Returns <code>true</code> if there are more rows and <code>false</code>
     * otherwise.
     *
     * @see RowIterator#next()
     * @return <code>true</code> if the iterator has more elements, otherwise
     *         <code>false</code>
     */
    @Override
    public abstract boolean hasNext();

    /**
     * Returns the next <code>DataRow</code>.
     *
     * @return the next row in the <code>DataTable</code>
     * @throws java.util.NoSuchElementException if there are no more rows
     */
    @Override
    public abstract DataRow next();

    /**
     * NOT supported by the DataTable iterator! DataTables are immutable
     * read-only objects after their creation. Do not call this method, it will
     * throw an exception.
     *
     * @throws UnsupportedOperationException if the <tt>remove</tt>
     *                operation is not supported by this Iterator.
     */
    @Override
    public final void remove() {
        throw new UnsupportedOperationException("Can't remove row from table."
                + " Data tables are read-only.");
    }

    /**
     * Interface for classes building {@link RowIterator}s that only iterate over parts of a table.
     *
     * @param <I> the row iterator to be built
     *
     * @since 3.7
     */
    // TODO move to extra class
    public static interface RowIteratorBuilder<I extends RowIterator> {

        /**
         * Iterate only over selected columns of the table. Accessing {@link org.knime.core.data.DataCell}s with indices
         * other than the specified indices will lead to an
         * {@link org.knime.core.data.UnmaterializedCell.UnmaterializedDataCellException} being thrown.
         *
         * @param indices the indices of columns over which to iterate
         * @return this {@link RowIteratorBuilder}
         * @throws IndexOutOfBoundsException for indices smaller than 0 or larger than the width of the table
         * @throws IllegalArgumentException if there are duplicates amongst the indices
         *
         * @see org.knime.core.data.DataRow
         */
        RowIteratorBuilder<I> filterColumns(int... indices);

        /**
         * Returns a row iterator over selected columns of each row from the table. Accessing
         * {@link org.knime.core.data.DataCell}s from a column with a name other than the ones specified will lead to an
         * {@link org.knime.core.data.UnmaterializedCell.UnmaterializedDataCellException} being thrown.
         *
         * @param columns the names of columns over which to iterate
         * @return this {@link RowIteratorBuilder}
         * @throws IllegalArgumentException if a column name is not found or specified multiple times
         *
         * @see org.knime.core.data.DataRow
         */
        RowIteratorBuilder<I> filterColumns(String... columns);

        /**
         * Build a new row iterator with the behavior specified via methods invoked in this builder.
         *
         * @return a new row iterator
         */
        I build();

    }

    /**
     * A {@link RowIteratorBuilder} that always builds default {@link RowIterator}s, i.e., iterators iterating over all
     * rows and columns of a table.
     *
     * @param <I> the row iterator to be built
     *
     * @since 3.7
     */
    public static class DefaultRowIteratorBuilder<I extends RowIterator> implements RowIteratorBuilder<I> {

        private final Supplier<I> m_iteratorSupplier;

        private final DataTableSpec m_spec;

        /**
         * Constructs a new {@link DefaultRowIteratorBuilder}.
         *
         * @param iteratorSupplier the supplier default iterators that are to be returned when
         *            {@link DefaultRowIteratorBuilder#build()} is invoked
         * @param spec the specification of the table over which to iterate
         */
        public DefaultRowIteratorBuilder(final Supplier<I> iteratorSupplier, final DataTableSpec spec) {
            m_iteratorSupplier = CheckUtils.checkArgumentNotNull(iteratorSupplier, "Argument must not be null");
            m_spec = CheckUtils.checkArgumentNotNull(spec, "Spec must not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowIteratorBuilder<I> filterColumns(final int... indices) {
            m_spec.verifyIndices(indices);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowIteratorBuilder<I> filterColumns(final String... columns) {
            return filterColumns(m_spec.columnsToIndices(columns));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public I build() {
            return m_iteratorSupplier.get();
        }

    }
}

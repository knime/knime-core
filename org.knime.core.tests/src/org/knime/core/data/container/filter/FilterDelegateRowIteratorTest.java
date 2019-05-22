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
 */
package org.knime.core.data.container.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.custom;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.equal;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.greaterOrEqual;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.lesserOrEqual;
import static org.knime.core.data.container.filter.predicate.TypedColumn.boolCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.intCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.longCol;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.predicate.FilterPredicate;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Unit tests for the the {@link FilterDelegateRowIterator} class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class FilterDelegateRowIteratorTest {

    private static final List<DataRow> ROWS =
        IntStream.range(0, 100)
            .mapToObj(i -> new DefaultRow(new RowKey(Integer.toString(i)), new IntCell(i),
                new StringCell(Integer.toString(i)), new LongCell(i), new DoubleCell(i),
                i % 2 == 1 ? BooleanCell.TRUE : BooleanCell.FALSE))
            .collect(Collectors.toList());

    private static class TestIterator extends CloseableRowIterator {
        private final Iterator<DataRow> m_delegate = ROWS.iterator();

        @Override
        public boolean hasNext() {
            return m_delegate.hasNext();
        }

        @Override
        public DataRow next() {
            return m_delegate.next();
        }

        @Override
        public void close() {
        }
    }

    /**
     * Tests that {@link TableFilter TableFilters} filtering some specific rows are correctly handled by a
     * {@link FilterDelegateRowIterator}.
     */
    @Test
    public void testFilterSome() {
        // keep only rows with an index that is even and between 13 and 17 (i.e. 14, 16)
        FilterPredicate pred =
            greaterOrEqual(intCol(0), 10).and(lesserOrEqual(longCol(2), 20l)).and(equal(boolCol(4), false));
        TableFilter filter = (new TableFilter.Builder()).withFilterPredicate(pred).withFromRowIndex(13)
            .withToRowIndex(17).build();

        try (final CloseableRowIterator rowIt = new FilterDelegateRowIterator(new TestIterator(), filter, null)) {
            assertTrue(rowIt.hasNext());
            assertEquals("14", rowIt.next().getKey().getString());
            assertTrue(rowIt.hasNext());
            assertEquals("16", rowIt.next().getKey().getString());
            assertFalse(rowIt.hasNext());
        }
    }

    /**
     * Tests that {@link TableFilter TableFilters} filtering all rows are correctly handled by a
     * {@link FilterDelegateRowIterator}.
     */
    @Test(expected = NoSuchElementException.class)
    public void testFilterAll() {
        TableFilter filter = TableFilter.filterRows(custom(intCol(0), i -> false));
        try (final CloseableRowIterator rowIt = new FilterDelegateRowIterator(new TestIterator(), filter, null)) {
            assertFalse(rowIt.hasNext());
            rowIt.next();
        }
    }

    /**
     * Tests that {@link TableFilter TableFilters} filtering no rows are correctly handled by a
     * {@link FilterDelegateRowIterator}.
     */
    @Test
    public void testFilterNone() {
        final Iterator<DataRow> referenceIt = ROWS.iterator();
        final TableFilter filter = TableFilter.filterRows(custom(intCol(0), i -> true));
        try (final CloseableRowIterator rowIt = new FilterDelegateRowIterator(new TestIterator(), filter, null)) {
            while (rowIt.hasNext() && referenceIt.hasNext()) {
                assertEquals(rowIt.next(), referenceIt.next());
            }
            assertEquals(rowIt.hasNext(), referenceIt.hasNext());
        }
    }

}

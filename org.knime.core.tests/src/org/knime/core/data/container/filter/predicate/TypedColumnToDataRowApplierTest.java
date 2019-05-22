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
package org.knime.core.data.container.filter.predicate;

import static org.junit.Assert.assertEquals;
import static org.knime.core.data.container.filter.predicate.TypedColumn.boolCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.doubleCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.intCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.longCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.rowKey;
import static org.knime.core.data.container.filter.predicate.TypedColumn.stringCol;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.filter.predicate.FilterPredicateToDataRowApplier.TypedColumnToDataRowApplier;
import org.knime.core.data.container.filter.predicate.IndexedColumn.BooleanColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.DoubleColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.IntColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.LongColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.StringColumn;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Unit tests for the {@link TypedColumn} class and the TypedColumnToDataRowApplier.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class TypedColumnToDataRowApplierTest {

    private static final DataRow ROW = new DefaultRow(new RowKey("1"), new IntCell(0),
        new LongCell(1l), new DoubleCell(2d), new StringCell("3"), BooleanCell.TRUE);

    /**
     * Tests the {@link RowKeyColumn}.
     */
    @Test
    public void testRowKey() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals("1"));
        assertEquals(rowKey().accept(columnVisitor), Boolean.TRUE);
    }

    /**
     * Tests the {@link IntColumn}.
     */
    @Test
    public void testIntCol() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals(0));
        assertEquals(intCol(0).accept(columnVisitor), Boolean.TRUE);
    }

    /**
     * Tests the {@link LongColumn}.
     */
    @Test
    public void testLongCol() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals(1l));
        assertEquals(longCol(1).accept(columnVisitor), Boolean.TRUE);
    }

    /**
     * Tests the {@link DoubleColumn}.
     */
    @Test
    public void testDoubleCol() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals(2d));
        assertEquals(doubleCol(2).accept(columnVisitor), Boolean.TRUE);
    }

    /**
     * Tests the {@link StringColumn}.
     */
    @Test
    public void testStringCol() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals("3"));
        assertEquals(stringCol(3).accept(columnVisitor), Boolean.TRUE);
    }

    /**
     * Tests the {@link BooleanColumn}.
     */
    @Test
    public void testBoolCol() {
        TypedColumnToDataRowApplier columnVisitor = new TypedColumnToDataRowApplier(ROW, t -> t.equals(Boolean.TRUE));
        assertEquals(boolCol(4).accept(columnVisitor), Boolean.TRUE);
    }

}

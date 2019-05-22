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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.custom;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.equal;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.greater;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.greaterOrEqual;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.isMissing;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.lesser;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.lesserOrEqual;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.notEqual;
import static org.knime.core.data.container.filter.predicate.TypedColumn.intCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.longCol;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.And;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.Or;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.CustomPredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.EqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.MissingValuePredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.NotEqualTo;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Unit tests for the {@link FilterPredicate} class and the {@link FilterPredicateToDataRowApplier}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class FilterPredicateToDataRowApplierTest {

    private static final DataTableSpec INT_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("int", IntCell.TYPE).createSpec());

    private static final DataTableSpec LONG_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("long", LongCell.TYPE).createSpec());

    private static final FilterPredicateToDataRowApplier ONE =
        new FilterPredicateToDataRowApplier(new DefaultRow(RowKey.createRowKey(1l), new IntCell(1)));

    private static final FilterPredicateToDataRowApplier TWO =
        new FilterPredicateToDataRowApplier(new DefaultRow(RowKey.createRowKey(2l), new IntCell(2)));

    private static final FilterPredicateToDataRowApplier THREE =
        new FilterPredicateToDataRowApplier(new DefaultRow(RowKey.createRowKey(3l), new IntCell(3)));

    private static final FilterPredicateToDataRowApplier MISSING =
        new FilterPredicateToDataRowApplier(new DefaultRow(RowKey.createRowKey(4l), DataType.getMissingCell()));

    /**
     * Tests the {@link MissingValuePredicate}.
     */
    @Test
    public void testMissingValuePredicate() {
        final FilterPredicate missing = isMissing(intCol(0));
        assertFalse(missing.accept(ONE));
        assertFalse(missing.accept(TWO));
        assertFalse(missing.accept(THREE));
        assertTrue(missing.accept(MISSING));
    }

    /**
     * Tests the {@link CustomPredicate}.
     */
    @Test
    public void testCustomPredicate() {
        final FilterPredicate udfEq2 = custom(intCol(0), i -> i == 2);
        assertFalse(udfEq2.accept(ONE));
        assertTrue(udfEq2.accept(TWO));
        assertFalse(udfEq2.accept(THREE));
        assertFalse(udfEq2.accept(MISSING));
    }

    /**
     * Tests the {@link EqualTo} predicate.
     */
    @Test
    public void testEq() {
        final FilterPredicate eq2 = equal(intCol(0), 2);
        assertFalse(eq2.accept(ONE));
        assertTrue(eq2.accept(TWO));
        assertFalse(eq2.accept(THREE));
        assertFalse(eq2.accept(MISSING));
    }

    /**
     * Tests the {@link NotEqualTo} predicate.
     */
    @Test
    public void testNeq() {
        final FilterPredicate neq2 = notEqual(intCol(0), 2);
        assertTrue(neq2.accept(ONE));
        assertFalse(neq2.accept(TWO));
        assertTrue(neq2.accept(THREE));
        assertFalse(neq2.accept(MISSING));
    }

    /**
     * Tests the {@link LesserThan} predicate.
     */
    @Test
    public void testLt() {
        final FilterPredicate lt2 = lesser(intCol(0), 2);
        assertTrue(lt2.accept(ONE));
        assertFalse(lt2.accept(TWO));
        assertFalse(lt2.accept(THREE));
        assertFalse(lt2.accept(MISSING));
    }

    /**
     * Tests the {@link LesserThanOrEqualTo} predicate.
     */
    @Test
    public void testLeq() {
        final FilterPredicate leq2 = lesserOrEqual(intCol(0), 2);
        assertTrue(leq2.accept(ONE));
        assertTrue(leq2.accept(TWO));
        assertFalse(leq2.accept(THREE));
        assertFalse(leq2.accept(MISSING));
    }

    /**
     * Tests the {@link GreaterThan} predicate.
     */
    @Test
    public void testGt() {
        final FilterPredicate gt2 = greater(intCol(0), 2);
        assertFalse(gt2.accept(ONE));
        assertFalse(gt2.accept(TWO));
        assertTrue(gt2.accept(THREE));
        assertFalse(gt2.accept(MISSING));
    }

    /**
     * Tests the {@link GreaterThanOrEqualTo} predicate.
     */
    @Test
    public void testGeq() {
        final FilterPredicate geq2 = greaterOrEqual(intCol(0), 2);
        assertFalse(geq2.accept(ONE));
        assertTrue(geq2.accept(TWO));
        assertTrue(geq2.accept(THREE));
        assertFalse(geq2.accept(MISSING));
    }

    /**
     * Tests the {@link Not} predicate.
     */
    @Test
    public void testNegate() {
        final FilterPredicate neq2 = equal(intCol(0), 2).negate();
        assertTrue(neq2.accept(ONE));
        assertFalse(neq2.accept(TWO));
        assertTrue(neq2.accept(THREE));
        assertTrue(neq2.accept(MISSING));
    }

    /**
     * Tests the {@link Or} predicate.
     */
    @Test
    public void testOr() {
        final FilterPredicate lt2OrGt2 = lesser(intCol(0), 2).or(greater(intCol(0), 2));
        assertTrue(lt2OrGt2.accept(ONE));
        assertFalse(lt2OrGt2.accept(TWO));
        assertTrue(lt2OrGt2.accept(THREE));
        assertFalse(lt2OrGt2.accept(MISSING));
    }

    /**
     * Tests the {@link And} predicate.
     */
    @Test
    public void testAnd() {
        final FilterPredicate leq2AndGeq2 = lesserOrEqual(intCol(0), 2).and(greaterOrEqual(intCol(0), 2));
        assertFalse(leq2AndGeq2.accept(ONE));
        assertTrue(leq2AndGeq2.accept(TWO));
        assertFalse(leq2AndGeq2.accept(THREE));
        assertFalse(leq2AndGeq2.accept(MISSING));
    }

    /**
     * Tests that validating a {@link FilterPredicate} using a {@link FilterPredicateValidator} throws an
     * {@link IndexOutOfBoundsException} when the predicate contains columns with indices below 0.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsLow() {
        equal(intCol(-1), 2).accept(new FilterPredicateValidator(INT_SPEC));
    }

    /**
     * Tests that validating a {@link FilterPredicate} using a {@link FilterPredicateValidator} throws an
     * {@link IndexOutOfBoundsException} when the predicate contains columns with indices larger than the table width
     * specified in the {@link DataTableSpec}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsHigh() {
        equal(intCol(1), 2).accept(new FilterPredicateValidator(INT_SPEC));
    }

    /**
     * Tests that validating a {@link FilterPredicate} using a {@link FilterPredicateValidator} throws no exception if
     * the column's type in the {@link DataTableSpec} is equal to the type specified in the {@link TypedColumn} class.
     */
    @Test
    public void testIntSpecCompatibleToIntColumn() {
        equal(intCol(0), 2).accept(new FilterPredicateValidator(INT_SPEC));
    }

    /**
     * Tests that validating a {@link FilterPredicate} using a {@link FilterPredicateValidator} throws an
     * {@link IllegalArgumentException} if the column's type in the {@link DataTableSpec} in the {@link DataTableSpec}
     * is compatible to, but not equal to the type in the {@link TypedColumn}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIntSpecIncompatibleToLongColumn() {
        equal(longCol(0), 2l).accept(new FilterPredicateValidator(INT_SPEC));
    }

    /**
     * Tests that validating a {@link FilterPredicate} using a {@link FilterPredicateValidator} throws an
     * {@link IllegalArgumentException} if the column's type in the {@link DataTableSpec} in the {@link TypedColumn} is
     * compatible to, but not equal to the type in the {@link DataTableSpec}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLongSpecIncompatibleToIntColumn() {
        equal(intCol(0), 2).accept(new FilterPredicateValidator(LONG_SPEC));
    }

}

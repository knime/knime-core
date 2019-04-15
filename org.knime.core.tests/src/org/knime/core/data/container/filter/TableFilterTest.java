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
 *   11 Apr 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container.filter;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Unit tests for the {@link TableFilter} class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class TableFilterTest {

    private static final DataTableSpec SPEC =
        new DataTableSpec(new DataColumnSpecCreator("int", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("long", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("double", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("string", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("boolean", BooleanCell.TYPE).createSpec());

    /**
     * Tests that attempting to build a filter with an index below 0 for a to-be-materialized column throws an
     * {@link IndexOutOfBoundsException}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMaterializeColumnsIndexOutOfBoundsLow() {
        TableFilter.materializeCols(2, -1).validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with a too high index for a to-be-materialized column throws an
     * {@link IndexOutOfBoundsException}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMaterializeColumnsIndexOutOfBoundsHigh() {
        TableFilter.materializeCols(2, 5).validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with duplicate indices for to-be-materialized columns throws an
     * {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMaterializeColumnsDuplicateIndex() {
        TableFilter.materializeCols(2, 2).validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with an invalid column name for a to-be-materialized column throws an
     * {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMaterializeColumnsNonExistingColumnName() {
        TableFilter.materializeCols(SPEC, "long", "intt").validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with a from-row index below 0 throw an {@link IndexOutOfBoundsException}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testFilterRowsFromIndexOutOfBounds() {
        TableFilter.filterRowsFromIndex(-13).validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with a to-row index below 0 throw an {@link IndexOutOfBoundsException}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testFilterRowsToIndexOutOfBounds() {
        TableFilter.filterRowsToIndex(-5).validate(SPEC);
    }

    /**
     * Tests that attempting to build a filter with an invalid range of rows throw an {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRangeOfRows() {
        TableFilter.filterRangeOfRows(5, 3).validate(SPEC);
    }

}

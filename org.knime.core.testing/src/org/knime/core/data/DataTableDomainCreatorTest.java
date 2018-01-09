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
 *   05.06.2014 (thor): created
 */
package org.knime.core.data;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Testcases for {@link DataTableDomainCreator}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class DataTableDomainCreatorTest {
    /**
     * Check whether upper and lower bounds are computed correctly for double column (including infinity and NaN).
     */
    @Test
    public void testBoundsDouble() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("Double col", DoubleCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(colSpecCrea.createSpec());

        RowKey rowKey = new RowKey("Row0");
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, false);

        // initially bounds are null
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is(nullValue()));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is(nullValue()));

        // NaN values are ignored completely
        domainCreator.updateDomain(new DefaultRow(rowKey, Double.NaN));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is(nullValue()));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is(nullValue()));

        // missing cells are also ignored
        domainCreator.updateDomain(new DefaultRow(rowKey, DataType.getMissingCell()));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is(nullValue()));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is(nullValue()));

        // change lower and upper bound
        domainCreator.updateDomain(new DefaultRow(rowKey, 0.0));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(0)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(0)));

        // change upper bound
        domainCreator.updateDomain(new DefaultRow(rowKey, 1.0));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(0)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(1)));

        // change lower bound
        domainCreator.updateDomain(new DefaultRow(rowKey, -1.0));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(-1)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(1)));

        // ignore NaN (again)
        domainCreator.updateDomain(new DefaultRow(rowKey, Double.NaN));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(-1)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(1)));

        // ignore missing values (again)
        domainCreator.updateDomain(new DefaultRow(rowKey, DataType.getMissingCell()));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(-1)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(1)));

        // change lower bound to -Inf
        domainCreator.updateDomain(new DefaultRow(rowKey, Double.NEGATIVE_INFINITY));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(
            Double.NEGATIVE_INFINITY)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(1)));

        // change upper bound to +Inf
        domainCreator.updateDomain(new DefaultRow(rowKey, Double.POSITIVE_INFINITY));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new DoubleCell(
            Double.NEGATIVE_INFINITY)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new DoubleCell(
            Double.POSITIVE_INFINITY)));
    }

    /**
     * Check whether upper and lower bounds are computed correctly for int column.
     */
    @Test
    public void testBoundsInt() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("Int col", IntCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(colSpecCrea.createSpec());

        RowKey rowKey = new RowKey("Row0");
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, false);

        // initially bounds are null
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is(nullValue()));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is(nullValue()));

        // missing cells are ignored
        domainCreator.updateDomain(new DefaultRow(rowKey, DataType.getMissingCell()));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is(nullValue()));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is(nullValue()));

        // change lower and upper bound
        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(0)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(0)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(0)));

        // change upper bound
        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(1)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(0)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(1)));

        // change lower bound
        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(-1)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(-1)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(1)));

        // ignore missing values (again)
        domainCreator.updateDomain(new DefaultRow(rowKey, DataType.getMissingCell()));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(-1)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(1)));

        // change lower bound to MIN_VALUE
        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(Integer.MIN_VALUE)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(Integer.MIN_VALUE)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(1)));

        // change upper bound to MAX_VALUE
        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(Integer.MAX_VALUE)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell)new IntCell(Integer.MIN_VALUE)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell)new IntCell(Integer.MAX_VALUE)));
    }

    /**
     * Checks whether possible values are computed correctly.
     */
    @Test
    public void testPossibleValues() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("String col", StringCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(colSpecCrea.createSpec());

        RowKey rowKey = new RowKey("Row0");
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, false);
        domainCreator.setMaxPossibleValues(2);

        // initially no values
        Set<DataCell> expectedValues = new LinkedHashSet<>();
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));

        // add two values
        expectedValues.add(new StringCell("v1"));
        domainCreator.updateDomain(new DefaultRow(rowKey, "v1"));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));

        expectedValues.add(new StringCell("v2"));
        domainCreator.updateDomain(new DefaultRow(rowKey, "v2"));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));

        // add more than the maximum number removes all values
        domainCreator.updateDomain(new DefaultRow(rowKey, "v3"));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(nullValue()));
    }

    /**
     * Check whether a negative number of possible values is rejected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxPossibleValues() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("String col", StringCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(colSpecCrea.createSpec());
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, false);
        domainCreator.setMaxPossibleValues(-1);
    }


    /**
     * Checks whether bounds are initialized correctly if requested.
     */
    @Test
    public void testInitBounds() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("Int col", IntCell.TYPE);
        DataColumnDomainCreator domainCrea = new DataColumnDomainCreator();
        domainCrea.setLowerBound(new IntCell(-2));
        domainCrea.setUpperBound(new IntCell(2));
        colSpecCrea.setDomain(domainCrea.createDomain());
        DataColumnSpec intColSpec = colSpecCrea.createSpec();

        DataTableSpec tableSpec = new DataTableSpec(intColSpec);

        RowKey rowKey = new RowKey("Row0");
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, true);

        // check initialized bounds
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell) new IntCell(-2)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell) new IntCell(2)));

        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(1)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell) new IntCell(-2)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell) new IntCell(2)));


        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(3)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell) new IntCell(-2)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell) new IntCell(3)));

        domainCreator.updateDomain(new DefaultRow(rowKey, new IntCell(-3)));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected lower bound", colDomain.getLowerBound(), is((DataCell) new IntCell(-3)));
        assertThat("Unexpected upper bound", colDomain.getUpperBound(), is((DataCell) new IntCell(3)));
    }

    /**
     * Checks whether possible values are initialized correctly if requested.
     */
    @Test
    public void testInitValues() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("String col", StringCell.TYPE);
        DataColumnDomainCreator domainCrea = new DataColumnDomainCreator();
        domainCrea.setValues(Collections.singleton(new StringCell("v99")));
        colSpecCrea.setDomain(domainCrea.createDomain());
        DataColumnSpec stringColSpec = colSpecCrea.createSpec();


        DataTableSpec tableSpec = new DataTableSpec(stringColSpec);
        RowKey rowKey = new RowKey("Row0");
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, true);
        domainCreator.setMaxPossibleValues(2);

        // check initial values
        Set<DataCell> expectedValues = new LinkedHashSet<>();
        expectedValues.add(new StringCell("v99"));
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));

        // add two values
        expectedValues.add(new StringCell("v1"));
        domainCreator.updateDomain(new DefaultRow(rowKey, "v1"));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));


        // check whether a initial set of more than 60 possible values is retained if no new possible values
        // appear in the data
        domainCrea = new DataColumnDomainCreator();
        Set<DataCell> initialValues = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            initialValues.add(new StringCell(Integer.toString(i)));
        }
        domainCrea.setValues(initialValues);
        colSpecCrea.setDomain(domainCrea.createDomain());
        stringColSpec = colSpecCrea.createSpec();


        tableSpec = new DataTableSpec(stringColSpec);
        domainCreator = new DataTableDomainCreator(tableSpec, true);
        domainCreator.setMaxPossibleValues(60);

        // check initial values
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(initialValues));

        // add already existing value
        domainCreator.updateDomain(new DefaultRow(rowKey, "2"));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(initialValues));
    }


    /**
     * Checks whether the default maximum of possible values is taken from the system property.
     */
    @Test
    public void testDefaultNumberOfPossibleValues() {
        DataColumnSpecCreator colSpecCrea = new DataColumnSpecCreator("String col", StringCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(colSpecCrea.createSpec());

        DataTableDomainCreator domainCreator = new DataTableDomainCreator(tableSpec, false);

        Set<DataCell> expectedValues = new LinkedHashSet<>();
        for (int i = 0; i < DataContainer.MAX_POSSIBLE_VALUES; i++) {
            RowKey rowKey = new RowKey("Row" + i);
            StringCell c = new StringCell(Integer.toString(i));
            domainCreator.updateDomain(new DefaultRow(rowKey, c));
            expectedValues.add(c);
        }

        // all possible values should be present
        DataColumnDomain colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(expectedValues));

        // add more than the maximum number removes all values
        domainCreator.updateDomain(new DefaultRow(new RowKey("One value too many"), new StringCell("One value too many")));
        colDomain = domainCreator.createSpec().getColumnSpec(0).getDomain();
        assertThat("Unexpected possible values", colDomain.getValues(), is(nullValue()));

    }
}

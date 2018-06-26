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
 *   Mar 14, 2016 (wiswedel): created
 */
package org.knime.core.data.vector.doublevector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.number.OrderingComparison;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author wiswedel
 */
public class DoubleVectorCellTest {

    @Test
    public void testCreation() throws Exception {
        double[] d = IntStream.range(0, 10000).mapToDouble(i -> i).toArray();
        DataCell cell = DoubleVectorCellFactory.createCell(d);
        Assert.assertTrue(cell instanceof DenseDoubleVectorCell);
        Assert.assertTrue(cell instanceof DoubleVectorValue);
        DoubleVectorValue v = (DoubleVectorValue)cell;
        Assert.assertEquals("length mismatch", 10000, v.getLength());

        IntStream.range(0, 10000).forEach(i -> Assert.assertEquals("value, index " + i, i, v.getValue(i), 0.0));
    }

    @Test
    public void testSerialization() throws Exception {
        double[] d = IntStream.range(0, 10000).mapToDouble(i -> i).toArray();
        DataCell cell = DoubleVectorCellFactory.createCell(d);
        DataContainer c = new DataContainer(
            new DataTableSpec(new DataColumnSpecCreator("foo", DoubleVectorCellFactory.TYPE).createSpec()));
        c.addRowToTable(new DefaultRow("row", cell));
        c.close();
        DataTable table = c.getTable();
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DataContainer.writeToStream(table, output, new ExecutionMonitor());
            output.close();
            bytes = output.toByteArray();
        }

        ContainerTable containerTable;
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            containerTable = DataContainer.readFromStream(input);
        }
        DataCell cell2 = containerTable.iterator().next().getCell(0);

        Assert.assertNotSame(c, cell2);
        Assert.assertEquals(cell, cell2);
    }

    @Test
    public void testCompare() throws Exception {
        double[] d1 = IntStream.range(0, 10000).mapToDouble(i -> i).toArray();
        DataCell cell1 = DoubleVectorCellFactory.createCell(d1);
        double[] d2 = IntStream.range(0, 10000).mapToDouble(i -> i).toArray();
        d2[100] = 99.0;
        DataCell cell2 = DoubleVectorCellFactory.createCell(d2);

        DataValueComparator comparator = DoubleVectorCellFactory.TYPE.getComparator();
        Assert.assertThat("must be equal", comparator.compare(cell1, cell1), CoreMatchers.equalTo(0));
        Assert.assertThat("must be smaller", comparator.compare(cell1, cell2), OrderingComparison.greaterThan(0));
        Assert.assertThat("must be larger", comparator.compare(cell2, cell1), OrderingComparison.lessThan(0));

        Assert.assertThat("shorter array must be smaller", comparator.compare(
            DoubleVectorCellFactory.createCell(new double[0]), cell2), OrderingComparison.lessThan(0));
    }

}

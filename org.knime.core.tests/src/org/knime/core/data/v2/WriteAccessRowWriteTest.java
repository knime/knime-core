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
 *   Jul 21, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.data.v2.schema.ValueSchemaUtils;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.IntReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.table.access.BooleanAccess.BooleanWriteAccess;
import org.knime.core.table.access.DoubleAccess.DoubleWriteAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.row.WriteAccessRow;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link WriteAccessRowWrite}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
@RunWith(MockitoJUnitRunner.class)
public class WriteAccessRowWriteTest {

    private WriteAccessRowWrite m_testInstance;

    @Mock
    private WriteAccessRow m_writeAccessRow;

    @Mock
    private StringWriteAccess m_rowKeyAccess;

    @Mock
    private BooleanWriteAccess m_booleanAccess;

    @Mock
    private IntWriteAccess m_intAccess;

    @Mock
    private DoubleWriteAccess m_doubleAccess;

    static final DataTableSpec SOURCE_SPEC = new DataTableSpecCreator()//
            .addColumns(//
                new DataColumnSpecCreator("boolean", BooleanCell.TYPE).createSpec(), //
                new DataColumnSpecCreator("int", IntCell.TYPE).createSpec(), //
                new DataColumnSpecCreator("double", DoubleCell.TYPE).createSpec()//
            ).createSpec();

    static final ValueSchema SCHEMA = ValueSchemaUtils.create(SOURCE_SPEC, RowKeyType.CUSTOM, NotInWorkflowWriteFileStoreHandler.create());

    @Before
    public void init() {
        stubWriteAccessRow();

        m_testInstance = new WriteAccessRowWrite(SCHEMA, m_writeAccessRow);
    }

    private void stubWriteAccessRow() {
        final WriteAccess[] accesses = {m_rowKeyAccess, m_booleanAccess, m_intAccess, m_doubleAccess};
        when(m_writeAccessRow.getWriteAccess(anyInt())).then(i -> accesses[(int)i.getArgument(0)]);
    }

    @Test
    public void testSetNoMissingValues() {
        final DataRow testRow = createRow("test", true, 4, 1.3);
        m_testInstance.setFrom(RowRead.from(testRow));
        verify(m_rowKeyAccess).setStringValue("test");
        verify(m_booleanAccess).setBooleanValue(true);
        verify(m_intAccess).setIntValue(4);
        verify(m_doubleAccess).setDoubleValue(1.3);
    }


    static DataRow createRow(final String rowKey, final Boolean booleanValue, final Integer intValue,
        final Double doubleValue) {
        final RowKey key = new RowKey(rowKey);
        final DataCell booleanCell = createIfNotNull(booleanValue, BooleanCellFactory::create);
        final DataCell intCell = createIfNotNull(intValue, IntCellFactory::create);
        final DataCell doubleCell = createIfNotNull(doubleValue, DoubleCellFactory::create);
        return new DefaultRow(key, booleanCell, intCell, doubleCell);
    }

    private static <T> DataCell createIfNotNull(final T value, final Function<T, DataCell> cellFactory) {
        return value != null ? cellFactory.apply(value) : new MissingCell("no value");
    }

    @Test
    public void testWithMissingValues() {
        final DataRow testRow = createRow("test", null, 4, null);
        m_testInstance.setFrom(RowRead.from(testRow));
        verify(m_rowKeyAccess).setStringValue("test");
        verify(m_booleanAccess).setMissing();
        verify(m_intAccess).setIntValue(4);
        verify(m_doubleAccess).setMissing();
    }

    @Test
    public void testGetNumColumns() {
        assertEquals(3, m_testInstance.getNumColumns());
    }

    @Test
    public void testSetRowKeyWithString() {
        m_testInstance.setRowKey("foobar");
        verify(m_rowKeyAccess).setStringValue("foobar");
    }

    @Test
    public void testSetRowKeyWithRowKeyValue() {
        RowKeyValue rowKeyValue = mock(RowKeyValue.class);
        when(rowKeyValue.getString()).thenReturn("row key value");
        m_testInstance.setRowKey(rowKeyValue);
        verify(m_rowKeyAccess).setStringValue("row key value");
    }

    @Test
    public void testSetFromRowRead() {
        RowRead rowRead = mock(RowRead.class);
        when(rowRead.getNumColumns()).thenReturn(3);
        RowKeyValue rowKeyValue = mock(RowKeyValue.class);
        IntReadValue intValue = mock(IntReadValue.class);
        DoubleReadValue doubleValue = mock(DoubleReadValue.class);
        when(intValue.getIntValue()).thenReturn(42);
        when(doubleValue.getDoubleValue()).thenReturn(13.37);
        when(rowRead.getRowKey()).thenReturn(rowKeyValue);
        when(rowKeyValue.getString()).thenReturn("rowReadRowKeyValue");
        when(rowRead.isMissing(0)).thenReturn(true);
        when(rowRead.getValue(1)).thenReturn(intValue);
        when(rowRead.getValue(2)).thenReturn(doubleValue);
        m_testInstance.setFrom(rowRead);
        verify(m_rowKeyAccess).setStringValue("rowReadRowKeyValue");
        verify(m_booleanAccess).setMissing();
        verify(m_intAccess).setIntValue(42);
        verify(m_doubleAccess).setDoubleValue(13.37);
    }

    @Test
    public void testGetWriteValue() {
        BooleanWriteValue booleanWriteValue = m_testInstance.getWriteValue(0);
        booleanWriteValue.setBooleanValue(true);
        verify(m_booleanAccess).setBooleanValue(true);
        IntWriteValue intWriteValue = m_testInstance.getWriteValue(1);
        intWriteValue.setIntValue(17);
        verify(m_intAccess).setIntValue(17);
        DoubleWriteValue doubleWriteValue = m_testInstance.getWriteValue(2);
        doubleWriteValue.setDoubleValue(1.35);
        verify(m_doubleAccess).setDoubleValue(1.35);

    }
}

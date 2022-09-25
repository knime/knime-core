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
 *   4 Jul 2022 (Carsten Haubold): created
 */
package org.knime.core.data.v2.value.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.knime.core.data.AdapterCell;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.AbstractAdapterCellValueFactory;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.table.access.BufferedAccesses;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class AdapterCellValueFactoryTest {

    public static class MyPrimaryAdapterCell extends AdapterCell {
        private static final long serialVersionUID = 1L;

        private final String m_value;

        @SuppressWarnings("unchecked")
        public MyPrimaryAdapterCell(final String value) {
            super(new StringCell(value));
            m_value = value;
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return getAdapterMap().equals(((MyPrimaryAdapterCell)dc).getAdapterMap());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getAdapterMap());
        }

        public String getValue() {
            return m_value;
        }
    }

    public static class MyPrimaryAdapterCellValueFactory extends AbstractAdapterCellValueFactory {

        @Override
        public MyPrimaryAdapterCellReadValue createReadValue(final StructReadAccess access) {
            return new MyPrimaryAdapterCellReadValue(access, m_dataRepository);
        }

        @Override
        public MyPrimaryAdapterCellWriteValue createWriteValue(final StructWriteAccess access) {
            return new MyPrimaryAdapterCellWriteValue(access, m_writeFileStoreHandler);
        }

        @Override
        protected DataSpec getPrimarySpec() {
            return StringDataSpec.INSTANCE;
        }

        static final class MyPrimaryAdapterCellWriteValue
            extends AbstractAdapterCellWriteValue<MyPrimaryAdapterCell> {
            private MyPrimaryAdapterCellWriteValue(final StructWriteAccess access,
                final IWriteFileStoreHandler fsHandler) {
                super(access, fsHandler);
            }

            @Override
            protected void setPrimaryValue(final MyPrimaryAdapterCell value, final WriteAccess writeAccess) {
                ((StringWriteAccess)writeAccess).setStringValue(value.getValue());
            }
        }

        static final class MyPrimaryAdapterCellReadValue extends AbstractAdapterCellReadValue {
            private MyPrimaryAdapterCellReadValue(final StructReadAccess access, final IDataRepository dataRepository) {
                super(access, dataRepository);
            }

            @Override
            protected AdapterCell getAdapterCell(final ReadAccess readAccess) {
                return new MyPrimaryAdapterCell(((StringReadAccess)readAccess).getStringValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAdapterTypeSaveLoadViaValueFactory() throws Exception {
        MyPrimaryAdapterCell c1 = new MyPrimaryAdapterCell("Test");
        var additionalCell = new XMLCellFactory().createCell("<xml><contents>Test</contents></xml>");
        MyPrimaryAdapterCell c2 = (MyPrimaryAdapterCell)c1.cloneAndAddAdapter(additionalCell, XMLValue.class);

        var factory = new MyPrimaryAdapterCellValueFactory();
        factory.initializeForWriting(NotInWorkflowWriteFileStoreHandler.create());
        var bufferedAccess = BufferedAccesses
            .createBufferedAccess(new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE));
        var writeValue = factory.createWriteValue((StructWriteAccess)bufferedAccess);
        writeValue.setValue(c2);

        var readValue = factory.createReadValue((StructReadAccess)bufferedAccess);
        DataCell c3 = readValue.getDataCell();
        assertTrue(c3 instanceof AdapterCell);
        assertTrue(c3 instanceof MyPrimaryAdapterCell);
        MyPrimaryAdapterCell a3 = (MyPrimaryAdapterCell)c3;
        assertTrue(a3.isAdaptable(XMLValue.class));
        assertEquals(c1.getValue(), a3.getValue());
        assertEquals(additionalCell.toString(), a3.getAdapter(XMLValue.class).toString());
    }

    @Test
    public void testAdapterTypeSaveLoadViaValueFactory_MissingCell() throws Exception {
        MyPrimaryAdapterCell c1 = new MyPrimaryAdapterCell("Test");
        @SuppressWarnings("unchecked")
        MyPrimaryAdapterCell c2 =
            (MyPrimaryAdapterCell)c1.cloneAndAddAdapter(new MissingCell("Something went wrong"), DoubleValue.class);

        var factory = new MyPrimaryAdapterCellValueFactory();
        factory.initializeForWriting(NotInWorkflowWriteFileStoreHandler.create());
        var bufferedAccess = BufferedAccesses
            .createBufferedAccess(new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE));
        var writeValue = factory.createWriteValue((StructWriteAccess)bufferedAccess);
        writeValue.setValue(c2);

        var readValue = factory.createReadValue((StructReadAccess)bufferedAccess);
        DataCell c3 = readValue.getDataCell();
        assertTrue(c3 instanceof AdapterCell);
        assertTrue(c3 instanceof MyPrimaryAdapterCell);
        MyPrimaryAdapterCell a3 = (MyPrimaryAdapterCell)c3;
        assertTrue(a3.isAdaptable(DoubleValue.class));
        assertEquals(c1.getValue(), a3.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAdapterTypeSaveLoadViaValueFactory_DoNotDuplicateAdapters() throws Exception {
        MyPrimaryAdapterCell c1 = new MyPrimaryAdapterCell("Test");
        var additionalCell = new XMLCellFactory().createCell("<xml><contents>Test</contents></xml>");
        MyPrimaryAdapterCell c2 = (MyPrimaryAdapterCell)c1.cloneAndAddAdapter(additionalCell, XMLValue.class);

        var factory = new MyPrimaryAdapterCellValueFactory();
        factory.initializeForWriting(NotInWorkflowWriteFileStoreHandler.create());
        var bufferedAccess = BufferedAccesses
            .createBufferedAccess(new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE));
        var writeValue = factory.createWriteValue((StructWriteAccess)bufferedAccess);
        writeValue.setValue(c2);

        var readValue = factory.createReadValue((StructReadAccess)bufferedAccess);
        DataCell c3 = readValue.getDataCell();
        assertTrue(c3 instanceof AdapterCell);
        assertTrue(c3 instanceof MyPrimaryAdapterCell);
        MyPrimaryAdapterCell a3 = (MyPrimaryAdapterCell)c3;
        var adapterMap = a3.getAdapterMap();
        Set<DataCell> adapterSet = Collections.newSetFromMap(new IdentityHashMap<>());
        adapterSet.addAll(adapterMap.values());
        assertEquals(2, adapterSet.size());
        assertEquals(c2.getAdapterMap().keySet(), adapterMap.keySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAdapterCellSetWriteValueFromReadValue() throws Exception {
        MyPrimaryAdapterCell c1 = new MyPrimaryAdapterCell("Test");
        var additionalCell = new XMLCellFactory().createCell("<xml><contents>Test</contents></xml>");
        MyPrimaryAdapterCell c2 = (MyPrimaryAdapterCell)c1.cloneAndAddAdapter(additionalCell, XMLValue.class);

        var factory = new MyPrimaryAdapterCellValueFactory();
        factory.initializeForWriting(NotInWorkflowWriteFileStoreHandler.create());
        var bufferedAccess = BufferedAccesses
            .createBufferedAccess(new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE));
        var writeValue = factory.createWriteValue((StructWriteAccess)bufferedAccess);
        writeValue.setValue(c2);
        var readValue = factory.createReadValue((StructReadAccess)bufferedAccess);

        var bufferedAccess2 = BufferedAccesses
                .createBufferedAccess(new StructDataSpec(StringDataSpec.INSTANCE, VarBinaryDataSpec.INSTANCE));
        var writeValue2 = factory.createWriteValue((StructWriteAccess)bufferedAccess2);
        setValue(writeValue2, readValue);
        var readValue2 = factory.createReadValue((StructReadAccess)bufferedAccess2);

        DataCell c3 = readValue2.getDataCell();
        assertTrue(c3 instanceof AdapterCell);
        assertTrue(c3 instanceof MyPrimaryAdapterCell);
        MyPrimaryAdapterCell a3 = (MyPrimaryAdapterCell)c3;
        assertTrue(a3.isAdaptable(XMLValue.class));
        assertEquals(c1.getValue(), a3.getValue());
        assertEquals(additionalCell.toString(), a3.getAdapter(XMLValue.class).toString());
    }

    @SuppressWarnings("unchecked")
    private static <V extends DataValue & AdapterValue, W extends WriteValue<V>> void setValue(final W writeValue, final MyPrimaryAdapterCellValueFactory.MyPrimaryAdapterCellReadValue value) {
        writeValue.setValue((V)value);
    }
}

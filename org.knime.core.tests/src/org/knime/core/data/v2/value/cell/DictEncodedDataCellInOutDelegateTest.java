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
 *   Oct 13, 2021 (Carsten Haubold, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.value.cell;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.knime.core.data.AdapterCellTest.MyAdapterCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.DataTableValueSchemaTest;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.table.io.ReadableDataInputStream;
import org.xml.sax.SAXException;

/**
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class DictEncodedDataCellInOutDelegateTest {

    /**
     * A {@link DataCell} that is not registered with the corresponding DataType extension point in KNIME and thus falls
     * back to DataCell default serialization. And because it does not have a serializer in the registry, it falls back
     * to standard Java serialization to facilitate saving and loading old DataCells that do not implement a
     * {@link DataCellSerializer} yet.
     *
     * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
     */
    public static final class DummyDataCell extends DataCell {

        private static final long serialVersionUID = 42L;

        private String m_data = null;

        DummyDataCell(final String data) {
            m_data = data;
        }

        public DummyDataCell() {
            // used for deserialization
        }

        static final DataType TYPE = DataType.getType(DummyDataCell.class);

        @Override
        public String toString() {
            return m_data;
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            final var that = (DummyDataCell)dc;
            return this.m_data.equals(that.m_data);
        }

        @Override
        public int hashCode() {
            return m_data.hashCode();
        }
    }

    @Test
    public void testWriteReadXMLDataCell()
        throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        final var xmlString = "<dummyXML>Test</dummyXML>";
        final var cell = XMLCellFactory.create(xmlString);
        testWriteReadDataCell(cell);
    }

    @Test
    public void testWriteReadUnknownDataCell() throws IOException {
        final var cell = new DummyDataCell("foobar");
        // The log will contain warnings because DummyDataCell doesn't have a DataCellSerializer implemented
        // and that we're falling back to plain Java serialization, but that's exactly what we want to test.
        testWriteReadDataCell(cell);
    }

    private static void testWriteReadDataCell(final DataCell cell) throws IOException {
        final IDataRepository dataRepository = null;
        final IWriteFileStoreHandler fileStoreHandler = new DataTableValueSchemaTest.DummyWriteFileStoreHandler();
        final var baseBuffer = new ByteArrayOutputStream();
        final var outStream = new DataOutputStream(baseBuffer);
        try (final var out = new DictEncodedDataCellDataOutputDelegator(fileStoreHandler, outStream)) {
            out.writeDataCell(cell);

            final var inStream = new ReadableDataInputStream(new ByteArrayInputStream(baseBuffer.toByteArray()));
            try (final var in = new DictEncodedDataCellDataInputDelegator(dataRepository, inStream,
                DictEncodedDataCellDataInputDelegator.getSerializedCellNames(cell))) {
                final var readCell = in.readDataCell();
                assertEquals(cell, readCell);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testReadDataCellWithoutClassNameThrowsException()
        throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        final var xmlString = "<dummyXML>Test</dummyXML>";
        final var cell = XMLCellFactory.create(xmlString);
        final IDataRepository dataRepository = null;
        final IWriteFileStoreHandler fileStoreHandler = new DataTableValueSchemaTest.DummyWriteFileStoreHandler();
        final var baseBuffer = new ByteArrayOutputStream();
        final var outStream = new DataOutputStream(baseBuffer);
        try (final var out = new DictEncodedDataCellDataOutputDelegator(fileStoreHandler, outStream)) {
            out.writeDataCell(cell);

            final var inStream = new ReadableDataInputStream(new ByteArrayInputStream(baseBuffer.toByteArray()));
            try (final var in = new DictEncodedDataCellDataInputDelegator(dataRepository, inStream, "")) {
                in.readDataCell(); // throws because no cell class name was specified in constructor
            }
        }
    }

    @Test
    public void testReadAdapterCell()
        throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        // This calls readDataCell twice, once for the AdapterCell and once for the contained cell.
        final var xmlString = "<dummyXML>Test</dummyXML>";
        final var innerCell = XMLCellFactory.create(xmlString);
        @SuppressWarnings("unchecked")
        final var cell = new MyAdapterCell(innerCell, XMLValue.class);
        final IDataRepository dataRepository = null;
        final IWriteFileStoreHandler fileStoreHandler = new DataTableValueSchemaTest.DummyWriteFileStoreHandler();
        final var baseBuffer = new ByteArrayOutputStream();
        final var outStream = new DataOutputStream(baseBuffer);
        try (final var out = new DictEncodedDataCellDataOutputDelegator(fileStoreHandler, outStream)) {
            out.writeDataCell(cell);
            final var classNames = DictEncodedDataCellDataInputDelegator.getSerializedCellNames(cell).split(";");
            assertEquals(2, classNames.length);
            assertEquals(cell.getClass().getName(), classNames[0]);
            assertEquals(innerCell.getClass().getName(), classNames[1]);

            final var inStream = new ReadableDataInputStream(new ByteArrayInputStream(baseBuffer.toByteArray()));
            try (final var in = new DictEncodedDataCellDataInputDelegator(dataRepository, inStream,
                DictEncodedDataCellDataInputDelegator.getSerializedCellNames(cell))) {
                in.readDataCell();
            }
        }
    }
}

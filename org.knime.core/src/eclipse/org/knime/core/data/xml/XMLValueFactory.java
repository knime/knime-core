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
 */
package org.knime.core.data.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.util.LockedSupplier;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.filestore.TableOrFileStoreValueFactory;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A {@link ValueFactory} to (de-)serialize {@link XMLValue}s - in the form of {@link XMLCell} or {@link XMLBlobCell} -
 * in the columnar backend.
 *
 * {@link XMLBlobCell} instances will be written to a {@link FileStore} so that the table serialized by the columnar
 * backend does not become huge immediately. Smaller {@link XMLValue}s (= not {@link XMLBlobCell}s) are immediately
 * written into the table.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public class XMLValueFactory extends TableOrFileStoreValueFactory<XMLValue<Document>> {

    static final ObjectSerializer<XMLValue<Document>> SERIALIZER = (out, value) -> out.writeUTF(value.toString());

    static final ObjectDeserializer<XMLValue<Document>> DESERIALIZER = in -> {
        try {
            return new XMLCellContent(in.readUTF(), false);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException(ex);
        }
    };

    /**
     * Create an instance of the {@link XMLValueFactory}
     */
    public XMLValueFactory() {
        super(SERIALIZER, DESERIALIZER);
    }

    @Override
    public XMLReadValue createReadValue(final StructReadAccess access) {
        return new XMLReadValue(access);
    }

    @Override
    public XMLWriteValue createWriteValue(final StructWriteAccess access) {
        return new XMLWriteValue(access);
    }

    private class XMLReadValue extends TableOrFileStoreReadValue implements XMLValue<Document>, XMLCellContentProvider {
        protected XMLReadValue(final StructReadAccess access) {
            super(access);
        }

        @SuppressWarnings({"deprecation", "unchecked"})
        @Override
        public Document getDocument() {
            return ((XMLValue<Document>)getDataCell()).getDocument();
        }

        @SuppressWarnings("unchecked")
        @Override
        public LockedSupplier<Document> getDocumentSupplier() {
            return ((XMLValue<Document>)getDataCell()).getDocumentSupplier();
        }

        @Override
        protected DataCell createCell(final XMLValue<Document> data) {
            final XMLCellContent content;
            if (data instanceof XMLCellContentProvider xmlContentProvider) {
                content = xmlContentProvider.getXMLCellContent();
            } else {
                try (var docSupplier = data.getDocumentSupplier()) {
                    content = new XMLCellContent(docSupplier);
                }
            }
            return new XMLCell(content);
        }

        @Override
        protected XMLFileStoreCell createFileStoreCell(final Integer hash) {
            return new XMLFileStoreCell(hash);
        }

        @Override
        public String toString() {
            return getDataCell().toString();
        }

        @Override
        public XMLCellContent getXMLCellContent() {
            return ((XMLCellContentProvider)getDataCell()).getXMLCellContent();
        }
    }

    private class XMLWriteValue extends TableOrFileStoreWriteValue {
        protected XMLWriteValue(final StructWriteAccess access) {
            super(access);
        }

        @Override
        protected boolean isCorrespondingReadValue(final XMLValue<Document> value) {
            return value instanceof XMLReadValue;
        }

        @Override
        protected ObjectSerializerFileStoreCell<?> getFileStoreCell(final XMLValue<Document> value) throws IOException {
            if (value instanceof XMLFileStoreCell fsCell) {
                return fsCell;
            } else if (value instanceof XMLBlobCell) {
                return new XMLFileStoreCell(createFileStore(), value);
            }
            return null;
        }
    }
}

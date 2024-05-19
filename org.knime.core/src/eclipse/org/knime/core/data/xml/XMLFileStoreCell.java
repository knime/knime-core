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
 *   9 May 2023 (chaubold): created
 */
package org.knime.core.data.xml;

import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.util.LockedSupplier;
import org.knime.core.data.v2.filestore.NoOpSerializer;
import org.knime.core.data.v2.filestore.TableOrFileStoreValueFactory.ObjectSerializerFileStoreCell;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;
import org.w3c.dom.Document;

/**
 *
 * @since 5.1
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("serial")
public final class XMLFileStoreCell extends ObjectSerializerFileStoreCell<XMLCellContent>
    implements XMLValue<Document>, StringValue, XMLCellContentProvider {

    private static final ObjectSerializer<XMLCellContent> SERIALIZER = XMLValueFactory.SERIALIZER::serialize;

    private static final ObjectDeserializer<XMLCellContent> DESERIALIZER = input -> {
        return contentFromValue(XMLValueFactory.DESERIALIZER.deserialize(input));
    };

    private static XMLCellContent contentFromValue(final XMLValue<Document> value) {
        try (var supplier = value.getDocumentSupplier()) {
            return new XMLCellContent(supplier);
        }
    }

    // Empty constructor for de-serialization (no cached hash code)
    private XMLFileStoreCell() {
        this(null);
    }

    /**
     * Initialize with an empty file store and content
     *
     * @param fs {@link FileStore} to which the content will be written
     */
    XMLFileStoreCell(final FileStore fs, final XMLValue<Document> value) {
        super(fs, contentFromValue(value), SERIALIZER, DESERIALIZER);
    }

    XMLFileStoreCell(final Integer hashCode) {
        super(SERIALIZER, DESERIALIZER, hashCode);
    }

    @Override
    public String getStringValue() {
        return getContent().getStringValue();
    }

    @Override
    public Document getDocument() {
        return getContent().getDocument();
    }

    @Override
    public LockedSupplier<Document> getDocumentSupplier() {
        return getContent().getDocumentSupplier();
    }

    @Override
    public String toString() {
        return getContent().toString();
    }

    @Override
    public XMLCellContent getXMLCellContent() {
        return super.getContent();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean equalContent(final DataValue otherValue) {
        return XMLValue.equalContent(this, (XMLValue<Document>)otherValue);
    }

    /**
     * Serializer for {@link XMLFileStoreCell}s
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class XMLSerializer extends NoOpSerializer<XMLFileStoreCell> {

        public XMLSerializer() {
            super(XMLFileStoreCell::new);
        }
    }
}

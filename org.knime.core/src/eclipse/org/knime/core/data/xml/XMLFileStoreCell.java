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

import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.util.LockedSupplier;
import org.knime.core.data.v2.filestore.AbstractFileStoreSerializableValueFactory.AbstractFileStoreSerializableCell;
import org.knime.core.table.io.ReadableDataInput;
import org.w3c.dom.Document;

/**
 *
 * @since 5.1
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("serial")
public final class XMLFileStoreCell extends AbstractFileStoreSerializableCell
    implements XMLValue<Document>, StringValue {

    private XMLCellContent m_content;

    /**
     * Initialize with an empty file store and content
     *
     * @param fs {@link FileStore} to which the content will be written
     */
    XMLFileStoreCell(final FileStore fs, final XMLValue<Document> value) {
        super(fs);
        m_content = contentFromValue(value);
    }

    private static XMLCellContent contentFromValue(final XMLValue<Document> value) {
        try (var supplier = value.getDocumentSupplier()) {
            return new XMLCellContent(supplier);
        }
    }

    /**
     * Deserialization constructor, FileStore will be provided by framework
     */
    XMLFileStoreCell() {
    }

    @Override
    protected void deserialize(final ReadableDataInput inputStream) {
        try {
            m_content = contentFromValue(XMLValueFactory.DESERIALIZER.deserialize(inputStream));
        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't read XML", ex);
        }
    }

    private XMLCellContent getContentLazily() {
        // TODO: don't load immediately
        return m_content;
    }

    @Override
    public String getStringValue() {
        return getContentLazily().getStringValue();
    }

    @Override
    public Document getDocument() {
        return getContentLazily().getDocument();
    }

    @Override
    public LockedSupplier<Document> getDocumentSupplier() {
        return getContentLazily().getDocumentSupplier();
    }

    @Override
    public String toString() {
        return getContentLazily().toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean equalContent(final DataValue otherValue) {
        return XMLValue.equalContent(this, (XMLValue<Document>)otherValue);
    }

    @Override
    protected void serialize(final DataOutput output) {
        try {
            XMLValueFactory.SERIALIZER.serialize(output, m_content);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save XML", ex);
        }
    }

    /**
     * Serializer for {@link XMLFileStoreCell}s
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class XMLSerializer implements DataCellSerializer<XMLFileStoreCell> {

        @Override
        public void serialize(final XMLFileStoreCell cell, final DataCellDataOutput output) throws IOException {
            // Nothing to do, all data is in FileStore
            // TODO: maybe store the hash?
        }

        @Override
        public XMLFileStoreCell deserialize(final DataCellDataInput input) throws IOException {
            // Nothing to do, all data is in FileStore
            // TODO: maybe store the hash?
            return new XMLFileStoreCell();
        }

    }
}

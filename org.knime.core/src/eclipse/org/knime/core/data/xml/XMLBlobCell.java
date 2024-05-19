/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   16.12.2010 (hofer): created
 */
package org.knime.core.data.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.util.LockedSupplier;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * {@link BlobDataCell} implementation that encapsulates a
 * {@link XMLCellContent}.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class XMLBlobCell extends BlobDataCell implements XMLValue<Document>, StringValue, XMLCellContentProvider {
    /**
     * Serializer for {@link XMLBlobCell}s.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class XMLSerializer implements DataCellSerializer<XMLBlobCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final XMLBlobCell cell,
                final DataCellDataOutput output) throws IOException {
            try {
                output.writeUTF(cell.getStringValue());
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException("Could not serialize XML", ex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XMLBlobCell deserialize(final DataCellDataInput input)
                throws IOException {
            String s = input.readUTF();
            try {
                return new XMLBlobCell(new XMLCellContent(s, false));
            } catch (ParserConfigurationException e) {
                throw new IOException(e.getMessage(), e);
            } catch (SAXException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the serializer for XML cells.
     *
     * @return a serializer
     * @deprecated use {@link DataTypeRegistry#getSerializer(Class)} instead
     */
    @Deprecated
    public static DataCellSerializer<XMLBlobCell> getCellSerializer() {
        return new XMLSerializer();
    }


    private final XMLCellContent m_content;

    // cache the hash
    private Integer m_hash = null;

    /**
     * Create a new instance.
     * @param content the content of this cell
     */
    public XMLBlobCell(final XMLCellContent content) {
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        return m_content.getStringValue();
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #getDocumentSupplier()} instead. See {@link XMLValue#getDocument()} for detailed
     *             information.
     */
    @Deprecated
    @Override
    public Document getDocument() {
        return m_content.getDocument();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_content.toString();
    }

    @Override
    public XMLCellContent getXMLCellContent() {
        return m_content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        XMLBlobCell that = (XMLBlobCell)dc;
        return m_content.equals(that.m_content);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected boolean equalContent(final DataValue otherValue) {
        return XMLValue.equalContent(this, (XMLValue<Document>)otherValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (null == m_hash) {
            m_hash = m_content.hashCode();
        }

        return m_hash;
    }

    /**
     * {@inheritDoc}
     * @since 3.6
     */
    @Override
    public LockedSupplier<Document> getDocumentSupplier() {
        return m_content.getDocumentSupplier();
    }

}

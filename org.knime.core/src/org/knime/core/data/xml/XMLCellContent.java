/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   16.12.2010 (hofer): created
 */
package org.knime.core.data.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.xml.io.XMLCellReaderFactory;
import org.knime.core.data.xml.io.XMLCellWriter;
import org.knime.core.data.xml.io.XMLCellWriterFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class encapsulates a {@link Document}. It is the common content
 * of a {@link XMLCell} and a {@link XMLBlobCell}.
 *
 * @author Heiko Hofer
 */
public class XMLCellContent implements XMLValue {
    private SoftReference<String> m_xmlString;

    private final Document m_content;

    /**
     * Creates a {@link Document} by parsing the passed string. It must
     * contain a valid XML document.
     *
     * @param xmlString an XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot
     *          be created.
     * @throws SAXException If xmlString cannot be parsed
     * @throws XMLStreamException
     */
    XMLCellContent(final String xmlString) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
       this(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
    }

    /**
     * Creates a {@link Document} by parsing the contents of the passed
     * {@link InputStream}. It must contain a valid XML document.
     *
     * @param is an XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot
     *          be created.
     * @throws SAXException If xmlString cannot be parsed.
     * @throws XMLStreamException
     */
    XMLCellContent(final InputStream is) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
        m_content = (XMLCellReaderFactory.createXMLCellReader(is)
                .readXML()).getDocument();


    }

    /**
     * Creates a new instance which encapsulates the passed XML document.
     *
     * @param doc an XML document
     */
    XMLCellContent(final Document doc) {
        m_content = doc;
        // Transform CDATA to text
        m_content.getDomConfig().setParameter("cdata-sections", Boolean.FALSE);
        // Resolve entities
        m_content.getDomConfig().setParameter("entities", Boolean.FALSE);
        // normalizeDocument adds e.g. missing xmls attributes
        m_content.normalizeDocument();
    }

    /**
     * Return the document. The returned document must not be changed!
     * @return The document.
     */
    @Override
    public Document getDocument() {
        return m_content;
    }

    /**
     * Returns the XML Document as a string.
     * @return The XML Document as a string.
     */
    String getStringValue() {
        if (null != m_xmlString && null != m_xmlString.get()) {
            return m_xmlString.get();
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLCellWriter writer = XMLCellWriterFactory.createXMLCellWriter(os);
            writer.write(this);
            String xmlString = os.toString();
            m_xmlString = new SoftReference<String>(xmlString);
            return xmlString;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof XMLCellContent) {
            XMLCellContent that = (XMLCellContent) obj;
            return this.getStringValue().equals(that.getStringValue());
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }
}

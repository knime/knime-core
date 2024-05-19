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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.util.LockedSupplier;
import org.knime.core.data.xml.io.XMLCellReaderFactory;
import org.knime.core.data.xml.io.XMLCellWriter;
import org.knime.core.data.xml.io.XMLCellWriterFactory;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class encapsulates a {@link Document}. It is the common content of a
 * {@link XMLCell} and a {@link XMLBlobCell}.
 *
 * @author Heiko Hofer
 */
public class XMLCellContent implements XMLValue<Document>, XMLCellContentProvider {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(XMLCellContent.class);

    private final String m_xmlString;

    private SoftReference<Document> m_content;

    private final ReentrantLock m_lock = new ReentrantLock();

    /**
     * Creates a {@link Document} by parsing the passed string. It must contain
     * a valid XML document.
     *
     * @param xmlString an XML document
     * @param checkXML if the XML string should be parsed in order to check if
     *            it is a valid XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot be
     *             created.
     * @throws SAXException if the xml string cannot be parsed
     */
    XMLCellContent(final String xmlString, final boolean checkXML)
            throws IOException, ParserConfigurationException, SAXException {
        if (checkXML) {
            try {
                // check if XML string is valid XML
                Document doc = parse(xmlString);
                // store the normalized string as cell content
                m_xmlString = serialize(doc);
                m_content = new SoftReference<Document>(doc);
            } catch (IOException ex) {
                Throwable cause = ex;
                while ((cause.getCause() != cause) && (cause.getCause() != null)) {
                    cause = cause.getCause();
                }
                if (cause instanceof SAXException) {
                    throw (SAXException)cause;
                } else {
                    throw ex;
                }
            }
        } else {
            m_xmlString = xmlString;
            m_content = new SoftReference<Document>(null);
        }
    }

    /**
     * Creates a {@link Document} by parsing the contents of the passed
     * {@link InputStream}. It must contain a valid XML document.
     *
     * @param is an XML document
     * @throws IOException If any IO errors occur.
     * @throws ParserConfigurationException If {@link DocumentBuilder} cannot be
     *             created.
     * @throws SAXException if the XML document from the input stream cannot be parsed
     */
    XMLCellContent(final InputStream is) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException  {
        try {
            Document doc = parse(is);
            m_content = new SoftReference<Document>(doc);
            m_xmlString = serialize(doc);
        } catch (IOException ex) {
            Throwable cause = ex;
            while ((cause.getCause() != cause) && (cause.getCause() != null)) {
                cause = cause.getCause();
            }
            if (cause instanceof SAXException) {
                throw ex;
            } else {
                throw ex;
            }
        }
    }

    /**
     * Creates a new instance which encapsulates the passed XML document.
     *
     * @param doc an XML document
     */
    XMLCellContent(final Document doc) {
        m_content = new SoftReference<Document>(doc);
        // Transform CDATA to text
        DOMConfiguration domConfig = doc.getDomConfig();
        domConfig.setParameter("cdata-sections", Boolean.FALSE);
        // Resolve entities
        domConfig.setParameter("entities", Boolean.FALSE);
        // normalizeDocument adds e.g. missing xmls attributes
        doc.normalizeDocument();
        String s = null;
        try {
            s = serialize(doc);
        } catch (IOException ex) {
            // should not happen
        }
        m_xmlString = s;
    }

    /**
     * Creates a new instance which encapsulates the document held by the passed {@link LockedSupplier}.
     *
     * @param documentSupplier {@link LockedSupplier} holding the document.
     * @since 3.6
     */
    public XMLCellContent(final LockedSupplier<Document> documentSupplier) {
        String s = null;
        try {
            /* Serialize the xml string as in the other constructor.
             * This guarantees that we work on our own copy later on. */
            Document doc = documentSupplier.get();

            DOMConfiguration domConfig = doc.getDomConfig();
            domConfig.setParameter("cdata-sections", Boolean.FALSE);
            // Resolve entities
            domConfig.setParameter("entities", Boolean.FALSE);
            // normalizeDocument adds e.g. missing xmls attributes
            doc.normalizeDocument();

            s = serialize(doc);
            m_content = new SoftReference<>(null);
        } catch (IOException ex) {
            // should not happen
        }

        m_xmlString = s;

    }

    /**
     * Return the document. The returned document must not be changed!
     *
     * @return The document.
     *
     * @deprecated use {@link #getDocumentSupplier()} instead. See {@link XMLValue#getDocument()} for detailed
     *             information.
     */
    @Deprecated
    @Override
    public Document getDocument() {
        Document doc = m_content.get();
        if (doc == null) {
            try {
                doc = parse(m_xmlString);
                m_content = new SoftReference<Document>(doc);
            } catch (Exception ex) {
                var detail = "";
                if (ex.getCause() instanceof SAXParseException parseException) {
                    detail = ": " + parseException.getMessage();
                }

                LOGGER.error("Error while parsing XML in XML Cell" + detail, ex);
            }
        }
        return doc;
    }

    /**
     * Returns the XML Document as a string.
     *
     * @return The XML Document as a string.
     */
    String getStringValue() {
        return m_xmlString;
    }

    @Override
    public XMLCellContent getXMLCellContent() {
        return this;
    }

    private static String serialize(final Document doc) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try (XMLCellWriter writer = XMLCellWriterFactory.createXMLCellWriter(os)) {
            writer.write(new XMLValue<Document>() {
                private final ReentrantLock m_lock = new ReentrantLock();

                @Deprecated
                @Override
                public Document getDocument() {
                    return doc;
                }

                @Override
                public LockedSupplier<Document> getDocumentSupplier() {
                    return new LockedSupplier<Document>(doc, m_lock);
                }
            });
        }
        return os.toString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("deprecation")
    private static Document parse(final String xmlString) throws IOException, ParserConfigurationException {
        return XMLCellReaderFactory.createXMLCellReader(new StringReader(xmlString)).readXML().getDocument();
    }

    @SuppressWarnings("deprecation")
    private static Document parse(final InputStream is) throws IOException, ParserConfigurationException {
        return XMLCellReaderFactory.createXMLCellReader(is).readXML().getDocument();
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
            XMLCellContent that = (XMLCellContent)obj;
            return XMLValue.equalContent(this, that);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return XMLValue.hashCode(this);
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.6
     */
    @Override
    public LockedSupplier<Document> getDocumentSupplier() {
        return new LockedSupplier<Document>(getDocument(), m_lock);
    }
}

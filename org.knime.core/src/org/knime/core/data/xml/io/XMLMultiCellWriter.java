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
 *   08.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.OutputStream;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.knime.core.data.xml.XMLValue;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Heiko Hofer
 */
class XMLMultiCellWriter implements XMLCellWriter {
    private static String IDENT_CHAR = "    ";
    private static String LINEFEED_CHAR = "\n";
    private XMLStreamWriter m_writer;
    private final boolean m_writeRoot;

    XMLMultiCellWriter(final OutputStream os)  throws XMLStreamException {
        initWriter(os);
        writeHeader();
        m_writeRoot = false;
    }

    /**
     * @param os
     * @param rootElement
     * @param rootAttributes
     * @throws XMLStreamException
     */
    XMLMultiCellWriter(final OutputStream os, final QName rootElement,
            final Map<QName, String> rootAttributes) throws XMLStreamException {
        initWriter(os);
        writeHeader();
        m_writeRoot = true;
        // write root element
        String elementPrefix = rootElement.getPrefix();
        if (elementPrefix == null) {
            elementPrefix = "";
        }
        m_writer.writeCharacters(LINEFEED_CHAR);
        m_writer.writeStartElement(elementPrefix, rootElement.getLocalPart(),
                rootElement.getNamespaceURI());
        // Write attributes of the root element
        for (QName attr : rootAttributes.keySet()) {
            String attrPrefix = attr.getPrefix();
            String attrLocalName = attr.getLocalPart();
            String attrValue = rootAttributes.get(attr);

            if (null == attrPrefix || attrPrefix.isEmpty()) {
                if ("xmlns".equals(attrLocalName)) {
                    // default namespace definition
                    m_writer.writeDefaultNamespace(attrValue);
                } else {
                    // attribute without namespace prefix
                    m_writer.writeAttribute(attrLocalName, attrValue);
                }
            } else {
                if ("xmlns".equals(attrPrefix)) {
                    // namespace definition
                    m_writer.writeNamespace(attrLocalName, attrValue);
                } else {
                    // attribute with namespace prefix
                    m_writer.writeAttribute(attrPrefix, attr.getNamespaceURI(),
                            attrLocalName, attrValue);
                }
            }
        }
    }

    /**
     * @throws XMLStreamException
     *
     */
    private void writeHeader() throws XMLStreamException {
        // write header of the xml file
        m_writer.writeStartDocument("UTF-8", "1.0");
    }

    /**
     * @param os
     * @throws XMLStreamException
     */
    private void initWriter(final OutputStream os) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        m_writer = factory.createXMLStreamWriter(os, "UTF-8");
    }

    /**
     * @throws XMLStreamException
     */
    @Override
    public void write(final XMLValue cell) throws XMLStreamException {
        Document doc = cell.getDocument();
        Node child = doc.getFirstChild();
        int depth = m_writeRoot ? 1 : 0;
        while (child != null) {
            XMLCellWriterUtil.writeNode(m_writer, child, depth, IDENT_CHAR,
                    LINEFEED_CHAR);
            child = child.getNextSibling();
        }
    }

    /**
     * @throws XMLStreamException
     */
    @Override
    public void close() throws XMLStreamException {
        if (m_writeRoot) {
            m_writer.writeCharacters(LINEFEED_CHAR);
            m_writer.writeEndElement();
        }
        m_writer.writeEndDocument();
        m_writer.close();
    }



}

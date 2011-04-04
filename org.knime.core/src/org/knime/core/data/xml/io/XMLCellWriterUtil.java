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

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author Heiko Hofer
 */
public class XMLCellWriterUtil {

    /**
     * Writes a {@link Node}
     */
    static boolean writeNode(final XMLStreamWriter writer, final Node node,
            final int depth, final String identChar, final String linefeedChar)
            throws XMLStreamException {
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            linefeedIdent(writer, linefeedChar, depth, identChar);
            writeElement(writer, (Element)node, depth, identChar, linefeedChar);
            return true;
        case Node.TEXT_NODE:
            writer.writeCharacters(node.getNodeValue());
            return false;
        case Node.CDATA_SECTION_NODE:
            writer.writeCData(node.getNodeValue());
            return false;
        case Node.COMMENT_NODE:
            linefeedIdent(writer, linefeedChar, depth, identChar);
            writer.writeComment(node.getNodeValue());
            return true;
        case Node.ENTITY_REFERENCE_NODE:
            writer.writeEntityRef(node.getNodeName());
            return false;
        case Node.PROCESSING_INSTRUCTION_NODE:
            linefeedIdent(writer, linefeedChar, depth, identChar);
            String target = node.getNodeName();
            String data = node.getNodeValue();
            if (data == null || data.length() == 0) {
                writer.writeProcessingInstruction(target);
            } else {
                writer.writeProcessingInstruction(target, data);
            }
            return true;
        case Node.DOCUMENT_TYPE_NODE:
            linefeedIdent(writer, linefeedChar, depth, identChar);
            DocumentType docType = (DocumentType) node;
            String publicId = docType.getPublicId();
            String systemId = docType.getSystemId();
            String internalSubset = docType.getInternalSubset();
            StringBuilder dtd = new StringBuilder();
            dtd.append("<!DOCTYPE " + docType.getName());
            if (publicId != null) {
                dtd.append(" PUBLIC \"" + publicId + "\" ");
            } else {
                if (systemId != null) {
                    dtd.append(" SYSTEM ");
                }
            }
            if (systemId != null) {
                dtd.append("\"" + systemId + "\"");
            }
            if (internalSubset != null) {
                dtd.append(" [\n" + internalSubset + "]");
            }
            dtd.append(">\n");
            writer.writeDTD(dtd.toString());

            return true;
        default:
            throw new XMLStreamException(
                    "Unrecognized or unexpected node class: "
                            + node.getClass().getName());
        }
    }


    /**
     * Writes an element and its children
     */
    private static void writeElement(final XMLStreamWriter writer,
            final Element element, final int depth, final String identChar,
            final String linefeedChar)
            throws XMLStreamException {
        String elementPrefix = element.getPrefix();
        if (elementPrefix == null) {
            elementPrefix = XMLConstants.DEFAULT_NS_PREFIX;
        }
        String elementUri = element.getNamespaceURI();
        if (elementUri == null) {
            elementUri = XMLConstants.NULL_NS_URI;
        }
        String nsContextURI =
            writer.getNamespaceContext().getNamespaceURI(elementPrefix);

        writer.writeStartElement(elementPrefix, element.getLocalName(),
                elementUri);
        // Write attributes
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0, len = attrs.getLength(); i < len; ++i) {
            Attr attr = (Attr)attrs.item(i);
            String attrPrefix = attr.getPrefix();
            String attrLocalName = attr.getLocalName();
            String attrValue = attr.getValue();

            if (null == attrPrefix || attrPrefix.isEmpty()) {
                if ("xmlns".equals(attrLocalName)) {
                    // default namespace definition
                    if (!attrValue.equals(nsContextURI)) {
                        writer.writeDefaultNamespace(attrValue);
                    }
                } else {
                    // attribute without namespace prefix
                    writer.writeAttribute(attrLocalName, attrValue);
                }
            } else {
                if ("xmlns".equals(attrPrefix)) {
                    // namespace definition
                    if (!attrValue.equals(nsContextURI)) {
                        writer.writeNamespace(attrLocalName, attrValue);
                    }
                } else {
                    // attribute with namespace prefix
                    writer.writeAttribute(attrPrefix, attr.getNamespaceURI(),
                            attrLocalName, attrValue);
                }
            }
        }
        // write children
        Node child = element.getFirstChild();
        boolean ident = false;
        while (child != null) {
            boolean i = writeNode(writer, child, depth + 1, identChar,
                    linefeedChar);
            ident = i || ident;
            child = child.getNextSibling();
        }

        if (ident) {
            writer.writeCharacters(linefeedChar);
            ident(writer, depth, identChar);
        }
        writer.writeEndElement();
    }


    private static void linefeedIdent(final XMLStreamWriter writer,
            final String linefeedChar, final int depth,
            final String identChar) throws XMLStreamException {
        writer.writeCharacters(linefeedChar);
        ident(writer, depth, identChar);
    }

    private static void ident(final XMLStreamWriter writer, final int depth,
            final String identChar) throws XMLStreamException {
        if (identChar.isEmpty()) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            writer.writeCharacters(identChar);
        }
    }
}

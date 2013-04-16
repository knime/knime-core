/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Utility class to write xml.
 *
 * @author Heiko Hofer
 */
class XMLCellWriterUtil {

	/**
	 * Writes a {@link Node}
	 * @param writer the writer to use
	 * @param node the node to write
	 * @param pre the predecessor of this node
	 * @param depth the depth of this node in the XML tree
	 * @param indentChar the char that should be used for indentation
	 * @param linefeedChar the linefeed character used for pretty printing
	 * @param preserveSpaceStack if spaces should be preserved
	 * @throws XMLStreamException when exception occurs
	 */
	static void writeNode(final XMLStreamWriter writer, final Node node,
			final Node pre,
			final int depth, final String indentChar, final String linefeedChar,
			final List<Boolean> preserveSpaceStack) throws XMLStreamException {
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			spaceBefore(writer, pre, linefeedChar, depth, indentChar,
					preserveSpaceStack);
			boolean hasXmlSpaceAttr = updatePreserveSpaceStack((Element) node,
					preserveSpaceStack);
			writeElement(writer, (Element) node, depth, indentChar,
					linefeedChar, preserveSpaceStack);
			if (hasXmlSpaceAttr) {
				preserveSpaceStack.remove(0);
			}
			break;
		case Node.TEXT_NODE:
			writer.writeCharacters(node.getNodeValue());
			break;
		case Node.CDATA_SECTION_NODE:
			writer.writeCData(node.getNodeValue());
			break;
		case Node.COMMENT_NODE:
			spaceBefore(writer, pre, linefeedChar, depth, indentChar,
					preserveSpaceStack);
			writer.writeComment(node.getNodeValue());
			break;
		case Node.ENTITY_REFERENCE_NODE:
			writer.writeEntityRef(node.getNodeName());
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			spaceBefore(writer, pre, linefeedChar, depth, indentChar,
					preserveSpaceStack);
			String target = node.getNodeName();
			String data = node.getNodeValue();
			if (data == null || data.length() == 0) {
				writer.writeProcessingInstruction(target);
			} else {
				writer.writeProcessingInstruction(target, data);
			}
			break;
		case Node.DOCUMENT_TYPE_NODE:
			spaceBefore(writer, pre, linefeedChar, depth, indentChar,
					preserveSpaceStack);
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

			break;
		default:
			throw new XMLStreamException(
					"Unrecognized or unexpected node class: "
							+ node.getClass().getName());
		}
	}

	private static boolean updatePreserveSpaceStack(final Element element,
			final List<Boolean> preserveSpaceStack) {
		boolean hasXmlSpaceAttr = false;
		NamedNodeMap attrs = element.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (attr.getNodeName().equals("xml:space")
					&& attr.getNamespaceURI().equals(XMLConstants.XML_NS_URI)) {
				if (attr.getNodeValue().equals("preserve")) {
					hasXmlSpaceAttr = true;
					preserveSpaceStack.add(0, true);
				} else if (attr.getNodeValue().equals("default")) {
					hasXmlSpaceAttr = true;
					preserveSpaceStack.add(0, false);
				} else {
					// Wrong declaration ignored.
				}
			}
		}
		return hasXmlSpaceAttr;
	}

	/**
	 * Writes an element and its children
	 */
	private static void writeElement(final XMLStreamWriter writer,
			final Element element,
			final int depth, final String indentChar,
			final String linefeedChar, final List<Boolean> preserveSpaceStack)
			throws XMLStreamException {

		NamedNodeMap attrs = element.getAttributes();
		// write element
		String elementPrefix = element.getPrefix();
		if (elementPrefix == null) {
			elementPrefix = XMLConstants.DEFAULT_NS_PREFIX;
		}
		String elementUri = element.getNamespaceURI();
		if (elementUri == null) {
			elementUri = XMLConstants.NULL_NS_URI;
		}
		String nsContextURI = writer.getNamespaceContext().getNamespaceURI(
				elementPrefix);
		String name = null != element.getLocalName()
			? element.getLocalName() : element.getNodeName();

		writer.writeStartElement(elementPrefix, name, elementUri);
		// Write attributes
		for (int i = 0, len = attrs.getLength(); i < len; ++i) {
			Attr attr = (Attr) attrs.item(i);
			String attrPrefix = attr.getPrefix();
			String attrLocalName = null != attr.getLocalName()
				? attr.getLocalName()
				: attr.getName();
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
		Node pre = null;
		while (child != null) {
			writeNode(writer, child, pre, depth + 1, indentChar,
					linefeedChar, preserveSpaceStack);
			pre = child;
			child = child.getNextSibling();
		}
		spaceBefore(writer, pre, linefeedChar, depth, indentChar,
				preserveSpaceStack);
		writer.writeEndElement();
	}

	private static void spaceBefore(final XMLStreamWriter writer,
			final Node pre,
			final String linefeedChar, final int depth, final String identChar,
			final List<Boolean> preserveSpaceStack) throws XMLStreamException {
		short type = null != pre ? pre.getNodeType() : Node.ELEMENT_NODE;
		boolean preserveSpace = !preserveSpaceStack.isEmpty()
			&& preserveSpaceStack.get(0);

		if (!preserveSpace && (type == Node.ELEMENT_NODE
				|| type == Node.DOCUMENT_TYPE_NODE
				|| type == Node.PROCESSING_INSTRUCTION_NODE
				|| type == Node.COMMENT_NODE)) {
			linefeedIndent(writer, linefeedChar, depth, identChar);
		}
	}

	private static void linefeedIndent(final XMLStreamWriter writer,
			final String linefeedChar, final int depth, final String indentChar)
			throws XMLStreamException {
		writer.writeCharacters(linefeedChar);
		indent(writer, depth, indentChar);
	}

	private static void indent(final XMLStreamWriter writer, final int depth,
			final String indentChar) throws XMLStreamException {
		if (indentChar.isEmpty()) {
			return;
		}
		for (int i = 0; i < depth; i++) {
			writer.writeCharacters(indentChar);
		}
	}
}

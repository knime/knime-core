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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.knime.core.data.xml.XMLValue;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * An @link{XMLCellWriter} to write XMLCells that can optionally be enclosed
 * in a root element.
 *
 * @author Heiko Hofer
 */
class XMLMultiCellWriter implements XMLCellWriter {
	private static String INDENT_CHAR = "    ";
	private static String LINEFEED_CHAR = "\n";
	private XMLStreamWriter m_writer;
	private final boolean m_hasDedicatedRoot;
	private final List<Boolean> m_preserveSpaceStack;
	private final OutputStream m_os;
    private static XMLOutputFactory XMLOUTPUTFACTORY =
        XMLOutputFactory.newInstance();

	/**
	 * Create writer to write xml cells. This writer can be configured to skip
	 * writing the header.
	 *
	 * @param os the xml cells are written to this resource.
	 * @param writeHeader true when the xml header should be written
	 * @throws IOException when header could not be written.
	 */
	XMLMultiCellWriter(final OutputStream os, final boolean writeHeader)
	throws IOException {
		try {
			initWriter(os);
			if (writeHeader) {
				writeHeader();
			}
		} catch (XMLStreamException e) {
		    throw new IOException(e);
		}
		m_hasDedicatedRoot = false;
		m_preserveSpaceStack = new LinkedList<Boolean>();
		m_os = os;
	}

	/**
	 * Create writer to write xml cells enclosed in the given root element.
	 *
	 * @param os the xml cells are written to this resource.
	 * @param rootElement the qualified name of the root element
	 * @param rootAttributes the attributes of the root element
	 * @throws IOException when writer could not be initialized and when
	 * header could not be written
	 */
	XMLMultiCellWriter(final OutputStream os, final QName rootElement,
			final Map<QName, String> rootAttributes) throws IOException {
		m_os = os;
		try {
			initWriter(os);
			writeHeader();

			m_hasDedicatedRoot = true;
			m_preserveSpaceStack = new LinkedList<Boolean>();
			// Check if xml:space definition is in the rootAttributes
			for (QName attr : rootAttributes.keySet()) {
				String attrPrefix = attr.getPrefix();
				String attrLocalName = attr.getLocalPart();
				String attrNamespaceURI = attr.getNamespaceURI();

				if (null != attrPrefix && attrPrefix.equals("xml")
						&& attrLocalName.equals("space")
						&& attrNamespaceURI.equals(XMLConstants.XML_NS_URI)) {
					String attrValue = rootAttributes.get(attr);
					if (attrValue.equals("preserve")) {
						m_preserveSpaceStack.add(0, true);
					} else if (attrValue.equals("default")) {
						m_preserveSpaceStack.add(0, false);
					} else {
						// Wrong declaration ignored.
					}
				}
			}
			// write root element
			String elementPrefix = rootElement.getPrefix();
			if (elementPrefix == null) {
				elementPrefix = "";
			}
			m_writer.writeCharacters(LINEFEED_CHAR);
			m_writer.writeStartElement(elementPrefix,
					rootElement.getLocalPart(),
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
						m_writer.writeAttribute(attrPrefix,
								attr.getNamespaceURI(),
								attrLocalName, attrValue);
					}
				}
			}
		} catch (XMLStreamException e) {
		    throw new IOException(e);
		}
	}

	/** Write the xml header.
	 */
	private void writeHeader() throws XMLStreamException {
		// write header of the xml file
		m_writer.writeStartDocument("UTF-8", "1.0");
	}

	/** Initialize the stream writer object.
	 */
	private void initWriter(final OutputStream os) throws XMLStreamException {
		m_writer = XMLOUTPUTFACTORY.createXMLStreamWriter(os, "UTF-8");
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final XMLValue cell)
	throws IOException {
		Document doc = cell.getDocument();
		Node child = doc.getFirstChild();
		Node pre = null;
		int depth = m_hasDedicatedRoot ? 1 : 0;
		while (child != null) {
			try {
				XMLCellWriterUtil.writeNode(m_writer, child, pre, depth,
						INDENT_CHAR,
						LINEFEED_CHAR, m_preserveSpaceStack);
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
			pre = child;
			child = child.getNextSibling();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		try {
			if (m_hasDedicatedRoot) {
				m_writer.writeCharacters(LINEFEED_CHAR);
				m_writer.writeEndElement();
			}
			m_writer.writeEndDocument();
			m_writer.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	    // close stream since m_writer.close() does no necessarily do it
		m_os.close();
	}

}

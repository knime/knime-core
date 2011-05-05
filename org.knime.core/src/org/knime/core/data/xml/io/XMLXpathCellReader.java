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
 *   09.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Read nodes from XML source which match an XPath expression. Note, that the
 * XPath syntax is very limited. Absolute path to nodes and the |-Operator is
 * supported, only.
 * 
 * @author Heiko Hofer
 */
public class XMLXpathCellReader implements XMLCellReader {
	private final InputStream m_in;
	private XMLStreamReader m_parser;
	private final DocumentBuilder m_builder;
	private final LimitedXPathMatcher m_xpathMatcher;
	private final List<Document> m_docs;
	private final List<Node> m_currNodes;
	private boolean m_reentrent;
	private final List<String> m_base;
	private final List<String> m_space;
	private final List<String> m_lang;

	/**
	 * Create a new instance.
	 * 
	 * @param is the xml source
	 * @param xpathMatcher nodes of the input that match will be read, only
	 * @throws ParserConfigurationException
	 * @throws XMLStreamException
	 */
	public XMLXpathCellReader(final InputStream is,
			final LimitedXPathMatcher xpathMatcher)
			throws ParserConfigurationException, XMLStreamException {
		this.m_in = is;

		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		domFactory.setXIncludeAware(true);

		m_builder = domFactory.newDocumentBuilder();

		this.m_xpathMatcher = xpathMatcher;

		m_docs = new LinkedList<Document>();
		m_currNodes = new LinkedList<Node>();

		m_reentrent = false;
		m_base = new LinkedList<String>();
		m_space = new LinkedList<String>();
		m_lang = new LinkedList<String>();
		initStreamParser();
	}

	/** Initialize the stream parser object. */
	private void initStreamParser() throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
				Boolean.TRUE);
		m_parser = factory.createXMLStreamReader(m_in);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XMLValue readXML() throws IOException {
		if (!m_xpathMatcher.nodeMatches()) {
			return null;
		}
		try {
			while (m_parser.hasNext()) {
				switch (m_parser.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					updateBasePath();
					updateXmlSpaceDefinition();
					updateXmlLangDefinition();
					for (int i = 0; i < m_docs.size(); i++) {
						Element element = createElement(m_docs.get(i));
						m_currNodes.get(i).appendChild(element);
						m_currNodes.set(i, element);
					}
					boolean match = m_xpathMatcher.startElement(
							m_parser.getName());
					if (match) {
						Document doc = m_builder.newDocument();
						Element element = createElement(doc);
						// set standard xml attributes
						// (see: http://www.w3.org/XML/1998/namespace)
						if (!m_base.isEmpty() && null != m_base.get(0)) {
							element.setAttributeNS(XMLConstants.XML_NS_URI,
									"xml:base", m_base.get(0));
						}
						if (!m_space.isEmpty() && null != m_space.get(0)) {
							element.setAttributeNS(XMLConstants.XML_NS_URI,
									"xml:space", m_space.get(0));
						}
						if (!m_lang.isEmpty() && null != m_lang.get(0)) {
							element.setAttributeNS(XMLConstants.XML_NS_URI,
									"xml:lang", m_lang.get(0));
						}
						doc.appendChild(element);
						m_docs.add(doc);
						m_currNodes.add(element);
					}
	
					break;
				case XMLStreamConstants.END_ELEMENT:
					if (!m_reentrent) {
						m_xpathMatcher.endElement();
						if (!m_base.isEmpty()) {
							m_base.remove(0);
						}
						if (!m_space.isEmpty()) {
							m_space.remove(0);
						}
						if (!m_lang.isEmpty()) {
							m_lang.remove(0);
						}
					}
					for (int i = m_docs.size() - 1; i >= 0; i--) {
						Node curr = m_currNodes.get(i);
						Node first = m_docs.get(i).getFirstChild();
						if (curr.isSameNode(first)) {
							XMLValue cell = null;
						    cell = createDataCell(m_docs.get(i));
							m_docs.remove(i);
							m_currNodes.remove(i);
							m_reentrent = true;
							return cell;
						} else {
							m_currNodes.set(i, curr.getParentNode());
						}
					}
					m_reentrent = false;
					break;
				case XMLStreamConstants.CHARACTERS:
					if (!m_parser.isWhiteSpace()
							|| (m_parser.isWhiteSpace() && !m_space.isEmpty()
									&& null != m_space.get(0) && m_space.get(0)
									.equals("preserve"))) {
						for (int i = 0; i < m_docs.size(); i++) {
							String str = m_parser.getText();
							Text text = m_docs.get(i).createTextNode(str);
							m_currNodes.get(i).appendChild(text);
						}
					}
					break;
				case XMLStreamConstants.SPACE:
					if (!m_space.isEmpty() && null != m_space.get(0)
							&& m_space.get(0).equals("preserve")) {
						for (int i = 0; i < m_docs.size(); i++) {
							String str = m_parser.getText();
							Text text = m_docs.get(i).createTextNode(str);
							m_currNodes.get(i).appendChild(text);
						}
					}
					break;
				case XMLStreamConstants.COMMENT:
					for (int i = 0; i < m_docs.size(); i++) {
						String str = m_parser.getText();
						Comment comment = m_docs.get(i).createComment(str);
						m_currNodes.get(i).appendChild(comment);
					}
	
					break;
				case XMLStreamConstants.ENTITY_REFERENCE:
					for (int i = 0; i < m_docs.size(); i++) {
						String str = m_parser.getText();
						EntityReference ref = 
							m_docs.get(i).createEntityReference(str);
						m_currNodes.get(i).appendChild(ref);
					}
					break;
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					for (int i = 0; i < m_docs.size(); i++) {
						String piTarget = m_parser.getPITarget();
						String piName = m_parser.getPIData();
						ProcessingInstruction pi = m_docs.get(i)
								.createProcessingInstruction(piTarget, piName);
						m_currNodes.get(i).appendChild(pi);
					}
					break;
				case XMLStreamConstants.DTD:
					// not needed use DOM-Reader for full documents
					break;
				case XMLStreamConstants.END_DOCUMENT:
					// never called
					m_parser.close();
				default:
					break;
				}
				m_parser.next();
			}
			return null;
		} catch(XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private Element createElement(final Document doc) {
		String prefix = m_parser.getPrefix();
		String localName = m_parser.getLocalName();
		String qname = null == prefix || prefix.isEmpty() ? localName : prefix
				+ ":" + localName;
		Element element = doc
				.createElementNS(m_parser.getNamespaceURI(), qname);
		// add xmlns declarations
		for (int i = 0; i < m_parser.getNamespaceCount(); i++) {
			String nsPrefix = m_parser.getNamespacePrefix(i);
			String nsQName = null == nsPrefix || nsPrefix.isEmpty() ? "xmlns"
					: "xmlns:" + nsPrefix;
			element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
					nsQName, m_parser.getNamespaceURI(i));
		}

		// And then the attributes:
		for (int i = 0; i < m_parser.getAttributeCount(); i++) {
			String attrLocalName = m_parser.getAttributeLocalName(i);
			String attrPrefix = m_parser.getAttributePrefix(i);
			String attrQName = null == attrPrefix || attrPrefix.isEmpty() 
					? attrLocalName
					: attrPrefix + ":" + attrLocalName;
			element.setAttributeNS(m_parser.getAttributeNamespace(i),
					attrQName, m_parser.getAttributeValue(i));
		}

		return element;
	}

	private void updateBasePath() {
		String basePath = m_base.isEmpty() ? null : m_base
				.get(m_base.size() - 1);
		for (int i = 0; i < m_parser.getAttributeCount(); i++) {
			if (m_parser.getAttributeLocalName(i).equals("base")
					&& m_parser.getAttributePrefix(i).equals(
							XMLConstants.XML_NS_PREFIX)) {
				basePath = m_parser.getAttributeValue(i);
			}
		}
		m_base.add(0, basePath);
	}

	private void updateXmlSpaceDefinition() {
		String space = m_space.isEmpty() ? null : m_space
				.get(m_space.size() - 1);
		for (int i = 0; i < m_parser.getAttributeCount(); i++) {
			if (m_parser.getAttributeLocalName(i).equals("space")
					&& m_parser.getAttributePrefix(i).equals(
							XMLConstants.XML_NS_PREFIX)) {
				space = m_parser.getAttributeValue(i);
			}
		}
		m_space.add(0, space);
	}

	private void updateXmlLangDefinition() {
		String lang = m_lang.isEmpty() ? null : m_lang.get(m_lang.size() - 1);
		for (int i = 0; i < m_parser.getAttributeCount(); i++) {
			if (m_parser.getAttributeLocalName(i).equals("lang")
					&& m_parser.getAttributePrefix(i).equals(
							XMLConstants.XML_NS_PREFIX)) {
				lang = m_parser.getAttributeValue(i);
			}
		}
		m_lang.add(0, lang);
	}

	/** Create data cell from document. */
	private XMLValue createDataCell(final Document doc) {
		// normalizeDocument adds e.g. missing xmls attributes
		doc.normalizeDocument();
		return (XMLValue)XMLCellFactory.create(doc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		try {
			m_parser.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
		m_in.close();
	}

}

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
 *   31.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A @link{XMLCellReader} to read a single cell from given.
 *
 * @author Heiko Hofer
 */
class XMLDOMCellReader implements XMLCellReader {
    private final InputSource m_in;

    private final DocumentBuilder m_builder;

    private boolean m_first = true;

    private static final DocumentBuilderFactory PARSER_FAC;

    static {
        if (System.getProperty("org.apache.xerces.xni.parser.XMLParserConfiguration") == null) {
        	// setting this property makes all Xerces parsers use a grammar pool, see
        	// http://xerces.apache.org/xerces2-j/faq-grammars.html#faq-4
//            System.setProperty("org.apache.xerces.xni.parser.XMLParserConfiguration",
//                XMLGrammarCachingConfiguration.class.getName());
        }
        PARSER_FAC = DocumentBuilderFactory.newInstance();

        PARSER_FAC.setValidating(false);
        PARSER_FAC.setNamespaceAware(true);
        PARSER_FAC.setXIncludeAware(true);
        try {
            PARSER_FAC.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
        } catch (ParserConfigurationException ex) {
            NodeLogger.getLogger(XMLDOMCellReader.class).warn(
                    "Could not enable usage of Java encodings in XML parser: "
                    + ex.getMessage(), ex);
        }

//        MemoryAlertSystem.getInstance().addListener(new MemoryAlertListener() {
//            private final XMLGrammarCachingConfiguration m_conf = new XMLGrammarCachingConfiguration();
//
//            @Override
//            protected boolean memoryAlert(final MemoryAlert alert) {
//                // all XMLGrammarCachingConfigurations use a shared static pool, therefore
//                // freeing this pool also frees the pool used by the parsers
//                NodeLogger.getLogger(XMLDOMCellReader.class)
//                    .debug("Clearing XML grammar cache due to low memory event");
//                m_conf.clearGrammarPool();
//                return false;
//            }
//        });
    }


    private XMLDOMCellReader(final InputSource is) throws ParserConfigurationException {
        m_in = is;
        m_builder = PARSER_FAC.newDocumentBuilder();
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_XML_DISABLE_EXT_ENTITIES)) {
            m_builder.setEntityResolver(NoExternalEntityResolver.getInstance());
        }
    }

    /**
     * Create a new instance of a {@link XMLCellReader} to read a single cell
     * from given {@link InputStream}.
     *
     * @param is the resource to read from
     * @throws ParserConfigurationException when the factory object for DOMs
     *             could not be created
     */
    public XMLDOMCellReader(final InputStream is)
            throws ParserConfigurationException {
        this(new InputSource(is));
    }

    /**
     * Create a new instance of a {@link XMLCellReader} to read a single cell
     * from given {@link Reader}.
     *
     * @param reader the resource to read from
     * @throws ParserConfigurationException when the factory object for DOMs
     *             could not be created
     */
    public XMLDOMCellReader(final Reader reader) throws ParserConfigurationException {
        this(new InputSource(reader));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public XMLValue<Document> readXML() throws IOException {
        if (m_first) {
            m_first = false;

            Document doc;
            try {
                doc = m_builder.parse(m_in);
            } catch (SAXException e) {
                throw new IOException(e);
            }
            removeEmptyTextRecursive(doc, new LinkedList<Boolean>());
            return (XMLValue<Document>)XMLCellFactory.create(doc);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (m_in.getByteStream() != null) {
            m_in.getByteStream().close();
        } else if (m_in.getCharacterStream() != null) {
            m_in.getCharacterStream().close();
        }
    }

    /**
     * Removes all descendent text nodes that contain only whitespace. These
     * come from the newlines and indentation between child elements. Take
     * xml:space declaration into account.
     */
    private void removeEmptyTextRecursive(final Node node,
            final List<Boolean> preserveSpaceStack) {
        boolean hasXmlSpaceAttr = false;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (attr.getNodeName().equals("xml:space")
                        && attr.getNamespaceURI().equals(
                                XMLConstants.XML_NS_URI)) {
                    if (attr.getNodeValue().equals("preserve")) {
                        hasXmlSpaceAttr = true;
                        preserveSpaceStack.add(0, true);
                    } else if (attr.getNodeValue().equals("default")) {
                        hasXmlSpaceAttr = true;
                        preserveSpaceStack.add(0, false);
                    } else {
                        // Wrong attribute value of xml:space, ignored.
                    }
                }
            }

        }
        boolean preserveSpace =
                !preserveSpaceStack.isEmpty() && preserveSpaceStack.get(0);
        List<Node> toRemove = new ArrayList<Node>();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    removeEmptyTextRecursive(child, preserveSpaceStack);
                    break;
                case Node.CDATA_SECTION_NODE:
                case Node.TEXT_NODE:
                    if (!preserveSpace) {
                        String str = child.getNodeValue();
                        if (null == str || str.trim().isEmpty()) {
                            toRemove.add(child);
                        } else {
                            ((CharacterData)child).setData(str);
                        }
                    }
                    break;
                default:
                    // do nothing
            }
        }
        for (Node child : toRemove) {
            node.removeChild(child);
        }
        if (hasXmlSpaceAttr) {
            preserveSpaceStack.remove(0);
        }
    }

}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   31.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A @link{XMLCellReader} to read a single cell from given
 *
 * @link{InputStream .
 *
 * @author Heiko Hofer
 */
class XMLDOMCellReader implements XMLCellReader {
    private final InputStream m_in;

    private final DocumentBuilder m_builder;

    private boolean m_first;

    private static final DocumentBuilderFactory PARSER_FAC;

    static {
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
    }

    /**
     * Create a new instance of a @link{XMLCellReader} to read a single cell
     * from given @link{InputStream}.
     *
     * @param is the resource to read from
     * @throws ParserConfigurationException when the factory object for DOMs
     *             could not be created.
     */
    public XMLDOMCellReader(final InputStream is)
            throws ParserConfigurationException {
        this.m_in = is;

        m_builder = PARSER_FAC.newDocumentBuilder();
        m_first = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLValue readXML() throws IOException {
        if (m_first) {
            m_first = false;

            InputSource source = null;

            source = new InputSource(m_in);

            Document doc;
            try {
                doc = m_builder.parse(source);
            } catch (SAXException e) {
                throw new IOException(e);
            }
            removeEmptyTextRecursive(doc, new LinkedList<Boolean>());
            XMLValue cell = (XMLValue)XMLCellFactory.create(doc);
            return cell;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_in.close();
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

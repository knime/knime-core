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
 *   31.03.2011 (hofer): created
 */
package org.knime.core.data.xml.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.DataCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Heiko Hofer
 */
class XMLDOMCellReader implements XMLCellReader {
    private InputStream m_in;
    private DocumentBuilder m_builder;
    private boolean m_first;

    /**
     * @param is
     * @throws ParserConfigurationException
     */
    public XMLDOMCellReader(final InputStream is) throws ParserConfigurationException {
        this.m_in = is;



        DocumentBuilderFactory domFactory =
                DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        domFactory.setXIncludeAware(true);
        m_builder = domFactory.newDocumentBuilder();

        m_first = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell readXML() throws XMLStreamException, IOException {
        if (m_first) {
            m_first = false;

            InputSource source = null;

            source = new InputSource(m_in);

            Document doc;
            try {
                doc = m_builder.parse(source);
            } catch (SAXException e) {
                throw new XMLStreamException(e);
            }
            // TODO: strip white space and trim nodes?
            removeEmptyTextRecursive(doc);
            DataCell cell = XMLCellFactory.create(doc);
            return cell;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws XMLStreamException, IOException {
        m_in.close();
    }


    /**
     *  Removes all descendent text nodes that contain only whitespace. These
     *  come from the newlines and indentation between child elements, and
     *  could be removed by by the parser if you had a DTD that specified
     *  element-only content.
     */
    private void removeEmptyTextRecursive(final Node node)
    {
        List<Node> toRemove = new ArrayList<Node>();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            switch (child.getNodeType())
            {
                case Node.ELEMENT_NODE :
                    removeEmptyTextRecursive(child);
                    break;
                case Node.CDATA_SECTION_NODE :
                case Node.TEXT_NODE :
                    String str = child.getNodeValue();
                    if (null == str || str.trim().isEmpty()) {
                        toRemove.add(child);
                    } else {
                        ((CharacterData)child).setData(str.trim());
                    }
                    break;
                default :
                    // do nothing
            }
        }
        for (Node child : toRemove) {
            node.removeChild(child);
        }
    }

}

/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   Mar 29, 2011 (morent): created
  */

package org.knime.core.data.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author morent
 *
 */
public class PMMLCellContent extends XMLCellContent implements PMMLValue {

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
    PMMLCellContent(final String xmlString) throws IOException,
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
    PMMLCellContent(final InputStream is) throws IOException,
            ParserConfigurationException, SAXException, XMLStreamException {
        super(is);
    }

    /**
     * Creates a new instance which encapsulates the passed XML document.
     *
     * @param doc an XML document
     */
    PMMLCellContent(final Document doc) {
       super(doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPMMLVersion() {
        Node pmml = getDocument().getElementsByTagName(
                PMMLPortObject.PMML_ELEMENT).item(0);
        return pmml.getAttributes().getNamedItem("version").getNodeValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<PMMLModelType> getModelTypes() {
        Set<PMMLModelType> types = new TreeSet<PMMLModelType>();
        for (Node node : getModels()) {
            types.add(PMMLModelType.getType(node.getNodeName()));
        }
        return types;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getModels(final PMMLModelType type) {
        List<Node> nodes = new LinkedList<Node>();
        NodeList list = getDocument().getElementsByTagName(type.toString());
        for (int i = 0; i < list.getLength(); i++) {
            nodes.add(list.item(i));
        }
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getModels() {
        List<Node> nodes = new LinkedList<Node>();
        for (PMMLModelType type : PMMLModelType.values()) {
            nodes.addAll(getModels(type));
        }
        return nodes;
    }


    /**
     * @return The XML Document as a string.
     */
    @Override
    String getStringValue() {
        return super.getStringValue();
    }

}

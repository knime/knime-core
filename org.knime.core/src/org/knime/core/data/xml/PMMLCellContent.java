/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.node.port.pmml.PMMLModelType;
import org.w3c.dom.Document;
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
       return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PMMLModelType> getModelTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return The XML Document as a string.
     */
    @Override
    String getStringValue() {
        return super.getStringValue();
    }

}

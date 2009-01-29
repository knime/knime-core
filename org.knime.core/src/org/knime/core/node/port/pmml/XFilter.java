/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.core.node.port.pmml;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class XFilter extends XMLFilterImpl {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            XFilter.class);
    
    
    private static final Map<String, String> xmlns_map 
        = new HashMap<String, String>();
    
    static {
        xmlns_map.put("3.0", "http://www.dmg.org/PMML-3_0");
        xmlns_map.put("3.1", "http://www.dmg.org/PMML-3_1");
        xmlns_map.put("3.2", "http://www.dmg.org/PMML-3_2");        
    }
    
    private final String m_xmlns;
    
    public XFilter(final String version) {
        m_xmlns = xmlns_map.get(version);
        if (m_xmlns == null) {
            throw new IllegalArgumentException(
                    "Version "  + version + " is not supported!");
       }
        
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void startElement(String arg0, String arg1, String arg2,
            Attributes atts) throws SAXException {
//        LOGGER.debug("uri: " + arg0);
//        LOGGER.debug("localname: " + arg1);
//        LOGGER.debug("name: " + arg2);
        AttributesImpl filteredAtts = new AttributesImpl();
        for (int i = 0; i < atts.getLength(); i++) {
            if (!atts.getQName(i).toLowerCase().startsWith("x-")
                    && !atts.getQName(i).toLowerCase().startsWith("xsi:")) {
                filteredAtts.addAttribute(atts.getURI(i), atts.getQName(i),
                        atts.getLocalName(i), atts.getType(i), 
                        atts.getValue(i));
            }
        }
        if (arg2.equals("PMML") && atts.getValue("xmlns") == null) {
            filteredAtts.addAttribute(null, null, "xmlns", "CDATA", 
                    m_xmlns); 
            filteredAtts.addAttribute(null, null, "xmlns:xsi", "CDATA", 
                    "http://www.w3.org/2001/XMLSchema-instance");
        }
        if (arg2.toLowerCase().startsWith("x-")) {
            // ignore
            LOGGER.debug("ignore x- element");
        } else {
            super.startElement(m_xmlns, 
                    arg1, arg2, filteredAtts);
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if (name.toLowerCase().startsWith("x-")) {
            // do nothing
            LOGGER.debug("ignore x- element");
        } else {
            super.endElement(m_xmlns, localName, name);
        }
    }

}

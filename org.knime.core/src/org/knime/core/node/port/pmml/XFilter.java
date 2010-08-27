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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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

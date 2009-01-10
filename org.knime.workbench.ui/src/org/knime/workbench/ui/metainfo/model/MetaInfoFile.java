/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.workbench.ui.metainfo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.NodeLogger;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public final class MetaInfoFile {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaInfoFile.class);
    
    private MetaInfoFile() {
        // utility class
    }

    public static final String METAINFO_FILE = "workflowset.meta";
    
    public static boolean createMetaInfoFile(final File parent) {
        try {
        File meta = new File(parent, METAINFO_FILE); 
        SAXTransformerFactory fac 
            = (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler handler = fac.newTransformerHandler();
    
        Transformer t = handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");

        OutputStream out = new FileOutputStream(meta);
        handler.setResult(new StreamResult(out));

        handler.startDocument();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "nrOfElements", "CDATA", ""
                + 2);
        handler.startElement(null, null, "KNIMEMetaInfo", atts);
        
        // author
        atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", 
                MetaGUIElement.TEXT);
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", 
                "Author");
        handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        handler.endElement(null, null, MetaGUIElement.ELEMENT);
        
        // creation date
        atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", 
                MetaGUIElement.DATE);
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", 
                "Creation Date");
        handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        handler.endElement(null, null, MetaGUIElement.ELEMENT);  
        
        // comments 
        // creation date
        atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", 
                MetaGUIElement.MULTILINE);
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", 
                "Comments");
        handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        handler.endElement(null, null, MetaGUIElement.ELEMENT);  
        
        // TODO: add here all default elements
        
        handler.endElement(null, null, "KNIMEMetaInfo");
        handler.endDocument();
        out.close();
        } catch (Exception e) {
            LOGGER.error("Error while trying to create default " 
                    + "meta info file for" + parent.getName(), e);
            return false;
        }
        return true;
    }
    
}

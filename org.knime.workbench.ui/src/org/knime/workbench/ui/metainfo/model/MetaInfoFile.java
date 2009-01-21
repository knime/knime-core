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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public final class MetaInfoFile {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaInfoFile.class);
    
    public static final String PREF_KEY_META_INFO_TEMPLATE 
        = "org.knime.ui.metainfo.template";
    
    private MetaInfoFile() {
        // utility class
    }

    public static final String METAINFO_FILE = "workflowset.meta";
    
    public static void createMetaInfoFile(final File parent) {
        // look into preference store
        File f = getFileFromPreferences(); 
        if (f != null) {
            writeFileFromPreferences(parent, f);
        } else {
            createDefaultFileFallback(parent);
        }
    }
    
    private static void writeFileFromPreferences(final File parent, 
            final File f) {
        File dest = new File(parent, METAINFO_FILE);
        try {
            FileUtil.copy(f, dest);
        } catch (IOException io) {
            LOGGER.error("Error while creating meta info template for "
                    + parent.getName()
                    + ". Creating default file...", io);
            createDefaultFileFallback(parent);
        }
    }

    private static File getFileFromPreferences() {
        String fileName = KNIMEUIPlugin.getDefault().getPreferenceStore()
            .getString(PREF_KEY_META_INFO_TEMPLATE);
        if (fileName == null) {
            return null;
        }
        File f = new File(fileName);
        return f;
    }

    private static void createDefaultFileFallback(final File parent) {
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
//            atts.addAttribute(null, null, MetaGUIElement.READ_ONLY, "CDATA", 
//                    "true");
            handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
            Calendar current = Calendar.getInstance();
            String date = DateMetaGUIElement.createStorageString(
                    current.get(Calendar.DAY_OF_MONTH), 
                    current.get(Calendar.MONTH), 
                    current.get(Calendar.YEAR));
            char[] dateChars = date.toCharArray();
            handler.characters(dateChars, 0, dateChars.length);
            handler.endElement(null, null, MetaGUIElement.ELEMENT);

            
            // comments 
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
            }
    }
    
}

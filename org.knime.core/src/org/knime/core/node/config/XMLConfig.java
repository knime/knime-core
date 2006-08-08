/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.knime.core.node.InvalidSettingsException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.Serializer;
import com.sun.org.apache.xml.internal.serialize.SerializerFactory;

/**
 * A class used to load and save Config objects into an XML file.
 * <p>
 * This implementation uses a SAX Parser to create and save the xml files. This
 * got necessary since predictive params may get big and using a DOM parser
 * keeps the entire xml-tree in memory.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class XMLConfig {
    private static SAXParserFactory factory;
    
    static {
        String old = System.getProperty("javax.xml.parsers.SAXParserFactory");
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        
        try {
            factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
        } finally {        
            if (old != null) {
                System.setProperty("javax.xml.parsers.SAXParserFactory", old);
            } else {
                System.clearProperty("javax.xml.parsers.SAXParserFactory");
            }
        }
    }
    
    private XMLConfig() {

    }

    /** dtd name from class name. */
    static final String DTD_NAME = XMLConfig.class.getName().replace('.', '/')
            + ".dtd";

    /** The old DTD name from the 1.0.0 release. */
    static final String OLD_DTD_NAME = XMLConfig.class.getName().replace('.',
            '/').replace("org/knime/", "de/unikn/knime/") + ".dtd";

    /**
     * Read config entries from an XML file. The entries being read are stored
     * in a newly created <code>Config</code> object.
     * 
     * @param config The Config, which is used as factory to create a new
     *            instance from (representing the root from the XML file).
     * @param is The XML inputstream storing the configuration to read
     * @return The root Config.
     * @throws IOException If the stream could not be read.
     */
    static Config load(final Config config, final InputStream is)
            throws IOException {
        Config cfg = config.getInstance("ignored");
        try {
            internalLoad(cfg, is);
        } catch (SAXException se) {
            IOException ioe = new IOException(se.getMessage());
            ioe.initCause(se);
            throw ioe;
        } catch (ParserConfigurationException pce) {
            IOException ioe = new IOException(pce.getMessage());
            ioe.initCause(pce);
            throw ioe;
        } finally {
            is.close();
        }
        // return first child of cfg
        for (String s : cfg.keySet()) {
            try {
                return cfg.getConfig(s);
            } catch (InvalidSettingsException ise) {
                throw new IOException("Reading from \"" + is.toString()
                        + "\" failed; does not start with config.");
            }
        }
        throw new IOException("Reading from \"" + is.toString()
                + "\" failed; no tags defined.");
    }

    /*
     * Helper method to read the xml given by the inputstream to the config
     * object.
     */
    private static void internalLoad(final Config c, final InputStream in)
            throws SAXException, IOException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        XMLReader reader = saxParser.getXMLReader();
        XMLContentHandler xmlContentHandler = new XMLContentHandler(c, in
                .toString());
        reader.setContentHandler(xmlContentHandler);
        reader.setEntityResolver(xmlContentHandler);
        reader.setErrorHandler(xmlContentHandler);        
        reader.parse(new InputSource(in));
    }

    /**
     * Saves given Config into an XML stream. The stream is closed at the end.
     * 
     * @param config The Config the save.
     * @param os The stream to write Config as XML to.
     * @throws IOException If te Config could not be stored.
     */
    static void save(final Config config, final OutputStream os)
            throws IOException {
        SerializerFactory f = SerializerFactory.getSerializerFactory("xml");
        OutputFormat format = new OutputFormat();
        format.setIndent(2);
        format.setDoctype(null, DTD_NAME);
        Serializer serializer = f.makeSerializer(os, format);
        try {
            XMLContentHandler.asXML(config, serializer.asContentHandler());
        } catch (SAXException se) {
            IOException ioe = new IOException("Saving xml to " + os.toString()
                    + " failed: " + se.getMessage());
            ioe.initCause(se);
            throw ioe;
        } finally {
            // Note: When using the GZIP stream, it is also required by the
            // ZLIB native library in order to support certain optimizations
            // to flush the stream.
            os.flush();
            os.close();
        }
    }

}

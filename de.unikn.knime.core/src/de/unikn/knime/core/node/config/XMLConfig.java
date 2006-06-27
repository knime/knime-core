/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.Serializer;
import com.sun.org.apache.xml.internal.serialize.SerializerFactory;

import de.unikn.knime.core.node.InvalidSettingsException;

/**
 * A class used to load and save Config objects into an XML file. 
 * <p>This implementation uses a SAX Parser to create and save the xml files. 
 * This got necessary since predictive params may get big and using a DOM parser
 * keeps the entire xml-tree in memory.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class XMLConfig {

    private XMLConfig() {

    }
    
    /** dtd name from class name. */
    static final String DTD_NAME = 
        XMLConfig.class.getName().replace('.', '/') + ".dtd";


    /**
     * Read config entries from an XML file. Depending on the readRoot flag, the
     * entries read are either stored in the passed config parameter, or stored
     * in a newly created <code>Config</code> object. The only difference is
     * the name of the resulting config object. With readRoot set to true the
     * name will be the one stored in the root of the XML file, if set to false,
     * the name will remain unchanged. If a new config object is created the
     * passed config parameter is used as factory to create a new instance.
     * 
     * @param config The Config to save settings into if <code>readRoot</code>
     *            is set to false. Otherwise it's used as factory to create a
     *            new instance from (representing the root from the XML file).
     * @param is The XML inputstream storing the configuration to read
     * @param readRoot if <code>true</code> the root element from the stream
     *            is used to init a new Config object, which will be returned
     *            then; otherwise the content of the root element will be stored
     *            in the passed config object (and null will be returned).
     * @return The root Config (if <code>readRoot</code> is set to
     *         <code>true</code>) or null (if <code>readRoot</code> is
     *         <code>false</code>).
     * @throws IOException If the stream could not be read.
     */
    static Config load(final Config config, final InputStream is,
            final boolean readRoot) throws IOException {
        Config cfg;
        if (readRoot) {
            cfg = config.getInstance("ignored");
        } else {
            cfg = config;
        }
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
        if (readRoot) {   
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
        } else {
            return null;
        }
    }
    
    /** Helper method to read the xml given by the inputstream to the config 
     * object. */
    private static void internalLoad(final Config c, final InputStream in)
            throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setValidating(true);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLReader reader = saxParser.getXMLReader();
        XMLContentHandler xmlContentHandler = 
            new XMLContentHandler(c, in.toString());
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
            os.close();
            os.flush();
        }
    }

}

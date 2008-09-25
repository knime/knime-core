/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 *
 * History
 *   17.01.2006(sieb, ohl): reviewed
 */
package org.knime.core.node.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
    private static final SAXParserFactory parserFactory;

    private static final SAXTransformerFactory transformerFactory;

    static {
        try {
            transformerFactory =
                    (SAXTransformerFactory)TransformerFactory.newInstance();
            parserFactory = SAXParserFactory.newInstance();
            parserFactory.setValidating(false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** dtd name from class name. */
    static final String DTD_NAME =
            XMLConfig.class.getName().replace('.', '/') + ".dtd";

    private XMLConfig() {

    }

    /**
     * Reads from the given input stream into the given config object.
     *
     * @param c Where to put the results.
     * @param in Where to read from, stream will be closed when done.
     * @throws SAXException If stream can't be properly parsed.
     * @throws IOException If IO problem occur.
     * @throws ParserConfigurationException If not properly configured.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    static void load(final Config c, final InputStream in) throws SAXException,
            IOException, ParserConfigurationException {
        SAXParser saxParser = parserFactory.newSAXParser();
        XMLReader reader = saxParser.getXMLReader();
        XMLContentHandler xmlContentHandler =
                new XMLContentHandler(c, in.toString());
        reader.setContentHandler(xmlContentHandler);
        reader.setEntityResolver(xmlContentHandler);
        reader.setErrorHandler(xmlContentHandler);

        // ====================================================================
        // This hack filter the DTD declaration out of the stream, so that
        // bug #1201 does not occur any more. Some time in time in the future
        // we may remove this part if no DTD-based XMLConfigs exist any more.
        // If some one messed with the file by hand, this may fail!
        BufferedReader buf = new BufferedReader(new InputStreamReader(in));
        String line = buf.readLine().trim(); // this must be the XML declaration
        if (!"<?xml version=\"1.0\" encoding=\"UTF-8\"?>".equals(line)) {
            throw new IOException("No valid XML file");
        }
        buf.mark(2048);
        line = buf.readLine().trim();
        if (line.startsWith("<!")) {
            while (!line.endsWith(">")) {
                line = buf.readLine().trim();
            }
        } else {
            buf.reset();
        }
        // ====================================================================

        reader.parse(new InputSource(buf));
    }

    /**
     * Saves given Config into an XML stream. The stream is closed at the end.
     *
     * @param config the Config the save
     * @param os the stream to write Config as XML to
     * @throws IOException if the Config could not be stored
     */
    static void save(final Config config, final OutputStream os)
            throws IOException {
        TransformerHandler tfh = null;
        try {
            tfh = transformerFactory.newTransformerHandler();
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        Transformer t = tfh.getTransformer();

        t.setOutputProperty(OutputKeys.METHOD, "xml");
        // t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_NAME);
        t.setOutputProperty(OutputKeys.INDENT, "yes");

        tfh.setResult(new StreamResult(os));

        try {
            XMLContentHandler.asXML(config, tfh);
        } catch (SAXException se) {
            IOException ioe =
                    new IOException("Saving xml to " + os.toString()
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

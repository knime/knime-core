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
        BufferedReader buf = new BufferedReader(
                new InputStreamReader(in, "UTF-8"));
        String line = buf.readLine(); // this must be the XML declaration
        if (line != null) {
            if (!"<?xml version=\"1.0\" encoding=\"UTF-8\"?>".equals(line.trim())) {
                throw new IOException("No valid XML file");
            }
            buf.mark(2048);
            line = buf.readLine();
            if ((line != null) && line.trim().startsWith("<!")) {
                while ((line != null) && !line.trim().endsWith(">")) {
                    line = buf.readLine();
                }
            } else {
                buf.reset();
            }
        }
        // ====================================================================
        
        try {
            reader.parse(new InputSource(buf));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to parse xml: " + e.getMessage(), e);
        }
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

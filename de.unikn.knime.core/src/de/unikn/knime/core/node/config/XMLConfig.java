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
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A class used to load and save Config objects into an XML file.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
final class XMLConfig {

    private XMLConfig() {

    }
    
    /** dtd name from class name. */
    private static final String DTD_NAME = 
        XMLConfig.class.getName().replace('.', '/') + ".dtd";

    private static InputSource getInputSourceDTD() 
            throws IOException {
        // gets URL for systemId which specifies the dtd file+path
        ClassLoader classLoader = XMLConfig.class.getClassLoader();
        URL dtdURL = classLoader.getResource(DTD_NAME);
        InputStream is = dtdURL.openStream();
        return new InputSource(is);
    }

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
        Document doc = null;
        try {
            doc = getLoadingDoc(is);
        } catch (SAXException saxe) {
            IOException ioe = new IOException();
            ioe.initCause(saxe);
            throw ioe;
        } catch (IOException ioe) {
            throw ioe;
        }
        Element element = (Element)doc.getChildNodes().item(1);

        if (readRoot) {
            Config cfg = config.getInstance(element.getAttribute("key"));
            load(cfg, element);
            return cfg;
        } else {
            load(config, element);
            return null;
        }

    }

    private static Document getLoadingDoc(final InputStream in)
            throws SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // set namespaceAware to true to get a DOM Level 2 tree with nodes
        // containing namespace information. This is necessary because the
        // default value from JAXP 1.0 was defined to be false.
        dbf.setNamespaceAware(true);
        // sets validation with dtd file
        dbf.setValidating(true);
        // optional: set various configuration options
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        // the opposite of creating entity ref nodes is expanding them inline
        dbf.setExpandEntityReferences(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ConfigErrorHandler(in.toString()));
            // set EntityResolver for dtd validation
            db.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(final String publicId,
                        final String systemId) 
                            throws SAXException, IOException {
                    return getInputSourceDTD();
                }
            });
            // open XML stream source
            InputSource source = new InputSource(in);
            // set dtd file, need to call EntityResolver
            source.setSystemId(DTD_NAME);
            return db.parse(source);
        } catch (ParserConfigurationException x) {
            throw new Error(x);
        }
    }

    /**
     * Recursively stores the child entries of the given element into the passed
     * config object.
     * 
     * @param config The config object to fill.
     * @param element the childs of this will be stored in the config
     * @throws IOException if the file coudln't be read or contained an invalid
     *             entry
     */
    private static void load(final Config config, final Element element)
            throws IOException {
        NodeList entries = element.getChildNodes();
        int numEntries = entries.getLength();
        for (int i = 0; i < numEntries; i++) {
            Element entry = (Element)entries.item(i);
            if (entry.getNodeName().equals(ConfigEntries.config.name())) {
                Config subconfig = config.addConfig(entry.getAttribute("key"));
                load(subconfig, entry);
            } else {
                String key = entry.getAttribute("key");
                String type = entry.getAttribute("type");
                String value = entry.getAttribute("value");

                ConfigEntries configEntryType;
                // transform runtime IllegalArgumentException into a IOException
                // to force exception handling in caller methods.
                try {
                    configEntryType = ConfigEntries.valueOf(type);
                } catch (IllegalArgumentException iae) {
                    throw new IOException("Invalid type ('" + type
                            + "') for key '" + key + "' in XML file.");
                }
                // handle null values and be backward compatible
                if (entry.hasAttribute("isnull")) {
                    if (entry.getAttribute("isnull").equals("true")) {
                        value = null;
                    }
                }

                ConfigurableEntry ab = configEntryType.createEntry(key, value);
                config.addEntry(key, ab);
            }
        }
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
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            IOException ioe = new IOException();
            ioe.initCause(pce);
            throw ioe;
        }
        Document doc = db.newDocument();
        Element root = (Element)doc.appendChild(doc.createElement("config"));
        root.setAttribute("key", config.getKey());
        save(config, root, doc);
        emitDocument(doc, os);
        os.close();
    }

    /**
     * Recursively builds the XML-DOM representing the content of the config. 
     * @param config of which the content will be added to the DOM 
     * @param element part of the DOM representing the passed config. 
     * @param doc the DOM, the element is part of.
     */
    private static void save(final Config config, final Element element,
            final Document doc) {
        for (String key : config.keySet()) {
            AbstractConfigEntry e = config.getEntry(key);
            if (e instanceof Config) {
                Element entry = (Element)element.appendChild(doc
                        .createElement(ConfigEntries.config.name()));
                entry.setAttribute("key", e.getKey());
                save((Config)e, entry, doc);
            } else {
                Element entry = (Element)element.appendChild(doc
                        .createElement("entry"));
                entry.setAttribute("key", e.getKey());
                entry.setAttribute("type", e.getType().name());
                String value = ((ConfigurableEntry)e).toStringValue();

                if (value == null) {
                    entry.setAttribute("isnull", "true");
                    value = "";
                }
                entry.setAttribute("value", value);
            }
        }
    }

    /**
     * Does the actual writing of the XML file. Writes the passed document as
     * XML representation into the specified output stream.
     * 
     * @param doc the document to save.
     * @param os stream to write document in XML format into. 
     * @throws IOException if writing to the stream failed. 
     */
    private static void emitDocument(final Document doc, final OutputStream os)
            throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_NAME);
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
        DOMSource doms = new DOMSource(doc);
        StreamResult sr = new StreamResult(os);
        try {
            t.transform(doms, sr);
        } catch (TransformerException te) {
            IOException ioe = new IOException();
            ioe.initCause(te);
            throw ioe;
        }
    }

    private static class ConfigErrorHandler implements ErrorHandler {
        /** The XML file to parse. */
        private final String m_xmlFile;

        /**
         * ErrorHandler for given XML file.
         * 
         * @param xmlFile The XML file parsing.
         */
        ConfigErrorHandler(final String xmlFile) {
            m_xmlFile = xmlFile;
        }

        /**
         * Returns a string describing parse exception details.
         * 
         * @param spe <code>SAXParseException</code>.
         * @return String describing parse exception details.
         */
        private String getParseExceptionInfo(final SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            return "line=" + spe.getLineNumber() + ": " + spe.getMessage()
                    + "\n" + "xml: URI=" + m_xmlFile + "\n" + "dtd: URI="
                    + systemId;
        }

        /**
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        public void warning(final SAXParseException spe) throws SAXException {
            final String message = "Warning: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        /**
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        public void error(final SAXParseException spe) throws SAXException {
            final String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        /**
         * @see org.xml.sax.ErrorHandler#fatalError(SAXParseException)
         */
        public void fatalError(final SAXParseException spe) 
                throws SAXException {
            final String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

    }

}

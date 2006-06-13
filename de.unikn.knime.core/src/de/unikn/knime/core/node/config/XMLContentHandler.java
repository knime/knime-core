/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   Jun 13, 2006 (wiswedel): created
 */
package de.unikn.knime.core.node.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility class to handle SAX events while parsing the xml file.
 * @author wiswedel, University of Konstanz
 */
class XMLContentHandler extends DefaultHandler {
    
    private final Stack<Config> m_elementStack;
    private final String m_fileName;
    
    /**
     * Creates new instance.
     * @param config The config object as root of the xml tree, this
     * class adds sub-entrys to this root node.
     * @param fileName The file name for eventual error messages. 
     */
    XMLContentHandler(final Config config, final String fileName) {
        m_elementStack = new Stack<Config>();
        m_elementStack.push(config);
        m_fileName = fileName;
    }

    /**
     * @see DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
        throws SAXException {
        System.out.println(new String(ch, start, length));
    }

    /**
     * @see DefaultHandler#endElement(String, String, String)
     */
    @Override
    public void endElement(
            final String uri, final String localName, final String qName) 
        throws SAXException {
        if (ConfigEntries.config.name().equals(qName)) {
            m_elementStack.pop();
        } 
        // ignore closing of "entry" tags
    }
    
    /**
     * @see DefaultHandler#error(SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        String message = getParseExceptionInfo(e);
        throw new SAXException(message, e);
    }
    
    /**
     * @see DefaultHandler#warning(SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
        String message = getParseExceptionInfo(e);
        throw new SAXException(message, e);
    }

    /**
     * @see DefaultHandler#fatalError(SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        String message = getParseExceptionInfo(e);
        throw new SAXException(message, e);
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
                + "\n" + "xml: URI=" + m_fileName + "\n" + "dtd: URI="
                + systemId;
    }

    /**
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, 
            final String qName, final Attributes attributes) 
        throws SAXException {
        Config peek = m_elementStack.peek();
        if (ConfigEntries.config.name().equals(qName)) {
            // create sub config
            Config subConfig = peek.addConfig(attributes.getValue("key"));
            m_elementStack.push(subConfig);
        } else if ("entry".equals(qName)) {
            String key = attributes.getValue("key");
            String type = attributes.getValue("type");
            String value = attributes.getValue("value");

            ConfigEntries configEntryType;
            // transform runtime IllegalArgumentException into a IOException
            // to force exception handling in caller methods.
            try {
                configEntryType = ConfigEntries.valueOf(type);
            } catch (IllegalArgumentException iae) {
                throw new SAXException("Invalid type ('" + type
                        + "') for key '" + key + "' in XML file.");
            }
            // handle null values and be backward compatible
            boolean isNull = "true".equals(attributes.getValue("isnull"));
            if (isNull) {
                value = null;
            }
            ConfigurableEntry ab = configEntryType.createEntry(key, value);
            peek.addEntry(key, ab);
        } else {
            // only "config" and "entry" are valid tag names 
            throw new SAXException("\"" + qName 
                    + "\" is not a valid tag name.");
        }
    }

    /**
     * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
     */
    @Override
    public InputSource resolveEntity(
            final String publicId, final String systemId) 
        throws IOException, SAXException {
        assert systemId != null && systemId.endsWith(XMLConfig.DTD_NAME); 
        // gets URL for systemId which specifies the dtd file+path
        ClassLoader classLoader = XMLConfig.class.getClassLoader();
        URL dtdURL = classLoader.getResource(XMLConfig.DTD_NAME);
        InputStream is = dtdURL.openStream();
        return new InputSource(is);
    }
    
    /** Utitlity method that writes the given config object to a content 
     * handler. The content handler will take care to write to a file.
     * @param c The config to write, must not be <code>null</code>.
     * @param handler To write to.
     * @throws SAXException If that fails.
     */
    static void asXML(final Config c, final ContentHandler handler) 
        throws SAXException {
        handler.startDocument();
        internalAsXML(c, handler);
        handler.endDocument();
    }
    
    private static void internalAsXML(final Config c, 
            final ContentHandler handler) throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "key", "CDATA", c.getKey());
        handler.startElement(null, null, ConfigEntries.config.name(), attr);
        for (String key : c.keySet()) {
            AbstractConfigEntry e = c.getEntry(key);
            if (e instanceof Config) {
                internalAsXML((Config)e, handler);
            } else {
                AttributesImpl a = new AttributesImpl();
                a.addAttribute(null, null, "key", "CDATA", e.getKey());
                a.addAttribute(null, null, "type", "CDATA", e.getType().name());
                String value = ((ConfigurableEntry)e).toStringValue();

                if (value == null) {
                    a.addAttribute(null, null, "isnull", "CDATA", "true");
                    value = "";
                }
                a.addAttribute(null, null, "value", "CDATA", value);
                handler.startElement(null, null, "entry", a);
                handler.endElement(null, null, "entry");
            }
        }
        handler.endElement(null, null, ConfigEntries.config.name());
    }
}

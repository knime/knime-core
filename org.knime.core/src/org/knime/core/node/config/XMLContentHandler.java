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
 *   Jun 13, 2006 (wiswedel): created
 */
package org.knime.core.node.config;

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
 *
 * @author wiswedel, University of Konstanz
 */
class XMLContentHandler extends DefaultHandler {

    private final Stack<Config> m_elementStack;

    private final String m_fileName;

    private boolean m_isFirst = true;

    /**
     * Creates new instance.
     *
     * @param config The config object as root of the xml tree, this class adds
     *            sub-entrys to this root node.
     * @param fileName The file name for eventual error messages.
     */
    XMLContentHandler(final Config config, final String fileName) {
        m_elementStack = new Stack<Config>();
        m_elementStack.push(config);
        m_fileName = fileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (ConfigEntries.config.name().equals(qName)) {
            m_elementStack.pop();
        }
        // ignore closing of "entry" tags
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        String message = getParseExceptionInfo(e);
        throw new SAXException(message, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
        String message = getParseExceptionInfo(e);
        throw new SAXException(message, e);
    }

    /**
     * {@inheritDoc}
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
        return "line=" + spe.getLineNumber() + ": " + spe.getMessage() + "\n"
                + "xml: URI=" + m_fileName + "\n" + "dtd: URI=" + systemId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        Config peek = m_elementStack.peek();
        if (ConfigEntries.config.name().equals(qName)) {
            if (m_isFirst) {
                m_isFirst = false;
                peek.setKey(attributes.getValue("key"));
            } else {
                // create sub config
                Config subConfig = peek.addConfig(attributes.getValue("key"));
                m_elementStack.push(subConfig);
            }
        } else if ("entry".equals(qName)) {
            assert !m_isFirst : "First element in xml is not a config";
            String key = attributes.getValue("key");
            String type = attributes.getValue("type");
            String value = attributes.getValue("value");

            value = unescape(value);

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
            AbstractConfigEntry ab = configEntryType.createEntry(key, value);
            peek.addEntry(ab);
        } else {
            // only "config" and "entry" are valid tag names
            throw new SAXException("\"" + qName + "\" is not a valid tag name.");
        }
    }

    /**
     * {@inheritDoc}
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

    /**
     * Utility method that writes the given config object to a content handler.
     * The content handler will take care to write to a file.
     *
     * @param c The config to write, must not be <code>null</code>.
     * @param handler To write to.
     * @throws SAXException If that fails.
     */
    static void asXML(final Config c, final ContentHandler handler)
            throws SAXException {
        handler.startDocument();
        internalAsXML(c, handler, 0);
        handler.endDocument();
    }

    private static void internalAsXML(final Config c,
            final ContentHandler handler, final int depth) throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        if (depth == 0) {
            attr.addAttribute(null, null, "xmlns", "CDATA",
                    "http://www.knime.org/2008/09/XMLConfig");
            attr.addAttribute(null, null, "xmlns:xsi", "CDATA",
                    "http://www.w3.org/2001/XMLSchema-instance");
            attr.addAttribute(null, null, "xsi:schemaLocation", "CDATA",
                    "http://www.knime.org/2008/09/XMLConfig "
                    + "http://www.knime.org/XMLConfig_2008_09.xsd");
        }

        attr.addAttribute("", "", "key", "CDATA", c.getKey());
        handler.startElement("", "", ConfigEntries.config.name(), attr);
        for (String key : c.keySet()) {
            AbstractConfigEntry e = c.getEntry(key);
            if (e instanceof Config) {
                internalAsXML((Config)e, handler, depth + 1);
            } else {
                AttributesImpl a = new AttributesImpl();
                a.addAttribute("", "", "key", "CDATA", key);
                a.addAttribute("", "", "type", "CDATA", e.getType().name());
                String value = e.toStringValue();

                if (value == null) {
                    a.addAttribute("", "", "isnull", "CDATA", "true");
                    value = "";
                }
                value = escape(value);
                a.addAttribute("", "", "value", "CDATA", value);
                handler.startElement("", "", "entry", a);
                handler.endElement("", "", "entry");
            }
        }
        handler.endElement("", "", ConfigEntries.config.name());
    }

    /**
     * Escapes all forbidden XML characters so that we can save them
     * nevertheless. They are escaped as &quot;%%ddddd&quot;, with ddddd being
     * their decimal Unicode.
     *
     * @param s the string to escape
     * @return the escaped string
     */
    static final String escape(final String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < c.length; i++) {
            if (((c[i] < 32) || (c[i] > 0xd7ff))
                    || ((i < c.length - 1) && (c[i] == '%') && c[i + 1] == '%')) {
                // if c contains '%' we encode the '%'
                buf.append("%%");
                if (c[i] < 10) {
                    buf.append('0');
                }
                if (c[i] < 100) {
                    buf.append('0');
                }
                if (c[i] < 1000) {
                    buf.append('0');
                }
                if (c[i] < 10000) {
                    buf.append('0');
                }

                buf.append(Integer.toString(c[i]));
            } else {
                buf.append(c[i]);
            }
        }

        return buf.toString();
    }

    /**
     * Unescapes all forbidden XML characters that were previous escaped by
     * {@link #escape(String)}. Must pay attention to handle not escaped
     * strings for backward compatibility (it will not correctly handle them,
     * they still are unescaped, but it must not fail on those strings).
     *
     * @param s the escaped string
     * @return the unescaped string
     */
    static final String unescape(final String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < c.length; i++) {
            if ((c[i] == '%') && (i < c.length - 6) && c[i + 1] == '%'
                    && Character.isDigit(c[i + 2])
                    && Character.isDigit(c[i + 3])
                    && Character.isDigit(c[i + 4])
                    && Character.isDigit(c[i + 5])
                    && Character.isDigit(c[i + 6])) {
                buf
                        .append((char)((c[i + 2] - '0') * 10000
                                + (c[i + 3] - '0') * 1000 + (c[i + 4] - '0')
                                * 100 + (c[i + 5] - '0') * 10 + (c[i + 6] - '0')));
                i += 6;
            } else {
                buf.append(c[i]);
            }
        }

        return buf.toString();
    }
}

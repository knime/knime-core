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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.node.NodeLogger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/** 
 * XML properties class that gets an xml <code>URL</code> and dtd file
 * path as input to initialize the xml <code>org.w3c.dom.Document</code> which 
 * can be accessed to get parts of the document tree. The class always throws a 
 * <code>DOMException</code>, if the <code>Document</code> does not match the 
 * query element/attribute combination. Any error during parsing will throw a 
 * <code>SAXParseException</code>. Note, so far this class only returns node
 * elements of type <code>org.w3c.dom.Node#ELEMENT_NODE</code>.
 *  
 * @author Thomas Gabriel, University of Konstanz
 */
public class XMLProperties implements ErrorHandler {
    
    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(XMLProperties.class);
    
    /** Keeps the URL to the xml file. */
    private final URL m_xmlFile;
    
    /** Keeps the parsed xml Document. */    
    private Document m_doc;
    
    /**
     * Initializes a new xml properties object by parsing the 
     * <code>xmlURL</code> which internally represents a xml 
     * <code>Document</code>.
     * @param  xmlURL <code>URL</code> where to find the XML file.
     * @param  dtdURL The DTD's URL used to parse the given xml file.
     * @throws IOException If XML <code>URL</code> not valid.
     * @throws SAXException Parser exception caused by the 
     *         <code>DocumentBuilder</code>.
     * @throws ParserConfigurationException Parser configuration exception.
     * @throws IllegalArgumentException If xml <code>URL</code> is 
     *         <code>null</code>.
     */
    public XMLProperties(final URL xmlURL, final URL dtdURL)
            throws IOException, SAXException, ParserConfigurationException {
        
        // check xml URL
        if (xmlURL == null) {
            throw new IllegalArgumentException(
                "URL of XML file must not be null!");
        }
        m_xmlFile = xmlURL;
        
        // create a DocumentBuilderFactory and configure it
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    
        // set namespaceAware to true to get a DOM Level 2 tree with nodes
        // containing namespace information.  This is necessary because the
        // default value from JAXP 1.0 was defined to be false.
        dbf.setNamespaceAware(true);
    
        // sets validation with DTD file
        dbf.setValidating(true);

        // optional: set various configuration options
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        // the opposite of creating entity ref nodes is expanding them inline
        dbf.setExpandEntityReferences(true);
    
        // create a DocumentBuilder that satisfies the constraints
        // specified by the DocumentBuilderFactory
        DocumentBuilder db = dbf.newDocumentBuilder();
    
        // set this ErrorHandler before parsing
        db.setErrorHandler(this);
        
        // set EntityResolver for DTD validation
        db.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(
                    final String publicId, final String systemId)
                    throws SAXException, IOException {
                InputStream is = dtdURL.openStream();
                return new InputSource(is);
            }          
        });
        
        // open XML stream source
        final InputSource source = new InputSource(xmlURL.openStream());
        // set DTD file, need to call EntityResolver
        source.setSystemId("");
            
        // parse the input file
        m_doc = db.parse(source);
        
    }   // XMLProperties(URL,URL)
    
    /* --- parser error handler's functions --- */
    
    /* 
     * Returns a string describing parse exception details.
     * @param  spe <code>SAXParseException</code>.
     * @return String describing parse exception details. 
     */
    private String getParseExceptionInfo(final SAXParseException spe) {
        String systemId = spe.getSystemId();
        if (systemId == null) {
            systemId = "null";
        }
        return "line=" + spe.getLineNumber() + ": " + spe.getMessage()
            + "\n" + "xml: URI=" + m_xmlFile.getPath() 
            + "\n" + "dtd: URI=" + systemId;
    }
    
    /**
     * {@inheritDoc}
     */
    public void warning(final SAXParseException spe) throws SAXException {
        LOGGER.warn(getParseExceptionInfo(spe), spe);
    }
    
    /**
     * {@inheritDoc}
     */ 
    public void error(final SAXParseException spe) throws SAXException {
        final String message = "Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }

    /**
     * {@inheritDoc}
     */
    public void fatalError(final SAXParseException spe) throws SAXException {
        final String message = "Fatal Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }
    
    /**
     * Throws a <code>DOMException</code> with a message code and a message
     * extended by the xml file.
     * @see   DOMException
     * @param code of DOMexception.
     * @param message string.
     */ 
    private void throwDOMException(final short code, final String message) {
        // throw a real DOMException
        throw new DOMException(code, message + ": " 
                + m_xmlFile.getPath() + "!");
    }
    
    /* --- xml document get functions --- */
    
    /** 
     * Returns a <code>Node</code> from the document which can be associated 
     * with the <code>elementName</code> argument which must be of type 
     * <code>Node#ELEMENT_NODE</code>.
     * @param  elementName xml element name to search for.
     * @param  required <code>true</code> if node element is required in xml 
     *         file, otherwise <code>false</code>.
     * @return Node for element name.
     * @throws DOMException If element is not in xml file but is required to be.
     * @throws IllegalArgumentException If element name is <code>null</code>.
     */
    public final Node getNodeElement(
            final String elementName, final boolean required) {
        // check element name
        if (elementName == null) {
            throw new IllegalArgumentException(
                "Element name must not be null!");
        }
        // get node for element
        Node node = searchNodeElement(m_doc, elementName);
        // check node
        if (node == null && required) {
            // throw exception if element can not be associated with a node
            throwDOMException(DOMException.NOT_FOUND_ERR,
                "Node for element '" + elementName 
                + "' not specified in xml file");
        }
        // return node for current element from the document 
        return node;
    }
    
    /** 
     * Returns the attribute value for a xml element and attribute name.
     * @param  elementName element name to look for specified attribute.
     * @param  attName xml attribute name to search for.
     * @param  required <code>true</code> if element must be specified in xml
     *         file, otherwise <code>false</code>.
     * @return attribute value at element and for attribute specified if 
     *         available, otherwise <code>null</code>.
     * @throws DOMException if element is not in xml file but is required to be.
     * @throws IllegalArgumentException if one of the arguments is 
     *         <code>null</code>.
     */
     public final String getAttributeValue(
            final String  elementName, 
            final String  attName, 
            final boolean required) {
        // check element name
        if (elementName == null) {
            throw new IllegalArgumentException(
                "Element name must not be null!");
        }
        // check attribute name
        if (attName == null) {
            throw new IllegalArgumentException(
                "Attribute name must not be null!");
        }
        // find element node for name
        Node elementNode = getNodeElement(elementName, required);
        // check node
        if (elementNode == null) {
            // if required throw DOMException
            if (required) {
                // throw exception if element can not be associated with a node
                throwDOMException(DOMException.NOT_FOUND_ERR,
                    "Node for element '" + elementName 
                    + "' not specified in xml file");
            } else {
                // if not required return null
                return null;
            }
        }
        // get attribute value for name
        return getAttributeValue(elementNode, attName);
    }   // getAttributeValue(String,String,boolean)
    
    /** 
     * Returns the attribute value for a xml element and attribute name.
     * @param  elementNode The element node to look for specified attribute.
     * @param  attName The xml attribute name to search for.
     * @return attribute value at element and for attribute specified if 
     *         available, otherwise <code>null</code>.
     * @throws IllegalArgumentException If one of the arguments is 
     *         <code>null</code>.
     * @throws DOMException If element with attribute name is not in xml file.
     */
     public final String getAttributeValue(
             final Node elementNode, final String attName) {
        // check element node
        if (elementNode == null) {
            throw new IllegalArgumentException(
                "Element node must not be null!");
        }
        // check attribute name
        if (attName == null) {
            throw new IllegalArgumentException(
                "Attribute name must not be null!");
        }
        // get current node attribute list
        NamedNodeMap atts = elementNode.getAttributes();
        // check attribute list
        if (atts == null) {
            // throw exception if element with attribute is not in xml file
            throwDOMException(DOMException.NOT_FOUND_ERR,
                "Attribute of element '" + elementNode.getNodeName() 
                + "' with attribute '" + attName 
                + "' not specified in xml file");
        }       
        // get attribute node for name
        Node attNode = atts.getNamedItem(attName);
        // check attribute node
        if (attNode == null) {
            return null;
        }
        // get attribute's node value
        String value = attNode.getNodeValue();
        // check attribute node value
        if (value == null) {
            // throw exception if element with attribute is not in xml file
            throwDOMException(DOMException.NOT_FOUND_ERR,
                "Attribute value of element '" + elementNode.getNodeName() 
                + "' with attribute '" + attName 
                + "' not specified in xml file");
        }
        // return attribute value from the xml document
        return value;
    }   // getAttributeValue(String,String)
    
    /* --- auxiliary functions --- */
    
    /* 
     * Returns <code>Node</code> for element name which must be of type 
     * <code>Node#ELEMENT_NODE</code>. This function performs a recursive search
     * through the document tree and will return if the current node matches the
     * element name, otherwise <code>null</code>.
     * @param  elementName xml element name to search for.
     * @return Node element name, otherwise <code>null</code>.
     */  
    private static Node searchNodeElement(
            final Node node, final String elementName) {
        // init return result with default; var is init in every new fct call
        Node result = null;    
        // check if current node matches element name and is of node type
        // Node.ELEMENT_NODE
        if ((node.getNodeType() == Node.ELEMENT_NODE) 
        &&  (node.getNodeName().equals(elementName))) {
            // return current node
            return node;
        }
        
        // if current node is not the right
        // get all its children
        NodeList nodeList = node.getChildNodes();
        // call this function with all child nodes
        for (int i = 0; i < nodeList.getLength(); i++) {
            // set return value to current result
            result = searchNodeElement(nodeList.item(i), elementName);
            // the first time the result is not null
            if (result != null) {
                // break the loop
                break;
            }
        }
        // and finally return the result
        return result;
    }   // searchNodeElement(Node,String)

}   // XMLProperties

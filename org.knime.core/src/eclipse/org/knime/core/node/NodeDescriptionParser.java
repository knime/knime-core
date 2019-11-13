/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * Created on 28.05.2013 by thor
 */
package org.knime.core.node;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is used to parse the XML files with the node description that accompany every node factory. It
 * automatically determines the version of the node description and create the appriopriate proxy object. This object is
 * thread safe (synchronized internally).
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.8
 */
class NodeDescriptionParser {
    private static final EntityResolver RESOLVER = new EntityResolver() {
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
            String path;
            if ("-//UNIKN//DTD KNIME Node 2.0//EN".equals(publicId)) {
                path = "Node.dtd";
            } else if ("-//UNIKN//DTD KNIME Node 1.0//EN".equals(publicId)) {
                path = "Node1xx.dtd";
            } else if ("http://knime.org/node/v1.3.xsd".equals(systemId)) {
                path = "Node_v1.3.xsd";
            } else if ("http://knime.org/node/v2.7.xsd".equals(systemId)) {
                path = "Node_v2.7.xsd";
            } else if ("http://knime.org/node/v2.8.xsd".equals(systemId)) {
                path = "Node_v2.8.xsd";
            } else if ("http://knime.org/node/v2.10.xsd".equals(systemId)) {
                path = "Node_v2.10.xsd";
            } else if ("http://knime.org/node/v2.12.xsd".equals(systemId)) {
                path = "Node_v2.12.xsd";
            } else if ("http://knime.org/node/v3.1.xsd".equals(systemId)) {
                path = "Node_v3.1.xsd";
            } else if ("http://knime.org/node/v3.6.xsd".equals(systemId)) {
                path = "Node_v3.6.xsd";
            } else if ("http://knime.org/node/v4.1.xsd".equals(systemId)) {
                path = "Node_v4.1.xsd";
            } else if ("http://www.knime.org/Node.dtd".equals(systemId)) {
                path = "Node.dtd";
            } else {
                return null;
            }

            return new InputSource(NodeDescriptionParser.class.getResourceAsStream(path));
        }
    };

    private final DocumentBuilder m_parser;

    /**
     * Creates a new node description parser.
     *
     * @throws ParserConfigurationException if no appropriate parser can be found (highly unlikely)
     */
    public NodeDescriptionParser() throws ParserConfigurationException {
        DocumentBuilderFactory fac = NodeDescription.getDocumentBuilderFactory();
        m_parser = fac.newDocumentBuilder();
        m_parser.setEntityResolver(RESOLVER);
    }

    /**
     * Parses the node description for the given node factory class. It returns a proxy object for the node description
     * depending on the version of the node description. The correct version is determined via the namespace (if a
     * schema is used in the XML file) or via the document type (if a DTD is used). The fallback is to assume node
     * descriptions compatible with 2.7. It is assumed that the XML file containing the description is in the same
     * package as the node factory and has exactly the same name but with a <tt>.xml</tt> suffix.
     *
     * @param factoryClass the class of the factory for which the node description should be read.
     * @return the node description
     * @throws SAXException if the XML file is not well-formed
     * @throws IOException if the XML file cannot be read
     * @throws XmlException if the XML file is not valid
     */
    @SuppressWarnings("resource")
    public NodeDescription parseDescription(
        @SuppressWarnings("rawtypes") final Class<? extends NodeFactory> factoryClass) throws SAXException,
        IOException, XmlException {

        String descriptionFile = factoryClass.getSimpleName() + ".xml";
        InputStream inStream = factoryClass.getResourceAsStream(descriptionFile);
        if (inStream == null) {
            // could be a node factory hierarchy, check superclasses for node descriptions
            Class<?> superClass = factoryClass.getSuperclass();
            while ((inStream == null) && (superClass != null)) {
                descriptionFile = superClass.getSimpleName() + ".xml";
                inStream = superClass.getResourceAsStream(descriptionFile);
                superClass = superClass.getSuperclass();
            }

            if (inStream == null) {
                // OK, this is it, giving up
                NodeLogger.getLogger(NodeDescriptionParser.class).coding(
                    "No node description file found for " + factoryClass.getName());
                return new NoDescriptionProxy(factoryClass);
            }
        }


        Document doc;
        synchronized (m_parser) {
            doc = m_parser.parse(inStream);
        }

        String namespaceUri = doc.getDocumentElement().getNamespaceURI();
        if (namespaceUri == null) {
            DocumentType docType = doc.getDoctype();
            String publicId = (docType != null) ? docType.getPublicId() : null;
            if ((publicId == null) || "-//UNIKN//DTD KNIME Node 2.0//EN".equals(publicId)) {
                return new NodeDescription27Proxy(doc);
            } else if ("-//UNIKN//DTD KNIME Node 1.0//EN".equals(publicId)) {
                return new NodeDescription13Proxy(doc);
            } else {
                throw new XmlException("Unsupported document type for node description of " + factoryClass.getName()
                    + ": " + publicId);
            }
        } else if (namespaceUri.equals(org.knime.node.v41.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription41Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v36.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription36Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v31.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription31Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v212.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription212Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v210.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription210Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v28.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription28Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node2012.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription27Proxy(doc);
        } else if (namespaceUri.equals(org.knime.node.v13.KnimeNodeDocument.type.getContentModel().getName()
            .getNamespaceURI())) {
            return new NodeDescription13Proxy(doc);
        } else {
            throw new XmlException("Unsupported namespace for node description of " + factoryClass.getName() + ": "
                + namespaceUri);
        }
    }
}

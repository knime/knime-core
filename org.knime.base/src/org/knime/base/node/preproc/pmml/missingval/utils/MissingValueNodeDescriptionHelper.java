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
 * History
 *   31.03.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.pmml.missingval.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactory;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactoryManager;
import org.knime.core.node.NodeDescription;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Helper class to generate the {@link NodeDescription} for the Missing Value Handler node.
 *
 * @author Marcel Hanser, Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public final class MissingValueNodeDescriptionHelper {

    private MissingValueNodeDescriptionHelper() {

    }

    /**
     * Adds an additional option tag with name "Missing Value Handler Selection" to the fullDescription of the given
     * node description.
     *
     * To retrieve the individual handler descriptions the {@link MissingCellHandlerFactoryManager#getInstance()} is
     * used.
     *
     * @param parentDescription the parent description to add the tag to
     * @return a node description with the <option=''/> added
     * @throws IOException some problem on reading the description
     * @throws SAXException not going to happen
     */
    public static NodeDescription createNodeDescription(final NodeDescription parentDescription)
        throws SAXException, IOException {
        return createNodeDescription(parentDescription, null);
    }

    /**
     * Adds an additional option tag with name "Missing Value Handler Selection" to the fullDescription of the given
     * node description.
     *
     * Please note that if a {@link MissingCellHandlerFactoryManager} is provided, the node description is newly
     * compiled with every call what might cause some overhead.
     *
     * @param parentDescription the parent description to add the tag to
     * @param manager handler factory manager, if <code>null</code>
     *            {@link MissingCellHandlerFactoryManager#getInstance()} will be used
     * @return a node description with the <option=''/> added
     * @throws IOException some problem on reading the description
     * @throws SAXException not going to happen
     */
    public static NodeDescription createNodeDescription(final NodeDescription parentDescription,
        final MissingCellHandlerFactoryManager manager) throws SAXException, IOException {

        NodeDescription createNodeDescription = parentDescription;
        Element knimeNode = createNodeDescription.getXMLDescription();

        Element fullDescription = findFullDescription(knimeNode);

        if (fullDescription != null) {
            if (manager == null) {
                MissingCellHandlerDescriptionFactory.addShortDescriptionToNodeDescription(fullDescription);
            } else {
                List<MissingCellHandlerFactory> factories = manager.getFactories();
                factories.sort((a,b) ->  a.getDisplayName().compareTo(b.getDisplayName()));
                MissingCellHandlerDescriptionFactory.addShortDescriptionToNodeDescription(fullDescription, factories);
            }
            //a deep copy is created and returned as there exist some trouble
            //with the namespaces and the following xslt transformation
            return new NodeDescriptionXmlProxy(createNodeDescription, deepCopy(knimeNode));
        }

        return createNodeDescription;
    }

    /**
     * @return a deep copy of the knimeNode with the added option tag
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    private static Element deepCopy(final Element knimeNode) throws SAXException, IOException {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformerer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformerer.transform(new DOMSource(knimeNode), new StreamResult(buffer));
            String str = buffer.toString();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(str));
            return builder.parse(is).getDocumentElement();
        } catch (TransformerException | ParserConfigurationException e) {
            throw new RuntimeException("Error on deep copy of the node description", e);
        }
    }

    /**
     * @return the fullDescrption element of the given xmlDescription
     */
    private static Element findFullDescription(final Element xmlDescription) {
        NodeList childNodes = xmlDescription.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if ("fullDescription".equals(item.getNodeName())) {
                return (Element)item;
            }

        }
        return null;
    }
}

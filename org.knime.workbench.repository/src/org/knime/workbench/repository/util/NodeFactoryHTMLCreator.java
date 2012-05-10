/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ---------------------------------------------------------------------
 *
 * History
 *   21.08.2007 (Fabian Dill): created
 */
package org.knime.workbench.repository.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.knime.core.node.NodeLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Loads an transformer and transforms the XML description of a node (which is
 * passed as a DOM element into HTML.
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class NodeFactoryHTMLCreator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeFactoryHTMLCreator.class);

    private static final String SHORT_DESCR_TAG = "shortDescription";

    private static final String XSLT_FILE = "FullNodeDescription.xslt";

    private static final String HOWTO_FILE = "node_description_howto.html";

    private Transformer m_transformer;

    /**
     * Calls {@link #init()}.
     */
    private NodeFactoryHTMLCreator() {
        init();
    }

    private static NodeFactoryHTMLCreator instance;

    /**
     *
     * @return single instance of the HTML creator
     */
    public static NodeFactoryHTMLCreator getInstance() {
        if (instance == null) {
            instance = new NodeFactoryHTMLCreator();
        }
        return instance;
    }

    /**
     *
     * @param knimeNode DOM tree root of node factory XML description.
     * @return the full description as HTML
     */
    public String readFullDescription(final Element knimeNode) {
        if (knimeNode == null) {
            return getXMLDescriptionHowTo();
        }
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(knimeNode);
        try {
            m_transformer.transform(source, result);
        } catch (TransformerException ex) {
            LOGGER.coding("Unable to process fullDescription in " + "xml: "
                    + ex.getMessage(), ex);
            return "Unable to process fullDescription in " + "xml: "
                    + ex.getMessage();
        }
        return result.getWriter().toString();
    }

    private String getXMLDescriptionHowTo() {
        try {
            InputStream is = getClass().getResourceAsStream(HOWTO_FILE);
            BufferedReader buffer =
                    new BufferedReader(new InputStreamReader(is));
            StringBuilder result = new StringBuilder();
            result.append(DynamicNodeDescriptionCreator.instance().getHeader());
            String line;
            while ((line = buffer.readLine()) != null) {
                result.append(line + "\n");
            }
            result.append("</body></html>");
            return result.toString();
        } catch (IOException io) {
            LOGGER.error(io);
            return "No description available. Please add an XML description";
        }
    }

    /**
     * Read the short description of the node from the XML file. If the tag is
     * not available, returns <code>null</code>. This method is called from the
     * constructor.
     *
     * @param knimeNode DOM tree root of node factory XML description
     * @return The short description as defined in the XML or null if that
     *         fails.
     */
    public String readShortDescriptionFromXML(final Element knimeNode) {
        if (knimeNode == null) {
            return "No description available! Please add an XML description.";
        }
        Node w3cNode = knimeNode.getElementsByTagName(SHORT_DESCR_TAG).item(0);
        if (w3cNode == null) {
            return null;
        }
        Node w3cNodeChild = w3cNode.getFirstChild();
        if (w3cNodeChild == null) {
            return null;
        }
        return w3cNodeChild.getNodeValue();
    }

    /**
     * Initializes the XSLT transformer by loading the stylesheet.
     */
    public void init() {
        try {
            InputStream is = getClass().getResourceAsStream(XSLT_FILE);
            StreamSource stylesheet = new StreamSource(is);
            m_transformer =
                    TransformerFactory.newInstance().newTemplates(stylesheet)
                            .newTransformer();
            m_transformer.setParameter("css", DynamicNodeDescriptionCreator
                    .instance().getCss());
            m_transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            m_transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}

/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.knime.core.node.NodeLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Loads an transformer and transforms the XML description of a node (which is passed as a DOM element into HTML.
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class NodeFactoryHTMLCreator {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeFactoryHTMLCreator.class);

    private static final String SHORT_DESCR_TAG = "shortDescription";

    private static final String HOWTO_FILE = "node_description_howto.html";

    private Map<String, Transformer> m_transformers = new HashMap<String, Transformer>();

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("http://knime.org/node(?:2012|/v(\\d+\\.\\d+))");

    private final String m_css;

    private NodeFactoryHTMLCreator() throws IOException {
        InputStream is = getClass().getResourceAsStream("style.css");

        BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String line;
        StringBuilder buf = new StringBuilder();
        while ((line = in.readLine()) != null) {
            buf.append(line).append('\n');
        }
        m_css = buf.toString();
        in.close();
    }


    /**
     * The singleton instance.
     */
    public static final NodeFactoryHTMLCreator instance;

    static {
        NodeFactoryHTMLCreator nfhc = null;
        try {
            nfhc = new NodeFactoryHTMLCreator();
        } catch (IOException ex) {
            LOGGER.error("Could create node factory creator: " + ex.getMessage(), ex);
        }
        instance = nfhc;
    }

    /**
     *
     * @param knimeNode DOM tree root of node factory XML description.
     * @return the full description as HTML
     *
     * @throws FileNotFoundException if the stylesheet for the node cannot found found
     * @throws TransformerException if an error happens during the XML->HTML transformation
     */
    public String readFullDescription(final Element knimeNode) throws FileNotFoundException, TransformerException {
        if (knimeNode == null) {
            return getXMLDescriptionHowTo();
        }

        String namespaceUri = knimeNode.getNamespaceURI();
        Transformer transformer = m_transformers.get(namespaceUri);
        if (transformer == null) {
            Matcher matcher = NAMESPACE_PATTERN.matcher(namespaceUri);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unsupported namespace for knime node: " + namespaceUri);
            }
            final String version;
            if (matcher.group(1) != null) {
                version = matcher.group(1);
            } else {
                version = "2.7";
            }

            InputStream is = getClass().getResourceAsStream("FullNodeDescription_v" + version + ".xslt");
            if (is == null) {
                throw new FileNotFoundException("Could not find stylesheet 'FullNodeDescription_" + version + ".xslt");
            }
            StreamSource stylesheet = new StreamSource(is);
            transformer = TransformerFactory.newInstance().newTemplates(stylesheet).newTransformer();
            transformer.setParameter("css", m_css);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            m_transformers.put(namespaceUri, transformer);
        }

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(knimeNode);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

    private String getXMLDescriptionHowTo() {
        BufferedReader buffer = null;
        try {
            InputStream is = getClass().getResourceAsStream(HOWTO_FILE);
            buffer = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder result = new StringBuilder();
            result.append(DynamicNodeDescriptionCreator.instance().getHeader());
            String line;
            while ((line = buffer.readLine()) != null) {
                result.append(line + "\n");
            }
            result.append("</body></html>");
            return result.toString();
        } catch (IOException io) {
            LOGGER.error("Could not read node description howto: " + io.getMessage(), io);
            return "No description available. Please add an XML description";
        } finally {
            if (buffer != null) {
                try {
                    buffer.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * Read the short description of the node from the XML file. If the tag is not available, returns <code>null</code>.
     * This method is called from the constructor.
     *
     * @param knimeNode DOM tree root of node factory XML description
     * @return The short description as defined in the XML or null if that fails.
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
     * Returns the CSS style used for formatting the node descriptions.
     *
     * @return a CSS style
     */
    public String getCss() {
        return m_css;
    }
}

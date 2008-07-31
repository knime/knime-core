/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Interface for factories summarizing <code>NodeModel</code>,
 * <code>NodeView</code>, and <code>NodeDialogPane</code> for a specific
 * <code>Node</code> implementation.
 *
 * @author Michael Berthold, University of Konstanz
 */
public abstract class GenericNodeFactory<T extends GenericNodeModel> {
    private static final List<String> LOADED_NODE_FACTORIES =
            new ArrayList<String>();

    private static final List<String> RO_LIST =
            Collections.unmodifiableList(LOADED_NODE_FACTORIES);

    /**
     * Enum for all node types.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public static enum NodeType {
        /** A data producing node. */
        Source,
        /** A data consuming node. */
        Sink,
        /** A learning node. */
        Learner,
        /** A predicting node. */
        Predictor,
        /** A data manipulating node. */
        Manipulator,
        /** A visualizing node. */
        Visualizer,
        /** A meta node. */
        Meta,
        /** Start node of a loop. */
        LoopStart,
        /** End node of a loop. */
        LoopEnd,
        /** All other nodes. */
        Other,
        /** If not specified. */
        Unknown
    }

    // The logger for static methods
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(GenericNodeFactory.class);

    private final String m_nodeName;

    private final String m_shortDescription;

    /* port names */
    private final List<String> m_inDataPorts = new ArrayList<String>(4);
    private final List<String> m_outDataPorts = new ArrayList<String>(4);
    private final List<String> m_modelIns = new ArrayList<String>(4);
    private final List<String> m_modelOuts = new ArrayList<String>(4);

    /* port descriptions */
    private final List<String> m_inDataPortsDesc = new ArrayList<String>(4);
    private final List<String> m_outDataPortsDesc = new ArrayList<String>(4);
    private final List<String> m_modelInsDesc = new ArrayList<String>(4);
    private final List<String> m_modelOutsDesc = new ArrayList<String>(4);

    private List<Element> m_views;

    private final URL m_icon;

    private NodeType m_type;

    private final Element m_knimeNode;

    private final String m_fullAsHTML;

    private static DocumentBuilder parser;

    private static Transformer transformer;

    private static URL defaultIcon = null;

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    static {
        try {
            String imagePath =
                GenericNodeFactory.class.getPackage().getName().replace(
                        '.', '/') + "/default.png";

            URL iconURL = GenericNodeFactory.class.getClassLoader().getResource(
                    imagePath);

            defaultIcon = iconURL;
        } catch (Exception ioe) {
            LOGGER.error("Default icon could not be read.", ioe);
        }
    }

    /**
     * Instantiates the parser and the transformer for processing the xml node
     * description. Prints log message if that fails.
     */
    private static void instantiateParser() {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();

            // sets validation with DTD file
            f.setValidating(true);

            parser = f.newDocumentBuilder();

            DefaultHandler dh = new DefaultHandler() {
                @Override
                public InputSource resolveEntity(final String pubId,
                        final String sysId) throws IOException, SAXException {
                    if (pubId != null) {
                        String path = GenericNodeFactory.class.getPackage().getName();
                        if (pubId.equals("-//UNIKN//DTD KNIME Node 1.0//EN")) {
                            path = path.replace('.', '/') + "/Node1xx.dtd";
                        } else if (pubId.equals("-//UNIKN//DTD KNIME Node 2.0//EN")) {
                            path = path.replace('.', '/') + "/Node.dtd";
                        } else {
                            return super.resolveEntity(pubId, sysId);
                        }

                        InputStream in =
                                GenericNodeFactory.class.getClassLoader()
                                        .getResourceAsStream(path);
                        return new InputSource(in);
                    } else {
                        return super.resolveEntity(pubId, sysId);
                    }
                }
            };
            parser.setEntityResolver(dh);
            // parser.setErrorHandler(dh);

            StreamSource stylesheet =
                    new StreamSource(GenericNodeFactory.class.getClassLoader()
                            .getResourceAsStream(
                                    GenericNodeFactory.class.getPackage().getName()
                                            .replace('.', '/')
                                            + "/FullNodeDescription.xslt"));

            transformer =
                    TransformerFactory.newInstance().newTemplates(stylesheet)
                            .newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (ParserConfigurationException ex) {
            NodeLogger.getLogger(GenericNodeFactory.class).error(ex.getMessage(), ex);
        } catch (TransformerConfigurationException ex) {
            NodeLogger.getLogger(GenericNodeFactory.class).error(ex.getMessage(), ex);
        } catch (TransformerFactoryConfigurationError ex) {
            NodeLogger.getLogger(GenericNodeFactory.class).error(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a new <code>GenericNodeFactory</code> and tries to read to properties
     * file named <code>Node.xml</code> in the same package as the factory.
     */
    protected GenericNodeFactory() {
        if (parser == null) {
            instantiateParser();
        }

        ClassLoader loader = getClass().getClassLoader();
        InputStream propInStream;
        String path;
        Class<?> clazz = getClass();

        do {
            path = clazz.getPackage().getName();
            path =
                    path.replace('.', '/') + "/" + clazz.getSimpleName()
                            + ".xml";

            propInStream = loader.getResourceAsStream(path);
            clazz = clazz.getSuperclass();
        } while ((propInStream == null) && (clazz != Object.class));

        // fall back node name if no xml file available or invalid.
        String defaultNodeName = getClass().getSimpleName();
        if (defaultNodeName.endsWith("NodeFactory")) {
            defaultNodeName =
                    defaultNodeName.substring(0, defaultNodeName.length()
                            - "NodeFactory".length());
        } else if (defaultNodeName.endsWith("Factory")) {
            defaultNodeName =
                    defaultNodeName.substring(0, defaultNodeName.length()
                            - "Factory".length());
        }
        if (propInStream == null) {
            m_logger.error("Could not find XML description "
                    + "file for node '" + getClass().getName() + "'");
            m_shortDescription = "No description available";
            m_knimeNode = null;
            m_icon = null;
            m_nodeName = defaultNodeName;
            m_fullAsHTML =
                    "<html><body><font color=\"red\">NO XML FILE!"
                            + "</font></body></html>";
        } else {
            Document doc = null;
            Exception exception = null;
            try {
                synchronized (parser) {
                    parser.setErrorHandler(new DefaultHandler() {
                        @Override
                        public void error(final SAXParseException ex)
                                throws SAXException {
                            m_logger.coding("XML node file does not conform "
                                    + "with DTD: " + ex.getMessage(), ex);
                        }
                    });
                    doc = parser.parse(new InputSource(propInStream));
                }
            } catch (SAXException ex) {
                exception = ex;
            } catch (IOException ex) {
                exception = ex;
            }
            if (exception != null) {
                m_logger.coding(exception.getMessage() + " (" + path + ")",
                        exception);
                m_shortDescription = "No description available";
                m_knimeNode = null;
                m_icon = null;
                m_nodeName = defaultNodeName;
                m_fullAsHTML =
                        "<html><body><font color=\"red\">"
                                + "INVALID XML FILE!</font><br/>"
                                + exception.getClass().getName() + ": "
                                + exception.getMessage() + "</body></html>";
                return;
            }
            m_knimeNode = doc.getDocumentElement();
            m_icon = readIconFromXML();

            try {
                m_type = NodeType.valueOf(m_knimeNode.getAttribute("type"));
            } catch (IllegalArgumentException ex) {
                m_logger.coding("Unknown node type '"
                        + m_knimeNode.getAttribute("type") + "'");
                m_type = NodeType.Unknown;
            }

            String nodeName = readNameFromXML();
            if (nodeName == null || nodeName.length() == 0) {
                m_logger.coding("Unable to read \"name\" tag from XML");
                m_nodeName = defaultNodeName;
            } else {
                m_nodeName = nodeName;
            }
            String shortDescription = readShortDescriptionFromXML();
            if (shortDescription == null || shortDescription.length() == 0) {
                m_logger.coding("Unable to read \"shortDescription\" "
                        + "tag from XML");
                m_shortDescription = "Unknown node";
            } else {
                m_shortDescription = shortDescription;
            }
            readPortsFromXML();
            readViewsFromXML();
            m_fullAsHTML = readFullDescription();
            // DO NOT call "checkConsistency(createNodeModel());" here as that
            // would call an abstract method from within the constructor -
            // local fields in the derived GenericNodeFactory have not been initialized
        }
        addLoadedFactory(this.getClass());
    }

    private static final Pattern ICON_PATH_PATTERN =
            Pattern.compile("[^\\./]+/\\.\\./");

    /**
     * Reads the icon tag from the xml and returns the icon. If not available or
     * the icon is not readable, an default icon is returned. This method is
     * called from the constructor.
     * <p>
     * This method does not return null as the icon is optional, i.e. it doesn't
     * hurt if it is missing.
     *
     * @return The icon as given in the xml attribute <i>icon</i>.
     */
    private URL readIconFromXML() {
        String imagePath = m_knimeNode.getAttribute("icon");
        imagePath = imagePath.replaceAll("//", "/");

        if (imagePath.startsWith("./")) {
            imagePath = imagePath.substring("./".length());
        }
        if (!imagePath.startsWith("/")) {
            imagePath =
                    getClass().getPackage().getName().replace('.', '/') + "/"
                            + imagePath;

            Matcher m = ICON_PATH_PATTERN.matcher(imagePath);
            while (m.find()) {
                imagePath = imagePath.replaceAll("[^./]+/../", "");
                m = ICON_PATH_PATTERN.matcher(imagePath);
            }
        }

        URL iconURL = getClass().getClassLoader().getResource(imagePath);

        return iconURL;
    }

    /**
     * Read the name of the node from the xml file. If the tag is not available,
     * returns <code>null</code>. This method is called from the constructor.
     *
     * @return The name as defined in the xml or null if that fails.
     */
    private String readNameFromXML() {
        Node w3cNode = m_knimeNode.getElementsByTagName("name").item(0);
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
     * Read the short description of the node from the xml file. If the tag is
     * not available, returns <code>null</code>. This method is called from
     * the constructor.
     *
     * @return The short description as defined in the xml or null if that
     *         fails.
     */
    private String readShortDescriptionFromXML() {
        Node w3cNode =
                m_knimeNode.getElementsByTagName("shortDescription").item(0);
        if (w3cNode == null) {
            return null;
        }
        Node w3cNodeChild = w3cNode.getFirstChild();
        if (w3cNodeChild == null) {
            return null;
        }
        return w3cNodeChild.getNodeValue();
    }

    private String readFullDescription() {
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(m_knimeNode);
        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            m_logger.coding("Unable to process fullDescription in " + "xml: "
                    + ex.getMessage(), ex);
        }
        return result.getWriter().toString();
    }

    /**
     * The XML description can be used with the
     * <code>GenericNodeFactoryHTMLCreator</code> in order to get
     * a converted HTML description of it, which fits the overall KNIME HTML
     * style.
     * @return XML description of this node
     */
    public Element getXMLDescription() {
        return m_knimeNode;
    }

    /**
     * Read the port descriptions of the node from the xml file. If an error
     * occurs (no such element in the xml, parsing exception ...), a coding
     * problem is reported to the node logger.
     */
    private void readPortsFromXML() {
        Node w3cNode = m_knimeNode.getElementsByTagName("ports").item(0);
        if (w3cNode == null) {
            return;
        }
        NodeList w3cNodeChildren = w3cNode.getChildNodes();
        for (int i = 0; i < w3cNodeChildren.getLength(); i++) {
            if (!(w3cNodeChildren.item(i) instanceof Element)) {
                continue;
            }
            Element port = (Element)w3cNodeChildren.item(i);
            // attempt to read index - this attribute will be used in the
            // addToPortDescription method, make it fail fast here!
            String indexString = port.getAttribute("index");
            try {
                int index = Integer.parseInt(indexString);
                if (index < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                m_logger.coding("Illegal index \"" + indexString
                        + "\" in port description");
                continue;
            }
            if (port.getNodeName().equals("dataIn")) {
                addToPort(m_inDataPorts, m_inDataPortsDesc, port);
            } else if (port.getNodeName().equals("dataOut")) {
                addToPort(m_outDataPorts, m_outDataPortsDesc, port);
            } else if (port.getNodeName().equals("predParamIn")
                    || port.getNodeName().equals("modelIn")) {
                if (port.getNodeName().equals("predParamIn")) {
                    m_logger.coding("Do not use <predParamIn> any more, use "
                            + "<modelIn> instead");
                }
                addToPort(m_modelIns, m_modelInsDesc, port);
            } else if (port.getNodeName().equals("predParamOut")
                    || port.getNodeName().equals("modelOut")) {
                if (port.getNodeName().equals("predParamOut")) {
                    m_logger.coding("Do not use <predParamOut> any more, use "
                            + "<modelOut> instead");
                }
                addToPort(m_modelOuts, m_modelOutsDesc, port);
            }
        }

        int nullIndex;
        if (m_inDataPorts != null) {
            // look for null descriptions and print error if found
            nullIndex = m_inDataPorts.indexOf(null);
            if (nullIndex >= 0) {
                m_logger.coding("No description for input port " + nullIndex
                        + ".");
            }
        }

        if (m_outDataPorts != null) {
            nullIndex = m_outDataPorts.indexOf(null);
            if (nullIndex >= 0) {
                m_logger.coding("No description for output port " + nullIndex
                        + ".");
            }
        }

        if (m_modelIns != null) {
            nullIndex = m_modelIns.indexOf(null);
            if (nullIndex >= 0) {
                m_logger.coding("No description for prediction input port "
                        + nullIndex + ".");
            }
        }

        if (m_modelOuts != null) {
            nullIndex = m_modelOuts.indexOf(null);
            if (nullIndex >= 0) {
                m_logger.coding("No description for prediction output port "
                        + nullIndex + ".");
            }
        }

    }

    /**
     * Read the view descriptions of the node from the xml file. If an error
     * occurs (no such element in the xml, parsing exception ...), a coding
     * problem is reported to the node logger.
     */
    private void readViewsFromXML() {
        Node w3cNode = m_knimeNode.getElementsByTagName("views").item(0);
        if (w3cNode == null) {
            return;
        }
        m_views = new ArrayList<Element>(4);
        NodeList allViews = ((Element)w3cNode).getElementsByTagName("view");
        for (int i = 0; i < allViews.getLength(); i++) {
            Element view = (Element)allViews.item(i);
            // attempt to read index
            String indexString = view.getAttribute("index");
            int index;
            try {
                index = Integer.parseInt(indexString);
                if (index < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                m_logger.coding("Invalid index \"" + indexString
                        + "\" for view description.");
                continue;
            }
            // make sure the description fits!
            for (int k = m_views.size(); k <= index; k++) {
                m_views.add(null);
            }
            if (m_views.get(index) != null) {
                m_logger.coding("Duplicate view description in "
                        + "XML for index " + index + ".");
            }
            m_views.set(index, view);
        }
    }

    private void addToPort(final List<String> nameList,
            final List<String> descList, final Element port) {
        int index = Integer.parseInt(port.getAttribute("index"));
        for (int k = nameList.size(); k <= index; k++) {
            nameList.add("");
        }
        if (nameList.get(index).length() > 0) {
            m_logger.coding("Duplicate port description in "
                    + "XML for index " + index + ".");
        }
        if (port.getAttribute("name").length() > 0) {
            nameList.set(index, port.getAttribute("name").trim());
        }
        for (int k = descList.size(); k <= index; k++) {
            descList.add(nameList.get(k));
        }
        Node w3cNode = port.getFirstChild();
        if (w3cNode == null) {
            return;
        }
        String value = w3cNode.getNodeValue();
        if (value == null || value.length() == 0) {
            return;
        }
        descList.set(index, value.trim().replaceAll("(?:\\s+|\n)", " "));
    }

    /**
     * Returns the name of this node.
     *
     * @return the node's name.
     */
    public final String getNodeName() {
        return m_nodeName;
    }

    /**
     * Returns a name for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port description
     */
    public String getInportName(final int index) {
        if (index >= 0 && index < m_inDataPorts.size()) {
            String name = m_inDataPorts.get(index);
            return (name == null ? "" : name);
        }
        int modelIndex = index - m_inDataPorts.size();
        if (modelIndex >= 0 && modelIndex < m_modelIns.size()) {
            String name = m_modelIns.get(modelIndex);
            return (name == null ? "" : name);
        }
        return "";
    }

    /**
     * Returns a name for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    public String getOutportName(final int index) {
        if (index >= 0 && index < m_outDataPorts.size()) {
            String name = m_outDataPorts.get(index);
            return (name == null ? "" : name);
        }
        int modelIndex = index - m_outDataPorts.size();
        if (modelIndex >= 0 && modelIndex < m_modelOuts.size()) {
            String name = m_modelOuts.get(modelIndex);
            return (name == null ? "" : name);
        }
        return "";
    }

    /**
     * Returns a description for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port description
     */
    public final String getInportDescription(final int index) {
        if (index >= 0 && index < m_inDataPortsDesc.size()) {
            String name = m_inDataPortsDesc.get(index);
            return (name == null ? "No description available" : name);
        }
        int modelIndex = index - m_inDataPortsDesc.size();
        if (modelIndex >= 0 && modelIndex < m_modelInsDesc.size()) {
            String name = m_modelInsDesc.get(modelIndex);
            return (name == null ? "No description available" : name);
        }
        return "No description available";
    }

    /**
     * Returns a description for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    public final String getOutportDescription(final int index) {
        if (index >= 0 && index < m_outDataPortsDesc.size()) {
            String name = m_outDataPortsDesc.get(index);
            return (name == null ? "No description available" : name);
        }
        int modelIndex = index - m_outDataPortsDesc.size();
        if (modelIndex >= 0 && modelIndex < m_modelOutsDesc.size()) {
            String name = m_modelOutsDesc.get(modelIndex);
            return (name == null ? "No description available" : name);
        }
        return "No description available";
    }

    /**
     * Returns a description for a view.
     *
     * @param index the index of the view, starting at 0
     * @return a view description
     */
    protected final String getViewDescription(final int index) {
        Element e;
        if ((m_views == null) || (index >= m_views.size())
                || ((e = m_views.get(index)) == null)) {
            return "No description available";
        } else {
            return e.getFirstChild().getNodeValue().trim().replaceAll(
                    "(?:\\s+|\n", " ");
        }
    }

    /**
     * Creates and returns a new instance of the node's corresponding model.
     *
     * @return A new GenericNodeModel for this node. Never <code>null</code>!
     */
    public abstract T createNodeModel();

    /**
     * Access method for <code>createNodeModel()</code>. This method will
     * also do sanity checks for the correct labeling of the port description:
     * The port count (in, out, modelIn, modelOut) is only available in the
     * GenericNodeModel. The first time, this method is called, the port count is
     * retrieved from the GenericNodeModel and the xml description is validated against
     * the info from the model. If inconsistencies are identified, log messages
     * will be written and the full description of the node is adapted such that
     * the user (preferably the implementor) immediately sees the problem.
     *
     * @return The model as from createNodeModel()
     */
    final T callCreateNodeModel() {
        T result = createNodeModel();
        checkConsistency(result);
        return result;
    }

    /**
     * Returns the number of possible views.
     *
     * @return The number of views available for this node.
     * @see #createNodeView(int,GenericNodeModel)
     */
    protected abstract int getNrNodeViews();

    /**
     * Returns the node name as view name, the index is not considered.
     *
     * @param index The view index,
     * @return A node view name.
     */
    protected final String getNodeViewName(final int index) {
        Element e;
        if ((m_views == null) || (index >= m_views.size())
                || ((e = m_views.get(index)) == null)) {
            return "NoName";
        } else {
            return e.getAttribute("name");
        }
    }

    /**
     * Creates and returns a new node view for the given index.
     *
     * @param viewIndex The index for the view to create.
     * @param nodeModel The underlying model.
     * @return A new node view for the given index.
     * @throws IndexOutOfBoundsException If the <code>viewIndex</code> is out
     *             of range.
     *
     * @see #getNrNodeViews()
     */
    public abstract GenericNodeView<T> createNodeView(final int viewIndex,
            final T nodeModel);

    /**
     * Returns <code>true</code> if the <code>Node</code> provided a dialog.
     *
     * @return <code>true</code> if a <code>NodeDialogPane</code> is
     *         available.
     */
    protected abstract boolean hasDialog();

    /**
     * Creates and returns a new node dialog pane.
     *
     * @return The new node dialog pane.
     */
    protected abstract GenericNodeDialogPane createNodeDialogPane();

    /**
     * @deprecated Use the
     *  <code>GenericNodeFactoryHTMLCreator</code>
     *  in connection with the {@link #getXMLDescription()} method.
     *
     * @return A short description (like 50 characters) of the functionality the
     *         corresponding node provides. This string should not contain any
     *         formatting or html specific parts or characters.
     */
    @Deprecated
    public final String getNodeOneLineDescription() {
        return m_shortDescription;
    }

    /**
     * Returns the icon for the node.
     *
     * @return the node's icon
     */
    public final URL getIcon() {
        return m_icon;
    }

    /**
     * Returns the formatted html source as given in the node factory's xml
     * description. The xml content is processed with a stylesheet that layouts
     * all available information.
     *
     * @deprecated Use the
     *  <code>GenericNodeFactoryHTMLCreator</code>
     *  in connection with the {@link #getXMLDescription()}.
     * @return An html string containing a full description of the node's
     *         functionality, all parameters, inport data, output of the node,
     *         and views.
     *
     */
    @Deprecated
    public final String getNodeFullHTMLDescription() {
        return m_fullAsHTML;
    }

    /**
     * Called when the GenericNodeModel is instantiated the first time. We do some
     * sanity checks here, for instance: Do the number of ports in the xml match
     * with the port count in the node model...
     *
     * @param m The GenericNodeModel to check against.
     */
    private void checkConsistency(final GenericNodeModel m) {
//        if ((m.getNrDataIns() > 0)
//                && ((m_inDataPorts == null) || (m.getNrDataIns() != m_inDataPorts
//                        .size()))) {
//            m_logger.coding("Missing or surplus input port name");
//        }
//        if ((m.getNrDataOuts() > 0)
//                && ((m_outDataPorts == null) || (m.getNrDataOuts() != m_outDataPorts
//                        .size()))) {
//            m_logger.coding("Missing or surplus output port name");
//        }
//        if ((m.getNrModelIns() > 0)
//                && ((m_modelIns == null) || (m.getNrModelIns() != m_modelIns
//                        .size()))) {
//            m_logger.coding("Missing or surplus predictor input port name");
//        }
//        if ((m.getNrModelOuts() > 0)
//                && ((m_modelOuts == null) || m.getNrModelOuts() != m_modelOuts
//                        .size())) {
//            m_logger.coding("Missing or surplus predictor output port name");
//        }
        if ((getNrNodeViews() > 0)
                && ((m_views == null) || getNrNodeViews() != m_views.size())) {
            m_logger.coding("Missing or surplus view description");
        }

        if (m_inDataPorts != null) {
            for (int i = 0; i < m_inDataPorts.size(); i++) {
                if (m_inDataPorts.get(i) == null) {
                    m_logger.coding("Missing description for input port " + i);
                }
            }
        }

        if (m_outDataPorts != null) {
            for (int i = 0; i < m_outDataPorts.size(); i++) {
                if (m_outDataPorts.get(i) == null) {
                    m_logger.coding("Missing description for output port " + i);
                }
            }
        }

        if (m_modelIns != null) {
            for (int i = 0; i < m_modelIns.size(); i++) {
                if (m_modelIns.get(i) == null) {
                    m_logger.coding("Missing description for predictor input"
                            + " port " + i);
                }
            }
        }

        if (m_modelOuts != null) {
            for (int i = 0; i < m_modelOuts.size(); i++) {
                if (m_modelOuts.get(i) == null) {
                    m_logger.coding("Missing description for predictor output"
                            + " port " + i);
                }
            }
        }

        if (m_views != null) {
            for (int i = 0; i < m_views.size(); i++) {
                if (m_views.get(i) == null) {
                    m_logger.coding("Missing description for view " + i);
                }
            }
        }
    }

    /**
     * Returns the type of the node.
     *
     * @return the node's type
     */
    public NodeType getType() {
        return m_type;
    }

    /**
     * Returns the default icon for nodes that do not define their own.
     *
     * @return an URL to the default icon
     */
    public static URL getDefaultIcon() {
        return defaultIcon;
    }

    /**
     * Returns a collection of all loaded node factories.
     *
     * @return a collection array of fully qualified node factory class names
     */
    public static List<String> getLoadedNodeFactories() {
        return RO_LIST;
    }

    /**
     * Adds the given factory class to the list of loaded factory classes.
     *
     * @param factoryClass a factory class
     */
    public static void addLoadedFactory(
            final Class<? extends GenericNodeFactory> factoryClass) {
        LOADED_NODE_FACTORIES.add(factoryClass.getName());
    }
}

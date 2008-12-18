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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Interface for <code>NodeFactory</code>s summarizing {@link NodeModel},
 * {@link NodeView}, and {@link NodeDialogPane} for a specific <code>Node</code>
 * implementation.
 *
 * @author Michael Berthold, University of Konstanz
 * @param <T> the concrete type of the {@link NodeModel}
 */
public abstract class NodeFactory<T extends NodeModel> {
    private static final List<String> LOADED_NODE_FACTORIES =
            new ArrayList<String>();

    private static final List<String> RO_LIST =
            Collections.unmodifiableList(LOADED_NODE_FACTORIES);

    /**
     * Enum for all node types.
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

    /** The logger for static methods. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(NodeFactory.class);

    private final String m_nodeName;

    private static class PortDescription {
        private final String m_description;

        private final String m_name;
        /**
         * A port description container holding port description and name.
         * @param description of the port
         * @param name and its name
         */
        PortDescription(final String description, final String name) {
            m_description = description;
            m_name = name;
        }
    }

    /* port descriptions */
    private final List<PortDescription> m_inPorts =
            new ArrayList<PortDescription>(4);

    private final List<PortDescription> m_outPorts =
            new ArrayList<PortDescription>(4);

    private List<Element> m_views;

    private final URL m_icon;

    private NodeType m_type;

    private final Element m_knimeNode;

    private static DocumentBuilder parser;

    private static URL defaultIcon = null;

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    static {
        try {
            String imagePath =
                    NodeFactory.class.getPackage().getName().replace('.', '/')
                            + "/default.png";

            URL iconURL =
                    NodeFactory.class.getClassLoader().getResource(imagePath);

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
                        String path = NodeFactory.class.getPackage().getName();
                        if (pubId.equals("-//UNIKN//DTD KNIME Node 1.0//EN")) {
                            path = path.replace('.', '/') + "/Node1xx.dtd";
                        } else if (pubId
                                .equals("-//UNIKN//DTD KNIME Node 2.0//EN")) {
                            path = path.replace('.', '/') + "/Node.dtd";
                        } else {
                            return super.resolveEntity(pubId, sysId);
                        }

                        InputStream in =
                                NodeFactory.class.getClassLoader()
                                        .getResourceAsStream(path);
                        return new InputSource(in);
                    } else {
                        return super.resolveEntity(pubId, sysId);
                    }
                }
            };
            parser.setEntityResolver(dh);
            // parser.setErrorHandler(dh);
        } catch (ParserConfigurationException ex) {
            NodeLogger.getLogger(NodeFactory.class).error(ex.getMessage(), ex);
        } catch (TransformerFactoryConfigurationError ex) {
            NodeLogger.getLogger(NodeFactory.class).error(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a new <code>NodeFactory</code> and tries to read to properties
     * file named <code>Node.xml</code> in the same package as the factory.
     */
    protected NodeFactory() {
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
        URL icon = null;
        NodeType type = null;
        Element knimeNode = null;
        String nodeName = defaultNodeName;
        if (propInStream == null) {
            m_logger.error("Could not find XML description "
                    + "file for node '" + getClass().getName() + "'");
        } else {
            Document doc = null;
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
                knimeNode = doc.getDocumentElement();
                icon = readIconFromXML(knimeNode);

                try {
                    type = NodeType.valueOf(knimeNode.getAttribute("type"));
                } catch (IllegalArgumentException ex) {
                    m_logger.coding("Unknown node type '"
                            + knimeNode.getAttribute("type") + "'");
                    type = NodeType.Unknown;
                }

                nodeName = readNameFromXML(knimeNode);
                if (nodeName == null || nodeName.length() == 0) {
                    m_logger.coding("Unable to read \"name\" tag from XML");
                    nodeName = defaultNodeName;
                }
                String shortDescription = readShortDescriptionFromXML(
                        knimeNode);
                if (shortDescription == null
                        || shortDescription.length() == 0) {
                    m_logger.coding("Unable to read \"shortDescription\" "
                            + "tag from XML");
                }
                readPortsFromXML(knimeNode);
                readViewsFromXML(knimeNode);
                // DO NOT call "checkConsistency(createNodeModel());" here as
                // that would call an abstract method from within the
                // constructor - local fields in the derived NodeFactory have
                // not been initialized
            } catch (Exception ex) {
                m_logger.coding(ex.getMessage() + " (" + path + ")", ex);
                knimeNode = null;
                icon = null;
                nodeName = defaultNodeName;
                type = NodeType.Unknown;
            }
        }
        m_knimeNode = knimeNode;
        m_icon = icon;
        m_nodeName = nodeName;
        m_type = type;
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
    private URL readIconFromXML(final Element knimeNode) {
        String imagePath = knimeNode.getAttribute("icon");
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
                imagePath = imagePath.replaceAll("[^./]+/\\.\\./", "");
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
    private String readNameFromXML(final Element knimeNode) {
        Node w3cNode = knimeNode.getElementsByTagName("name").item(0);
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
    private String readShortDescriptionFromXML(final Element knimeNode) {
        Node w3cNode =
                knimeNode.getElementsByTagName("shortDescription").item(0);
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
     * The XML description can be used with the
     * <code>NodeFactoryHTMLCreator</code> in order to get a converted HTML
     * description of it, which fits the overall KNIME HTML style.
     *
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
    private void readPortsFromXML(final Element knimeNode) {
        Node w3cNode = knimeNode.getElementsByTagName("ports").item(0);
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
            addToPort(port);
        }

        // look for null descriptions and print error if found
        int nullIdx = m_inPorts.indexOf(null);
        if (nullIdx >= 0) {
            m_logger.coding("No description for input port " + nullIdx + ".");
        }

        nullIdx = m_outPorts.indexOf(null);
        if (nullIdx >= 0) {
            m_logger.coding("No description for output port " + nullIdx + ".");
        }
    }

    /**
     * Read the view descriptions of the node from the xml file. If an error
     * occurs (no such element in the xml, parsing exception ...), a coding
     * problem is reported to the node logger.
     */
    private void readViewsFromXML(final Element knimeNode) {
        Node w3cNode = knimeNode.getElementsByTagName("views").item(0);
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

    private void addToPort(final Element port) {
        String elemName = port.getNodeName();
        if ("modelIn".equals(elemName) || "modelOut".equals(elemName)
                || "predParamsIn".equals(elemName)
                || "predParamsOut".equals(elemName)) {
            throw new IllegalArgumentException(elemName + " is not supported "
                    + " inside the node factory xml file any more. "
                    + "It has been replaced by portIn/portOut. "
                    + "Also update the publicID of the factory xml to 2.0.");
        }

        final List<PortDescription> portList;
        if ("inPort".equals(elemName) || "dataIn".equals(elemName)) {
            portList = m_inPorts;
        } else {
            portList = m_outPorts;
        }

        int index = Integer.parseInt(port.getAttribute("index"));
        for (int k = portList.size(); k <= index; k++) {
            portList.add(null);
        }
        if (portList.get(index) != null) {
            m_logger.coding("Duplicate port description in " + "XML for index "
                    + index + ".");
        }

        String portName = port.getAttribute("name");

        Node w3cNode = port.getFirstChild();
        if (w3cNode == null) {
            return;
        }
        String value = w3cNode.getNodeValue();
        if (value == null || value.length() == 0) {
            return;
        }
        String portDescription = value.trim().replaceAll("(?:\\s+|\n)", " ");

        portList.set(index, new PortDescription(portDescription, portName));
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
        if ((index >= m_inPorts.size()) || (m_inPorts.get(index) == null)) {
            // can happen if no XML file for the node exists
            return "No name available";
        } else {
            return m_inPorts.get(index).m_name;

        }
    }

    /**
     * Returns a name for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    public String getOutportName(final int index) {
        if ((index >= m_outPorts.size()) || (m_outPorts.get(index) == null)) {
            // can happen if no XML file for the node exists
            return "No name available";
        } else {
            return m_outPorts.get(index).m_name;
        }
    }

    /**
     * Returns a description for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port description
     */
    public final String getInportDescription(final int index) {
        if ((index >= m_inPorts.size()) || (m_inPorts.get(index) == null)) {
            // can happen if no XML file for the node exists
            return "No description available";
        } else {
            return m_inPorts.get(index).m_description;
        }
    }

    /**
     * Returns a description for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    public final String getOutportDescription(final int index) {
        if ((index >= m_outPorts.size()) || (m_outPorts.get(index) == null)) {
            // can happen if no XML file for the node exists
            return "No description available";
        } else {
            return m_outPorts.get(index).m_description;
        }
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
     * @return A new NodeModel for this node. Never <code>null</code>!
     */
    public abstract T createNodeModel();

    /**
     * Access method for <code>createNodeModel()</code>. This method will
     * also do sanity checks for the correct labeling of the port description:
     * The port count (in, out) is only available in the
     * NodeModel. The first time, this method is called, the port count is
     * retrieved from the NodeModel and the xml description is validated against
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
     * Returns the number of possible views or 0 if no view is available.
     *
     * @return number of views available for this node
     * @see #createNodeView(int,NodeModel)
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
     * @param nodeModel the underlying model
     * @return a new node view for the given index
     * @throws IndexOutOfBoundsException If the <code>viewIndex</code> is
     *         smaller 0 or greater or equal to the values returned by
     *         {@link #getNrNodeViews()}
     * @see #getNrNodeViews()
     */
    public abstract NodeView<T> createNodeView(final int viewIndex,
            final T nodeModel);

    /**
     * Returns <code>true</code> if this node provides a dialog to adjust
     * node specific settings.
     *
     * @return <code>true</code> if a <code>NodeDialogPane</code> is
     *         available
     * @see #createNodeDialogPane()
     */
    protected abstract boolean hasDialog();

    /**
     * Creates and returns a new node dialog pane, if {@link #hasDialog()}
     * returns <code>true</code>.
     *
     * @return node dialog pane
     * @see #hasDialog()
     */
    protected abstract NodeDialogPane createNodeDialogPane();

    /**
     * Returns the icon for the node.
     *
     * @return the node's icon
     */
    public final URL getIcon() {
        return m_icon;
    }

    /**
     * Called when the NodeModel is instantiated the first time. We do some
     * sanity checks here, for instance: Do the number of ports in the xml match
     * with the port count in the node model...
     *
     * @param m The NodeModel to check against.
     */
    private void checkConsistency(final NodeModel m) {
        if ((getNrNodeViews() > 0)
                && ((m_views == null) || getNrNodeViews() != m_views.size())) {
            m_logger.coding("Missing or surplus view description");
        }

        for (int i = 0; i < m_inPorts.size(); i++) {
            if (m_inPorts.get(i) == null) {
                m_logger.coding("Missing description for input port " + i);
            }
        }

        for (int i = 0; i < m_outPorts.size(); i++) {
            if (m_outPorts.get(i) == null) {
                m_logger.coding("Missing description for output port " + i);
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
    @SuppressWarnings("unchecked")
    public static void addLoadedFactory(
            final Class<? extends NodeFactory> factoryClass) {
        LOADED_NODE_FACTORIES.add(factoryClass.getName());
    }
}

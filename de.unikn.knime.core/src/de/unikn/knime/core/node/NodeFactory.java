/* 
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
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
import org.xml.sax.helpers.DefaultHandler;

/**
 * Interface for factories summarizing <code>NodeModel</code>,
 * <code>NodeView</code>, and <code>NodeDialogPane</code> for a specific
 * <code>Node</code> implementation.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public abstract class NodeFactory {
    private final String m_nodeName, m_shortDescription;
    private final List<String> m_inPorts = new ArrayList<String>(4);
    private final List<String> m_outPorts = new ArrayList<String>(4);
    private final List<String> m_predParamIns = new ArrayList<String>(4);
    private final List<String> m_predParamOuts = new ArrayList<String>(4);
    private final List<Element> m_views = new ArrayList<Element>(4);
    private final ImageIcon m_icon;
    private final Element m_knimeNode;
    private final String m_fullAsHTML;
    private boolean m_hasXMLBeenValidated = false;
    
    private static DocumentBuilder parser;
    private static Transformer transformer;

    
    private static final ImageIcon DEFAULT_ICON = new ImageIcon(
        NodeFactory.class.getClassLoader().getResource(
        NodeFactory.class.getPackage().getName().replace('.', '/')
        + "/default.png"));
    
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    
    /** Instantiates the parser and the transformer for processing the 
     * xmll node description. Prints log message if that fails.
     */
    private static void instantiateParser() {
        try {                    
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setValidating(true);
            parser = f.newDocumentBuilder();
             
            DefaultHandler dh = new DefaultHandler() {
                @Override
                public InputSource resolveEntity(
                        final String publicId, final String systemId) 
                    throws IOException, SAXException {
                    if ((publicId != null) && publicId.equals(
                            "-//UNIKN//DTD KNIME Node 1.0//EN")) {
                        String path = NodeFactory.class.getPackage().getName();
                        path = path.replace('.', '/') + "/Node.dtd";
                        InputStream in = NodeFactory.class.getClassLoader().
                            getResourceAsStream(path);
                        return new InputSource(in);
                    } else {
                        return super.resolveEntity(publicId, systemId);
                    }
                }
                
            };
            parser.setEntityResolver(dh);
            parser.setErrorHandler(dh);
            
            StreamSource stylesheet = new StreamSource(
                    NodeFactory.class.getClassLoader().getResourceAsStream(
                    NodeFactory.class.getPackage().getName().replace(
                    '.', '/') + "/FullNodeDescription.xslt"));
            
            transformer = TransformerFactory.newInstance().
                newTemplates(stylesheet).newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (ParserConfigurationException ex) {
            NodeLogger.getLogger(NodeFactory.class).error(ex.getMessage(), ex);
        } catch (TransformerConfigurationException ex) {
            NodeLogger.getLogger(NodeFactory.class).error(ex.getMessage(), ex);
        } catch (TransformerFactoryConfigurationError ex) {
            NodeLogger.getLogger(NodeFactory.class).error(ex.getMessage(), ex);
        }
    }
    
    /**
     * Creates a new <code>NodeFactory</code> and tries to read to properties
     * file named <code>Node.xml</code> in the same package as the factory.  
     */
    public NodeFactory() {
        if (parser == null) {
            instantiateParser();
        }
        String path = getClass().getPackage().getName();
        path = path.replace('.', '/') + "/"
            + getClass().getSimpleName() + ".xml";
        ClassLoader loader = getClass().getClassLoader();
        InputStream propInStream = loader.getResourceAsStream(path);
        // fall back node name if no xml file available or invalid.
        String defaultNodeName = getClass().getSimpleName();
        if (defaultNodeName.endsWith("NodeFactory")) {
            defaultNodeName = defaultNodeName.substring(0, 
                    defaultNodeName.length() - "NodeFactory".length());
        } else if (defaultNodeName.endsWith("Factory")) {
            defaultNodeName = defaultNodeName.substring(0, 
                    defaultNodeName.length() - "Factory".length());
        }
        if (propInStream == null) {
            m_logger.error("Could not find XML description "
                    + "file for node '" + getClass().getName() + "'");
            m_shortDescription = "No description available";
            m_knimeNode = null;
            m_icon = null;
            m_nodeName = defaultNodeName;
            m_fullAsHTML = "<html><body><font color=\"red\">NO XML FILE!" 
                + "</font></body></html>";
        } else {
            Document doc = null;
            Exception exception = null;
            try {
                doc = parser.parse(new InputSource(propInStream));
            } catch (SAXException ex) {
                exception = ex;
            } catch (IOException ex) {
                exception = ex;
            }
            if (exception != null) {
                m_logger.coding(exception.getMessage() + " (" + path 
                        + ")", exception);
                m_shortDescription = "No description available";
                m_knimeNode = null;
                m_icon = null;
                m_nodeName = defaultNodeName;
                m_fullAsHTML = "<html><body><font color=\"red\">" 
                    + "INVALID XML FILE!</font><br/>"
                    + exception.getClass().getName() + ": "
                    + exception.getMessage()
                    + "</body></html>";
                return;
            }
            m_knimeNode = doc.getDocumentElement();
            m_icon = readIconFromXML();
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
            // local fields in the derived NodeFactory have not been initialized
        }
    }
    
    /** Reads the icon tag from the xml and returns the icon. If not available
     * or the icon is not readable, an default icon is returned. This method is
     * called from the constructor.
     * <p>This method does not return null as the icon is optional, i.e. it 
     * doesn't hurt if it is missing.
     * @return The icon as given in the xml attribute <i>icon</i>.
     */
    private ImageIcon readIconFromXML() {
        String imagePath = m_knimeNode.getAttribute("icon");
        if (!imagePath.startsWith("/")) {
            imagePath = getClass().getPackage().getName().replace('.', '/') 
                + "/" + imagePath;
        }
        
        URL iconURL = getClass().getClassLoader().getResource(imagePath);
        ImageIcon result;
        if (iconURL == null) {
            result = DEFAULT_ICON;
        } else {            
            result = new ImageIcon(iconURL);
        }
        return result;
    }
    
    /** Read the name of the node from the xml file. If the tag is not 
     * available, returns <code>null</code>. This method is called from 
     * the constructor.
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
    
    /** Read the short description of the node from the xml file. If the 
     * tag is not available, returns <code>null</code>. This method is called 
     * from the constructor.
     * @return The short description as defined in the xml or null if 
     * that fails.
     */
    private String readShortDescriptionFromXML() {
        Node w3cNode = m_knimeNode.getElementsByTagName(
            "shortDescription").item(0);
        if (w3cNode == null) {
            return null;
        }
        Node w3cNodeChild = w3cNode.getFirstChild();
        if (w3cNodeChild == null) {
            return null;
        }
        return w3cNodeChild.getNodeValue();
    }
    
    /** Read the port descriptions of the node from the xml file. If an error
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
            Element port = (Element) w3cNodeChildren.item(i);
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
                addToPortDescription(m_inPorts, port);
            } else if (port.getNodeName().equals("dataOut")) {
                addToPortDescription(m_outPorts, port);
            } else if (port.getNodeName().equals("predParamIn")) {
                addToPortDescription(m_predParamIns, port);
            } else if (port.getNodeName().equals("predParamOut")) {
                addToPortDescription(m_predParamOuts, port);
            }
        }
        // look for null descriptions and print error if found
        int nullIndex = m_inPorts.indexOf(null);
        if (nullIndex >= 0) {
            m_logger.coding("No description for input port " + nullIndex + ".");
        }
        nullIndex = m_outPorts.indexOf(null);
        if (nullIndex >= 0) {
            m_logger.coding(
                    "No description for output port " + nullIndex + ".");
        }
        nullIndex = m_predParamIns.indexOf(null);
        if (nullIndex >= 0) {
            m_logger.coding("No description for prediction input port " 
                    + nullIndex + ".");
        }
        nullIndex = m_predParamOuts.indexOf(null);
        if (nullIndex >= 0) {
            m_logger.coding("No description for prediction output port " 
                    + nullIndex + ".");
        }
    }

    /** Read the view descriptions of the node from the xml file. If an error
     * occurs (no such element in the xml, parsing exception ...), a coding
     * problem is reported to the node logger.
     */
    private void readViewsFromXML() {
        Node w3cNode = m_knimeNode.getElementsByTagName("views").item(0);
        if (w3cNode == null) {
            return;
        }
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

    private void addToPortDescription(
            final List<String> destList, final Element port) {
        int index = Integer.parseInt(port.getAttribute("index"));
        for (int k = destList.size(); k <= index; k++) {
            destList.add(null);
        }
        Node w3cNode = port.getFirstChild();
        if (w3cNode == null) {
            return;
        }
        String value = w3cNode.getNodeValue();
        if (value == null || value.length() == 0) {
            return;
        }
        destList.set(index, value);
    }
    
    private String readFullDescription() {
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(m_knimeNode);
        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            m_logger.coding("Unable to process fullDescription in " 
                    + "xml: " + ex.getMessage(), ex);
        }
        return result.getWriter().toString();
    }
    
    /**
     * Returns the name of this node.
     * 
     * @return the node's name.
     */
    public final String getNodeName() { return m_nodeName; }

    
    /**
     * Returns a description for an input port.
     * 
     * @param index the index of the input port, starting at 0
     * @return an input port description
     */
    protected final String getInportDescription(final int index) {
        return m_inPorts.get(index);
    }
    

    /**
     * Returns a description for an output port.
     * 
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    protected final String getOutportDescription(final int index) {
        return m_outPorts.get(index);
    }


    
    /**
     * Returns a description for an predictor parameter input port.
     * 
     * @param index the index of the input port, starting at 0
     * @return an predictor parameter input port description
     */
    protected final String getPredParamInDescription(final int index) {
        return m_predParamIns.get(index);
    }
    

    /**
     * Returns a description for an predictor parameter output port.
     * 
     * @param index the index of the output port, starting at 0
     * @return an predictor parameter output port description
     */
    protected final String getPredParamOutDescription(final int index) {
        return m_predParamOuts.get(index);
    }

    
    /**
     * Returns a description for a view.
     * 
     * @param index the index of the view, starting at 0
     * @return a view description
     */
    protected final String getViewDescription(final int index) {
        Element e = m_views.get(index);
        if (e == null) {
            return "No description available";
        } else {
            return e.getFirstChild().getNodeValue();
        }
    }
    
    
    /**
     * Creates and returns a new instance of the node's corresponding model.
     * 
     * @return A new NodeModel for this node. Never <code>null</code>!
     */
    protected abstract NodeModel createNodeModel();
    
    /**
     * Access method for <code>createNodeModel()</code>. This method will also
     * do sanity checks for the correct labeling of the port description: The
     * port count (in, out, modelIn, modelOut) is only available in the
     * NodeModel. The first time, this method is called, the port count is
     * retrieved from the NodeModel and the xml description is validated 
     * against the info from the model. If inconsistencies are identified,
     * log messages will be written and the full description of the node is 
     * adapted such that the user (preferably the implementor) immediately sees
     * the problem.
     * @return The model as from createNodeModel()
     */
    final NodeModel callCreateNodeModel() {
        NodeModel result = createNodeModel();
        if (!m_hasXMLBeenValidated) {
            m_hasXMLBeenValidated = true;
            checkConsistency(result);
        }
        return result;
    }

    
    /**
     * Returns the number of possible views.
     * 
     * @return The number of views available for this node. 
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
        Element e = m_views.get(index);
        if (e == null) {
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
    public abstract NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel);

    
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
    protected abstract NodeDialogPane createNodeDialogPane();

    
    /**
     * @return A short description (like 50 characters) of the functionality the
     *         corresponding node provides. This string should not contain any
     *         formatting or html specific parts or characters.
     */
    public final String getNodeOneLineDescription() {
        return m_shortDescription;
    }

    
    /**
     * Returns the icon for the node.
     * @return the node's icon
     */
    protected final ImageIcon getIcon() { 
        return m_icon; 
    }
    
    
    /**
     * Returns the formatted html source as given in the node factory's 
     * xml description. The xml content is processed with a stylesheet that
     * layouts all available information.
     * 
     * @return An html string containing a full description of the node's
     *         functionality, all parameters, inport data, output of the
     *         node, and views.
     */
    public final String getNodeFullHTMLDescription() {
        return m_fullAsHTML;
    }
    
    /** Called when the NodeModel is instantiated the first time. We do some
     * sanity checks here, for instance: Do the number of ports in the xml
     * match with the port count in the node model...
     * @param m The NodeModel to check against.
     */
    private void checkConsistency(final NodeModel m) {
        if (m.getNrDataIns() != m_inPorts.size()) {
            m_logger.coding("Missing or surplus input port description");
        }
        if (m.getNrDataOuts() != m_outPorts.size()) {
            m_logger.coding("Missing or surplus output port description");
        }
        if (m.getNrModelIns() != m_predParamIns.size()) {
            m_logger.coding(
                    "Missing or surplus predictor input port description");
        }
        if (m.getNrModelOuts() != m_predParamOuts.size()) {
            m_logger.coding(
                    "Missing or surplus predictor output port description");
        }
        if (getNrNodeViews() != m_views.size()) {
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

        for (int i = 0; i < m_predParamIns.size(); i++) {
            if (m_predParamIns.get(i) == null) {
                m_logger.coding(
                        "Missing description for predictor input port " + i);
            }
        }

        for (int i = 0; i < m_predParamOuts.size(); i++) {
            if (m_predParamOuts.get(i) == null) {
               m_logger.coding(
                       "Missing description for predictor output port " + i);
            }
        }
        
        for (int i = 0; i < m_views.size(); i++) {
            if (m_views.get(i) == null) {
                m_logger.coding("Missing description for view " + i);
            }
        }
    }
}

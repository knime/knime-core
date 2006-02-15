/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
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
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Interface for factories summarizing <code>NodeModel</code>,
 * <code>NodeView</code>, and <code>NodeDialogPane</code> for a specific
 * <code>Node</code> implementation.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public abstract class NodeFactory {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeFactory.class);

    /** Key for the node's short description. */
    private static final String ONELINE_ID = "shortdescription";

    /** Key for the node's full description. */
    private static final String FULL_ID = "fulldescription";

    /** Properties object to description and additional properties from. */
    private final Properties m_props;

    /**
     * Creates a new <code>NodeFactory</code> and tries to read to properties
     * file named the same way as this NodeFactory.
     */
    protected NodeFactory() {
        // try reading it from the properties file
        m_props = new Properties();
        String path = null;
        try {
            path = this.getClass().getPackage().getName();
            path = path.replace('.', '/');
            path = path.concat("/" + getDescriptionFileName());
            ClassLoader loader = this.getClass().getClassLoader();
            InputStream propInStream = loader.getResourceAsStream(path);
            if (propInStream == null) {
                LOGGER.warn("Unable to find XML file for "
                        + this.getClass().getName());
            } else {
                m_props.loadFromXML(propInStream);
            }
        } catch (InvalidPropertiesFormatException ipfe) {
            LOGGER.warn("Unable to parse XML description for "
                    + this.getClass().getName(), ipfe);
        } catch (IOException ioe) {
            LOGGER.warn("Unable to read XML description for "
                    + this.getClass().getName(), ioe);
        }
    }

    /**
     * @return The node's name.
     */
    public abstract String getNodeName();

    /**
     * Creates and returns a new instance of the node's corresponding model.
     * 
     * @return The node's model.
     */
    public abstract NodeModel createNodeModel();

    /**
     * Returns the number of possible views.
     * 
     * @return The number of views available for this node.
     * 
     * @see #createNodeView(int,NodeModel)
     */
    public abstract int getNrNodeViews();
    
    /**
     * Returns the node name as view name, the index is not considered.
     * @param index The view index, 
     * @return A node view name.
     * TODO (tg) make abstract and support real view name
     */
    public final String getNodeViewName(final int index) {
        assert index == index;
        return getNodeName();
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
    public abstract boolean hasDialog();

    /**
     * Creates and returns a new node dialog pane.
     * 
     * @return The new node dialog pane.
     */
    public abstract NodeDialogPane createNodeDialogPane();

    /**
     * Provides the name of the properties file for the corresponding node. The
     * descriptions returned by the two methods
     * <code>#getNodeOneLineDescription()</code> and
     * <code>#getNodeFullDescription()</code> must be stored in a Java
     * Properties File. The file must at least contain the two properties
     * <code>shortdescription</code> and <code>fulldescription</code>. The
     * file must be stored in the same package as the corresponding 
     * <code>NodeFactory</code> class.
     * 
     * @see java.util.Properties
     * @see #getNodeFullHTMLDescription()
     * @see #getNodeOneLineDescription()
     * @return The file name of the properties file describing the functionality
     *         of the corresponding node.
     */
    private String getDescriptionFileName() {
        String className = this.getClass().getSimpleName();
        return className + ".xml";
    }

    /**
     * @return A short description (like 50 characters) of the functionality the
     *         corresponding node provides. This string should not contain any
     *         formatting or html specific parts or characters.
     */
    public final String getNodeOneLineDescription() {
        String oneLineDescription = m_props.getProperty(ONELINE_ID);
        // Set default values if properties (of props file) don't exist.
        if (oneLineDescription == null) {
            return " - no description available - ";
        }
        return oneLineDescription;
    }

    /**
     * Returns a string containing a full description of the node's
     * functionality and of all parameters, inport data, and the output of the
     * node. This string will be displayed in a HTML browser window.
     * <p>
     * Preferably the page returned should start with a description of what the
     * node does. Followed by a list of input and output ports explaining what
     * should be connected to which port and what could be expected at which
     * outport. Then a section should be added mentioning all settings and
     * parameters available (usually in the node's dialog), and listing the
     * possible values of these settings (or available options, or ranges). Last
     * a description of the node's view(s) should be added briefly telling what
     * is being displayed in the view window.
     * 
     * @return A string containing a full description of the node's
     *         functionality, all parameters, inport data, and the output of the
     *         node. This string will be placed between html tags and body tags.
     *         Html formatting tags can be used, but only as permitted inside an
     *         html body.
     */
    public final String getNodeFullHTMLDescription() {
        String fullDescription = m_props.getProperty(FULL_ID);
        // Set default values if properties (of props file) don't exist.
        if (fullDescription == null) {
            return "<html><body>" + "<font face=\"Arial\" size=-1><b>"
                    + getNodeName() + "<br><br></b></font>"
                    + "<font face=\"Arial\" size=-1>"
                    + getNodeOneLineDescription() + "</font></body></html>";
        } else {
            return "<html><body>" + "<font face=\"Arial\" size=-1><b>"
                    + getNodeName() + "<br><br></b></font>"
                    + "<font face=\"Arial\" size=-1>" + fullDescription
                    + "<ul>" + getOptions() + "</ul>" + "</font></body></html>";
        }
    }

    /** 
     * Returns all other parameters/options defined in the XML properties file. 
     */
    private final String getOptions() {
        TreeSet<String> set = new TreeSet<String>();
        for (Object key : m_props.keySet()) {
            if (!key.equals(FULL_ID) && !key.equals(ONELINE_ID)) {
                set.add((String)key);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (String key : set) {
            sb.append("<li><b>" + key + "</b>");
            sb.append(m_props.get(key) + "</li><br>");
        }
        return sb.toString();
    }

} // NodeFactory

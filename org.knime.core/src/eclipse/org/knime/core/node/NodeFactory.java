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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.extension.NodeFactoryExtensionManager;
import org.knime.core.node.missing.MissingNodeFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Abstract factory class for all components that make up a node, i.e. {@link NodeModel}, {@link NodeDialogPane}, and
 * {@link NodeView}. It also provides access to the node description. The ddefault behaviour is to assume that the XML
 * file containing the description is in the same package as the node factory and has exactly the same name but with a
 * <tt>.xml</tt> suffix.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @param <T> the concrete type of the {@link NodeModel}
 */
public abstract class NodeFactory<T extends NodeModel> {

    /**
     * Enum for all node types.
     *
     * N.B New additions to this enum should also be added to org.knime.workbench.editor2.figures.DisplayableNodeType
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
        /** A metanode. */
        Meta,
        /** Start node of a loop. */
        LoopStart,
        /** End node of a loop. */
        LoopEnd,
        /** Start node of a scope.
         * @since 2.8*/
        ScopeStart,
        /** End node of a scope.
         * @since 2.8*/
        ScopeEnd,
        /** A node contributing to quick/web form.
         * @deprecated use {@link #Configuration} instead */
        @Deprecated
        QuickForm,
        /** A node contributing a configuration input to a component dialog.
         * @since 4.2*/
        Configuration,
        /** All other nodes. */
        Other,
        /** A missing node (framework use only).
         * @since 2.7 */
        Missing,
        /** If not specified. */
        Unknown,
        /** @since 2.10 */
        Subnode,
        /** @since 2.10 */
        VirtualIn,
        /** @since 2.10 */
        VirtualOut
    }

    private static final Pattern ICON_PATH_PATTERN = Pattern.compile("[^\\./]+/\\.\\./");

    private static final NodeDescriptionParser PARSER;

    static {
        NodeDescriptionParser p = null;
        try {
            p = new NodeDescriptionParser();
        } catch (ParserConfigurationException ex) {
            NodeLogger.getLogger(NodeFactory.class).error(
                "Could not create node description parser:" + ex.getMessage(), ex);
        }
        PARSER = p;
    }

    private NodeDescription m_nodeDescription;

    private URL m_icon;

    private static final URL defaultIcon = NodeFactory.class.getResource("default.png");

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private boolean m_initialized = false;


    /**
     * Creates a new <code>NodeFactory</code> and initializes the node description.
     */
    protected NodeFactory() {
       this(false);
    }

    /**
     * Creates a new <code>NodeFactory</code> optionally without initializing the node description.
     *
     * @param lazyInitialization if set to <code>true</code> the full initialization is postponed until the
     *            {@link #init()} method is called.
     * @since 2.6
     */
    protected NodeFactory(final boolean lazyInitialization) {
        if (!lazyInitialization) {
            init();
        }
    }

    /**
     * Creates the description for this node. The default implementation reads the factory's XML file. Subclasses may
     * override this method in order to create the description by other means.
     *
     * @return the node description
     * @throws SAXException if the XML file is not well-formed
     * @throws IOException if the XML file cannot be read
     * @throws XmlException if the XML file is not valid
     * @since 2.8
     */
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return PARSER.parseDescription(this.getClass());
    }

    /**
     * Initializes the node factory by reading and processing the node description. This method should only be called if
     * the NodeFactory was created with the constructor {@link #NodeFactory(boolean)}.
     *
     * @since 2.6
     */
    public synchronized void init() {
        if (m_initialized) {
            m_logger.debug("Factory is already initialized. Nothing to do.");
            return;
        }
        try {
            m_nodeDescription = createNodeDescription();
        } catch (SAXException ex) {
            m_logger.error("Broken XML file for node description of " + getClass().getName() + ": " + ex.getMessage(),
                ex);
            m_nodeDescription = new NoDescriptionProxy(getClass());
        } catch (IOException ex) {
            m_logger.error(
                "I/O error while reading node description of " + getClass().getName() + ": " + ex.getMessage(), ex);
            m_nodeDescription = new NoDescriptionProxy(getClass());
        } catch (XmlException ex) {
            m_logger.error("Node description of " + getClass().getName() + " does not conform to used XML schema: "
                + ex.getMessage(), ex);
            m_nodeDescription = new NoDescriptionProxy(getClass());
        }

        m_icon = resolveIcon(m_nodeDescription.getIconPath());

        // DO NOT call "checkConsistency(createNodeModel());" here as
        // that would call an abstract method from within the
        // constructor - local fields in the derived NodeFactory have
        // not been initialized

        addBundleInformation();
        addLoadedFactory(getClass());
        m_initialized = true;
    }

    /**
     * Creates an input stream containing the properties for the node. This
     * can be overridden by subclasses to provide this information dynamically.
     *
     * @return the input stream to read the properties from
     * @since 2.6
     * @deprecated this method is not used any more, use {@link #createNodeDescription()} instead
     */
    @Deprecated
    protected InputStream getPropertiesInputStream() {
        ClassLoader loader = getClass().getClassLoader();
        InputStream propInStream;
        String path;
        Class<?> clazz = getClass();

        do {
            path = clazz.getPackage().getName();
            path = path.replace('.', '/') + "/" + clazz.getSimpleName() + ".xml";

            propInStream = loader.getResourceAsStream(path);
            clazz = clazz.getSuperclass();
        } while ((propInStream == null) && (clazz != Object.class));
        m_logger.debug("Parsing \"" + path + "\" for node properties.");
        return propInStream;
    }

    /**
     * Returns the original node description.
     *
     * @return the original node description
     */
    NodeDescription getNodeDescription() {
        return m_nodeDescription;
    }

    /**
     * Adds information about the bundle/feature in which this node resides to the XML description tree. Note that the
     * bundle information does not have a namespace!
     */
    private void addBundleInformation() {
        Element root = m_nodeDescription.getXMLDescription();

        if ((root != null) && !(this instanceof MissingNodeFactory)) { // for running in non-osgi context
            NodeAndBundleInformationPersistor nodeInfo = NodeAndBundleInformationPersistor.create(this);

            Document doc = root.getOwnerDocument();
            Element bundleElement = doc.createElement("osgi-info");
            bundleElement.setAttribute("bundle-symbolic-name",
                nodeInfo.getFeatureSymbolicName().orElse(nodeInfo.getBundleSymbolicName().orElse("<Unknown>")));
            bundleElement.setAttribute("bundle-name",
                nodeInfo.getFeatureName().orElse(nodeInfo.getBundleName().orElse("<Unknown>")));
            bundleElement.setAttribute("bundle-vendor",
                nodeInfo.getFeatureVendor().orElse(nodeInfo.getBundleVendor().orElse("<Unknown>")));
            bundleElement.setAttribute("factory-package", this.getClass().getPackage().getName());
            root.appendChild(bundleElement);
        }
    }



    /**
     * Resolves the icon using the classloader. If not available or
     * the icon is not readable, a default icon is returned.
     * <p>
     * This method does not return <code>null</code> as the icon is optional, i.e. it doesn't
     * hurt if it is missing.
     *
     * @return the icon as given in the xml attribute <i>icon</i>
     */
    private URL resolveIcon(final String path) {
        if ((path == null) || path.trim().isEmpty()) {
            return getDefaultIcon();
        }

        String imagePath = path;
        imagePath = imagePath.replaceAll("//", "/");
        if (imagePath.startsWith("./")) {
            imagePath = imagePath.substring("./".length());
        }
        Path p = Paths.get(imagePath);
        if (!p.isAbsolute() && !imagePath.startsWith("/")) {
            imagePath = getClass().getPackage().getName().replace('.', '/') + "/" + imagePath;

            Matcher m = ICON_PATH_PATTERN.matcher(imagePath);
            while (m.find()) {
                imagePath = imagePath.replaceAll("[^./]+/\\.\\./", "");
                m = ICON_PATH_PATTERN.matcher(imagePath);
            }
        } else {
            imagePath = p.toString();
        }

        URL iconURL = getClass().getClassLoader().getResource(imagePath);
        if (iconURL == null) {
            File iconFile = new File(imagePath);
            if (iconFile.exists() && iconFile.isFile()) {
                try {
                    iconURL = iconFile.toURI().toURL();
                } catch (MalformedURLException e) { /*do nothing */ }
            }
        }

        return iconURL;
    }


    /**
     * The XML description can be used with the
     * <code>NodeFactoryHTMLCreator</code> in order to get a converted HTML
     * description of it, which fits the overall KNIME HTML style.
     *
     * @return XML description of this node
     */
    public Element getXMLDescription() {
        return m_nodeDescription.getXMLDescription();
    }


    /**
     * Loads additional settings to this instance that were saved using
     * the {@link #saveAdditionalFactorySettings(ConfigWO)}.
     *
     * <p>See {@link #saveAdditionalFactorySettings(ConfigWO)} for details on
     * when to overwrite this method.
     *
     * <p>This method is called immediately after instantiation is not intended
     * to be called by client code.
     *
     * @param config The config to read from.
     * @throws InvalidSettingsException If the settings are invalid (which
     * will result in an error during workflow load -- the node will not load!)
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     */
    public void loadAdditionalFactorySettings(final ConfigRO config)
            throws InvalidSettingsException {
        // overwritten in subclasses
        init();
    }

    /** Saves additional settings of this instance. This method is called by
     * the framework upon workflow save and may be overwritten by subclasses.
     *
     * <p>This method is mainly used in a dynamic context, where node factories
     * are defined through a different extension that generates a set of
     * node factories and not just a single one.  Most derived node factories
     * will therefore not overwrite this method (e.g. none of the
     * wizard-generated factories).
     *
     * @param config To read from.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     */
    public void saveAdditionalFactorySettings(final ConfigWO config) {
        // overwritten in subclass
    }

    /**
     * Returns the name of this node.
     *
     * @return the node's name
     */
    public final String getNodeName() {
        return m_nodeDescription.getNodeName();
    }

    /**
     * Returns a name for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port name
     */
    public String getInportName(final int index) {
        String name = m_nodeDescription.getInportName(index);
        return (name == null) ? "No name available" : name;
    }

    /**
     * Returns a name for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port name
     */
    public String getOutportName(final int index) {
        String name = m_nodeDescription.getOutportName(index);
        return (name == null) ? "No name available" : name;
    }

    /**
     * Returns a description for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port description
     */
    public final String getInportDescription(final int index) {
        String description = m_nodeDescription.getInportDescription(index);
        return (description == null) ? "No description available" : description;
    }

    /**
     * Returns a description for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description
     */
    public final String getOutportDescription(final int index) {
        String description = m_nodeDescription.getOutportDescription(index);
        return (description == null) ? "No description available" : description;
    }

    /**
     * Returns a description for a view.
     *
     * @param index the index of the view, starting at 0
     * @return a view description
     */
    protected final String getViewDescription(final int index) {
        String description = m_nodeDescription.getViewDescription(index);
        return (description == null) ? "No description available" : description.replaceAll("(?:\\s+|\n)", "");
    }

    /**
     * Creates and returns a new instance of the node's corresponding model.
     *
     * @return A new {@link NodeModel} for this node. Never <code>null</code>!
     */
    public abstract T createNodeModel();

    /**
     * Creates a new node model using a specific context. This method must be overriden by nodes that make use of
     * contexts.
     *
     * @param context the node context
     * @return a new {@link NodeModel}
     * @deprecated use {@link #callCreateNodeModel(NodeCreationConfiguration)} instead
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    protected T createNodeModel(final NodeCreationContext context) {
        // normally correct implementations overwrite this
        m_logger.coding("If you register a node to be created in a certain"
            + " context, you should extend ContextAwareNodeFactory");
        return createNodeModel();
    }

    /**
     * Creates a new node model using a specific creation configuration. This method must be overriden by nodes that
     * make use of creation configurations.
     *
     * @param creationConfig the node creation configuration
     * @return a new {@link NodeModel}
     * @since 4.1
     */
    protected T createNodeModel(final NodeCreationConfiguration creationConfig) {
        // normally correct implementations overwrite this
        m_logger.coding("If you register a node to be created in a certain"
            + " context, you should extend ConfigurableNodeFactory");
        return createNodeModel();
    }

    /**
     * Access method for {@link #createNodeModel()}. If assertions are enabled, this method will also do sanity checks
     * for the correct labeling of the port description: The port count (in, out) is only available in the NodeModel.
     * The first time, this method is called, the port count is retrieved from the NodeModel and the xml description is
     * validated against the info from the model. If inconsistencies are identified, log messages will be written and
     * the full description of the node is adapted such that the user (preferably the implementor) immediately sees the
     * problem.
     *
     * @param creationConfig the creation configuration
     * @param adaptedDescription the node description
     * @return the model as from createNodeModel()
     */
    final T callCreateNodeModel(final NodeCreationConfiguration creationConfig,
        final NodeDescription adaptedDescription) {
        T result;
        final NodeDescription description;
        if (creationConfig == null) {
            result = createNodeModel();
            description = m_nodeDescription;
        } else {
            assert adaptedDescription != null;
            result = createNodeModel(creationConfig);
            description = adaptedDescription;
        }
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            checkConsistency(result, description);
        }
        return result;
    }

    /**
     * Returns the number of views or 0 if no view is available.
     *
     * @return number of views available for this node
     * @see #createNodeView(int,NodeModel)
     */
    protected abstract int getNrNodeViews();

    /**
     * Returns the name for this node's view at the given index.
     *
     * @param index the view index, starting at 0
     * @return the view's name
     */
    protected final String getNodeViewName(final int index) {
        String name = m_nodeDescription.getViewName(index);
        return (name == null) ? "NoName" : name;
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
    public abstract NodeView<T> createNodeView(final int viewIndex, final T nodeModel);

    /** Generalization of {@link #createNodeView(int, NodeModel)} to allow for
     * creation of a more flexible {@link AbstractNodeView}. Implementations
     * will typically overwrite the {@link #createNodeView(int, NodeModel)}
     * method unless they wish to return, e.g. an
     * {@link ExternalApplicationNodeView}.
     *
     * <p><strong>Note:</strong>This method is going to be removed in KNIME
     * v3.0, whereby the return type of the
     * {@link #createNodeView(int, NodeModel)} will be changed
     * to {@link AbstractNodeView}. (This change is postponed to v3.0 in order
     * to ensure binary compatibility of 2.0.x plugins with the 2.x series).
     * @param viewIndex The index for the view to create
     * @param nodeModel the underlying model
     * @return a new node view for the given index
     * @throws IndexOutOfBoundsException If the <code>viewIndex</code> is
     *         smaller 0 or greater or equal to the values returned by
     *         {@link #getNrNodeViews()}
     * @since 2.1
     */
    public AbstractNodeView<T> createAbstractNodeView(final int viewIndex, final T nodeModel) {
        return createNodeView(viewIndex, nodeModel);
    }

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
     * Creates and returns a new node dialog pane, if {@link #hasDialog()} returns <code>true</code>.
     *
     * @param creationConfig the node creation configuration
     * @return a new {@link NodeModel}
     * @since 4.1
     */
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        // normally correct implementations overwrite this
        m_logger.coding("If you register a node to be created in a certain"
            + " context, you should extend ConfigurableNodeFactory");
        return createNodeDialogPane();
    }

    /**
     * Returns the icon for the node.
     *
     * @return the node's icon
     */
    public URL getIcon() {
        return m_icon;
    }

    /**
     * Called when the NodeModel is instantiated the first time. We do some sanity checks here, for instance: Do the
     * number of ports in the xml match with the port count in the node model...
     *
     * @param m the NodeModel to check against
     * @param nodeDescription the node description
     */
    private void checkConsistency(final NodeModel m, final NodeDescription nodeDescription) {
        if (nodeDescription instanceof NoDescriptionProxy) {
            // no description available at all; this has already been reported
            return;
        }

        if (getNrNodeViews() != nodeDescription.getViewCount()) {
            m_logger.coding("Missing or surplus view description");
        }

        for (int i = 0; i < m.getNrInPorts(); i++) {
            if (nodeDescription.getInportName(i) == null) {
                m_logger.coding("Missing description for input port " + i);
            }
        }

        for (int i = 0; i < m.getNrOutPorts(); i++) {
            if (nodeDescription.getOutportName(i) == null) {
                m_logger.coding("Missing description for output port " + i);
            }
        }

        for (int i = 0; i < m_nodeDescription.getViewCount(); i++) {
            if (nodeDescription.getViewDescription(i) == null) {
                m_logger.coding("Missing description for view " + i);
            }
        }
    }

    /**
     * Returns the type of the node.
     *
     * @return the node's type
     */
    public NodeType getType() {
        return m_nodeDescription.getType();
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
     * Used to return the list of all node factories, deprecated since 4.2 and returning an empty list since then.
     *
     * @return an empty list
     * @deprecated Removed in 4.2 without replacement. Nodes are now collected in the framework only and not known
     * to any 3rd party extension.
     */
    @Deprecated
    public static List<String> getLoadedNodeFactories() {
        return Collections.emptyList();
    }

    /**
     * Adds the given factory class to the list of loaded factory classes.
     *
     * @param factoryClass a factory class
     * @deprecated 3rd party contributions should register their nodes through KNIME's node extension point, possibly
     *             setting the "hidden" flag.
     */
    @Deprecated
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLoadedFactory(final Class<? extends NodeFactory> factoryClass) {
        NodeFactoryExtensionManager.getInstance()
            .addLoadedFactory((Class<? extends NodeFactory<NodeModel>>)factoryClass);
    }

    /////////////////////////////////////////////////////////////
    // Default Factory methods for InteractiveNodeView providers
    // (implemented here because it needs access to the XML tree)
    /////////////////////////////////////////////////////////////

    /**
     * Returns the name of the interactive view if such a view exists. Otherwise <code>null</code> is returned.
     *
     * @return name of the interactive view or <code>null</code>
     * @since 2.8
     */
    public String getInteractiveViewName() {
        return m_nodeDescription.getInteractiveViewName();
    }

    /**
     * Sets if this node is deprecated. This method should not be called by clients, it's only called during
     * construction of the node repository.
     *
     * @param b <code>true</code> if the node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    void setIsDeprecated(final boolean b) {
        m_nodeDescription.setIsDeprecated(b);
    }

    /**
     * Returns whether this node (factory) is deprecated. Ordinary nodes will declare their deprecation through an
     * attribute in the extension point registration. Dynamic nodes (so those registered via the extension point
     * defining {@link org.knime.core.node.NodeSetFactory}) can overwrite a protected scope
     * method on {@link DynamicNodeFactory}.
     *
     * @return <code>true</code> if the node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    public final boolean isDeprecated() {
        return isDeprecatedInternal();
    }

    /**
     * Concrete implementations of {@link DynamicNodeFactory} can overwrite this method,
     * if they know about the deprecation status of the node they represent.<br>
     * Default implementation returns deprecated status of node description.
     * @return <code>true</code> if the node is deprecated, <code>false</code> otherwise
     * @since 3.4
     */
    boolean isDeprecatedInternal() {
        return m_nodeDescription.isDeprecated();
    }
}

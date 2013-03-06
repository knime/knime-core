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
 *   17.12.2007 (Fabian Dill): created
 */
package org.knime.workbench.helpview.wizard;

import static org.knime.workbench.repository.util.DynamicNodeDescriptionCreator.htmlString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.helpview.HelpviewPlugin;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class creates the Eclipse help files for one or more plugins.
 *
 * @author Fabian Dill, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public final class NodeDescriptionConverter {
    private final static NodeLogger LOGGER = NodeLogger
            .getLogger(NodeDescriptionConverter.class);

    private static final String NODES = "$nodes";

    private static final String HTML_DIR = "html";

    private static final String NODES_DIR = "nodes";

    private static final String TOC_DIR = "tocs";

    private static final String HELP_JAR = "org.eclipse.help";

    private static final String HELP_UI = "org.eclipse.help.ui";

    private static final String HELP_TOC = "org.eclipse.help.toc";

    private static final String REQUIRE = "Require-Bundle";

    private static final String EXT = "extension";

    private static final String POINT = "point";

    private static final String ROOT_ANCHOR = "node-descriptions";

    private File m_destinationDir;

    private static final NodeDescriptionConverter instance =
            new NodeDescriptionConverter();

    private String m_pluginID;

    private Document m_pluginXML;

    private IProgressMonitor m_monitor;

    private ProgressMonitorDialog m_dialog;

    private int m_nrPlugins;

    private int m_currentPlugin;

    private boolean m_first;

    private boolean m_canceled = false;

    private NodeDescriptionConverter() {
    }

    /**
     *
     * @return all extensions to the knmie node and knime category extension
     *         points
     */
    static List<IConfigurationElement> getConfigurationElements() {
        List<IConfigurationElement> configElements = new ArrayList<IConfigurationElement>();
        configElements.addAll(Arrays.asList(Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.knime.workbench.repository.nodes")));
        configElements.addAll(Arrays.asList(Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.knime.workbench.repository.categories")));
        configElements.addAll(Arrays.asList(Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.knime.workbench.repository.metanode")));
        configElements.addAll(Arrays.asList(Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.knime.workbench.repository.nodesets")));

        return configElements;
    }

    /**
     * Builds the documentation for all loaded plugins whose id matches the
     * given pattern.
     *
     * @param pattern a pattern
     * @throws Exception if an error occurs
     */
    public void buildDocumentationFor(final Pattern pattern) throws Exception {
        buildDocumentationFor(pattern, null);
    }

    /**
     * Builds the documentation for all loaded plugins whose id matches the
     * given pattern.
     *
     * @param pattern a pattern
     * @param destinationDir directory, where the created/modified files should
     *            be written to. If <code>null</code> they are written directly
     *            into the plugin's directory.
     * @throws Exception if an error occurs
     */
    public void buildDocumentationFor(final Pattern pattern,
            File destinationDir) throws Exception {
        List<IConfigurationElement> configs = getConfigurationElements();
        Set<String> processed = new HashSet<String>();
        if (destinationDir == null) {
            destinationDir = getPluginDir();
        }

        LOGGER.info("Building documentation for " + pattern.toString()
                + " into " + destinationDir.getAbsolutePath());

        for (IConfigurationElement e : configs) {
            String pluginId = e.getNamespaceIdentifier();
            if (pattern.matcher(pluginId).matches()
                    && !processed.contains(pluginId)) {
                System.out.print("Building documentation for " + pluginId
                        + "...");
                LOGGER.info("Building documentation for " + pluginId + "...");
                buildDocumentationFor(pluginId, destinationDir);
                processed.add(pluginId);
                System.out.println("done");
                LOGGER.info("done");
            }
        }
    }

    /**
     *
     * @param plugins the plugin names
     */
    public void buildDocumentationWithProgress(final String[] plugins) {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        m_dialog = new ProgressMonitorDialog(shell);
        m_dialog.setCancelable(true);
        m_monitor = m_dialog.getProgressMonitor();
        m_monitor.setTaskName("Retrieving information from repository...");
        m_nrPlugins = plugins.length;
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                try {
                    m_first = true;
                    monitor.beginTask("Building documentation", m_nrPlugins);
                    for (String s : plugins) {
                        if (m_monitor.isCanceled()) {
                            m_canceled = true;
                            return;
                        }
                        buildDocumentationFor(s);
                        m_currentPlugin++;
                        monitor.worked(m_currentPlugin);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            m_dialog.run(true, true, op);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            m_monitor.done();
        }
    }

    /**
     *
     * @return singleton instance of this class
     */
    public static final NodeDescriptionConverter instance() {
        return instance;
    }

    /**
     * Gets the {@link RepositoryManager} and transforms all NodeDescriptions
     * into HTML files, which are placed into the roots HTML directory.
     *
     * @param pluginId the id of the plugin for which the help files should be
     *            generated
     *
     * @throws Exception if something goes wrong
     */
    public void buildDocumentationFor(final String pluginId) throws Exception {
        buildDocumentationFor(pluginId, null);
    }

    /**
     * Gets the {@link RepositoryManager} and transforms all NodeDescriptions
     * into HTML files, which are placed into the roots HTML directory.
     *
     * @param pluginId the id of the plugin for which the help files should be
     *            generated
     * @param destinationDir directory, where the created/modified files should
     *            be written to. If <code>null</code> they are written directly
     *            into the plugin's directory.
     * @throws Exception if something goes wrong
     */
    public synchronized void buildDocumentationFor(final String pluginId,
            final File destinationDir) throws Exception {
        final Root root = RepositoryManager.INSTANCE.getRoot();

        if (m_monitor != null) {
            Display d = Display.getDefault();
            d.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (m_monitor.isCanceled()) {
                        m_canceled = true;
                        return;
                    }
                    if (m_first) {
                        m_monitor.beginTask("Processing " + m_pluginID,
                                m_nrPlugins * root.getChildren().length);
                        m_first = false;
                    } else {
                        m_monitor.subTask("Processing " + m_pluginID);
                    }
                }
            });
        }
        if (m_canceled) {
            return;
        }
        m_pluginID = pluginId;
        if (destinationDir == null) {
            m_destinationDir = getPluginDir();
        } else {
            m_destinationDir = destinationDir;
        }

        File nodesDir =
                new File(m_destinationDir, HTML_DIR + File.separator
                        + NODES_DIR);
        if (!nodesDir.exists()) {
            nodesDir.mkdirs();
        }
        File tocsDir = new File(m_destinationDir, TOC_DIR);
        if (!tocsDir.exists()) {
            tocsDir.mkdirs();
        }

        parsePluginXML();

        if (m_dialog != null) {
            m_dialog.setCancelable(false);
        }
        // processing
        processAll(root.getChildren(), null);

        // at the end -> persist plugin.xml
        Document doc = m_pluginXML;
        Source src = new DOMSource(doc);
        File f = new File(m_destinationDir, "plugin.xml");
        Result streamResult = new StreamResult(f);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(src, streamResult);

        // at the end add the two dependencies to the manifest file
        // org.eclipse.help, org.eclipse.help.ui
        updateManifest();

    }

    private void updateManifest() throws Exception {
        File inFile =
                new File(getPluginDir(), "META-INF" + File.separator
                        + "MANIFEST.MF");
        Manifest manifest = new Manifest(new FileInputStream(inFile));
        Attributes attrs = manifest.getMainAttributes();
        for (Map.Entry<Object, Object> o : manifest.getMainAttributes()
                .entrySet()) {
            if (o.getKey().equals(new Attributes.Name(REQUIRE))) {
                if (!((String)o.getValue()).contains(HELP_JAR)) {
                    attrs.putValue(o.getKey().toString(), o.getValue() + ","
                            + HELP_JAR + ";resolution:=optional");
                }
                if (!((String)o.getValue()).contains(HELP_UI)) {
                    attrs.putValue(o.getKey().toString(), o.getValue() + ","
                            + HELP_UI + ";resolution:=optional");
                }
                break;
            }
        }

        File outFile =
                new File(m_destinationDir, "META-INF" + File.separator
                        + "MANIFEST.MF");
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        FileOutputStream outStream = new FileOutputStream(outFile);
        manifest.write(outStream);
        outStream.close();
    }

    private void parsePluginXML() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        File location = new File(getPluginDir(), "plugin.xml");
        m_pluginXML = builder.parse(location);
    }

    private Element getTocExtension() {
        NodeList extensions = m_pluginXML.getElementsByTagName(EXT);
        for (int i = 0; i < extensions.getLength(); i++) {
            Element extension = (Element)extensions.item(i);
            if (extension.hasAttribute(POINT)) {
                if (extension.getAttribute(POINT).equals(HELP_TOC)) {
                    return extension;
                }
            }
        }
        return null;
    }

    private boolean processAll(final IRepositoryObject[] elements,
            final Document nodeToc) throws Exception {
        boolean nodeCreated = false;
        for (IRepositoryObject o : elements) {
            if (o instanceof Category) {
                Category c = (Category)o;
                if (c.getParent() instanceof Root && m_monitor != null) {
                    Display d = Display.getDefault();
                    d.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            m_monitor.worked(2);
                        }
                    });
                }
                // create toc file
                Document topicFile = createNodeTocFile(c);
                boolean hasNodes =
                        processAll(((Category)o).getChildren(), topicFile);
                if (c.getPluginID().equals(m_pluginID)) {
                    writeCategoryTocFile(c);
                }
                if (hasNodes) {
                    // write toc file
                    writeNodeTocFile(topicFile,
                            fileName(getFullPath(c) + NODES));
                    writeTocToPluginXML(c, true);
                }
                // TODO: why is nodeToc null?
            } else if (o instanceof NodeTemplate && nodeToc != null) {
                nodeCreated |= processNode((NodeTemplate)o, nodeToc);
            } else if (o instanceof MetaNodeTemplate && nodeToc != null) {
                nodeCreated |= processMetaNode((MetaNodeTemplate)o, nodeToc);
            }
        }
        return nodeCreated;
    }

    private boolean processMetaNode(final MetaNodeTemplate metaNode,
            final Document nodeToc) throws IOException {
        assert nodeToc != null;
        if (metaNode.getPluginID().equals(m_pluginID)) {
            // create HTML file
            StringBuilder builder = new StringBuilder();
            DynamicNodeDescriptionCreator.instance().addDescription(metaNode,
                    false, builder);
            String relativePath =
                    File.separator + HTML_DIR + "/" + NODES_DIR + "/" + "Meta_"
                            + fileName(metaNode.getName()) + ".html";
            File nodeDescription = new File(m_destinationDir, relativePath);
            FileWriter writer = new FileWriter(nodeDescription);
            writer.write(builder.toString());
            writer.flush();
            writer.close();
            // append topic
            Element topic = nodeToc.createElement("topic");
            topic.setAttribute("label", htmlString(metaNode.getName()));
            topic.setAttribute("href", "PLUGINS_ROOT/" + m_pluginID
                    + relativePath);
            nodeToc.getDocumentElement().appendChild(topic);
            return true;
        }
        return false;
    }

    private void writeTocToPluginXML(final Category c, final boolean node) {
        // register as toc file in plugin xml
        Element extension = getTocExtension();
        if (extension == null) {
            extension = m_pluginXML.createElement(EXT);
            extension.setAttribute(POINT, HELP_TOC);
            m_pluginXML.getDocumentElement().appendChild(extension);
        }
        Element toc = m_pluginXML.createElement("toc");
        if (node) {
            toc.setAttribute("file", TOC_DIR + "/" + fileName(getFullPath(c))
                    + NODES + ".xml");
        } else {
            toc.setAttribute("file", TOC_DIR + "/" + fileName(getFullPath(c))
                    + ".xml");
        }
        toc.setAttribute("extradir", "html");
        if (!alreadyExist(toc, extension)) {
            extension.appendChild(toc);
        }
    }

    private boolean alreadyExist(final Node node, final Element document) {
        // check here if this toc is already there
        // Node#isEqualNode
        NodeList nodes = document.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (node.isEqualNode(n)) {
                return true;
            }
        }
        return false;
    }

    private void writeNodeTocFile(final Document root, final String fileName)
            throws IOException, TransformerException {
        Document doc = root;
        Source src = new DOMSource(doc);
        File f =
                new File(m_destinationDir, TOC_DIR + File.separator + fileName
                        + ".xml");
        Result streamResult = new StreamResult(f);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(src, streamResult);
    }

    private Document createNodeTocFile(final Category c)
            throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = f.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createProcessingInstruction("NLS",
                "TYPE=\"org.eclipse.help.toc\""));
        Element root = doc.createElement("toc");
        String path =
                "../" + c.getPluginID() + "/tocs/" + fileName(getFullPath(c))
                        + ".xml#" + fileName(getFullPath(c));
        root.setAttribute("link_to", path);
        root.setAttribute("label", htmlString(c.getName()));
        doc.appendChild(root);
        return doc;
    }

    private void writeCategoryTocFile(final Category c) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createProcessingInstruction("NLS",
                "TYPE=\"org.eclipse.help.toc\""));
        Element root = doc.createElement("toc");

        if (c.getParent() instanceof Root) {
            root.setAttribute("link_to", "../" + "org.knime.workbench.help"
                    + "/toc.xml#" + ROOT_ANCHOR);
            root.setAttribute("label", fileName(getFullPath(c)));
            /*
             * <toc link_to=../org.knime.workbench.help/toc.xml#root
             * label=c.getID();
             */
        } else {
            String parentsPluginID = ((Category)c.getParent()).getPluginID();
            String parent = fileName(getFullPath((Category)c.getParent()));
            root.setAttribute("link_to", "../" + parentsPluginID + "/"
                    + TOC_DIR + "/" + parent + ".xml#" + parent);
            root.setAttribute("label", parent);
            /*
             * <toc link_to="../c.getParent().getPluginID()/
             * c.getParent().getID.xml#c.getID() label=c.getID() >
             */
        }
        Element topic = doc.createElement("topic");
        topic.setAttribute("label", htmlString(c.getName()));
        Element anchor = doc.createElement("anchor");
        anchor.setAttribute("id", fileName(getFullPath(c)));
        topic.appendChild(anchor);
        root.appendChild(topic);
        doc.appendChild(root);
        /*
         * <topic label="htmlString(c.getName());> <anchor id=c.getID />
         * </topic> </toc>
         */
        writeTocToPluginXML(c, false);

        /*
         * <extension point="org.eclipse.help.toc"> <toc file="toc.xml"> </toc>
         * </extension>
         */

        writeNodeTocFile(doc, fileName(getFullPath(c)));
    }

    private boolean processNode(final NodeTemplate node, final Document nodeToc)
            throws Exception {
        assert nodeToc != null;
        if (node.getPluginID().equals(m_pluginID)) {
            // create HTML file
            StringBuilder builder = new StringBuilder();
            DynamicNodeDescriptionCreator.instance().addDescription(node,
                    false, builder);

            String relativePath =
                    HTML_DIR + "/" + NODES_DIR + "/" + fileName(node.getID())
                            + ".html";
            File nodeDescription = new File(m_destinationDir, relativePath);
            FileWriter writer = new FileWriter(nodeDescription);
            writer.write(builder.toString());
            writer.flush();
            writer.close();
            // append topic
            Element topic = nodeToc.createElement("topic");
            topic.setAttribute("label", htmlString(node.getName()));
            topic.setAttribute("href", "PLUGINS_ROOT/" + m_pluginID + "/"
                    + relativePath);
            nodeToc.getDocumentElement().appendChild(topic);
            return true;
        }
        return false;
    }

    /**
     *
     * @return path of this plugin
     * @throws IOException if something went wrong
     */
    private File getPluginDir() throws IOException {
        URL devWorkSpace;
        if (m_pluginID != null) {
            devWorkSpace =
                    FileLocator
                            .toFileURL(FileLocator.find(Platform
                                    .getBundle(m_pluginID), new Path("/"), null));
        } else {
            devWorkSpace =
                    FileLocator.toFileURL(FileLocator.find(HelpviewPlugin
                            .getDefault().getBundle(), new Path("/"), null));
        }
        File loc = new File(devWorkSpace.getFile());
        return loc;
    }

    /**
     * Removes all illegal characters for filenames from the given category/node
     * name.
     *
     * @param categoryName to be converted into valid filename
     * @return categoryName as valid filename
     */
    private static String fileName(final String categoryName) {
        StringBuilder encode = new StringBuilder();
        for (int i = 0; i < categoryName.length(); i++) {
            switch (categoryName.charAt(i)) {
                case '/':
                case ' ':
                    encode.append("_");
                    break;
                case '\\':
                case ':':
                case '*':
                case '?':
                case '"':
                case '<':
                case '>':
                    break;
                default:
                    encode.append(categoryName.charAt(i));
            }
        }
        return encode.toString();
    }

    private static String getFullPath(final Category c) {
        return (c.getPath() + "/" + c.getID()).replaceFirst("^/+", "");
    }
}

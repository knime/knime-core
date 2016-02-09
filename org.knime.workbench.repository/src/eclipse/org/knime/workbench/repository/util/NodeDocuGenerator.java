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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 10, 2013 (hornm): created
 */
package org.knime.workbench.repository.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.KNIMEConstants;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;

/**
 * Creates a summary of the node descriptions of a all available KNIME nodes that can be browsed in a node
 * repository-like menu. After generation just open the "index.html" in a browser.
 *
 * @author Martin Horn, University of Konstanz
 * @since 2.9
 */
public class NodeDocuGenerator implements IApplication {

    private static final String DESTINATION_ARG = "-destination";

    private static final String CATEGORY_ARG = "-category";

    private static final String PLUGIN_ARG = "-plugin";

    private static void printUsage() {
        System.err.println("Usage: NodeDocuGenerator options");
        System.err.println("Allowed options are:");
        System.err.println("\t-destination dir : directory where "
                + "the result should be written to (directory must exist)");
        System.err
                .println("\t-plugin plugin-id : Only nodes of the specified plugin will be considered. If not all available plugins will be processed.\n");
        System.err
                .println("\t-category category-path (e.g. /community) : Only nodes within the specified category path will be considered. If not specified '/' is used.\n");

    }

    private static final String HEADER_TEMPLATE = "header_template.html";

    private static final String NODE_DESCRIPTION_TEMPLATE = "node_description_template.html";

    private static final String NODE_REPOSITORY_TEMPLATE = "node_repository_template.html";

    private static final String[] FILES_TO_COPY = new String[]{"index.html", "empty_node_description.html",
        "plus-square-o.png", "minus-square.png", "knime_logo.png", "knime_default_icon.png"};

    /* target directory */
    private File m_directory;

    private String m_nodeDescriptionTemplate;

    private String m_nodeRepositoryTemplate;

    private StringBuilder m_nodeRepository = new StringBuilder();

    private String m_pluginId = null;

    private String m_catPath = "/";

    /**
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        Object o = context.getArguments().get("application.args");
        Display.getDefault();
        if ((o != null) && (o instanceof String[])) {
            String[] args = (String[])o;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals(DESTINATION_ARG)) {
                    m_directory = new File(args[i + 1]);
                } else if (args[i].equals(CATEGORY_ARG)) {
                    m_catPath = args[i + 1];
                } else if (args[i].equals(PLUGIN_ARG)) {
                    m_pluginId = args[i + 1];
                } else if (args[i].equals("-help")) {
                    printUsage();
                    return EXIT_OK;
                }
            }
        }

        if (m_directory == null) {
            System.err.println("No output directory specified");
            printUsage();
            return 1;
        } else if (!m_directory.exists() && !m_directory.mkdirs()) {
            System.err.println("Could not create output directory '" + m_directory.getAbsolutePath() + "'.");
            return 1;
        }


        // read html-templates
        m_nodeDescriptionTemplate = readFile(NODE_DESCRIPTION_TEMPLATE);
        m_nodeRepositoryTemplate = readFile(NODE_REPOSITORY_TEMPLATE);

        generate();

        return EXIT_OK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

    }

    /**
     * Starts generating the node reference documents.
     *
     * @throws Exception
     */
    private void generate() throws Exception {

        // copy static html files
        copyFiles(FILES_TO_COPY);

        m_nodeRepository.setLength(0);

        // write css file
        Writer css = createDocumentWriter("style.css", m_directory);
        css.write(NodeFactoryHTMLCreator.instance.getCss());
        css.close();

        System.out.println("Reading node repository");
        IRepositoryObject root = RepositoryManager.INSTANCE.getRoot();

        // determine the root category according to the user specified category
        // path (only if the specified category should appear as new root)
        // String[] cats = m_catPath.split("/");
        // for (int i = 1; i < cats.length; i++) {
        // IRepositoryObject[] children = null;
        // if (root instanceof Root) {
        // children = ((Root)root).getChildren();
        // } else if (root instanceof Category) {
        // children = ((Category)root).getChildren();
        // } else {
        // break;
        // }
        // if (children != null) {
        // for (int j = 0; j < children.length; j++) {
        // if (children[j].getID().equals(cats[i])) {
        // root = children[j];
        // break;
        // }
        // }
        // }
        //
        // }

        // replace '/' with points and remove leading '/'
        if (m_catPath.startsWith("/")) {
            m_catPath = m_catPath.substring(1);
        }
        m_catPath = m_catPath.replaceAll("/", ".");

        // recursively generate the node reference and the node description
        // pages
        generate(m_directory, root, null);

        // complete and write repository template
        String nodeRepo = m_nodeRepositoryTemplate.replace("[NODE_REPOSITORY]", m_nodeRepository.toString());
        Writer out = createDocumentWriter("node_repository.html", m_directory);
        out.write(nodeRepo);
        out.close();

        String header = readFile(HEADER_TEMPLATE);
        header =
                header.replace("[KNIME_VERSION]", KNIMEConstants.MAJOR + "." + KNIMEConstants.MINOR + "."
                        + KNIMEConstants.REV);
        out = createDocumentWriter("header.html", m_directory);
        out.write(header);
        out.close();

    }

    /**
     * Cleans node template ID for file name, by replacing all non word characters with "_".
     * @param nodeTemplate Node template with ID to clean.
     * @return Cleaned ID of node template. All non word characters have been replaced with "_".
     */
    private String cleanNodeIdForFileName(final NodeTemplate nodeTemplate) {
        String id = nodeTemplate.getID();
        String cleanedId = id.replaceAll("/", "_");
        cleanedId = cleanedId.replaceAll(":", "_");
        return cleanedId;
    }

    /**
     * Recursively generates the nodes description documents and the menu entries.
     *
     * @param directory
     * @param current
     * @param parent parent repository object as some nodes pointing to "frequently used"-repository object as a parent
     * @throws Exception
     * @throws TransformerException
     *
     * @return true, if the element was added to the documentation, false if it has been skipped
     */
    private boolean generate(final File directory, final IRepositoryObject current, final IRepositoryObject parent)
            throws TransformerException, Exception {
        // current length of the repository string to be able to revert it to
        // the current state
        int currentLength = m_nodeRepository.length();

        if (current instanceof NodeTemplate) {

            // skip node if not part of the specified plugin
            if (m_pluginId != null && !current.getContributingPlugin().equals(m_pluginId)) {

                return false;
            }

            // skip if not in a sub-category of the category specified
            // as argument
            if (m_catPath.length() > 0) {
                String catIdentifier = getCategoryIdentifier((Category)parent);
                if (!catIdentifier.startsWith(m_catPath)) {
                    return false;
                }
            }

            String nodeIdentifier = cleanNodeIdForFileName((NodeTemplate)current);//((NodeTemplate)current).getID();

            // write icon to disc
            URL iconURL = ((NodeTemplate)current).createFactoryInstance().getIcon();
            String nodeIcon;
            if (iconURL != null) {
                writeStreamToFile(iconURL.openStream(), nodeIdentifier + ".png");
                nodeIcon = nodeIdentifier + ".png";
            } else {
                nodeIcon = "knime_default_icon.png";
            }

            // the node repository-like menu
            m_nodeRepository.append("<li style=\"list-style-image: url(");
            m_nodeRepository.append(nodeIcon);
            m_nodeRepository.append(");\" class=\"knime-node\"><span class=\"childs\"><a href=\"");
            m_nodeRepository.append(current.getID());
            m_nodeRepository.append(".html\" target=\"Node Description\">");
            m_nodeRepository.append(((NodeTemplate)current).getName());
            m_nodeRepository.append("</a></span></li>\n");

            // create page with node description and return, as no more
            // children
            // are available
            Writer nodeDoc = createDocumentWriter(cleanNodeIdForFileName((NodeTemplate)current) + ".html", directory);
            String nodeDescription =
                    NodeFactoryHTMLCreator.instance.readFullDescription(((NodeTemplate)current).createFactoryInstance()
                            .getXMLDescription());
            // extract the body of the node description html-document
            nodeDescription =
                    nodeDescription
                            .substring(nodeDescription.indexOf("<body>") + 6, nodeDescription.indexOf("</body>"));
            nodeDescription = m_nodeDescriptionTemplate.replace("[NODE_DESCRIPTION]", nodeDescription);
            nodeDoc.write(nodeDescription);
            nodeDoc.flush();
            nodeDoc.close();

            return true;
        } else if (current instanceof Category || current instanceof Root) {
            System.out.println("Processing category " + getPath(current));
            IRepositoryObject[] repoObjs = ((IContainerObject)current).getChildren();

            if (current instanceof Category) {
                String catIdentifier = getCategoryIdentifier((Category)current);

                // write icon to disc and add html-tags
                ImageLoader loader = new ImageLoader();
                Image catImg = ((Category)current).getIcon();
                String catIcon;
                if (catImg != null) {
                    loader.data = new ImageData[]{catImg.getImageData()};
                    loader.save(directory + File.separator + catIdentifier + ".png", SWT.IMAGE_PNG);
                    catIcon = catIdentifier + ".png";
                } else {
                    catIcon = "knime_default_icon.png";
                }

                m_nodeRepository.append("<li class=\"knime-category\">");
                m_nodeRepository.append("<img width=\"16px\" src=\"");
                m_nodeRepository.append(catIcon);
                m_nodeRepository.append("\"/>&nbsp;");
                m_nodeRepository.append(((Category)current).getName());
                m_nodeRepository.append("</span><ul>");
            }

            boolean hasChildren = false;
            for (IRepositoryObject repoObj : repoObjs) {
                hasChildren = hasChildren | generate(directory, repoObj, current);
            }

            if (hasChildren) {
                m_nodeRepository.append("</ul></li>");
                return true;
            } else {
                // revert all entries done so far
                m_nodeRepository.setLength(currentLength);
                return false;
            }

        } else {
            // if the repository object is neither a node nor a category
            // (hence,
            // most likely a metanode), we just ignore them for now
            return false;
        }

    }

    /*
     * Helper to compose the category names/identifier of the super-categories
     * and the current one
     */
    private static String getCategoryIdentifier(final Category cat) {
        IContainerObject parent = cat.getParent();
        String identifier = cat.getID();
        while (!(parent instanceof Root)) {
            identifier = parent.getID() + "." + identifier;
            parent = parent.getParent();
        }
        return identifier;
    }

    /*
     * Helper to create a document of the given name at the given directory to
     * write into
     */
    private Writer createDocumentWriter(final String name, final File directory) throws IOException {
        return new BufferedWriter(new FileWriter(new File(directory.getAbsolutePath() + File.separator + name)));
    }

    /*
     * copies the given files into the target directory
     */
    private void copyFiles(final String... files) throws IOException {
        for (String f : files) {
            InputStream in = NodeDocuGenerator.class.getResourceAsStream(f);
            OutputStream out = new FileOutputStream(new File(m_directory.getAbsolutePath() + File.separator + f));
            IOUtils.copy(in, out);
            in.close();
            out.close();
        }
    }

    /*
     * Writes the stream into the given file (within the target directory)
     */
    private void writeStreamToFile(final InputStream in, final String fileName) throws IOException {
        Path p = Paths.get(m_directory.getAbsolutePath(), fileName);
        Files.createDirectories(p.getParent());

        try (OutputStream out = Files.newOutputStream(p)) {
            IOUtils.copy(in, out);
        }
        in.close();
    }

    /*
     * Stores the file content into a string.
     */
    private String readFile(final String file) throws IOException {
        InputStream is = NodeDocuGenerator.class.getResourceAsStream(file);

        BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String line;
        StringBuilder buf = new StringBuilder();
        while ((line = in.readLine()) != null) {
            buf.append(line).append('\n');
        }
        in.close();
        return buf.toString();
    }

    private static String getPath(final IRepositoryObject object) {
        if (object.getParent() != null) {
            return getPath(object.getParent()) + "/" + object.getName();
        } else {
            return "";
        }
    }
}

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
 */
package org.knime.testing.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.core.node.NodeFactory;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Scans the node repository and creates an html file listing all available
 * nodes. Usefult to fill the features list on <a href="http://www.knime.org">
 * http://www.knime.org</a>.
 * @author wiswedel, University of Konstanz
 */
public class KNIMEPrintNodesStdOutApplication implements IApplication {
    private static final String PARAM_FILE_NAME = "-outFile";
    private static final String PARAM_DIRECTORY = "-outDir";
    private static final String DEFAULT_FILE_NAME = "overview.html";
    private static final String DEFAULT_CSV_FILE_NAME = "overview.csv";
    private static final String DETAIL_DIR_NAME = "details";
    private boolean m_createDir = false;
    private File m_detailsDir;

    /** {@inheritDoc} */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        Object args =
            context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        boolean found = false;
        String filename = null;
        File directory = null;
        if (args instanceof String[]) {
            String[] as = (String[])args;
            for (int i = 0; i < as.length; i++) {
                if (PARAM_FILE_NAME.equals(as[i])) {
                    if (i + 1 <= as.length - 1) {
                        filename = as[i + 1];
                        found = true;
                    }
                } else if (PARAM_DIRECTORY.equals(as[i]) && (i + 1 <= as.length - 1)) {
                    directory = new File(as[i + 1]);
                    if (directory.exists() && !directory.isDirectory()) {
                        throw new IllegalArgumentException(
                                directory.getCanonicalPath()
                                + " already exists, but is no directory.");
                    }
                    directory.mkdirs();
                    // create sub directory for detailed node descriptions
                    m_detailsDir = new File(directory, DETAIL_DIR_NAME);
                    m_detailsDir.mkdir();
                    m_createDir = true;
                }
            }
        }

        File file = null;
        File csvFile = new File(directory, DEFAULT_CSV_FILE_NAME);
        if (m_createDir) {
            String name = null;
            if (!found) {
                name = DEFAULT_FILE_NAME; // use default file name
            } else {
             // get file name last segment of absolute file path
                name = new File(filename).getName();
            }
            file = new File(directory, name);
        } else if (found) {
            file = new File(filename);
        } else {
            // at least one option must be specified
            System.err.println("Please provide at least one of the options: \n"
                    + PARAM_FILE_NAME + " file_to_write.html\n"
                    + PARAM_DIRECTORY + " directory to write html files to.\n"
                    + "If the directory is specified html files with the long"
                    + " descriptions of the nodes are created in addition.");
            return IApplication.EXIT_OK;
        }

        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager freezes
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        Root root = RepositoryManager.INSTANCE.getRoot();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {
            writer.write("<html><body>\n");
            print(writer, 0, root, false);
            writer.write("</body></html>\n");
        }

        // generate CSV
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile.toPath())) {
            writer.append("True path" + ',' + "System path" + ',' + "Name" + ',' + "Factory" + "," + "NodeModel");
            writer.newLine();
            printCSV(writer, root, "");
        }

        System.out.println("Node description generation successfully finished");
        return IApplication.EXIT_OK;
    }

    /** Recursive print of nodes, categories, meta nodes to argument writer. */
    private void print(final Writer writer, final int indent,
            final IRepositoryObject object, final boolean topLevel)
        throws IOException {
        indent(indent, writer);
        if (object instanceof Root) {
            writer.write("<ul>");
            Root r = (Root)object;
            for (IRepositoryObject child : r.getChildren()) {
                writer.append('\n');
                print(writer, indent + 2, child, true);
            }
            writer.append('\n');
            indent(indent, writer);
            writer.write("</ul>");
        } else {
            writer.write("<li>");
            if (object instanceof Category) {
                Category c = (Category)object;
                writer.write(topLevel ? "<strong>" : "");
                writer.append(StringEscapeUtils.escapeHtml4(c.getName()));
                writer.write(topLevel ? "</strong>" : "");
                writer.write("<ul>");
                for (IRepositoryObject child : c.getChildren()) {
                    writer.append('\n');
                    print(writer, indent + 2, child, false);
                }
                writer.append('\n');
                indent(indent, writer);
                writer.write("</ul>");
                writer.append('\n');
                indent(indent, writer);
            } else if (object instanceof NodeTemplate) {
                NodeTemplate t = (NodeTemplate)object;
                writer.write("<em>");
                String nodeName = t.getName();
                String detailFileName = t.getCategoryPath() + "/" + nodeName;
                detailFileName = detailFileName.replaceAll("\\W+", "_")
                        + ".html";
                if (m_createDir) {
                    // add link to page containing the long description
                    writer.append("<a href=\"./" + DETAIL_DIR_NAME + "/"
                            + detailFileName  + "\" target=\"_blank\">");
                }
                writer.append(StringEscapeUtils.escapeHtml4(nodeName));
                if (m_createDir) {
                    writer.append("</a>");
                }
                writer.write("</em> - ");
                try {
                    Element nodeXML
                            = t.createFactoryInstance().getXMLDescription();
                    writer.append(readShortDescriptionFromXML(
                            nodeXML, indent));
                    if (m_createDir) {
                        Writer detailsWriter =
                                new OutputStreamWriter(new FileOutputStream(new File(m_detailsDir, detailFileName)),
                                        Charset.forName("UTF-8"));
                        detailsWriter.append(createHTMLDescription(t));
                        detailsWriter.close();
                    }
                } catch (Exception e) {
                    writer.append("ERROR reading description: " + e);
                }
            } else if (object instanceof MetaNodeTemplate) {
                MetaNodeTemplate m = (MetaNodeTemplate)object;
                writer.write("<em>");
                writer.append(StringEscapeUtils.escapeHtml4(m.getName()));
                writer.write("</em>");
                String description = m.getDescription();
                if (description != null) {
                    writer.append(" - ");
                    writer.append(m.getDescription());
                }
            }
            writer.write("</li>");
        }
    }



    /** Recursive print of nodes to argument writer. */
    private void printCSV(final BufferedWriter writer, final IRepositoryObject object, final String hist)
        throws IOException {

        if (object instanceof Root) {
            Root r = (Root)object;
            for (IRepositoryObject child : r.getChildren()) {
                printCSV(writer, child, hist +  "/"+ child.getName());
            }
        } else {
            if (object instanceof Category) {
                Category c = (Category)object;
                for (IRepositoryObject child : c.getChildren()) {
                    printCSV(writer, child, hist + "/"+ child.getName());
                }
            } else if (object instanceof NodeTemplate) {
                NodeTemplate t = (NodeTemplate)object;
                String nodeModel = "";
                try {
                    NodeFactory nf = t.getFactory().newInstance();
                    nodeModel = nf.createNodeModel().getClass().getName();
                } catch (Exception e) {
                    nodeModel = "n/a"; // some nodesfactorys don't have nodemodels. e.g.
                }
                writer.append(hist + ','
                    + t.getCategoryPath()  + ','
                    + t.getName()  + ','
                    + t.getFactory().toString() + ','
                    + nodeModel);
                writer.newLine();
            }

//            NativeNodeContainer nc;
//            else if (object instanceof MetaNodeTemplate) {
//            // Metanodes are not of interest for the usage statics analysis
//                MetaNodeTemplate m = (MetaNodeTemplate)object;
//                writer.append(hist + ','
//                    + m.getCategoryPath()  + ','
//                    + m.getName()  + ','
//                    + m.getContributingPlugin());
//            }
        }
    }


    private static String readShortDescriptionFromXML(final Element knimeNode,
            final int indent) {
        if (knimeNode == null) {
            return "No description available! Please add an XML description.";
        }
        Node w3cNode =
                knimeNode.getElementsByTagName("shortDescription").item(0);
        if (w3cNode == null) {
            return null;
        }
        Node w3cNodeChild = w3cNode.getFirstChild();
        if (w3cNodeChild == null) {
            return null;
        }
        String shortDescription = w3cNodeChild.getNodeValue().trim();
        char[] indentChars = new char[indent];
        Arrays.fill(indentChars, ' ');
        return shortDescription.replace("\n", "\n" + new String(indentChars));
    }


    /**
     * Creates the HTML description for the node template.
     *
     * @param node the node template to generate the html description for
     * @return the html node description
     */
    private static String createHTMLDescription(final NodeTemplate node) {

        StringBuilder builder = new StringBuilder();
        DynamicNodeDescriptionCreator.instance().addDescription(node,
                false, builder);
        // insert a header directly after the body tag.

        String bodyHeader =
            "<div style=\"float:left; margin-right:5px;\">"
            + "<img src=\"http://knime.com/files/knime_logo_small.png\" /></div>"
            + "<p>The following node is available in the Open Source KNIME "
            + "predictive analytics and data mining platform version "
            + getVersionString()
            + ". Discover over  1000 other nodes, as well as enterprise "
            + "functionality at "
            + "\n<a href=\"http://knime.com\">http://knime.com</a>.</p>"
            + "<div style=\"clear: both\";></div>";
        String bodyTag = "<body>";
        builder.insert(builder.indexOf(bodyTag) + bodyTag.length(), bodyHeader);
        return builder.toString();
    }

    /**
     * @return the current KNIME version
     */
    private static String getVersionString() {
        Bundle eclipseCore = Platform.getBundle("org.knime.core");
        String version = eclipseCore.getHeaders().get("Bundle-Version");
        return version.substring(0, version.lastIndexOf('.'));
    }

    private static final void indent(final int indent, final Writer writer)
    throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.append(' ');
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
    }

}

/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 18, 2009 (wiswedel): created
 */
package org.knime.testing.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Scans the node repository and creates an html file listing all available
 * nodes. Usefult to fill the features list on <a href="http://www.knime.org">
 * http://www.knime.org</a>.
 * @author wiswedel, University of Konstanz
 */
public class KNIMEPrintNodesStdOutApplication implements IApplication {

    /** {@inheritDoc} */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        Object args =
            context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        Writer writer = new OutputStreamWriter(System.out);
        String name = "-outFile";
        boolean found = false;
        if (args instanceof String[]) {
            String[] as = (String[])args;
            for (int i = 0; i < as.length; i++) {
                if (name.equals(as[i])) {
                    if (i + 1 <= as.length - 1) {
                        writer = new BufferedWriter(new FileWriter(as[i + 1]));
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            System.err.println("Options: " + name + " file_to_write.html");
        }
        Root root = RepositoryManager.INSTANCE.getRoot();
        writer.write("<html><body>\n");
        print(writer, 0, root, false);
        writer.write("</body></html>\n");
        writer.close();
        return IApplication.EXIT_OK;
    }

    /** Recursive print of nodes, categories, meta nodes to argument writer. */
    private static void print(final Writer writer, final int indent,
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
                writer.append(c.getName());
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
                writer.append(t.getName());
                writer.write("</em> - ");
                try {
                    writer.append(readShortDescriptionFromXML(
                            t.getFactory().newInstance().getXMLDescription(),
                            indent));
                } catch (Exception e) {
                    writer.append("ERROR reading description: " + e);
                }
            } else if (object instanceof MetaNodeTemplate) {
                MetaNodeTemplate m = (MetaNodeTemplate)object;
                writer.write("<em>");
                writer.append(m.getName());
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

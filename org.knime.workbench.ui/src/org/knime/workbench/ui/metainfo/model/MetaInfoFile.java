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
package org.knime.workbench.ui.metainfo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.nature.KNIMEWorkflowSetProjectNature;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class for the meta info file which contains the meta information
 * entered by the user for workflow groups and workflows, such as author, date,
 * comments.
 *
 * @author Fabian Dill, KNIME.com AG
 */
public final class MetaInfoFile {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaInfoFile.class);

    /** Preference key for a workflow template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WF
        = "org.knime.ui.metainfo.template.workflow";

    /** Preference key for a workflow group template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WFS
        = "org.knime.ui.metainfo.template.workflowset";

    private MetaInfoFile() {
        // utility class
    }

    /**
     * Creates a meta info file with default content.
     * @param parent parent file
     * @param isWorkflow true if it is a meta info for a workflow
     */
    public static void createMetaInfoFile(final File parent,
            final boolean isWorkflow) {
        // look into preference store
        File f = getFileFromPreferences(isWorkflow);
        if (f != null) {
            writeFileFromPreferences(parent, f);
        } else {
            createDefaultFileFallback(parent);
        }
    }

    private static void writeFileFromPreferences(final File parent,
            final File f) {
        File dest = new File(parent, WorkflowPersistor.METAINFO_FILE);
        try {
            FileUtil.copy(f, dest);
        } catch (IOException io) {
            LOGGER.error("Error while creating meta info template for "
                    + parent.getName()
                    + ". Creating default file...", io);
            createDefaultFileFallback(parent);
        }
    }

    private static File getFileFromPreferences(final boolean isWorkflow) {
        String key = PREF_KEY_META_INFO_TEMPLATE_WFS;
        if (isWorkflow) {
            key = PREF_KEY_META_INFO_TEMPLATE_WF;
        }
        String fileName = KNIMEUIPlugin.getDefault().getPreferenceStore()
            .getString(key);
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return new File(fileName);
    }

    private static void createDefaultFileFallback(final File parent) {
        try {
            File meta = new File(parent, WorkflowPersistor.METAINFO_FILE);
            SAXTransformerFactory fac
                = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = fac.newTransformerHandler();

            Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            OutputStream out = new FileOutputStream(meta);
            handler.setResult(new StreamResult(out));

            handler.startDocument();
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "nrOfElements", "CDATA", ""
                    + 2);
            handler.startElement(null, null, "KNIMEMetaInfo", atts);

            // author
            atts = new AttributesImpl();
            atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA",
                    MetaGUIElement.TEXT);
            atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA",
                    "Author");
            handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
            handler.endElement(null, null, MetaGUIElement.ELEMENT);

            // creation date
            atts = new AttributesImpl();
            atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA",
                    MetaGUIElement.DATE);
            atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA",
                    "Creation Date");
//            atts.addAttribute(null, null, MetaGUIElement.READ_ONLY, "CDATA",
//                    "true");
            handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
            Calendar current = Calendar.getInstance();
            String date = DateMetaGUIElement.createStorageString(
                    current.get(Calendar.DAY_OF_MONTH),
                    current.get(Calendar.MONTH),
                    current.get(Calendar.YEAR));
            char[] dateChars = date.toCharArray();
            handler.characters(dateChars, 0, dateChars.length);
            handler.endElement(null, null, MetaGUIElement.ELEMENT);


            // comments
            atts = new AttributesImpl();
            atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA",
                    MetaGUIElement.MULTILINE);
            atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA",
                    "Comments");
            handler.startElement(null, null, MetaGUIElement.ELEMENT, atts);
            handler.endElement(null, null, MetaGUIElement.ELEMENT);

            // TODO: add here all default elements

            handler.endElement(null, null, "KNIMEMetaInfo");
            handler.endDocument();
            out.close();
            } catch (Exception e) {
                LOGGER.error("Error while trying to create default "
                        + "meta info file for " + parent.getName(), e);
            }
    }

    /**
     * Creates a new workflow group project (with the referring nature).
     * @param name name of the project
     * @param natureId one of {@link KNIMEProjectNature}
     *  or {@link KNIMEWorkflowSetProjectNature}
     * @return the created project (already open and with description)
     * @throws CoreException if something goes wrong
     *
     * @see KNIMEWorkflowSetProjectNature
     */
    public static IProject createKnimeProject(final String name,
            final String natureId) throws CoreException {
        if (!KNIMEProjectNature.ID.equals(natureId)
                && !KNIMEWorkflowSetProjectNature.ID.equals(natureId)) {
            throw new IllegalArgumentException(
                    "Unsupported project nature " + natureId + ". "
                    + "Only KnimeProjectNature and "
                    + "KnimeWorkflowSetProjectNature are supported!");
        }
        IProject newProject = null;
        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            newProject = root.getProject(name);
            newProject.create(null);
            newProject.open(null);
            IProjectDescription desc = newProject.getDescription();
            desc.setNatureIds(new String[] {natureId});
            newProject.setDescription(desc, null);
        } catch (CoreException e) {
            LOGGER.error("Error while creating project "  + name, e);
            throw e;
        }
        return newProject;
    }
}

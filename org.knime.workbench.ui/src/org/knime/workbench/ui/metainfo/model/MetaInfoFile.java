/* This source code, its documentation and all appendant files
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
        File f = new File(fileName);
        return f;
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
     * @throws Exception if something goes wrong
     *
     * @see {@link KNIMEWorkflowSetProjectNature}
     */
    public static IProject createKnimeProject(final String name,
            final String natureId)
        throws Exception {
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
        } catch (Exception e) {
            LOGGER.error("Error while creating project "  + name, e);
        }
        return newProject;
    }
}

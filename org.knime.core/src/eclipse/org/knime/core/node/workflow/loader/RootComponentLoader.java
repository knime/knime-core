/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   9 Feb 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.workflow.WorkflowLoadHelper.UnknownKNIMEVersionLoadPolicy;
import org.knime.core.workflow.def.RootComponentDef;
import org.knime.core.workflow.def.impl.RootComponentDefBuilder;

/**
 * Loads a standalone component (a.k.a. template), i.e., a top-level component that is not part of a workflow but can
 * for instance be dragged from the KNIME explorer into a workflow to be inserted into a workflow.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class RootComponentLoader {

//    /**
//     * Create persistor for a workflow or template.
//     *
//     * @param directory The directory to load from
//     * @param templateSourceURI URI of the link to the template (will load a template and when the template is
//     *            instantiated and put into the workflow a link is created)
//     * @throws UnsupportedWorkflowVersionException If the workflow is of an unsupported version
//     */
//    public final TemplateNodeContainerPersistor createTemplateLoadPersistor(final File directory,
//        final URI templateSourceURI) throws IOException, UnsupportedWorkflowVersionException {
//
//        // TODO template.knime or workflow.knime
//        String fileName = "template.knime";
//        NodeSettingsRO settings = NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(dotKNIME)));
//
////        final MetaNodeTemplateInformation templateInfo;
////        if (isTemplateFlow() && templateSourceURI != null) {
////            try {
////                templateInfo = MetaNodeTemplateInformation.load(settings, version, isTemplateProject());
////                CheckUtils.checkSetting(Role.Template.equals(templateInfo.getRole()), "Role is not '%s' but '%s'",
////                    Role.Template, templateInfo.getRole());
////            } catch (InvalidSettingsException e) {
////                throw new IOException(
////                    String.format("Attempting to load template from \"%s\" but can't locate template information: %s",
////                        dotKNIME.getAbsolutePath(), e.getMessage()),
////                    e);
////            }
////        } else if (isTemplateFlow()) {
////            //            LOGGER.coding("Supposed to instantiate a template but the link URI is not set");
////            // metanode template from node repository
////            templateInfo = null;
////        } else {
////            templateInfo = null;
////        }
//
//        final TemplateNodeContainerPersistor persistor;
//            // some template is loaded
//            switch (templateInfo.getNodeContainerTemplateType()) {
//                case MetaNode:
//                    final ReferencedFile workflowDotKNIME;
//                    if (version.isOlderThan(LoadVersion.V2100)) {
//                        workflowDotKNIME = dotKNIMERef; // before 2.10 everything was stored in template.knime
//                    } else {
//                        workflowDotKNIME = new ReferencedFile(dotKNIMERef.getParent(), WorkflowPersistor.WORKFLOW_FILE);
//                    }
//                    persistor = new FileWorkflowPersistor(workflowDataRepository, workflowDotKNIME, this, version,
//                        !isTemplateFlow());
//                    break;
//                case SubNode:
//                    final ReferencedFile settingsDotXML =
//                        new ReferencedFile(dotKNIMERef.getParent(), SingleNodeContainerPersistor.SETTINGS_FILE_NAME);
//                    persistor =
//                        new FileSubNodeContainerPersistor(settingsDotXML, this, version, workflowDataRepository, true);
//                    break;
//                default:
//                    throw new IllegalStateException("Unsupported template type");
//            }
//
//            if (templateInfo != null) {
//            persistor.setOverwriteTemplateInformation(templateInfo.createLink(templateSourceURI, isTemplateProject()));
//
//            if (templateSourceURI != null) {
//                final String path = templateSourceURI.getPath();
//                persistor.setNameOverwrite(path.substring(path.lastIndexOf('/') + 1));
//            } else {
//                persistor.setNameOverwrite(directory.getName());
//            }
//        }
//        if (isSetDirtyAfterLoad) {
//            persistor.setDirtyAfterLoad();
//        }
//        return persistor;
//    }

    /**
     * Loads the workflow global information (load version, etc.) and the actual workflow.
     *
     * @param directory The directory that contains the workflow to load
     * @param handleUnknownVersion what to do if the version is unknown // TODO is this really used properly
     * @return a description of the workflow as POJOs
     * @throws IOException when the workflow settings cannot be parsed from the given directory, or the workflow format
     *             version cannot be extracted from the parsed workflow settings
     */
    public static RootComponentDef load(final File directory, final UnknownKNIMEVersionLoadPolicy handleUnknownVersion)
        throws IOException {

        //TODO use handleUnknownVersion
        var creatorLoader = new CreatorLoader(directory);
        var workflowConfig = creatorLoader.getWorkflowConfig();
        var workflowFormatVersion = creatorLoader.getWorkflowFormatVersion();

        return new RootComponentDefBuilder()//
            .setCreator(creatorLoader.getCreatorDef())//
            .setComponent(ComponentLoader.load, null)//
            .build();

    }

}

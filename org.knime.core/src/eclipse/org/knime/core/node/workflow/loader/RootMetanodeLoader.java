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
 *   10 Feb 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

/**
 * TODO probably needs a factory that peeks into the directory and returns an appropriate loader.
 * Maybe reuse the XML parsed for peaking to improve performance.
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class RootMetanodeLoader {


 // From WorkflowLoadHelper#createTemplateLoadPersistor
 //  * @param templateSourceURI URI of the link to the template (will load a template and when the template is
 //*            instantiated and put into the workflow a link is created)
 //final MetaNodeTemplateInformation templateInfo;
 //if (isTemplateFlow() && templateSourceURI != null) {
 // try {
//      templateInfo = MetaNodeTemplateInformation.load(settings, version, isTemplateProject());
//      CheckUtils.checkSetting(Role.Template.equals(templateInfo.getRole()),
//          "Role is not '%s' but '%s'", Role.Template, templateInfo.getRole());
 // } catch (InvalidSettingsException e) {
//      throw new IOException(String.format(
//          "Attempting to load template from \"%s\" but can't locate template information: %s",
//          dotKNIME.getAbsolutePath(), e.getMessage()), e);
 // }
 //} else if (isTemplateFlow()) {
 //// LOGGER.coding("Supposed to instantiate a template but the link URI is not set");
 // // metanode template from node repository
 // templateInfo = null;
 //} else {
 // templateInfo = null;
 //}
 //
 //final TemplateNodeContainerPersistor persistor;
 //// TODO only create new repo if workflow is a project?
 //WorkflowDataRepository workflowDataRepository = new WorkflowDataRepository();
 //// ordinary workflow is loaded
 //if (templateInfo == null) {
 // persistor = new FileWorkflowPersistor(workflowDataRepository, dotKNIMERef, this,
//      version, !isTemplateFlow());
 //} else {
 // // some template is loaded
 // switch (templateInfo.getNodeContainerTemplateType()) {
//      case MetaNode:
//          final ReferencedFile workflowDotKNIME;
//          if (version.isOlderThan(LoadVersion.V2100)) {
//              workflowDotKNIME = dotKNIMERef; // before 2.10 everything was stored in template.knime
//          } else {
//              workflowDotKNIME = new ReferencedFile(dotKNIMERef.getParent(), WorkflowPersistor.WORKFLOW_FILE);
//          }
//          persistor = new FileWorkflowPersistor(workflowDataRepository, workflowDotKNIME, this,
//              version, !isTemplateFlow());
//          break;
//      case SubNode:
//          final ReferencedFile settingsDotXML = new ReferencedFile(dotKNIMERef.getParent(),
//              SingleNodeContainerPersistor.SETTINGS_FILE_NAME);
//          persistor = new FileSubNodeContainerPersistor(settingsDotXML, this, version,
//              workflowDataRepository, true);
//          break;
//      default:
//          throw new IllegalStateException("Unsupported template type");
 // }
 //}
 //if (templateInfo != null) {
 // persistor.setOverwriteTemplateInformation(templateInfo.createLink(templateSourceURI, isTemplateProject()));
 //
 // if (templateSourceURI != null) {
//      final String path = templateSourceURI.getPath();
//      persistor.setNameOverwrite(path.substring(path.lastIndexOf('/') + 1));
 // } else {
//      persistor.setNameOverwrite(directory.getName());
 // }
 //}
}

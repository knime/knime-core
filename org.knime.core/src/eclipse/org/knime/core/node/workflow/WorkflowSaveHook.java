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
 *   Oct 25, 2017 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;

/**
 * Defines additional operations that get executed when a workflow is saved to disc, e.g. writing additional
 * metadata to the workflow directory (such as an OpenAPI description of the nodes defining REST parameters).
 *
 * <p><strong>Warning:</strong></p> Pending API.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 3.5
 */
public abstract class WorkflowSaveHook {

    /*--------- Static declarations ---------*/
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowSaveHook.class);
    private static final String EXT_POINT_ID = "org.knime.core.WorkflowSaveHook";
    private static final String EXT_POINT_ATTR_CLASS_NAME = "class";

    /*--------- (Static) Extension Point Handling ---------*/

    /** List containing all classes, collected from extension point contributions. */
    private static List<WorkflowSaveHook> workflowSaveHooks;

    /** Called by the framework class to get the list of registered save hooks.
     * @return A non-null, read-only list of registered mappers (potentially empty).
     * @noreference This method is not intended to be referenced by clients.
     */
    static synchronized List<WorkflowSaveHook> getRegisteredMappers() {
        if (workflowSaveHooks == null) {
            workflowSaveHooks = collectWorkflowSaveHooks();
        }
        return workflowSaveHooks;
    }

    private static List<WorkflowSaveHook> collectWorkflowSaveHooks() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
            return Collections.emptyList();
        }

        List<WorkflowSaveHook> resultList = new ArrayList<WorkflowSaveHook>();
        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String workflowSaveHookClassName = elem.getAttribute(EXT_POINT_ATTR_CLASS_NAME);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (StringUtils.isEmpty(workflowSaveHookClassName)) {
                LOGGER.errorWithFormat("The extension '%s' doesn't provide the required attribute '%s' - ignoring it",
                    decl, EXT_POINT_ATTR_CLASS_NAME);
                continue;
            }

            try {
                WorkflowSaveHook instance = (WorkflowSaveHook)elem.createExecutableExtension(EXT_POINT_ATTR_CLASS_NAME);
                resultList.add(instance);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of workflow save hook (class '"
                        + workflowSaveHookClassName + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
                }
            }
        }
        return Collections.unmodifiableList(resultList);
    }

    /** Name of the sub folder for artifacts created by save hooks inside the workflow directory. */
    public static final String ARTIFACTS_FOLDER_NAME = ".artifacts";

    /**
     * Iterates all implementations and calls their {@link #onSave(WorkflowManager, boolean, File)} method. Possibly
     * exceptions are logged to the NodeLogger.
     *
     * @param manager The workflow project to save
     * @param isSaveData The "save data" flag
     * @param workflowFolder The folder the workflow is saved to. This method will create a subfolder
     *            {@linkplain #ARTIFACTS_FOLDER_NAME}.
     */
    static void runHooks(final WorkflowManager manager, final boolean isSaveData, final File workflowFolder) {
        File hookFolder = new File(workflowFolder, ARTIFACTS_FOLDER_NAME);
        FileUtil.deleteRecursively(hookFolder);
        if (getRegisteredMappers().stream().noneMatch(WorkflowSaveHook::isEnabled)) {
            return;
        }
        if (!hookFolder.mkdir()) {
            LOGGER.errorWithFormat("Unable to create '%s' workflow (%s)",
                ARTIFACTS_FOLDER_NAME, hookFolder.getAbsolutePath());
        }
        getRegisteredMappers().stream().filter(WorkflowSaveHook::isEnabled).forEach(hook -> {
            try {
                hook.onSave(manager, isSaveData, hookFolder);
            } catch (Exception e) {
                LOGGER.error("Error saving additional data to workflow using " + hook.getClass().getName() + ": "
                        + e.getMessage(), e);
            }
        });
    }

    /**
     * Called prior {@link #onSave(WorkflowManager, boolean, File)} to determine whether the hook should be run. The
     * default implementation returns <code>true</code> but the contribution in the testing extension will return
     * <code>false</code> unless it's run as a test.
     *
     * @return <code>true</code> if the hook should be run, <code>false</code> otherwise
     */
    protected boolean isEnabled() {
        return true;
    }

    /** Called each time the workflow is saved to disk, either newly from scratch or incrementally. The
     * File argument points to a directory within the workflow folder to which implementations can write
     * to. The folder is wiped each time before the workflow is saved.
     *
     * @param workflow The non-<code>null</code> workflow (project) being saved.
     * @param isSaveData Whether the data (port content) is saved, too.
     * @param artifactsFolder The folder within the workflow directory. Directory will exist.
     * @throws IOException possible I/O problems
     */
    public abstract void onSave(final WorkflowManager workflow, final boolean isSaveData,
        final File artifactsFolder) throws IOException;
}

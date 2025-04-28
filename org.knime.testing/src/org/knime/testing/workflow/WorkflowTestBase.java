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
 *   Apr 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.testing.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;

/**
 * Responsible for loading the workflow in the same package as the test class. Mostly copied from
 * org.knime.core.workflow.tests
 *
 * @author Paul Bärnreuther
 */
public abstract class WorkflowTestBase {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private WorkflowManager m_manager;

    /**
     * Loads the workflow that is expected to lie in the same directory as the test class under the lowercased test
     * class name
     *
     * @return the base node id of the workflow
     * @throws Exception
     */
    protected NodeID loadAndSetWorkflow() throws Exception {
        File workflowDir = getWorkflowDirectory();
        return loadAndSetWorkflow(workflowDir);
    }

    /**
     * Loads the test workflow
     *
     * @param workflowDir to load the workflow from
     * @return the base node id of the workflow
     * @throws Exception
     */
    private NodeID loadAndSetWorkflow(final File workflowDir) throws Exception {
        WorkflowManager m = loadWorkflow(workflowDir, new ExecutionMonitor()).getWorkflowManager();
        setManager(m);
        return m.getID();
    }

    private void setManager(final WorkflowManager manager) {
        m_manager = manager;
    }

    /**
     * When {@link #loadAndSetWorkflow} has been called, use this method to access the manager
     *
     * @return the set workflow manager
     */
    protected WorkflowManager getManager() {
        return m_manager;
    }

    private WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec) throws Exception {
        return loadWorkflow(workflowDir, exec, DataContainerSettings.getDefault());
    }

    private WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec,
        final DataContainerSettings settings) throws Exception {
        return loadWorkflow(workflowDir, exec, settings, null);
    }

    private WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec,
        final DataContainerSettings settings, final Path mountpointRoot) throws Exception {
        final WorkflowContextV2 workflowContext;
        if (mountpointRoot == null) {
            workflowContext = WorkflowContextV2.forTemporaryWorkflow(workflowDir.toPath(), null);
        } else {
            workflowContext = WorkflowContextV2.builder() //
                .withAnalyticsPlatformExecutor(builder -> builder //
                    .withCurrentUserAsUserId() //
                    .withLocalWorkflowPath(workflowDir.toPath().toAbsolutePath())
                    .withMountpoint("LOCAL", mountpointRoot.toAbsolutePath())) //
                .withLocalLocation() //
                .build();
        }
        return loadWorkflow(workflowDir, exec, new WorkflowLoadHelper(workflowContext, settings) {

            @Override
            public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
                final LoadVersion workflowKNIMEVersion, final Version createdByKNIMEVersion,
                final boolean isNightlyBuild) {
                return UnknownKNIMEVersionLoadPolicy.Try;
            }
        });
    }

    private WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec,
        final WorkflowLoadHelper loadHelper) throws Exception {
        WorkflowLoadResult loadResult = WorkflowManager.ROOT.load(workflowDir, exec, loadHelper, false);
        WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new Exception("Errors reading workflow: " + loadResult.getFilteredError("", LoadResultEntryType.Ok));
        } else if (loadResult.getType() != LoadResultEntryType.Ok) {
            m_logger.info("Errors reading workflow (proceeding anyway): ");
            dumpLineBreakStringToLog(loadResult.getFilteredError("", LoadResultEntryType.Warning));
        }
        return loadResult;
    }

    private void dumpLineBreakStringToLog(final String s) throws IOException {
        try (var r = new BufferedReader(new StringReader(s))) {
            String line;
            while ((line = r.readLine()) != null) {
                m_logger.info(line);
            }
        }
    }

    private File getWorkflowDirectory() throws Exception {
        String wkfDirName = getNameOfWorkflowDirectory();
        return getWorkflowDirectory(wkfDirName);
    }

    /**
     * Name of directory containing the workflow, defaults to the current's class simple name with first letter in lower
     * case.
     *
     * @return workflow directory name
     */
    private String getNameOfWorkflowDirectory() {
        return StringUtils.uncapitalize(getClass().getSimpleName());
    }

    private File getWorkflowDirectory(final String pathRelativeToTestClass) throws Exception {
        ClassLoader l = getClass().getClassLoader();
        String workflowDirString = getClass().getPackage().getName();
        String workflowDirPath = workflowDirString.replace('.', '/');
        workflowDirPath = workflowDirPath.concat("/" + pathRelativeToTestClass);
        URL workflowURL = l.getResource(workflowDirPath);
        if (workflowURL == null) {
            throw new Exception("Can't load workflow that's expected to be in directory " + workflowDirPath);
        }

        if (!"file".equals(workflowURL.getProtocol())) {
            workflowURL = FileLocator.resolve(workflowURL);
        }

        File workflowDir = new File(workflowURL.getFile());
        if (!workflowDir.isDirectory()) {
            throw new Exception("Can't load workflow directory: " + workflowDir);
        }
        return workflowDir;
    }

}

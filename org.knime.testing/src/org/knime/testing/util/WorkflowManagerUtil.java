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
 *   Oct 4, 2021 (hornm): created
 */
package org.knime.testing.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;

/**
 * Utilities for tests using functionality related to the {@link WorkflowManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class WorkflowManagerUtil {

    static {
        try {
            NodeFactoryProvider.getInstance();
        } catch (IllegalStateException e) { // NOSONAR
            // HACK to make tests work in the build system where the org.knime.workbench.repository plugin
            // is not present (causes an exception on the first call
            // 'Invalid extension point: org.knime.workbench.repository.nodes')
        }
    }

    private static WorkflowLoadResult loadWorkflow(final File workflowDir, final WorkflowContextV2 workflowContext)
            throws IOException, InvalidSettingsException, CanceledExecutionException,
            UnsupportedWorkflowVersionException, LockFailedException {
        final var loadHelper = new TryAlwaysWorkflowLoadHelper() {
            @Override
            public WorkflowContextV2 getWorkflowContext() {
                return workflowContext;
            }
        };

        return WorkflowManager.loadProject(workflowDir, new ExecutionMonitor(), loadHelper);
    }

    /**
     * Loads a workflow into memory and sets a {@link WorkflowContextV2 workflow context} claiming that the workflow
     * lives on a {@code LOCAL} mountpoint with the given root directory.
     *
     * @param workflowDir
     * @param workspaceDir
     * @return the loaded workflow
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws UnsupportedWorkflowVersionException
     * @throws LockFailedException
     */
    public static WorkflowManager loadWorkflowInWorkspace(final Path workflowDir, final Path workspaceDir)
        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException,
        LockFailedException {
        return loadWorkflowInWorkspaceAndReturnLoadResult(workflowDir, workspaceDir).getWorkflowManager();
    }

    /**
     * Loads a workflow into memory and sets a {@link WorkflowContextV2 workflow context} claiming that the workflow
     * lives on a {@code LOCAL} mountpoint with the given root directory.
     *
     * @param workflowDir
     * @param workspaceDir
     * @return the {@link WorkflowLoadResult}
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws UnsupportedWorkflowVersionException
     * @throws LockFailedException
     */
    public static WorkflowLoadResult loadWorkflowInWorkspaceAndReturnLoadResult(final Path workflowDir,
        final Path workspaceDir) throws IOException, InvalidSettingsException, CanceledExecutionException,
        UnsupportedWorkflowVersionException, LockFailedException {
        return loadWorkflow(workflowDir.toFile(), WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withCurrentUserAsUserId() //
                .withLocalWorkflowPath(workflowDir.toAbsolutePath()) //
                .withMountpoint("LOCAL", workspaceDir.toAbsolutePath())) //
            .withLocalLocation() //
            .build());
    }

    /**
     * Loads a workflow into memory. Mainly copied from {@code org.knime.testing.core.ng.WorkflowLoadTest}.
     *
     * @param workflowDir
     * @return the loaded workflow
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws UnsupportedWorkflowVersionException
     * @throws LockFailedException
     */
    public static WorkflowManager loadWorkflow(final File workflowDir) throws IOException, InvalidSettingsException,
        CanceledExecutionException, UnsupportedWorkflowVersionException, LockFailedException {
        return loadWorkflow(workflowDir, WorkflowContextV2.forTemporaryWorkflow(workflowDir.toPath(), null)).getWorkflowManager();
    }

    /**
     * Helper to create an empty workflow.
     *
     * @return the new workflow manager without any nodes
     * @throws IOException
     */
    public static WorkflowManager createEmptyWorkflow() throws IOException {
        var dir = FileUtil.createTempDir("workflow");
        var workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        if (workflowFile.createNewFile()) {
            return WorkflowManager.ROOT.createAndAddProject("workflow", new WorkflowCreationHelper(
                WorkflowContextV2.forTemporaryWorkflow(workflowFile.getParentFile().toPath(), null)));
        } else {
            throw new IllegalStateException("Creating empty workflow failed");
        }
    }

    /**
     * Disposes the given workflow project.
     *
     * @param wfm
     */
    public static void disposeWorkflow(final WorkflowManager wfm) {
        var id = wfm.getID();
        if (WorkflowManager.ROOT.containsNodeContainer(id)) {
            WorkflowManager.ROOT.removeProject(id);
        }
    }

    /**
     * Creates a new node using the the given {@link NodeFactory}-instance and adds it to the provided workflow.
     *
     * @param wfm the workflow to add the node to
     * @param factory a factory instance
     * @return the new {@link NativeNodeContainer} instance
     */
    public static NativeNodeContainer createAndAddNode(final WorkflowManager wfm,
        final NodeFactory<? extends NodeModel> factory) {
        if (factory.isLazilyInitialized()) {
            factory.init();
        }
        final var nodeId = wfm.createAndAddNode(factory);
        return (NativeNodeContainer)wfm.getNodeContainer(nodeId);
    }

    /**
     * Creates a new node using the the given {@link NodeFactory}-instance and adds it to the provided workflow.
     *
     * @param wfm the workflow to add the node to
     * @param factory a factory instance
     * @param x node position
     * @param y node position
     * @return the new {@link NativeNodeContainer} instance
     */
    public static NativeNodeContainer createAndAddNode(final WorkflowManager wfm,
        final NodeFactory<? extends NodeModel> factory, final int x, final int y) {
        var nnc = createAndAddNode(wfm, factory);
        nnc.setUIInformation(NodeUIInformation.builder().setNodeLocation(x, y, 10, 10).build());
        return nnc;
    }

    private WorkflowManagerUtil() {
        // utility class
    }

}

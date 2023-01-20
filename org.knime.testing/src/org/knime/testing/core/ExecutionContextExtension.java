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
 *   Jan 6, 2023 (wiswedel): created
 */
package org.knime.testing.core;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;
import org.knime.testing.node.blocking.AbstractBlockingNodeModel;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;
import org.knime.testing.node.blocking.BlockingVariableNodeFactory;

/**
 * Extension used with {@link RegisterExtension} in JUnit5 unit tests to get an {@link ExecutionContext} injected as
 * a parameter in your test method.
 *
 * @author Bernd Wiswedel, KNIME GmbH
 */
public final class ExecutionContextExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    private final String m_blockID =
        ExecutionContextExtension.class.getSimpleName() + "-" + UUID.randomUUID().toString();

    private Path m_workflowDirPath;
    private WorkflowManager m_manager;
    private ExecutionContext m_executionContext;
    private NodeID m_lastAddedNodeID;


    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        BlockingRepository.put(m_blockID, LockedMethod.EXECUTE, new ReentrantLock());
        CheckUtils.checkState(m_manager == null, "WFM is supposed to be null before executing any test");
        m_workflowDirPath = FileUtil.createTempDir("knime-junit-temp-wfm",
            new File(System.getProperty("java.io.tmpdir")), false).toPath();
        var contextV2 = WorkflowContextV2.forTemporaryWorkflow(m_workflowDirPath, null);
        m_manager = WorkflowManager.ROOT.createAndAddProject(
            "JUnit-ExecutionContextExtension", new WorkflowCreationHelper(contextV2));
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        m_manager.shutdown();
        m_manager.getParent().removeNode(m_manager.getID());
        m_manager = null;
        m_executionContext = null;
        FileUtil.deleteRecursively(m_workflowDirPath.toFile());
        m_workflowDirPath = null;
        BlockingRepository.remove(m_blockID, LockedMethod.EXECUTE);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        CheckUtils.checkState(m_manager != null, "%s not properly initialized; is it a static field in %s",
            ExecutionContextExtension.class.getSimpleName(),
            context.getTestClass().map(Class::getName).orElse("<unknown>"));
        m_lastAddedNodeID = m_manager.createAndAddNode(new BlockingVariableNodeFactory());
        final var nodeContainer = m_manager.getNodeContainer(m_lastAddedNodeID, NativeNodeContainer.class, true);
        final var nodeModel = (AbstractBlockingNodeModel)nodeContainer.getNodeModel();
        final var lockIDModel = AbstractBlockingNodeModel.createLockIDModel();
        lockIDModel.setStringValue(m_blockID);
        var nodeSettings = new NodeSettings("settings");
        lockIDModel.saveSettingsTo(nodeSettings);
        nodeModel.loadValidatedSettingsFrom(nodeSettings);
        m_manager.resetAndConfigureAll();
        final var lock = BlockingRepository.get(m_blockID, LockedMethod.EXECUTE).orElseThrow();
        lock.lock(); // NOSONAR - released in #afterEach
        m_manager.executeUpToHere(m_lastAddedNodeID);
        m_executionContext = nodeModel.fetchExecutionContext().orElseThrow();
    }

    /**
     * @return the executionContext
     */
    public ExecutionContext getExecutionContext() {
        return m_executionContext;
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        ReentrantLock lock = BlockingRepository.get(m_blockID, LockedMethod.EXECUTE).orElseThrow();
        lock.unlock();
        m_manager.waitWhileInExecution(5, TimeUnit.SECONDS);
        m_manager.removeNode(m_lastAddedNodeID);
        m_lastAddedNodeID = null;
    }

    /**
     * Method used in a static context, e.g.
     *
     * <pre>
     * @RegisterExtension
     * static ExecutionContextExtension executionContextExtension = ExecutionContextExtension.create();
     * </pre>
     *
     * @return That new context.
     */
    public static ExecutionContextExtension create() {
        return new ExecutionContextExtension();
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return ExecutionContext.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
        throws ParameterResolutionException {
        return getExecutionContext();
    }
}

/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Functions.FailableRunnable;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.rules.TestWatcher;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.ThreadUtils;
import org.knime.core.util.Version;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public abstract class WorkflowTestCase {

    /** Names of workflow manager 'roots' that are used by some framework code, e.g. the parent object of all
     * metanodes in the metanode repository. Used to filter for 'dangling' workflows after test case completion.
     */
    public static final String[] KNOWN_CHILD_WFM_NAME_SUBSTRINGS = new String[]{"MetaNode Repository",
        "Workflow Template Root", "Streamer-Subnode-Parent", WorkflowManager.EXTRACTED_WORKFLOW_ROOT.getName()};

    private static List<NodeContainer> DANGLING_WORKFLOWS = new ArrayList<>();

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private WorkflowManager m_manager;

    private List<NodeID> m_loadedComponentNodeIDs = new ArrayList<>(0);

    protected NodeID loadAndSetWorkflow() throws Exception {
        File workflowDir = getDefaultWorkflowDirectory();
        return loadAndSetWorkflow(workflowDir);
    }

    protected NodeID loadAndSetWorkflow(final File workflowDir) throws Exception {
        WorkflowManager m = loadWorkflow(workflowDir, new ExecutionMonitor()).getWorkflowManager();
        setManager(m);
        return m.getID();
    }

    protected NodeID loadAndSetWorkflow(final DataContainerSettings settings) throws Exception {
        WorkflowManager m =
            loadWorkflow(getDefaultWorkflowDirectory(), new ExecutionMonitor(), settings).getWorkflowManager();
        setManager(m);
        return m.getID();
    }

    protected WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec) throws Exception {
        return loadWorkflow(workflowDir, exec, DataContainerSettings.getDefault());
    }

    protected WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec,
        final DataContainerSettings settings) throws Exception {
        final var workflowContext = WorkflowContextV2.forTemporaryWorkflow(workflowDir.toPath(), null);
        return loadWorkflow(workflowDir, exec, new WorkflowLoadHelper(workflowContext, settings) {

            @Override
            public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
                final LoadVersion workflowKNIMEVersion, final Version createdByKNIMEVersion,
                final boolean isNightlyBuild) {
                return UnknownKNIMEVersionLoadPolicy.Try;
            }
        });
    }


    protected WorkflowLoadResult loadWorkflow(final File workflowDir, final ExecutionMonitor exec,
        final WorkflowLoadHelper loadHelper) throws Exception {
        WorkflowLoadResult loadResult = WorkflowManager.ROOT.load(workflowDir, exec, loadHelper, false);
        WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new Exception("Errors reading workflow: "
                    + loadResult.getFilteredError("", LoadResultEntryType.Ok));
        } else {
            switch (loadResult.getType()) {
            case Ok:
                break;
            default:
                m_logger.info("Errors reading workflow (proceeding anyway): ");
                dumpLineBreakStringToLog(loadResult.getFilteredError("",
                    LoadResultEntryType.Warning));
            }
        }
        return loadResult;
    }
    
    protected MetaNodeLinkUpdateResult loadComponent(final File componentDir, final ExecutionMonitor exec,
            final WorkflowLoadHelper loadHelper) throws IOException, InvalidSettingsException,
            CanceledExecutionException, UnsupportedWorkflowVersionException {
        URI componentURI = componentDir.toURI();
        TemplateNodeContainerPersistor loadPersistor = loadHelper.createTemplateLoadPersistor(componentDir,
                componentURI);
        MetaNodeLinkUpdateResult loadResult = new MetaNodeLinkUpdateResult(
                "Shared instance from \"" + componentURI + "\"");
        WorkflowManager.ROOT.load(loadPersistor, loadResult, exec, false);
        m_loadedComponentNodeIDs.add(loadResult.getLoadedInstance().getID());
        return loadResult;
    }

    /**
     * @return
     * @throws Exception
     * @throws IOException */
    protected File getDefaultWorkflowDirectory() throws Exception {
        String classSimpleName = getClass().getSimpleName();
        classSimpleName = classSimpleName.substring(0, 1).toLowerCase() + classSimpleName.substring(1);
        return getWorkflowDirectory(classSimpleName);
    }

    protected File getWorkflowDirectory(final String pathRelativeToTestClass) throws Exception {
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
            throw new Exception("Can't load workflow directory: "
                    + workflowDir);
        }
        return workflowDir;
    }

    /**
     * @param manager the manager to set
     */
    protected void setManager(final WorkflowManager manager) {
        m_manager = manager;
    }

    /**
     * @return the manager
     */
    protected WorkflowManager getManager() {
        return m_manager;
    }

    protected void checkState(final NodeID id,
            final InternalNodeContainerState... expected) throws Exception {
        NodeContainer nc = findNodeContainer(id);
        checkState(nc, expected);
    }

    protected void checkStateOfMany(final InternalNodeContainerState state, final NodeID... ids) throws Exception {
        try (WorkflowLock lock = getManager().lock()) {
            for (NodeID id : ids) {
                checkState(id, state);
            }
        }
    }

    protected void checkState(final NodeContainer nc,
            final InternalNodeContainerState... expected) throws Exception {
        try (WorkflowLock lock = nc.getParent().lock()) {
            InternalNodeContainerState actual = nc.getInternalState();
            boolean matches = false;
            for (InternalNodeContainerState s : expected) {
                if (actual.equals(s)) {
                    matches = true;
                }
            }
            if (!matches) {
                String error = "node " + nc.getNameWithID() + " has wrong state; "
                + "expected (any of) " + Arrays.toString(expected) + ", actual "
                + actual + " (dump follows)";
                m_logger.info("Test failed: " + error);
                // don't use m_manager as corresponding project - some tests load 50+ workflows and
                // don't use the field in this superclass
                WorkflowManager project = nc instanceof WorkflowManager ? (WorkflowManager)nc : nc.getParent();
                project = project.getProjectWFM();
                dumpWorkflowToLog(project);
                org.junit.Assert.fail(error);
            }
        }
    }

    protected void checkMetaOutState(final NodeID metaID, final int portIndex,
            final InternalNodeContainerState... expected) throws Exception {
        NodeContainer nc = findNodeContainer(metaID);
        if (!(nc instanceof WorkflowManager)) {
            throw new IllegalArgumentException("Node with ID " + metaID
                    + " is not a meta node: " + nc.getNameWithID());
        }
        WorkflowOutPort p = ((WorkflowManager)nc).getOutPort(portIndex);
        InternalNodeContainerState actual = p.getNodeState();
        boolean matches = false;
        for (InternalNodeContainerState s : expected) {
            if (actual.equals(s)) {
                matches = true;
            }
        }
        if (!matches) {
            String error = "Workflow outport " + portIndex + " of WFM "
                + nc.getNameWithID() + " has wrong state; expected (any of) "
                + Arrays.toString(expected) + ", actual " + actual
                + " (dump follows)";
            m_logger.info("Test failed: " + error);
            dumpWorkflowToLog((WorkflowManager)nc);
            Assert.fail(error);
        }
    }

    /** Find the workflow manager that directly contains the node with the argument ID.
     * @param id ID to query
     * @return parent workflow of node with ID (might be meta node or wfm in subnode).
     */
    protected WorkflowManager findParent(final NodeID id) {
        CheckUtils.checkArgumentNotNull(m_manager, "WFM not set");
        final NodeID mgrID = m_manager.getID();
        CheckUtils.checkArgument(id.hasPrefix(mgrID), "NodeID %s has not same prefix as WFM: %s", id, mgrID);

        // this is tricky since we have to deal with subnode containers also. subnode contain a wfm with id suffix "0".
        // this could be a node to query in a subnode
        // 0       - ID of m_manager
        // 0:1     - ID of contained subnode container
        // 0:1:0   - ID of WFM in subnode container
        // 0:1:0:2 - ID of node within subnode

        // here is node contained in a meta node
        // 0       - ID of m_manager
        // 0:2     - ID of contained meta node / wfm
        // 0:1:2   - ID of node within meta node

        Stack<NodeID> prefixStack = new Stack<>();
        NodeID currentID = id;
        while (!currentID.hasSamePrefix(mgrID)) {
            currentID = currentID.getPrefix();
            prefixStack.push(currentID);
        }
        NodeContainerParent currentParent = m_manager;
        while (!prefixStack.isEmpty()) {
            if (currentParent instanceof WorkflowManager) {
                currentParent = ((WorkflowManager)currentParent).getNodeContainer(
                    prefixStack.pop(), NodeContainerParent.class, true);
            } else if (currentParent instanceof SubNodeContainer) {
                SubNodeContainer subnode = (SubNodeContainer)currentParent;
                NodeID expectedWFMID = prefixStack.pop();
                final WorkflowManager innerWFM = subnode.getWorkflowManager();
                Assert.assertEquals(innerWFM.getID(), expectedWFMID);
                currentParent = innerWFM;
            }
        }
        return (WorkflowManager)currentParent;
    }

    protected NodeContainer findNodeContainer(final NodeID id) {
        WorkflowManager parent = findParent(id);
        return parent.getNodeContainer(id);
    }

    protected ConnectionContainer findInConnection(final NodeID id,
            final int port)
        throws Exception {
        WorkflowManager parent = findParent(id);
        for (ConnectionContainer cc : parent.getConnectionContainers()) {
            if (cc.getDest().equals(id) && cc.getDestPort() == port) {
                return cc;
            }
        }
        return null;
    }

    protected Collection<IWriteFileStoreHandler> getWriteFileStoreHandlers() {
        WorkflowDataRepository dataRepository = m_manager.getWorkflowDataRepository();
        return dataRepository.getWriteFileStoreHandlers();
    }

    protected File getFileStoresDirectory(final NodeID id) throws Exception {
        NodeContainer nc = findNodeContainer(id);
        if (nc instanceof NativeNodeContainer) {
            IFileStoreHandler fsh = ((NativeNodeContainer)nc).getNode().getFileStoreHandler();
            if (fsh instanceof IWriteFileStoreHandler) {
                return ((IWriteFileStoreHandler)fsh).getBaseDir();
            }
        }
        return null;
    }

    protected File getArtifactsDirectory(final File workflowDirectory) {
        return new File(workflowDirectory, WorkflowSaveHook.ARTIFACTS_FOLDER_NAME);
    }

    protected static int countFilesInDirectory(final File directory) {
        return countFilesInDirectory(directory, null);
    }
    
    /** Recursively counts files in a dir adhering to the given predicte (may be null) */
    protected static int countFilesInDirectory(final File directory, final Predicate<File> matcherPredicate) {
        Predicate<File> predicate = matcherPredicate != null ? matcherPredicate : f -> true;
        int count = 0;
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                count += countFilesInDirectory(child, predicate);
            } else {
                if (predicate.test(child)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    protected static Collection<SingleNodeContainer> iterateSNCs(final WorkflowManager wfm, final boolean recurse) {
        ArrayList<SingleNodeContainer> result = new ArrayList<SingleNodeContainer>();
        for (NodeContainer nc : wfm.getNodeContainers()) {
            if (nc instanceof SingleNodeContainer) {
                result.add((SingleNodeContainer)nc);
            } else if (recurse) {
                WorkflowManager m;
                if (nc instanceof WorkflowManager) {
                    m = (WorkflowManager)nc;
                } else {
                    m = ((SubNodeContainer)nc).getWorkflowManager();
                }
                result.addAll(iterateSNCs(m, true));
            }
        }
        return result;
    }

    protected ConnectionContainer findLeavingWorkflowConnection(final NodeID id,
            final int port) throws Exception {
        NodeContainer nc = findNodeContainer(id);
        if (!(nc instanceof WorkflowManager)) {
            throw new IllegalArgumentException("Node " + id
                    + " is not a workflow manager");
        }
        for (ConnectionContainer cc
                : ((WorkflowManager)nc).getConnectionContainers()) {
            if (cc.getDest().equals(id) && cc.getDestPort() == port) {
                return cc;
            }
        }
        return null;
    }

    protected void executeAllAndWait() throws Exception {
        m_manager.getParent().executeUpToHere(m_manager.getID());
        waitWhileInExecution();
    }

    protected void executeAndWait(final NodeID... ids)
        throws Exception {
        executeDontWait(ids);
        for (NodeID id : ids) {
            waitWhileNodeInExecution(id);
        }
    }

    protected void executeDontWait(final NodeID... ids) {
        NodeID prefix = null;
        WorkflowManager parent = null;
        for (NodeID id : ids) {
            if (prefix == null) {
                prefix = id.getPrefix();
                parent = findParent(id);
            } else if (!prefix.equals(id.getPrefix())) {
                throw new IllegalArgumentException("Mixing NodeIDs of "
                        + "different levels " + Arrays.toString(ids));
            }
        }
        if (parent != null) {
            parent.executeUpToHere(ids);
        }
    }

    protected void reset(final NodeID... ids) {
        for (NodeID id : ids) {
            WorkflowManager parent = findParent(id);
            parent.resetAndConfigureNode(id);
        }
    }

    protected void deleteConnection(final NodeID destination, final int destPort) {
        WorkflowManager parent = findParent(destination);
        final ConnectionContainer inConn =
            parent.getIncomingConnectionFor(destination, destPort);
        parent.removeConnection(inConn);
    }

    protected void waitWhileInExecution() throws Exception {
        assert !m_manager.isLockedByCurrentThread();
        waitWhileNodeInExecution(m_manager);
    }

    protected void waitWhileNodeInExecution(final NodeID id) throws Exception {
        waitWhileNodeInExecution(findNodeContainer(id));
    }

    protected void waitWhileNodeInExecution(final NodeContainer node)
    throws Exception {
        waitWhile(node, nc -> nc.getInternalState().isExecutionInProgress(), -1);
    }

    /**
     *  Pause and wait while a node is not in a certain state.
     * 
     * @param nodeID The node of interest (only to fetch the corresponding NodeContainer)
     * @param hold The predicate on the NodeContainer associated with the ID, as long as it's test-method returns true
     * the method won't return (unless a timeout occurs)
     * @param secondsToWaitAtMost The seconds to wait at most, negative value for infinite wait 
     * @throws Exception
     */
    protected void waitWhile(NodeID nodeID, final Predicate<NodeContainer> hold, int secondsToWaitAtMost) throws Exception {
        waitWhile(findNodeContainer(nodeID), hold, secondsToWaitAtMost);
    }
    
    /**
     * Similar to {@link #waitWhile(NodeID, Predicate, int)}, except that the NodeContainer is passed as argument.
     */
    protected void waitWhile(NodeContainer nc, final Predicate<NodeContainer> hold, int secondsToWaitAtMost) throws Exception {
        if (!hold.test(nc)) {
            return;
        }

        final ReentrantLock lock = nc instanceof WorkflowManager ? ((WorkflowManager)nc).getReentrantLockInstance()
            : nc.getParent().getReentrantLockInstance();
        final Condition condition = lock.newCondition();
        NodeStateChangeListener l = new NodeStateChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final NodeStateEvent state) {
                lock.lock();
                try {
                    m_logger.info("Received " + state);
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };
        lock.lock();
        nc.addNodeStateChangeListener(l);
        try {
        	long millisRemaining = TimeUnit.SECONDS.toMillis(secondsToWaitAtMost);
            while (hold.test(nc)) {
                if (secondsToWaitAtMost > 0) {
                	if (millisRemaining < 0L) {
                		break;
                	}
                	final var start = System.currentTimeMillis();
                    condition.await(millisRemaining, TimeUnit.MILLISECONDS);
                    millisRemaining = millisRemaining - (System.currentTimeMillis() - start);
                } else {
                    condition.await(10, TimeUnit.SECONDS); // do another iteration (no break)
                }
            }
        } finally {
            lock.unlock();
            nc.removeNodeStateChangeListener(l);
        }
    }

	/**
	 * Calls a workflow-modifying method on the workflow and returns only after the
	 * async workflow listener event was sent.
	 * 
	 * <p>
	 * This method was added as part of AP-21327 - test cases would modify the
	 * workflow and perform another action immediately afterwards. The state of the
	 * workflow is then changed async'ly due to a delayed workflow event. So far
	 * it's an artifact of the test since workflow modifications involve user
	 * interaction.
	 * 
	 * @param manager  To register on
	 * @param runnable A runnable that changes the manager, e.g. adds a node, marks
	 *                 it dirty, swaps connection
	 * @throws Exception If the runnable throws an exception.
	 * @deprecated Only added temporarily on 2023-12 and 2023-07 branches, fixed
	 *             differently on master, see AP-20402
	 * 
	 */
    @Deprecated(forRemoval = true)
	static void doAndWaitForWorkflowEvent(final WorkflowManager manager, FailableRunnable<Exception> runnable)
			throws Exception {
		final Object waitable = new Object();
		manager.addListener(e -> {
			synchronized (waitable) {
				waitable.notifyAll();
			}
		});
		synchronized (waitable) {
			runnable.run();
			waitable.wait();
		}
	}

	@After
    public void tearDown() throws Exception {
        closeWorkflow();
        
        // dispose loaded components
        for (NodeID id : m_loadedComponentNodeIDs) {
            WorkflowManager.ROOT.removeProject(id);
        }

        // Executed after each test, checks that there are no open workflows dangling around.
        final List<NodeContainer> newDanglingWorkflows = getDanglingWorkflows();
        newDanglingWorkflows.removeAll(DANGLING_WORKFLOWS);
        DANGLING_WORKFLOWS = getDanglingWorkflows();
        assertTrue(newDanglingWorkflows.size() + " new dangling workflow(s) detected: " + newDanglingWorkflows,
            newDanglingWorkflows.isEmpty());
    }

    private static ArrayList<NodeContainer> getDanglingWorkflows() {
        return WorkflowManager.ROOT.getNodeContainers().stream()
            .filter(nc -> !StringUtils.containsAny(nc.getName(), WorkflowTestCase.KNOWN_CHILD_WFM_NAME_SUBSTRINGS))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * @throws Exception
     */
    protected void closeWorkflow() throws Exception {
        if (m_manager != null) {
            // in most cases we wait for individual nodes to finish. This
            // does not mean that the workflow state is also updated, give
            // it a second to finish its cleanup
            waitWhile(m_manager, mgr -> mgr.getInternalState().isExecutionInProgress(), 2);
            if (!WorkflowManager.ROOT.canRemoveNode(m_manager.getID())) {
                String error = "Cannot remove workflow, dump follows";
                m_logger.error(error);
                dumpWorkflowToLog(m_manager);
                m_logger.info("########## JVM Call Stacks (start) ##########");
                dumpLineBreakStringToLog(ThreadUtils.getJVMStacktraces());
                m_logger.info("########## JVM Call Stacks (end) ############");
                dumpLineBreakStringToLog(error);
                fail(error);
            }
            WorkflowManager.ROOT.removeProject(m_manager.getID());
            setManager(null);
        }
    }

    protected int getNrTablesInGlobalRepository() {
        return m_manager.getWorkflowDataRepository().getGlobalTableRepository().size();
    }

    protected void dumpWorkflowToLog() throws IOException {
        dumpWorkflowToLog(m_manager);
    }

    protected void dumpWorkflowToLog(final WorkflowManager manager) throws IOException {
        String toString = manager.printNodeSummary(manager.getID(), 0);
        dumpLineBreakStringToLog(toString);
    }

    protected void dumpLineBreakStringToLog(final String s) throws IOException {
        BufferedReader r = new BufferedReader(new StringReader(s));
        String line;
        while ((line = r.readLine()) != null) {
            m_logger.info(line);
        }
        r.close();
    }

    /** @return the logger set up for the concreate test case class. */
    protected final NodeLogger getLogger() {
        return m_logger;
    }

    /** Utility 'rule' that will cause test failures to log the current call stack to NodeLogger.error IF the
     * failure cause is of a certain exception class.
     * 
     * <p>
     * To be used like...
     * 
     * <pre>
     * &#064;Rule(order = Integer.MIN_VALUE)
     * public TestRule m_dumpCallStackOnErrorRule = new DumpCallStackOnErrorRule(TestTimedOutException.class);
     * </pre>
     */
    protected final class DumpCallStackOnErrorRule extends TestWatcher {

        private Class<? extends Throwable>[] m_triggeringExceptionClasses;

        @SafeVarargs
        DumpCallStackOnErrorRule(final Class<? extends Throwable>... triggeringExceptionClass) {
            m_triggeringExceptionClasses = triggeringExceptionClass;
        }

        protected void failed(Throwable e, org.junit.runner.Description description) {
            boolean shouldLog = m_triggeringExceptionClasses.length == 0 ||
                    Arrays.stream(m_triggeringExceptionClasses).anyMatch(t -> t.isAssignableFrom(e.getClass()));
            if (shouldLog) {
                String jvmStacktraces = ThreadUtils.getJVMStacktraces();
                NodeLogger logger = NodeLogger.getLogger(WorkflowTestCase.this.getClass());
                // logger has a limit of 10k chars -- can't dump all into one
                logger.errorWithFormat("---- BEGIN Thread Dump On %s ----", e.getClass().getSimpleName());
                Arrays.stream(jvmStacktraces.split("\n")).forEach(logger::error);
                logger.errorWithFormat("----- END Thread Dump On %s -----", e.getClass().getSimpleName());
            }
        }
    }

}

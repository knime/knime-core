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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pointer;

/**
 * Tests Save As.
 * @author wiswedel, University of Konstanz
 */
public class Bug_4423_saveDuringResetDeadlock extends WorkflowTestCase {

    private NodeID m_dataGenerator1;
    private NodeID m_tableView2;
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
        initFlow();
    }

    /**
     * @throws Exception */
    private void initFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowDirTemp);
        m_dataGenerator1 = new NodeID(baseID, 1);
        m_tableView2 = new NodeID(baseID, 2);
    }

    private enum Progress {
        NotStarted,
        Ongoing,
        Done;
    }

    @Test
    public void testExecAfterLoad() throws Exception {
        final Pointer<Exception> throwablePointer = Pointer.newInstance(null);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    runTest(throwablePointer);
                } catch (Exception ex) {
                    throwablePointer.set(ex);
                }
            }
        });
        final Exception exception = throwablePointer.get();
        if (exception != null) {
            throw exception;
        }
    }

    private void runTest(final Pointer<Exception> throwablePointer) throws Exception {
        final Display currentDisplay = Display.getCurrent();
        final Thread displayThread = currentDisplay.getThread();
        final Thread currentThread = Thread.currentThread();
        // reset and save are getting called from UI thread - replicate it here.
        assertTrue("Not executing in display thread: " + currentThread, currentThread == displayThread);

        final WorkflowManager workflowManager = getManager();
        executeAllAndWait();
        final NodeContainer nc = findNodeContainer(m_tableView2);
        final AtomicReference<Progress> saveProgressPointer = new AtomicReference<>(Progress.NotStarted);

        NodeContext.pushContext(nc);
        try {
            AbstractNodeView<?> view = ((NativeNodeContainer)nc).getNode().getView(
                0, "Programmatically opened in test flow");
            Node.invokeOpenView(view, "Programmatically opened in test flow");
        } finally {
            NodeContext.removeLastContext();
        }

        final NodeLogger logger = NodeLogger.getLogger(getClass());
        Runnable saveRunnable = new Runnable() {
            @Override
            public void run() {
                // in the full application (and as part of the bug report) this job is scheduled while the reset is
                // ongoing; note, it's not possible to replicate the exact behavior here as the whole test case is
                // run in the display thread - we have to schedule the job up-front
                Job saveJob = new Job("Workflow Save") {
                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {
                        saveProgressPointer.set(Progress.Ongoing);
                        try {
                            logger.info("Calling save");
                            workflowManager.save(m_workflowDirTemp, new ExecutionMonitor(), true);
                            logger.info("Called save");
                        } catch (Exception e) {
                            throwablePointer.set(e);
                        } finally {
                            saveProgressPointer.set(Progress.Done);
                        }
                        return Status.OK_STATUS;
                    }
                };
                saveJob.schedule();
                long wait = 5000;
                while ((saveJob.getResult() == null) && (wait > 0)) {
                    try {
                        Thread.sleep(250);
                        wait -= 250;
                    } catch (InterruptedException e) {
                        throwablePointer.set(e);
                    }
                }
                if (saveJob.getResult() == null) {
                    saveJob.cancel();
                    throwablePointer.set(new IllegalStateException(
                        "Workflow save job has not finished within 5 secs, very likely because we have a deadlock"));
                }
            }
        };
        // doesn't actually run as this thread is the display thread
        currentDisplay.asyncExec(saveRunnable);
        // this is the display thread, cannot execute async scheduled tasks
        assertEquals(Progress.NotStarted, saveProgressPointer.get());
        logger.info("Calling reset");
        reset(m_dataGenerator1);
        logger.info("Called reset");
        // give save executable time to do its job. In the current (buggy) code this is done indirectly via reset:
//                Semaphore.acquire(long) line: 39
//                JobManager.join(InternalJob) line: 847
//                Bug_4423_saveDuringResetDeadlock$1$1(InternalJob).join() line: 380
//                Bug_4423_saveDuringResetDeadlock$1$1(Job).join() line: 385
//                Bug_4423_saveDuringResetDeadlock$1.run() line: 139
//                RunnableLock.run() line: 35
//                Synchronizer.runAsyncMessages(boolean) line: 135
//                Display.runAsyncMessages(boolean) line: 3717
//                Display.readAndDispatch() line: 3366
//                ViewUtils.invokeAndWaitInEDT(Runnable) line: 156
//                TableContentModel.setDataTable(DataTable) line: 312
//                TableNodeModel.reset() line: 189
//                TableNodeModel(NodeModel).resetModel() line: 784
//                Node.reset() line: 1398
//                NativeNodeContainer.performReset() line: 580
//                NativeNodeContainer(SingleNodeContainer).reset() line: 392
//                WorkflowManager.invokeResetOnSingleNodeContainer(SingleNodeContainer) line: 4350
//                WorkflowManager.resetSuccessors(NodeID, int) line: 4620
//                WorkflowManager.resetSuccessors(NodeID) line: 4589
//                WorkflowManager.resetNodeAndSuccessors(NodeID) line: 4439
//                WorkflowManager.resetAndConfigureNodeAndSuccessors(NodeID, boolean) line: 4571
//                WorkflowManager.resetAndConfigureNode(NodeID) line: 4556
//                Bug_4423_saveDuringResetDeadlock(WorkflowTestCase).reset(NodeID...) line: 388
        // this might change in the future so we let the test case do its job:
        while(!Progress.Done.equals(saveProgressPointer.get())) {
            if (!currentDisplay.readAndDispatch()) {
                currentDisplay.sleep();
            }
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (m_workflowDirTemp != null && m_workflowDirTemp.isDirectory()) {
            FileUtil.deleteRecursively(m_workflowDirTemp);
            m_workflowDirTemp = null;
        }
    }

}

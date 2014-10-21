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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.core.TestrunConfiguration;

/**
 * Executed a workflows and checks if all nodes are executed (except nodes that are supposed to fail). The workflow is
 * canceled if it still running after the configured timeout.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowExecuteTest extends WorkflowTest {
    private static final Timer TIMEOUT_TIMER = new Timer("Workflow watchdog", true);

    private final TestrunConfiguration m_runConfiguration;

    WorkflowExecuteTest(final String workflowName, final IProgressMonitor monitor,
                        final TestrunConfiguration runConfiguration, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_runConfiguration = runConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        TimerTask watchdog = null;
        try {
            final TestflowConfiguration flowConfiguration = new TestflowConfiguration(m_context.getWorkflowManager());

            watchdog = new TimerTask() {
                private final long timeout = ((flowConfiguration.getTimeout() > 0) ? flowConfiguration.getTimeout()
                        : m_runConfiguration.getTimeout()) * 1000;

                private final long startTime = System.currentTimeMillis();

                @Override
                public void run() {
                    try {
                        internalRun();
                    } catch (Exception ex) {
                        result.addError(WorkflowExecuteTest.this, ex);
                    }
                }

                private void internalRun() {
                    if (m_progressMonitor.isCanceled()) {
                        result.addError(WorkflowExecuteTest.this, new InterruptedException("Testflow canceled by user"));
                        m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                        this.cancel();
                    } else if (System.currentTimeMillis() > startTime + timeout) {
                        String status =
                                m_context.getWorkflowManager().printNodeSummary(m_context.getWorkflowManager().getID(),
                                                                                0);
                        String message =
                                "Worklow running longer than " + (timeout / 1000.0) + " seconds.\n" + "Node status:\n"
                                        + status;
                        if (m_runConfiguration.isStacktraceOnTimeout()) {
                            MemoryUsage usage = getHeapUsage();

                            Formatter formatter = new Formatter();
                            formatter.format("Memory usage: %1$,.3f MB max, %2$,.3f MB used, %3$,.3f MB free",
                                usage.getMax() / 1024.0 / 1024.0, usage.getUsed() / 1024.0 / 1024.0,
                                (usage.getMax() - usage.getUsed()) / 1024.0 / 1024.0);
                            message += "\n" + formatter.out().toString();
                            message += "\nThread status:\n" + createStacktrace();
                        }
                        NodeLogger.getLogger(WorkflowExecuteTest.class).info(message);
                        result.addFailure(WorkflowExecuteTest.this, new AssertionFailedError(message));
                        m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                        this.cancel();
                    }
                }
            };

            TIMEOUT_TIMER.schedule(watchdog, 500, 500);
            m_context.getWorkflowManager().executeAllAndWaitUntilDone();
            if (!m_progressMonitor.isCanceled()) {
                checkExecutionStatus(result, m_context.getWorkflowManager(), flowConfiguration);
            }
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            if (watchdog != null) {
                watchdog.cancel();
            }
        }

    }

    private static String createStacktrace() {
        StringBuilder buf = new StringBuilder(4096);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        for (ThreadInfo ti : bean.dumpAllThreads(true, true)) {
            fillStackFromThread(ti, buf);
        }
        return buf.toString();
    }

    private static void fillStackFromThread(final ThreadInfo ti, final StringBuilder buf) {
        buf.append("\"" + ti.getThreadName() + "\" Id=" + ti.getThreadId() + " " + ti.getThreadState());
        if (ti.getLockName() != null) {
            buf.append(" on " + ti.getLockName());
        }
        if (ti.getLockOwnerName() != null) {
            buf.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
        }
        if (ti.isSuspended()) {
            buf.append(" (suspended)");
        }
        if (ti.isInNative()) {
            buf.append(" (in native)");
        }
        buf.append('\n');
        int i = 0;
        for (StackTraceElement ste : ti.getStackTrace()) {
            buf.append("\tat " + ste.toString());
            buf.append('\n');
            if ((i == 0) && (ti.getLockInfo() != null)) {
                Thread.State ts = ti.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        buf.append("\t-  blocked on " + ti.getLockInfo());
                        buf.append('\n');
                        break;
                    case WAITING:
                        buf.append("\t-  waiting on " + ti.getLockInfo());
                        buf.append('\n');
                        break;
                    case TIMED_WAITING:
                        buf.append("\t-  waiting on " + ti.getLockInfo());
                        buf.append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : ti.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    buf.append("\t-  locked " + mi);
                    buf.append('\n');
                }
            }
            i++;
        }

        LockInfo[] locks = ti.getLockedSynchronizers();
        if (locks.length > 0) {
            buf.append("\n\tNumber of locked synchronizers = " + locks.length);
            buf.append('\n');
            for (LockInfo li : locks) {
                buf.append("\t- " + li);
                buf.append('\n');
            }
        }
        buf.append('\n');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "execute workflow";
    }

    private void checkExecutionStatus(final TestResult result, final WorkflowManager wfm,
                                      final TestflowConfiguration flowConfiguration) {
        for (NodeContainer node : wfm.getNodeContainers()) {
            NodeContainerState status = node.getNodeContainerState();

            if (node instanceof SubNodeContainer) {
                checkExecutionStatus(result, ((SubNodeContainer)node).getWorkflowManager(), flowConfiguration);
            } else if (node instanceof WorkflowManager) {
                checkExecutionStatus(result, (WorkflowManager)node, flowConfiguration);
            } else if (node instanceof SingleNodeContainer) {
                if (!status.isExecuted() && !flowConfiguration.nodeMustFail(node.getID())) {
                    NodeMessage nodeMessage = node.getNodeMessage();
                    String error =
                            "Node '" + node.getNameWithID() + "' is not executed. Error message is: "
                                    + nodeMessage.getMessage();
                    result.addFailure(this, new AssertionFailedError(error));
                } else if (status.isExecuted() && flowConfiguration.nodeMustFail(node.getID())) {
                    String error = "Node '" + node.getNameWithID() + "' is executed although it should have failed.";
                    result.addFailure(this, new AssertionFailedError(error));
                }
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }
}

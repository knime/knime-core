/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   18.05.2012 (meinl): created
 */
package org.knime.testing.core;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.LockFailedException;

/**
 * A simple workflow test that runs the workflow and checks if all nodes are
 * executed in the end.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public class SimpleWorkflowTest implements WorkflowTest {
    /**
     * Factory for simple workflow tests.
     */
    public static final WorkflowTestFactory factory =
            new WorkflowTestFactory() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public WorkflowTest createTestcase(final File workflowDir,
                        final File saveLocation) {
                    return new SimpleWorkflowTest(workflowDir, saveLocation);
                }
            };

    /**
     * The maximum runtime for a single testcase in seconds. After the timeout
     * the workflow will be canceled.
     */
    public static final int TIMEOUT = 300;

    private final File m_knimeWorkFlow;

    private final File m_saveLoc;

    /**
     *
     * @param workflowFile the workflow dir
     * @param saveLoc the dir to save the flow into after execution, or
     *            <code>null</code>
     */
    public SimpleWorkflowTest(final File workflowFile, final File saveLoc) {
        m_knimeWorkFlow = workflowFile;
        m_saveLoc = saveLoc;
    }

    private WorkflowManager loadWorkflow(final TestResult result)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException, UnsupportedWorkflowVersionException,
            LockFailedException {
        WorkflowLoadResult loadRes =
                WorkflowManager.loadProject(m_knimeWorkFlow.getParentFile(),
                        new ExecutionMonitor(), WorkflowLoadHelper.INSTANCE);
        if (loadRes.hasErrors()) {
            result.addFailure(
                    this,
                    new AssertionFailedError(loadRes.getFilteredError("",
                            LoadResultEntryType.Error)));
        }
        return loadRes.getWorkflowManager();
    }

    private void runWorkflow(final WorkflowManager manager,
            final TestResult result) {
        manager.executeAllAndWaitUntilDone();

        for (NodeContainer node : manager.getNodeContainers()) {
            State status = node.getState();
            if (!status.equals(State.EXECUTED)) {
                String error =
                        "Node '" + node.getNameWithID() + "' is not executed.";
                NodeMessage nodeMessage = node.getNodeMessage();
                if (nodeMessage != null) {
                    error += "\n" + nodeMessage.getMessage();
                }
                result.addFailure(this, new AssertionFailedError(error));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        WorkflowManager manager = null;
        TimerTask timeout = null;
        try {
            manager = loadWorkflow(result);
            final WorkflowManager m = manager;

            timeout = new TimerTask() {
                @Override
                public void run() {
                    String status = m.printNodeSummary(m.getID(), 0);
                    result.addFailure(SimpleWorkflowTest.this,
                            new AssertionFailedError(
                                    "Worklow running longer than " + TIMEOUT
                                            + " seconds.\n" + status));
                    m.getParent().cancelExecution(m);
                }
            };

            KNIMETimer.getInstance().schedule(timeout, TIMEOUT * 1000);
            runWorkflow(manager, result);

            if (m_saveLoc != null) {
                manager.save(m_saveLoc, new ExecutionMonitor(), true);
            }
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            if (manager != null) {
                manager.shutdown();
                manager.getParent().removeNode(manager.getID());
            }
            result.endTest(this);
            if (timeout != null) {
                timeout.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_knimeWorkFlow.getParentFile().getName();
    }
}

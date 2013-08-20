/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Executed a workflows and checks if all nodes are executed (except nodes that are supposed to fail). The workflow is
 * canceled if it still running after the configured timeout.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowExecuteTest extends WorkflowTest {
    private static final Timer TIMEOUT_TIMER = new Timer("Workflow watchdog", true);

    private final TestrunConfiguration m_runConfiguration;

    private TestflowConfiguration m_flowConfiguration;

    private WorkflowManager m_manager;

    WorkflowExecuteTest(final String workflowName, final TestrunConfiguration runConfiguration) {
        super(workflowName);
        m_runConfiguration = runConfiguration;
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

        TimerTask watchdog = null;
        try {
            m_flowConfiguration = new TestflowConfiguration(m_manager);

            final int timeout =
                    (m_flowConfiguration.getTimeout() > 0) ? m_flowConfiguration.getTimeout() : m_runConfiguration
                            .getTimeout();
            watchdog = new TimerTask() {
                @Override
                public void run() {
                    String status = m_manager.printNodeSummary(m_manager.getID(), 0);
                    result.addFailure(WorkflowExecuteTest.this, new AssertionFailedError("Worklow running longer than "
                            + timeout + " seconds.\n" + status));
                    m_manager.getParent().cancelExecution(m_manager);
                }
            };

            TIMEOUT_TIMER.schedule(watchdog, timeout * 1000);
            m_manager.executeAllAndWaitUntilDone();
            checkExecutionStatus(result, m_manager);
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            if (watchdog != null) {
                watchdog.cancel();
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "execute workflow (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    private void checkExecutionStatus(final TestResult result, final WorkflowManager wfm) {
        for (NodeContainer node : wfm.getNodeContainers()) {
            NodeContainerState status = node.getNodeContainerState();

            if (node instanceof SingleNodeContainer) {
                if (!status.isExecuted() && !m_flowConfiguration.nodeMustFail(node.getID())) {
                    NodeMessage nodeMessage = node.getNodeMessage();
                    String error =
                            "Node '" + node.getNameWithID() + "' is not executed. Error message is: "
                                    + nodeMessage.getMessage();
                    result.addFailure(this, new AssertionFailedError(error));
                } else if (status.isExecuted() && m_flowConfiguration.nodeMustFail(node.getID())) {
                    String error = "Node '" + node.getNameWithID() + "' is executed although it should have failed.";
                    result.addFailure(this, new AssertionFailedError(error));
                }
            } else if (node instanceof WorkflowManager) {
                checkExecutionStatus(result, (WorkflowManager)node);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(final AtomicReference<WorkflowManager> managerRef) {
        m_manager = managerRef.get();
    }
}

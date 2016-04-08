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

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.core.TestrunConfiguration;
import org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJob;
import org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJobManager;

import junit.framework.TestResult;

/**
 * Executes a workflows in streaming mode (i.e. sets for each single node the {@link StreamingTestNodeExecutionJob}) and
 * checks if all nodes are executed (except nodes that are supposed to fail). The workflow is canceled if it still
 * running after the configured timeout.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Martin Horn, University of Konstanz
 */
class WorkflowExecuteStreamingTest extends WorkflowExecuteTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExecuteStreamingTest.class);

    private final File m_workflowDir;
    private final File m_testcaseRoot;

    WorkflowExecuteStreamingTest(final File workflowDir, final File testcaseRoot, final String workflowName,
        final IProgressMonitor monitor, final TestrunConfiguration runConfiguration,
        final WorkflowTestContext context) {
        super(workflowName, monitor, runConfiguration, context);

        m_workflowDir = workflowDir;
        m_testcaseRoot = testcaseRoot;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        try {
            if (m_context.getTestflowConfiguration().runStreamingTest()) {
                LOGGER.info("Loading workflow '" + m_workflowName + "' for streaming test");
                WorkflowManager wfm =
                    WorkflowLoadTest.loadWorkflow(this, result, m_workflowDir, m_testcaseRoot, m_runConfiguration);
                m_context.setWorkflowManager(wfm);

                super.run(result);

                WorkflowCloseTest.closeWorkflow(this, result, m_context);
            } else {
                result.startTest(this);
                ((WorkflowTestResult) result).testIgnored(this);
                result.endTest(this);
            }
        } catch (Throwable t) {
            result.addError(this, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "execute workflow in streaming mode";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureWorkflowManager(final WorkflowManager wfm) throws InvalidSettingsException {
        try (WorkflowLock lock = null) {
            for (NodeContainer node : wfm.getNodeContainers()) {
                NodeContainerState status = node.getNodeContainerState();

                if (node instanceof SubNodeContainer) {
                    configureWorkflowManager(((SubNodeContainer)node).getWorkflowManager());
                } else if (node instanceof WorkflowManager) {
                    configureWorkflowManager((WorkflowManager)node);
                } else if (node instanceof SingleNodeContainer) {

                    //only set the streaming executor if not loop start or end node and node is not executed
                    if (!status.isExecuted() && !((SingleNodeContainer)node).isModelCompatibleTo(LoopStartNode.class)
                        && !((SingleNodeContainer)node).isModelCompatibleTo(LoopEndNode.class)) {
                        wfm.setJobManager(node.getID(), new StreamingTestNodeExecutionJobManager());
                    }
                }
            }
        }
    }
}

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

import java.io.File;
import java.io.IOException;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.LockFailedException;

/**
 * Testcase that monitors loading a workflow. Errors and if desired also warnings during load are reported as failures.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowLoadTest extends WorkflowTest {
    private final File m_workflowDir;

    private final File m_testcaseRoot;

    private final TestrunConfiguration m_runConfiguration;

    /**
     * Creates a new test for loading a workflow.
     *
     * @param workflowDir the workflow dir
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param workflowName a unique name for the workflow
     * @param monitor a progress monitor, may be <code>null</code>
     * @param runConfiguration the run configuration
     * @param context the test context, must not be <code>null</code>
     */
    public WorkflowLoadTest(final File workflowDir, final File testcaseRoot, final String workflowName,
                            final IProgressMonitor monitor, final TestrunConfiguration runConfiguration,
                            final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_workflowDir = workflowDir;
        m_testcaseRoot = testcaseRoot;
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
        try {
            if (m_context.getWorkflowManager() == null) {
                m_context.setWorkflowManager(loadWorkflow(result));
            }
            recordPreExecutedNodes(m_context.getWorkflowManager());
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "load workflow (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    private WorkflowManager loadWorkflow(final TestResult result) throws IOException, InvalidSettingsException,
            CanceledExecutionException, UnsupportedWorkflowVersionException, LockFailedException {
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper() {
            /**
             * {@inheritDoc}
             */
            @Override
            public WorkflowContext getWorkflowContext() {
                WorkflowContext.Factory fac = new WorkflowContext.Factory(m_workflowDir);
                fac.setMountpointRoot(m_testcaseRoot);
                return fac.createContext();
            }
        };

        WorkflowLoadResult loadRes = WorkflowManager.loadProject(m_workflowDir, new ExecutionMonitor(), loadHelper);
        if (loadRes.hasErrors()) {
            result.addFailure(this, new AssertionFailedError(loadRes.getFilteredError("", LoadResultEntryType.Error)));
        }
        if (m_runConfiguration.isCheckForLoadWarnings() && loadRes.hasWarningEntries()) {
            result.addFailure(this, new AssertionFailedError(loadRes.getFilteredError("", LoadResultEntryType.Warning)));
        }

        return loadRes.getWorkflowManager();
    }

    private void recordPreExecutedNodes(final WorkflowManager manager) {
        for (NodeContainer node : manager.getNodeContainers()) {
            if (node instanceof SingleNodeContainer) {
                if (((SingleNodeContainer) node).getNodeContainerState().isExecuted()) {
                    m_context.addPreExecutedNode(node);
                }
            } else if (node instanceof WorkflowManager) {
                recordPreExecutedNodes((WorkflowManager)node);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }
}

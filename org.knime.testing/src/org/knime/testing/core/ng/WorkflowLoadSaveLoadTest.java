/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *   23.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;

import junit.framework.TestResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Test that loads the workflow, saves it (potentially converting it into a new format), and finally loading it again.
 * The workflow manager of the second load is then put into the test context for further use.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowLoadSaveLoadTest extends WorkflowTest {
    private final static NodeLogger LOGGER = NodeLogger.getLogger(WorkflowLoadSaveLoadTest.class);

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
    public WorkflowLoadSaveLoadTest(final File workflowDir, final File testcaseRoot, final String workflowName,
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
    public void run(final TestResult result) {
        result.startTest(this);
        try {
            LOGGER.info("Loading workflow '" + m_workflowName + "'");
            WorkflowManager manager =
                    WorkflowLoadTest.loadWorkflow(this, result, m_workflowDir, m_testcaseRoot, m_runConfiguration);
            m_context.setWorkflowManager(manager);

            LOGGER.info("Saving workflow '" + m_workflowName + "'");
            manager.save(m_workflowDir, new ExecutionMonitor(), true);
            manager.shutdown();
            manager.getParent().removeNode(manager.getID());

            LOGGER.info("Loading workflow '" + m_workflowName + "' again");
            manager = WorkflowLoadTest.loadWorkflow(this, result, m_workflowDir, m_testcaseRoot, m_runConfiguration);

            m_context.setWorkflowManager(manager);
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
        return "load-save-load workflow";
    }
}

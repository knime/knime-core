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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestResult;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class WorkflowTestSuite extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowTestSuite.class);

    private final File m_knimeWorkFlow;

    private final File m_testcaseRoot;

    private final TestrunConfiguration m_runConfiguration;

    private final List<WorkflowTest> m_allTests = new ArrayList<WorkflowTest>(8);

    /**
     * Creates a new suite of workflow tests. Which tests are actually executed is determined by the given run
     * configuration.
     *
     * @param workflowFile the workflow dir
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param runConfig run configuration for all test flows
     */
    public WorkflowTestSuite(final File workflowFile, final File testcaseRoot, final TestrunConfiguration runConfig) {
        super(workflowFile.getParentFile().getAbsolutePath().substring(testcaseRoot.getAbsolutePath().length() + 1));
        m_knimeWorkFlow = workflowFile;
        m_testcaseRoot = testcaseRoot;
        m_runConfiguration = runConfig;

        m_allTests.add(new WorkflowLoadTest(m_knimeWorkFlow, m_testcaseRoot, m_workflowName, m_runConfiguration));
        if (m_runConfiguration.isReportDeprecatedNodes()) {
            m_allTests.add(new WorkflowDeprecationTest(m_workflowName));
        }
        Map<SingleNodeContainer, List<AbstractNodeView<? extends NodeModel>>> views =
                new HashMap<SingleNodeContainer, List<AbstractNodeView<? extends NodeModel>>>();
        if (m_runConfiguration.isTestViews()) {
            m_allTests.add(new WorkflowOpenViewsTest(m_workflowName, views));
        }
        m_allTests.add(new WorkflowExecuteTest(m_workflowName, m_runConfiguration));
        m_allTests.add(new WorkflowNodeMessagesTest(m_workflowName));
        if (m_runConfiguration.isTestDialogs()) {
            m_allTests.add(new WorkflowDialogsTest(m_workflowName));
        }
        if (m_runConfiguration.isTestViews()) {
            m_allTests.add(new WorkflowCloseViewsTest(m_workflowName, views));
        }

        if (m_runConfiguration.getSaveLocation() != null) {
            m_allTests.add(new WorkflowSaveTest(m_workflowName, m_runConfiguration, m_testcaseRoot, m_knimeWorkFlow));
        }
        m_allTests.add(new WorkflowCloseTest(m_workflowName));
        if (!m_runConfiguration.isCheckLogMessages()) {
            m_allTests.add(new WorkflowLogMessagesTest(m_workflowName));
        }
        m_allTests.add(new WorkflowUncaughtExceptionsTest(m_workflowName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return m_allTests.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);
        AtomicReference<WorkflowManager> managerRef = new AtomicReference<WorkflowManager>();

        try {
            for (WorkflowTest test : m_allTests) {
                LOGGER.debug("====================== Executing " + test.getName() + " ======================");
                test.setup(managerRef);
                test.run(result);
            }
        } catch (Throwable ex) {
            result.addError(this, ex);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(null);
            result.endTest(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getWorkflowName() + " (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(final AtomicReference<WorkflowManager> managerRef) {
        // nothing to do here
    }
}

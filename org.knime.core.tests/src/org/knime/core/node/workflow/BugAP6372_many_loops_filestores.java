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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Tests a workflow consisting of:<br/>
 * Some-outer-loop-start -&gt; inner_loop1 -&gt; ... -&gt; inner_loop200 -> some-outer-loop-end.
 * The inner loops get some ID assigned and in KNIME 4.0.0 and before this was capped at 128 loops. See AP-6372.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP6372_many_loops_filestores extends WorkflowTestCase {

    private File m_workflowDir;
    private NodeID m_tableCreate_1;
    private NodeID m_outerLoopEnd_3;
    private NodeID m_testFileStore_804;
    private NodeID m_testFileStore_813;

    @Before
    public void setUp() throws Exception {
        // on my (BW) laptop (T470p) this runs in 17s ... but I am forseeing much larger runtimes on test instances
        // running win or mac; given that the bug was in pure Java code I find that an acceptable filter
        Assume.assumeThat("Not run on all systems due to runtime", Platform.getOS(), is(Platform.OS_LINUX));
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        // will save the workflow in one of the test ...don't write SVN folder
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_tableCreate_1 = baseID.createChild(1);
        m_outerLoopEnd_3 = baseID.createChild(3);
        m_testFileStore_804 = baseID.createChild(804);
        m_testFileStore_813 = baseID.createChild(813);
        return loadResult;
    }

    @Test(timeout = 60000L)
    public void testExecuteSaveThenReexecute() throws Exception {
        WorkflowManager manager = getManager();
        checkState(m_tableCreate_1, CONFIGURED);
        executeAllAndWait();
        checkState(m_testFileStore_804, EXECUTED);
        checkState(m_testFileStore_813, EXECUTED);
        checkState(manager, EXECUTED);
        manager.save(m_workflowDir, new ExecutionMonitor(), true);
        closeWorkflow();
        initWorkflowFromTemp();
        manager = getManager();
        assertThat("Workflow dirty state after save and load", manager.isDirty(), is(false));
        checkState(manager, EXECUTED);
        reset(m_testFileStore_804, m_testFileStore_813);
        checkStateOfMany(CONFIGURED, m_testFileStore_804, m_testFileStore_813);
        executeAndWait(m_testFileStore_804, m_testFileStore_813);
        checkStateOfMany(EXECUTED, m_testFileStore_804, m_testFileStore_813);
        checkState(manager, EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (m_workflowDir != null) {
            FileUtil.deleteRecursively(m_workflowDir);
        }
    }

}

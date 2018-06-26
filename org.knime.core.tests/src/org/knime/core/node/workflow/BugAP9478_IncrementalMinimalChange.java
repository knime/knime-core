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

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/** AP-9478: Incrementally saving workflow with small changes to UI will cause NullPointerException
 * https://knime-com.atlassian.net/browse/AP-9478
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP9478_IncrementalMinimalChange extends WorkflowTestCase {

    private File m_workflowDir;
    private NodeID m_dataGenerator_1;
    private NodeID m_createFileStore_4;
    private NodeID m_createBlob_2;

    private NodeID m_testFileStore_3;
    private NodeID m_testBlob_5;

    @Before
    public void setUp() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_dataGenerator_1 = baseID.createChild(1);
        m_createFileStore_4 = baseID.createChild(4);
        m_createBlob_2 = baseID.createChild(2);

        m_testFileStore_3 = baseID.createChild(3);
        m_testBlob_5 = baseID.createChild(5);
        return loadResult;
    }

    /** Load workflow, expect all nodes to be 'yellow', then run to completion. */
    @Test
    public void testLoadRunAllGreen() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_dataGenerator_1, m_createFileStore_4, m_createBlob_2);
        checkState(getManager(), InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    /** Load workflow, execute source nodes, save, close, run to completion */
    @Test
    public void testLoadPartialExecuteSaveExecute() throws Exception {
        executeAndWait(m_createFileStore_4, m_createBlob_2);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_dataGenerator_1, m_createBlob_2, m_createFileStore_4);
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_testFileStore_3, m_testBlob_5);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        closeWorkflow();

        initWorkflowFromTemp();
        Assert.assertThat("Workflow dirty state", getManager().isDirty(), is(false));
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_dataGenerator_1, m_createBlob_2, m_createFileStore_4);
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_testFileStore_3, m_testBlob_5);
        executeAllAndWait();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_testFileStore_3, m_testBlob_5);
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
    }

    /** Load workflow, execute source nodes, save, close, load, modify workflow, run to completion, save. */
    @Test
    public void testLoadPartialExecuteSaveModifyExecute() throws Exception {
        executeAndWait(m_createFileStore_4, m_createBlob_2);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        closeWorkflow();

        initWorkflowFromTemp();
        Assert.assertThat("Workflow dirty state", getManager().isDirty(), is(false));
        findNodeContainer(m_dataGenerator_1).setDirty();
        findNodeContainer(m_createBlob_2).setDirty();
        getManager().save(m_workflowDir, new ExecutionMonitor(), true); // this caused the NPE to be thrown
        closeWorkflow();

        initWorkflowFromTemp();
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDir);
    }

}

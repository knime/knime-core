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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Test save/load of an inactive (executed) node without configuration.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6729_InvalidConfigOnInactiveNodes extends WorkflowTestCase {

    private NodeID m_fileReader2;
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
        init();
    }

    private void init() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDirTemp,
            new ExecutionMonitor(), new ConfigurableWorkflowLoadHelper(m_workflowDirTemp));
        assertThat("Expected non-error/non-warning load result: " + loadResult.getMessage(),
            loadResult.getType(), is(LoadResultEntryType.Ok));
        final WorkflowManager workflowManager = loadResult.getWorkflowManager();
        setManager(workflowManager);
        NodeID baseID = workflowManager.getID();
        m_fileReader2 = new NodeID(baseID, 2);

    }

    /** Just load and double-check state of filereader is idle. */
    @Test
    public void testStateAfterLoad() throws Exception {
        checkState(m_fileReader2, IDLE);
        NodeMessage message = getManager().getNodeContainer(m_fileReader2).getNodeMessage();
        assertEquals(NodeMessage.Type.WARNING, message.getMessageType());
        assertThat("Unexpected warning message", StringUtils.lowerCase(message.getMessage()),
            containsString("no settings"));
    }

    /** Just load and double-check state of filereader is idle. */
    @Test
    public void testStateAfterExecute() throws Exception {
        executeAllAndWait();
        checkState(m_fileReader2, EXECUTED);
        NativeNodeContainer fileReader= getManager().getNodeContainer(m_fileReader2, NativeNodeContainer.class, true);
        assertThat(fileReader.isInactive(), is(Boolean.TRUE));
        NodeMessage message = getManager().getNodeContainer(m_fileReader2).getNodeMessage();
        assertEquals(NodeMessage.Type.RESET, message.getMessageType());
    }

    /** Just load and double-check state of filereader is idle. */
    @Test
    public void testStateAfterExecuteAndLoad() throws Exception {
        executeAllAndWait();
        checkState(m_fileReader2, EXECUTED);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        closeWorkflow();
        assertThat(getManager(), is(nullValue()));
        init();
        checkState(m_fileReader2, EXECUTED);
        NativeNodeContainer fileReader= getManager().getNodeContainer(m_fileReader2, NativeNodeContainer.class, true);
        assertThat(fileReader.isInactive(), is(Boolean.TRUE));
        NodeMessage message = getManager().getNodeContainer(m_fileReader2).getNodeMessage();
        assertEquals(NodeMessage.Type.RESET, message.getMessageType());
    }

    /** {@inheritDoc} */
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

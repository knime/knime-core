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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.io.File;

import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class BlobsInLoops extends WorkflowTestCase {

    /** This property is read in one of the java edit variables denoting a temp path to write to. */
    private static final String SYS_PROP_TMP_PATH = "blobTmpPath";

    private NodeID m_create1;
    private NodeID m_verify2;
    private NodeID m_source3;
    private NodeID m_source8;
    private NodeID m_tableWriter4;
    private NodeID m_tableReader5;
    private NodeID m_verify6;
    private NodeID m_loopEnd14;
    private NodeID m_verify15;

    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDir = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = m_tempFolder.newFolder("workflowCopy");
        File dataFileTemp = m_tempFolder.newFile("test.table"); // a .table file written by the workflow
        System.setProperty(SYS_PROP_TMP_PATH, dataFileTemp.getAbsolutePath());
        FileUtil.copyDir(workflowDir, m_workflowDirTemp);
        init();
    }

    private void init() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowDirTemp);
        m_create1 = baseID.createChild(1);
        m_verify2 = baseID.createChild(2);
        m_source3 = baseID.createChild(3);
        m_source8 = baseID.createChild(8);
        m_tableWriter4 = baseID.createChild(4);
        m_tableReader5 = baseID.createChild(5);
        m_verify6 = baseID.createChild(6);
        m_loopEnd14 = baseID.createChild(14);
        m_verify15 = baseID.createChild(15);
    }

    @Test
    public void testExecuteAll() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_create1, m_source3, m_source8);
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testSimpleExecThenSaveAndLoad() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_create1, m_source3);
        executeAndWait(m_create1, m_tableWriter4);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_create1, m_source3);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        closeWorkflow();
        Assert.assertThat("Workflow is null", getManager(), IsNull.nullValue());
        init();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_create1, m_source3);
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_verify2, m_tableReader5, m_verify6);
        executeAndWait(m_verify6, m_verify2);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_verify2, m_tableReader5, m_verify6);
    }

    @Test
    public void testLoopExecThenSaveAndLoad() throws Exception {
        checkState(m_source8, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_loopEnd14, m_verify15);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_loopEnd14, m_verify15);
        getManager().save(m_workflowDirTemp, new ExecutionMonitor(), true);
        closeWorkflow();
        Assert.assertThat("Workflow is null", getManager(), IsNull.nullValue());
        init();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_loopEnd14, m_verify15);
        reset(m_verify15);
        executeAndWait(m_verify15);
        checkState(m_verify15, InternalNodeContainerState.EXECUTED);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.clearProperty(SYS_PROP_TMP_PATH);
    }

}

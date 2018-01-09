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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;


/** 5510: Loop End node fails to save when all iterations produce empty tables
 * http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5510
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug5510_LoopEndEmptyTable extends WorkflowTestCase {

    private NodeID m_tableCreate_1;
    private NodeID m_loopEnd_Single_3;
    private NodeID m_loopEnd_Double_8;
    private File m_workflowDirTemp;

    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
        initFlow();
    }

    /**
     * @throws Exception */
    private void initFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowDirTemp);
        m_tableCreate_1 = new NodeID(baseID, 1);
        m_loopEnd_Single_3 = new NodeID(baseID, 3);
        m_loopEnd_Double_8 = new NodeID(baseID, 8);
    }

    @Test
    public void testExecuteThenSave() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableCreate_1, m_loopEnd_Single_3, m_loopEnd_Double_8);
        executeAllAndWait();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableCreate_1, m_loopEnd_Single_3, m_loopEnd_Double_8);
        final WorkflowManager mgr = getManager();
        for (NodeID id : Arrays.asList(m_loopEnd_Single_3, m_loopEnd_Double_8)) {
            NodeMessage nodeMessage = mgr.getNodeContainer(id).getNodeMessage();
            assertEquals(NodeMessage.Type.WARNING, nodeMessage.getMessageType());
            assertTrue(String.format("Message for node %s expected to contain word 'empty': \"%s\"",
                id.toString(), nodeMessage.getMessage()), nodeMessage.getMessage().contains("empty"));
        }
        mgr.save(m_workflowDirTemp, new ExecutionMonitor(), true);
        mgr.save(m_workflowDirTemp, new ExecutionMonitor(), true);
        mgr.getParent().removeNode(mgr.getID());
        initFlow();
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

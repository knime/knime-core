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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/** 6680: NPE on saving autobinner model in model to cell node
 * https://bugs.knime.org/show_bug.cgi?id=6680
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6680_autobinner_save_to_cell extends WorkflowTestCase {

    private NodeID m_modelToCell_3;
    private NodeID m_validator_4;

    private File m_workflowTempDir;


    @Before
    public void setUp() throws Exception {
        File workflowDirSVN = getDefaultWorkflowDirectory();
        m_workflowTempDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(workflowDirSVN, m_workflowTempDir);
        init();
    }

    private void init() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowTempDir);
        m_modelToCell_3 = new NodeID(baseID, 3);
        m_validator_4 = new NodeID(baseID, 4);
    }

    @Test
    public void testExecuteAll() throws Exception {
        checkState(getManager(), InternalNodeContainerState.CONFIGURED);
        checkState(m_modelToCell_3, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_validator_4, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecSaveLoad() throws Exception {
        executeAllAndWait();
        getManager().save(m_workflowTempDir, new ExecutionMonitor(), true);
        reset(m_validator_4);
        closeWorkflow();
        init();
        checkState(m_validator_4, InternalNodeContainerState.EXECUTED);
        reset(m_validator_4);
        executeAllAndWait();
        checkState(m_validator_4, InternalNodeContainerState.EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowTempDir);
    }
}

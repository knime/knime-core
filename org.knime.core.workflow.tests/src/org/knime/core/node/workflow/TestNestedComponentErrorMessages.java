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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Test for component error messages.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class TestNestedComponentErrorMessages extends WorkflowTestCase {

	@TempDir
    private File m_workflowDir;

    private NodeID m_component_19;

    private NodeID m_component_20;

    private NodeID m_component_20_0_19;

    private NodeID m_node_20_0_19_0_9;

    private NodeID m_node_19_0_9;

    private NodeID m_baseID;

    private NodeID m_component_22;

    private NodeID m_component_22_0_19;

    private NodeID m_node_22_0_19_0_9;

    /**
     * Creates and copies the workflow into a temporary directory.
     *
     * @throws Exception
     */
    @BeforeEach
    public void setup() throws Exception {
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        m_baseID = getManager().getID();

        m_component_20 = new NodeID(m_baseID, 20);
        m_component_20_0_19 = NodeID.fromString(m_baseID.toString() + ":20:0:19");
        m_node_20_0_19_0_9 = NodeID.fromString(m_baseID.toString() + ":20:0:19:0:9");

        m_component_19 = new NodeID(m_baseID, 19);
        m_node_19_0_9 = NodeID.fromString(m_baseID.toString() + ":19:0:9");

        m_component_22 = new NodeID(m_baseID, 22);
        m_component_22_0_19 = NodeID.fromString(m_baseID.toString() + ":22:0:19");
        m_node_22_0_19_0_9 = NodeID.fromString(m_baseID.toString() + ":22:0:19:0:9");

        return loadResult;
    }

    /**
     * Checks for the correct node messages for the individual nodes.
     *
     * @throws Exception
     */
    @Test
    public void testComponentErrorMessages() throws Exception {
        WorkflowManager wfm = getManager();
        wfm.executeAllAndWaitUntilDone();

        //check error messages after execute
		assertThat("unexpected node message",
				wfm.getNodeContainer(m_component_19).getNodeMessage().toStringWithDetails(),
				allOf(containsString("Fail in execution #9"), containsString("This node fails on each execution.")));
		assertThat("unexpected node message",
				wfm.getNodeContainer(m_component_20).getNodeMessage().toStringWithDetails(),
				containsString("Contains one node with execution failure"));
		assertThat("unexpected node message",
				wfm.findNodeContainer(m_component_20_0_19).getNodeMessage().toStringWithDetails(),
				allOf(containsString("Fail in execution #9"), containsString("This node fails on each execution.")));
        assertThat("unexpected node message", wfm.findNodeContainer(m_node_20_0_19_0_9).getNodeMessage().getMessage(),
            is("Execute failed: This node fails on each execution."));
        assertThat("unexpected node message", wfm.findNodeContainer(m_node_19_0_9).getNodeMessage().getMessage(),
            is("Execute failed: This node fails on each execution."));
		assertThat("unexpected node message",
				wfm.getNodeContainer(m_component_22).getNodeMessage().toStringWithDetails(),
				allOf(containsString("Inner Component #19"),
						containsString("Contains one node with execution failure")));
		assertThat("unexpected node message",
				wfm.findNodeContainer(m_component_22_0_19).getNodeMessage().toStringWithDetails(),
				allOf(containsString("Duplicate Row Filter #9"),
						containsString("At least one column has to be selected for duplicate detection.")));
        assertThat("unexpected node message", wfm.findNodeContainer(m_node_22_0_19_0_9).getNodeMessage().getMessage(),
            is("At least one column has to be selected for duplicate detection."));

    }

}
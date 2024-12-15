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

import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AP-18594: Marking for execution would takes hours before succeeding in case
 * many nested metanodes are contained in workflow
 * https://knime-com.atlassian.net/browse/AP-18594
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP18594_MetaNodes_slowDown_ExecutionOfWFM extends WorkflowTestCase { // NOSONAR

    private NodeID m_dataGenerator_1;
    private NodeID m_tableDiff_41;
    private NodeID m_cache_40;
    private NodeID m_dataGenerator_32_7;

    @BeforeEach
    public void setUp() throws Exception {
        var baseID = loadAndSetWorkflow();
        m_dataGenerator_1 = baseID.createChild(1);
        m_tableDiff_41 = baseID.createChild(41);
        m_cache_40 = baseID.createChild(40);
        m_dataGenerator_32_7 = baseID.createChild(32).createChild(7);
    }

    /** Load workflow, expect all nodes to be 'yellow', then run to completion. */
    @Test
    public void testLoadRunAllGreen() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_dataGenerator_1, m_tableDiff_41, m_cache_40);
        checkState(getManager(), InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(getManager(), EXECUTED);
    }

	/**
	 * Load workflow, execute end node, expect it to complete in timely fashion,
	 * expect side branches to not have executed
	 */
    @Test
    public void testLoadPartialExecuteToEnd() throws Exception {
    	checkState(getManager(), CONFIGURED);
    	checkState(m_dataGenerator_1, CONFIGURED);
    	executeAndWait(m_dataGenerator_1);
    	Awaitility.waitAtMost(3, TimeUnit.SECONDS).untilAsserted(() -> executeAndWait(m_tableDiff_41));
    	checkState(m_tableDiff_41, EXECUTED);
    	checkStateOfMany(CONFIGURED, m_dataGenerator_32_7, m_cache_40);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
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
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.NodeContainerTemplateLinkUpdateResult;

/**
 * See PE-37. Configurations on linked components was lost when an update is performed. Also indirectly
 * tests AP-3120.
 *
 * @author wiswedel, University of Konstanz
 */
public class BugPE37_subnode_update extends WorkflowTestCase {

    private NodeID m_diffChecker_Before_5;
    private NodeID m_diffChecker_Before_8;
    private NodeID m_diffChecker_Before_9;
    private NodeID m_diffChecker_Before_13;
    private NodeID m_diffChecker_After_19;
    private NodeID m_diffChecker_After_22;
    private NodeID m_diffChecker_After_23;
    private NodeID m_diffChecker_After_27;

    @Before
    public void setUp() throws Exception {
        NodeID wfmID = loadAndSetWorkflow();
        m_diffChecker_Before_5 = wfmID.createChild(5);
        m_diffChecker_Before_8 = wfmID.createChild(8);
        m_diffChecker_Before_9 = wfmID.createChild(9);
        m_diffChecker_Before_13 = wfmID.createChild(13);
        m_diffChecker_After_19 = wfmID.createChild(19);
        m_diffChecker_After_22 = wfmID.createChild(22);
        m_diffChecker_After_23 = wfmID.createChild(23);
        m_diffChecker_After_27 = wfmID.createChild(27);
    }

    /** Runs the workflow without updating linked metanodes and expects the 'left' side to be green. */
    @Test(timeout = 10000L)
    public void testRunBeforeUpdate() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, IDLE);

        executeAllAndWait();
        waitWhileInExecution();
        checkStateOfMany(EXECUTED, m_diffChecker_Before_5, m_diffChecker_Before_8,
            m_diffChecker_Before_9, m_diffChecker_Before_13);

        checkStateOfMany(CONFIGURED, m_diffChecker_After_19, m_diffChecker_After_22,
            m_diffChecker_After_23, m_diffChecker_After_27);
    }

    /** Runs the workflow after updating linked metanodes and expects the 'right' side to be green. */
    @Test(timeout = 10000L)
    public void testRunAfterUpdate() throws Exception {
        WorkflowManager manager = getManager();
        List<NodeID> linkedMetaNodes = manager.getLinkedMetaNodes(true);
        WorkflowLoadHelper lH = new WorkflowLoadHelper(true, manager.getContext());
        for (NodeID id : linkedMetaNodes) {
            NodeContainerTemplate tnc = (NodeContainerTemplate)manager.findNodeContainer(id);
            WorkflowManager parent = tnc.getParent();
            Assert.assertThat("No update for " + tnc.getNameWithID(), parent.checkUpdateMetaNodeLink(id, lH), is(true));
        }
        for (NodeID id : linkedMetaNodes) {
            NodeContainerTemplate tnc = (NodeContainerTemplate)manager.findNodeContainer(id);
            WorkflowManager parent = tnc.getParent();
            Assert.assertThat("No update for " + tnc.getNameWithID(), parent.checkUpdateMetaNodeLink(id, lH), is(true));
            Assert.assertThat("Update should be flagged", parent.hasUpdateableMetaNodeLink(id), is(true));
            Assert.assertThat("Can't update metanode link", parent.canUpdateMetaNodeLink(id), is(true));
        }
        for (NodeID id : linkedMetaNodes) {
            NodeContainerTemplate tnc = (NodeContainerTemplate)manager.findNodeContainer(id);
            WorkflowManager parent = tnc.getParent();
            NodeContainerTemplateLinkUpdateResult updateRes = parent.updateMetaNodeLink(id, new ExecutionMonitor(), lH);
            Assert.assertThat("Not expected to have errors", updateRes.hasErrors(), is(false));
        }

        executeAllAndWait();
        waitWhileInExecution();
        checkStateOfMany(CONFIGURED, m_diffChecker_Before_5, m_diffChecker_Before_8,
            m_diffChecker_Before_9, m_diffChecker_Before_13);

        checkStateOfMany(EXECUTED, m_diffChecker_After_19, m_diffChecker_After_22,
            m_diffChecker_After_23, m_diffChecker_After_27);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}

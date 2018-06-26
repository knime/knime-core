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

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class MetaWithIndependentThroughConns extends WorkflowTestCase {

    private NodeID m_topSource;
    private NodeID m_bottomSource;
    private NodeID m_topSink;
    private NodeID m_bottomSink;
    private NodeID m_metaWithOnlyThrough;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_topSource = new NodeID(baseID, 1);
        m_bottomSource = new NodeID(baseID, 2);
        m_metaWithOnlyThrough = new NodeID(baseID, 3);
        m_topSink = new NodeID(baseID, 4);
        m_bottomSink = new NodeID(baseID, 5);
    }

    @Test
    public void testStateOfMeta() throws Exception {
        checkState(m_topSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_bottomSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);

        // reset one sink -- no change
        getManager().resetAndConfigureNode(m_topSink);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);

        // reset one source -- meta node reset
        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
        // unconnected through connection -- no change
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        executeAllAndWait();
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);
        getManager().getParent().resetAndConfigureNode(getManager().getID());
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testPullExecutionFromSink() throws Exception {
        checkState(m_topSource, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_topSink);
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_bottomSink, InternalNodeContainerState.CONFIGURED);

        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testExecuteAllThenDeleteOneSourceConnection() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        // remove top connection
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.IDLE);

        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteAllThenDeleteThroughConnection() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        // remove top through connection
        WorkflowManager internalWFM = (WorkflowManager)(getManager()
                             .getNodeContainer(m_metaWithOnlyThrough));
        internalWFM.removeConnection(internalWFM.getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.IDLE);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteAllThenResetOneSource() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        getManager().resetAndConfigureNode(m_bottomSource);
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);

        checkState(m_bottomSink, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testInsertConnection() throws Exception {

        // top input deleted
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));

        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.IDLE);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        getManager().addConnection(m_bottomSource, 1, m_metaWithOnlyThrough, 0);

        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.CONFIGURED);

        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);

    }

}

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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;


/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestRecursiveMetaNodeUpdate extends WorkflowTestCase {

    private NodeID m_tableDiff_BeforeUpdate_11;
    private NodeID m_tableDiff_BeforeUpdate_13;
    private NodeID m_tableDiff_BeforeUpdate_15;
    private NodeID m_tableDiff_BeforeUpdate_17;
    private NodeID m_tableDiff_BeforeUpdate_21;
    private NodeID m_tableDiff_AfterUpdate_22;
    private NodeID m_tableDiff_AfterUpdate_23;
    private NodeID m_tableDiff_AfterUpdate_27;
    private NodeID m_tableDiff_AfterUpdate_28;
    private NodeID m_tableDiff_AfterUpdate_30;
    private NodeID m_metaNoUpdateAvail_4;
    private NodeID m_metaUpdateOnlyInChild_5;
    private NodeID m_metaUpdateTwoChildren_6;
    private NodeID m_metaDifferentDefault_7;
    private NodeID m_metaHiddenLink_19;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow(
            getWorkflowDirectory("testRecursiveMetaNodeUpdate_Group/TestRecursiveMetaNodeUpdate"));
        m_tableDiff_BeforeUpdate_11 = new NodeID(baseID, 11);
        m_tableDiff_BeforeUpdate_13 = new NodeID(baseID, 13);
        m_tableDiff_BeforeUpdate_15 = new NodeID(baseID, 15);
        m_tableDiff_BeforeUpdate_17 = new NodeID(baseID, 17);
        m_tableDiff_BeforeUpdate_21 = new NodeID(baseID, 21);
        m_tableDiff_AfterUpdate_22 = new NodeID(baseID, 22);
        m_tableDiff_AfterUpdate_23 = new NodeID(baseID, 23);
        m_tableDiff_AfterUpdate_27 = new NodeID(baseID, 27);
        m_tableDiff_AfterUpdate_28 = new NodeID(baseID, 28);
        m_tableDiff_AfterUpdate_30 = new NodeID(baseID, 30);
        m_metaNoUpdateAvail_4 = new NodeID(baseID, 4);
        m_metaUpdateOnlyInChild_5 = new NodeID(baseID, 5);
        m_metaUpdateTwoChildren_6 = new NodeID(baseID, 6);
        m_metaDifferentDefault_7 = new NodeID(baseID, 7);
        m_metaHiddenLink_19 = new NodeID(baseID, 19);
    }

    private WorkflowLoadHelper createTemplateLoadHelper() {
        return new WorkflowLoadHelper(true, getManager().getContext());
    }

    @Test
    public void testNoUpdateAfterLoad() throws Exception {
        assertTrue("expected update to be available",
            getManager().checkUpdateMetaNodeLink(m_metaUpdateTwoChildren_6, createTemplateLoadHelper()));
        executeAllAndWait();
        checkStateOfMany(InternalNodeContainerState.EXECUTED,
            m_tableDiff_BeforeUpdate_11, m_tableDiff_BeforeUpdate_13, m_tableDiff_BeforeUpdate_15,
            m_tableDiff_BeforeUpdate_17, m_tableDiff_BeforeUpdate_21);
        checkStateOfMany(InternalNodeContainerState.EXECUTED,
            m_metaNoUpdateAvail_4, m_metaUpdateOnlyInChild_5, m_metaUpdateTwoChildren_6,
            m_metaDifferentDefault_7, m_metaHiddenLink_19);

        checkStateOfMany(InternalNodeContainerState.CONFIGURED,
            m_tableDiff_AfterUpdate_22, m_tableDiff_AfterUpdate_23, m_tableDiff_AfterUpdate_27,
            m_tableDiff_AfterUpdate_28, m_tableDiff_AfterUpdate_30);
    }

    @Test
    public void testAllUpdateAfterLoad() throws Exception {
        getManager().updateMetaNodeLinks(createTemplateLoadHelper(), true, new ExecutionMonitor());
        executeAllAndWait();

        checkStateOfMany(InternalNodeContainerState.CONFIGURED,
            m_tableDiff_BeforeUpdate_11, m_tableDiff_BeforeUpdate_13, m_tableDiff_BeforeUpdate_15,
            m_tableDiff_BeforeUpdate_17, m_tableDiff_BeforeUpdate_21);

        checkStateOfMany(InternalNodeContainerState.EXECUTED,
            m_metaNoUpdateAvail_4, m_metaUpdateOnlyInChild_5, m_metaUpdateTwoChildren_6,
            m_metaDifferentDefault_7, m_metaHiddenLink_19);

        checkStateOfMany(InternalNodeContainerState.EXECUTED,
            m_tableDiff_AfterUpdate_22, m_tableDiff_AfterUpdate_23, m_tableDiff_AfterUpdate_27,
            m_tableDiff_AfterUpdate_28, m_tableDiff_AfterUpdate_30);
    }

    @Test
    public void testUpdateMetaDifferentDefault() throws Exception {
        executeAndWait(m_tableDiff_BeforeUpdate_11);
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.EXECUTED);
        executeAndWait(m_tableDiff_AfterUpdate_28);
        checkState(m_tableDiff_AfterUpdate_28, InternalNodeContainerState.CONFIGURED); // failed
        assertTrue("Expected meta node update available",
            getManager().checkUpdateMetaNodeLink(m_metaDifferentDefault_7, createTemplateLoadHelper()));
        getManager().updateMetaNodeLink(m_metaDifferentDefault_7, new ExecutionMonitor(), createTemplateLoadHelper());
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_tableDiff_BeforeUpdate_11);
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.CONFIGURED); // failed
        executeAndWait(m_tableDiff_AfterUpdate_28);
        checkState(m_tableDiff_AfterUpdate_28, InternalNodeContainerState.EXECUTED); // failed
    }

    @Test
    public void testUpdateOnlyInChild() throws Exception {
        executeAndWait(m_tableDiff_BeforeUpdate_15);
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.EXECUTED);
        executeAndWait(m_tableDiff_AfterUpdate_22);
        checkState(m_tableDiff_AfterUpdate_22, InternalNodeContainerState.CONFIGURED); // failed
        assertTrue("Expected meta node update available",
            getManager().checkUpdateMetaNodeLink(m_metaUpdateOnlyInChild_5, createTemplateLoadHelper()));
        getManager().updateMetaNodeLink(m_metaUpdateOnlyInChild_5, new ExecutionMonitor(), createTemplateLoadHelper());
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_tableDiff_BeforeUpdate_15);
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.CONFIGURED); // failed
        executeAndWait(m_tableDiff_AfterUpdate_22);
        checkState(m_tableDiff_AfterUpdate_22, InternalNodeContainerState.EXECUTED); // failed
    }

}

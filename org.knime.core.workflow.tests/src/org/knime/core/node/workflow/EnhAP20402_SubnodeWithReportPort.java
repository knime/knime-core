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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.util.stream.IntStream;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.report.IReportPortObject;
import org.knime.core.node.port.report.ReportConfiguration;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/** 
 * Enables/disables subnode's report output and checks if connections are properly retained. 
 * 
 * @author Bernd Wiswedel, KNIME
 */
public class EnhAP20402_SubnodeWithReportPort extends WorkflowTestCase {

    private NodeID m_subnode_4;
    private NodeID m_modelWriter_5;
    private NodeID m_subnodeOut_4_8;
    private NodeID m_cache_2;
    private NodeID m_tableCreator_7;
    private NodeID m_reportStart_6;

    @BeforeEach
    public void setUp() throws Exception {
        assumeFalse(Platform.OS_MACOSX.equals(Platform.getOS()), "Disabled on Mac due to DEVOPS-2105");
        NodeID baseID = loadAndSetWorkflow();
        m_subnode_4 = baseID.createChild(4);
        final var subnodeWFM = m_subnode_4.createChild(0);
        m_subnodeOut_4_8 = subnodeWFM.createChild(8);
        m_modelWriter_5 = baseID.createChild(5);
        m_cache_2 = baseID.createChild(2);
        m_tableCreator_7 = baseID.createChild(7);
        m_reportStart_6 = baseID.createChild(6);
    }

    @Test
    public void testRegularExecute() throws Exception {
        executeAndWait(m_cache_2);
        checkState(m_cache_2, EXECUTED);
        checkState(m_subnodeOut_4_8, EXECUTED);
        final var cacheInConnBefore = findInConnection(m_cache_2, 1);
        final var subNodeInConnBefore = findInConnection(m_subnode_4, 1);

        final var snc = getManager().getNodeContainer(m_subnode_4, SubNodeContainer.class, true);
        assertThat("has no report config", snc.getReportConfiguration().isEmpty(), is(true));
        assertThat("some other port is not report", snc.isReportPort(1, false), is(false));
        assertThat("last port is report", snc.isReportPort(2, false), is(false));

        final var reportConfig = ReportConfiguration.INSTANCE;

        // add report, check connections are retained    
        getManager().changeSubNodeReportOutput(m_subnode_4, reportConfig);
        checkState(m_subnode_4, InternalNodeContainerState.IDLE); // new unconnected input port

        // downstream nodes are reset (AP-20600)
        checkState(m_subnodeOut_4_8, InternalNodeContainerState.IDLE);
        checkState(m_cache_2, InternalNodeContainerState.IDLE);

        final var cacheInConnAfter = findInConnection(m_cache_2, 1);
        assertThat("Same connection to cache node", cacheInConnAfter, sameInstance(cacheInConnBefore));
        snc.getWorkflowManager().getNodeContainer(m_subnodeOut_4_8, NativeNodeContainer.class, true); // still present

        final var subNodeInConnAfter = findInConnection(m_subnode_4, 1);
        assertThat("Same connection to cache node", subNodeInConnAfter, sameInstance(subNodeInConnBefore));

        assertThat("Connection 1 within subnode is present", findInConnection(m_subnodeOut_4_8, 1), is(notNullValue()));
        assertThat("Connection 2 within subnode is present", findInConnection(m_subnodeOut_4_8, 2), is(notNullValue()));

        // ports on subnode - output
        assertThat("Number outputs on SNC after report adding", snc.getNrOutPorts(), is(4));
        assertThat("last port is report", snc.getOutputType(3).getPortObjectClass(),
                typeCompatibleWith(IReportPortObject.class));
        assertThat("some other port is not report", snc.isReportPort(2, false), is(false));
        assertThat("last port is report", snc.isReportPort(3, false), is(true));

        // ports on subnode - input
        assertThat("Number inputs on SNC after report adding", snc.getNrInPorts(), is(3));
        assertThat("last input port is report", snc.getInPort(2).getPortType().getPortObjectClass(), 
                typeCompatibleWith(IReportPortObject.class));

        getManager().addConnection(m_reportStart_6, 1, m_subnode_4, 2);
        executeAndWait(m_tableCreator_7, m_reportStart_6);
        checkState(m_subnode_4, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_subnode_4, InternalNodeContainerState.EXECUTED);

        // copy paste (via persistor)
        final var copyContent = WorkflowCopyContent.builder().setNodeIDs(m_subnode_4).build();
        final NodeID copiedSNCID = getManager().copyFromAndPasteHere(getManager(), copyContent).getNodeIDs()[0];
        final SubNodeContainer copiedSNC = getManager().getNodeContainer(copiedSNCID, SubNodeContainer.class, true);
        validateCopiedSNC(copiedSNC);

        // copy paste (via WorkflowDef)
        final var clipboardDef = getManager().copyToDef(copyContent, PasswordRedactor.unsafe());
        final NodeID copiedSNCID_Def = getManager().paste(clipboardDef).getNodeIDs()[0];
        final SubNodeContainer copiedSNC_Ddef = 
                getManager().getNodeContainer(copiedSNCID_Def, SubNodeContainer.class, true);
        validateCopiedSNC(copiedSNC_Ddef);

        getManager().addConnection(m_subnode_4, 3, m_modelWriter_5, 1);

        // now remove one of the other ports and expect report/model connection is kept
        MetaPortInfo[] portInfos = IntStream.range(0, 2).mapToObj(i -> MetaPortInfo.builder().setNewIndex(i)
                .setOldIndex(i).setPortType(snc.getOutputType(i)).setIsConnected(true).build())
                .toArray(MetaPortInfo[]::new);
        snc.getWorkflowManager().removeConnection(findInConnection(m_subnodeOut_4_8, 2));
        getManager().changeSubNodeOutputPorts(m_subnode_4, portInfos);

        assertThat("Number outputs after report added and other port deleted", snc.getNrOutPorts(), is(3));
        assertThat("last port is report", snc.getOutputType(2).getPortObjectClass(),
                typeCompatibleWith(IReportPortObject.class));

        assertThat("Connection 1 within subnode is present", findInConnection(snc.getVirtualOutNodeID(), 1),
                is(notNullValue()));

        final var modelConnection = findInConnection(m_modelWriter_5, 1);
        assertThat("model connection is present", modelConnection, is(notNullValue()));
        assertThat("model connection goes to report port", modelConnection.getSourcePort(), is(2));
    }

    private static void validateCopiedSNC(final SubNodeContainer copiedSNC) {
        assertThat("Number outputs on SNC after copying", copiedSNC.getNrOutPorts(), is(4));
        assertThat("last port is report", copiedSNC.getOutputType(3).getPortObjectClass(),
                typeCompatibleWith(IReportPortObject.class));
        assertThat("Number inputs on SNC after copying", copiedSNC.getNrInPorts(), is(3));
        assertThat("last input port is report", copiedSNC.getInPort(2).getPortType().getPortObjectClass(), 
                typeCompatibleWith(IReportPortObject.class));
    }

}
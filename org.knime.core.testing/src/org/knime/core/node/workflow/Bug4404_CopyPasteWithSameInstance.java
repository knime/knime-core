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
import org.knime.core.node.NodeSettings;


/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4404_CopyPasteWithSameInstance extends WorkflowTestCase {

    private NodeID m_meta100_5;
    private NodeID m_javaSnippet200_7;
    private NodeID m_javaSnippet300_8;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_meta100_5 = new NodeID(baseID, 5);
        m_javaSnippet200_7 = new NodeID(baseID, 7);
        m_javaSnippet300_8 = new NodeID(baseID, 8);
    }

    @Test
    public void testCopyAndPasteTwice() throws Exception {
        WorkflowManager manager = getManager();
        WorkflowCopyContent.Builder copyContent = WorkflowCopyContent.builder();
        copyContent.setNodeIDs(m_meta100_5);
        WorkflowPersistor copyPersistor = manager.copy(copyContent.build()); // copy once but paste twice
        WorkflowCopyContent paste1 = manager.paste(copyPersistor);
        NodeID meta200 = paste1.getNodeIDs()[0];
        WorkflowCopyContent paste2 = manager.paste(copyPersistor);
        NodeID meta300 = paste2.getNodeIDs()[0];
        fixDataGeneratorSettings(meta200, 200);
        fixDataGeneratorSettings(meta300, 300);

        manager.addConnection(meta200, 0, m_javaSnippet200_7, 1);
        manager.addConnection(meta300, 0, m_javaSnippet300_8, 1);
        executeAllAndWait();
        checkState(m_javaSnippet200_7, InternalNodeContainerState.EXECUTED);
        checkState(m_javaSnippet300_8, InternalNodeContainerState.EXECUTED);
    }

    private void fixDataGeneratorSettings(final NodeID metanodeID, final int rowCount) throws Exception {
        final NodeID dataGenID = new NodeID(metanodeID, 1);
        final WorkflowManager manager = getManager();
        WorkflowManager metanode = (WorkflowManager) manager.getNodeContainer(metanodeID);
        metanode.getNodeContainer(dataGenID);
        NodeSettings ns = new NodeSettings("settings");
        metanode.saveNodeSettings(dataGenID, ns);
        NodeSettings model = ns.getNodeSettings("model");
        model.addInt("patcount", rowCount);
        metanode.loadNodeSettings(dataGenID, ns);
    }

}

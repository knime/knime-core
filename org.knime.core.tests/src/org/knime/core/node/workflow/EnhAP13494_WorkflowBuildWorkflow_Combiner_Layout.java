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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.awt.Rectangle;
import java.io.File;
import java.util.Arrays;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * AP-13494: Improve layout of combined workflows
 * https://knime-com.atlassian.net/browse/AP-13494
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class EnhAP13494_WorkflowBuildWorkflow_Combiner_Layout extends WorkflowTestCase {

    private NodeID m_concatenate_12;
    private NodeID m_columnFilter_31;
    private NodeID m_stringInput_32;

    private File m_tmpFolder;

    @Before
    public void setUp() throws Exception {
        NodeID id = loadAndSetWorkflow();
        m_concatenate_12 = id.createChild(12);
        m_columnFilter_31 = id.createChild(31);
        m_stringInput_32 = id.createChild(32);

        m_tmpFolder = FileUtil.createTempDir(EnhAP13494_WorkflowBuildWorkflow_Combiner_Layout.class.getSimpleName());
        WorkflowManager manager = getManager();
        try (WorkflowLock lock = manager.lock()) {
            NodeSettings settingsOfStringInput = new NodeSettings("string-settings");
            manager.saveNodeSettings(m_stringInput_32, settingsOfStringInput);
            settingsOfStringInput.getNodeSettings("model").getNodeSettings("defaultValue").addString("string",
                m_tmpFolder.getAbsolutePath());
            manager.loadNodeSettings(m_stringInput_32, settingsOfStringInput);
        }

    }

    /** Run generator workflow and check properties of output (connection and bounding boxes)*/
    @Test
    public void runAndCountNodesInGeneratedWorkflow() throws Exception {
        final WorkflowManager manager = getManager();
        executeAllAndWait();
        checkState(manager, EXECUTED);
        // name is part of the 'Workflow Writer'
        File generatedWorkflowFolder = new File(m_tmpFolder, "enhAP13494_WorkflowBuildWorkflow_Combiner_Layout_GENERATED");
        WorkflowLoadResult generatedLoadResult = loadWorkflow(generatedWorkflowFolder, new ExecutionMonitor());
        WorkflowManager generatedManager = generatedLoadResult.getWorkflowManager();
        try {
            assertThat("Result when loading generated workflow", generatedLoadResult.getType(), is(LoadResultEntryType.Ok));
            NodeID generatedConcatenate_12 = generatedManager.getID().createChild(m_concatenate_12.getIndex());
            NodeID generatedColumnFilter_31 = generatedManager.getID().createChild(m_columnFilter_31.getIndex());

            Set<ConnectionContainer> incomingConnectionsForColumnFilter =
                    generatedManager.getIncomingConnectionsFor(generatedColumnFilter_31);
            assertTrue("Column Filter to have input connection in generated workflow",
                !incomingConnectionsForColumnFilter.isEmpty());

            assertThat("Column Filter predecessor",
                incomingConnectionsForColumnFilter.stream().findFirst().get().getSource(), is(generatedConcatenate_12));


            // no overlapping bounding boxes for any of the nodes
            for (NodeContainer nc1 : generatedManager.getNodeContainers()) {
                int[] b1 = nc1.getUIInformation().getBounds();
                Rectangle nc1BB = new Rectangle(b1[0], b1[1], b1[2], b1[3]);
                for (NodeContainer nc2 : generatedManager.getNodeContainers()) {
                    if (nc1 == nc2) {
                        continue;
                    }
                    int[] b2 = nc2.getUIInformation().getBounds();
                    Rectangle n2BB = new Rectangle(b2[0], b2[1], b2[2], b2[3]);
                    assertFalse(String.format("Bound box overlap nodes \"%s\" (%s) and \"%s\" (%s)",
                        nc1.getNameWithID(), Arrays.toString(b1), nc2.getNameWithID(), Arrays.toString(b2)),
                        nc1BB.intersects(n2BB));
                }
            }
        } finally {
            if (generatedManager != null) {
                generatedManager.getParent().removeProject(generatedManager.getID());
            }
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_tmpFolder);
    }

}

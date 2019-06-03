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
import static org.junit.Assert.assertNull;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;
import org.knime.core.util.FileUtil;

/**
 * Test the ability create file store cells in wrapped meta nodes.
 * https://bugs.knime.org/AP-5667
 * @author wiswedel, University of Konstanz
 */
@RunWith(Parameterized.class)
public class BugAP5667_FileStoresInSubnode extends WorkflowTestCase {

    /** Changes done to the workflow before we run the test. */
    private enum TestModifications {
        /** Run as is - the wrapped node is streamed (by default). */
        WithStreaming,
        /** Unset streaming executor on the wrapped node. */
        Plain,
        /** Collapse content of wrappednode into metanode. */
        MetaNodeInSubnode,
        /** Collapse content of wrappednode into component. */
        SubnodeInSubnode
    }

    @Parameters(name="{0}")
    public static TestModifications[] createParameters() {
        return TestModifications.values();
    }

    @Parameter
    public TestModifications m_testModification;

    private File m_workflowDir;
    private NodeID m_dataGen_1;
    private NodeID m_testFileStore_10;
    private NodeID m_subnode_5;
    private NodeID m_loopStart_3;
    private NodeID m_loopEnd_6;

    @Before
    public void setUp() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private void initWorkflowFromTemp() throws Exception {
        // will save the workflow in one of the test ...don't write SVN folder
        NodeID baseID = loadAndSetWorkflow(m_workflowDir);
        m_dataGen_1 = new NodeID(baseID, 1);
        m_subnode_5 = new NodeID(baseID, 5);
        m_loopStart_3 = new NodeID(baseID, 3);
        m_loopEnd_6 = new NodeID(baseID, 6);
        m_testFileStore_10 = new NodeID(baseID, 10);
    }

    @Test
    public void test() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, IDLE);
        tweakWorkflow(manager);
        checkState(m_dataGen_1, CONFIGURED);
        assertEquals(0, getWriteFileStoreHandlers().size());
        executeAllAndWait();
        checkState(manager, EXECUTED);
        // the file store handlers: data gen, loop start, test file store (loop nodes don't register)
        assertEquals("File stores: " + String.join("\n", getWriteFileStoreHandlers().stream().map(
            f -> f.toString()).collect(Collectors.toList())), 3, getWriteFileStoreHandlers().size());
        checkCountFileStores();
        manager.save(m_workflowDir, new ExecutionMonitor(), true);
        reset(m_dataGen_1);
        assertEquals(0, getWriteFileStoreHandlers().size());
        closeWorkflow();
        initWorkflowFromTemp();
        manager = getManager();
        checkState(manager, EXECUTED);
        reset(m_testFileStore_10);
        executeAllAndWait();
        checkCountFileStores();
        checkState(manager, EXECUTED);
    }

    private void tweakWorkflow(final WorkflowManager manager) {
        SubNodeContainer subnode = manager.getNodeContainer(m_subnode_5, SubNodeContainer.class, true);
        switch (m_testModification) {
            case WithStreaming:
                break;
            case Plain:
                subnode.setJobManager(ThreadNodeExecutionJobManagerFactory.INSTANCE.getInstance());
                break;
            case MetaNodeInSubnode:
            case SubnodeInSubnode:
                subnode.setJobManager(ThreadNodeExecutionJobManagerFactory.INSTANCE.getInstance());
                WorkflowManager innerWFM = subnode.getWorkflowManager();
                NodeID[] innerNodes = IntStream.of(2, 5).mapToObj(
                    i -> innerWFM.getID().createChild(i)).toArray(NodeID[]::new);
                CollapseIntoMetaNodeResult collapse = innerWFM.collapseIntoMetaNode(
                    innerNodes, new WorkflowAnnotation[0], "yet another level");
                if (m_testModification.equals(TestModifications.SubnodeInSubnode)) {
                    NodeID metaNodeID = collapse.getCollapsedMetanodeID();
                    innerWFM.convertMetaNodeToSubNode(metaNodeID);
                }
                break;
            default:
                throw new InternalError();
        }
    }

    private void checkCountFileStores() throws Exception {
        checkState(m_testFileStore_10, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEnd_6, InternalNodeContainerState.EXECUTED);
        checkState(m_subnode_5, InternalNodeContainerState.EXECUTED);
        File startFSDir = getFileStoresDirectory(m_loopStart_3);
        // it's 5 + 1 - 5 because each iteration keeps one file; 1 because the output of the last iteration is kept
        assertEquals("Unexpected number of physical file store files", 5 + 1, countFilesInDirectory(startFSDir));

        // there should be other nodes having a file store w/ directory
        for (SingleNodeContainer snc : iterateSNCs(getManager(), true)) {
            final NodeID id = snc.getID();
            if (!id.equals(m_loopStart_3)) {
                File fsDir = getFileStoresDirectory(id);
                assertNull(fsDir);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDir);
    }

}

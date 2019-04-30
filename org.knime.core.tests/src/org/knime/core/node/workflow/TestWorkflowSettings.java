/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 18, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.ThreadSafeDuplicateChecker;

/**
 * Tests checking that the workflow tests make proper use of {@link DataContainerSettings}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class TestWorkflowSettings extends WorkflowTestCase {

    private NodeID m_dataGenerator;

    private NodeID m_joiner;

    /**
     * Ensure that workflows run with default {@link DataContainerSettings}.
     *
     * @throws Exception - If something goes wrong while loading or executing the workflow
     */
    @Test
    public void testDefaultWorkflowSettings() throws Exception {
        test(DataContainerSettings.getDefault());
        checkState(m_dataGenerator, InternalNodeContainerState.EXECUTED);
        checkState(m_joiner, InternalNodeContainerState.EXECUTED);
        cleanAndCloseWorkflow();
    }

    /**
     * Ensures that altered {@link DataContainerSettings} are used while executing workflows. This is tested by forcing
     * the {@link ThreadSafeDuplicateChecker} to throw an exception.
     *
     * @throws Exception - If something goes wrong while loading or executing the workflow
     */
    @Test
    public void testBrokenWorkflowSettings() throws Exception {
        test(DataContainerSettings.getDefault().withDuplicateChecker(() -> new ThreadSafeDuplicateChecker() {

            @Override
            public void clear() {
            }

            @Override
            public void checkForDuplicates() throws DuplicateKeyException, IOException {
                throw new DuplicateKeyException("Test failed due to our own duplicate checker implementation");
            }

            @Override
            public void addKey(final String s) throws DuplicateKeyException, IOException {

            }

            @Override
            public void writeToDisk() throws IOException {

            }
        }));
        NodeMessage msgOfNode = findNodeContainer(m_dataGenerator).getNodeMessage();
        // should complain about duplicate key exception
        assertThat("Node not in error state, although an exception has been thrown.", msgOfNode.getMessageType(),
            is(NodeMessage.Type.ERROR));
        assertThat("Node message does not reflect the exceptions message", msgOfNode.getMessage(),
            CoreMatchers.containsString("Test failed due to our own duplicate checker implementation"));
        assertThat("Workflow Manager dirty state", getManager().isDirty(), is(true));
        checkState(m_dataGenerator, InternalNodeContainerState.CONFIGURED);
        checkState(m_joiner, InternalNodeContainerState.CONFIGURED);
        cleanAndCloseWorkflow();

    }

    /**
     * Runs a single test with the given settings
     *
     * @param settings the workflow execution settings
     * @throws Exception - If something goes wrong while loading or executing the workflow
     */
    private void test(final DataContainerSettings settings) throws Exception {
        loadWorkflow(settings);
        executeAndWait(m_joiner);
        waitWhileInExecution();
    }

    /**
     * Loads the workflow with the given settings
     *
     * @param settings the workflow execution settings
     * @throws Exception - If something goes wrong while loading the workflow
     */
    private void loadWorkflow(final DataContainerSettings settings) throws Exception {
        NodeID baseID = loadAndSetWorkflow(settings);
        m_dataGenerator = new NodeID(baseID, 3);
        m_joiner = new NodeID(baseID, 4);
        checkState(m_dataGenerator, InternalNodeContainerState.CONFIGURED);
        checkState(m_joiner, InternalNodeContainerState.CONFIGURED);
    }

    /**
     * Resets and closes the workflow.
     *
     * @throws Exception - If something goes wrong while closing the workflow
     */
    private void cleanAndCloseWorkflow() throws Exception {
        getManager().resetAndConfigureAll();
        assertEquals(getNrTablesInGlobalRepository(), 0);
        closeWorkflow();
    }

}

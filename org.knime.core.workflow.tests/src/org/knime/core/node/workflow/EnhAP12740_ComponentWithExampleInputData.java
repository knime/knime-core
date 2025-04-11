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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryCause;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;

/**
 * Tests to save components with example input data and to open them as component projects.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP12740_ComponentWithExampleInputData extends WorkflowTestCase {

	@TempDir
    private File m_workflowDir;
	
	@TempDir
	private File m_componentDir;

    private NodeID m_component_10;

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
        NodeID baseID = getManager().getID();
        m_component_10 = new NodeID(baseID, 10);
        return loadResult;
    }

    /**
     * Extracts a component from a workflow an saves it with and without example input data. <br>
     * For the component with example input data: checks that it can be executed completely. <br>
     * For the component without example input data: checks that the component input issues the expected warning
     * message.
     *
     * @throws Exception
     */
    @Test
    public void testSaveAndOpenComponent() throws Exception {
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_10);
        getManager().executePredecessorsAndWait(component.getID());

        /* extract and open with example input data */
        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(m_componentDir, new ExecutionMonitor(), inputData);
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(m_componentDir.toPath(), null));
        MetaNodeLinkUpdateResult loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 8); //nodes are expected to change state their from IDLE to CONFIGURED on load
        final SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();
        WorkflowManager wfm = componentProject.getWorkflowManager();
        wfm.executeAllAndWaitUntilDone();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> assertThat(
                "Execution of shared component failed. Node messages: "
                        + wfm.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR),
                componentProject.getVirtualOutNode().getInternalState(),
                is(InternalNodeContainerState.EXECUTED)));

        /* save and open without example input data */
        component.saveAsTemplate(m_componentDir, new ExecutionMonitor());
        loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 0); //no state changes expected since no example data available
        final SubNodeContainer componentProject2 = (SubNodeContainer)loadResult.getLoadedInstance();
        NativeNodeContainer componentInput = componentProject2.getVirtualInNode();
        NodeMessage nodeMessage = componentInput.getNodeMessage();
        assertThat("warning message expected", nodeMessage.getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("unexpected warning message", nodeMessage.getMessage(),
            is("No example input data stored with component"));
    }

    /**
     * Saves, loads, saves as and re-loads a component. Makes sure, among other things, that component nodes are saved
     * in IDLE-state, no matter what.
     *
     * @throws Exception
     */
    @Test
    public void testSaveAsAndOpenComponent() throws Exception {
        /* save a component */
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_10);
        getManager().executePredecessorsAndWait(component.getID());

        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(m_componentDir, new ExecutionMonitor(), inputData);

        /* load and save a component project with data to another location, re-load and execute */
        //first load
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(m_componentDir.toPath(), null));
        MetaNodeLinkUpdateResult loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();

        //save as
        File componentDir2 = FileUtil.createTempDir(getClass().getSimpleName());
        componentProject.saveAsTemplate(componentDir2, new ExecutionMonitor(), null);

        //re-load
        loadResult = loadComponent(componentDir2, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 8); //nodes are expected to change their state from IDLE to CONFIGURED on load
        final SubNodeContainer componentProject2 = (SubNodeContainer)loadResult.getLoadedInstance();
        WorkflowManager wfm = componentProject2.getWorkflowManager();
        wfm.executeAllAndWaitUntilDone();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(
                        "Execution of shared component failed. Node messages: "
                                + wfm.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR),
                        componentProject2.getVirtualOutNode().getInternalState(),
                        is(InternalNodeContainerState.EXECUTED)));
    }

    /**
     * Tests the changes tracker for a component project in order to verify that node state changes and other changes
     * are tracked and distinguished correctly.
     *
     * @throws Exception
     */
    @Test
    public void testChangesTrackerForComponentProject() throws Exception {
        /* save a component and load a component project */
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_10);
        getManager().executePredecessorsAndWait(component.getID());

        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(m_componentDir, new ExecutionMonitor(), inputData);
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(m_componentDir.toPath(), null));
        MetaNodeLinkUpdateResult loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();

        /* the actual tests */
        assertFalse(componentProject.getChangesTracker().isPresent(), "changes tracker should not be initialised");
        componentProject.initChangesTracker();
        WorkflowManager wfm = componentProject.getWorkflowManager();

        //execute node
        wfm.executeAllAndWaitUntilDone();
        assertFalse(componentProject.getTrackedChanges().get().hasOtherChanges(), "no other changes expected");
        assertTrue(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "node state changes expected");

        //reset node
        wfm.resetAndConfigureNode(componentProject.getVirtualOutNodeID());
        assertFalse(componentProject.getTrackedChanges().get().hasOtherChanges(), "no other changes expected");
        assertTrue(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "node state changes expected");

        //save
        componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null); //should reset the changes tracker
        assertFalse(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "no node state changes expected");

        //reset all
        componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.resetAndConfigureAll();
        assertFalse(componentProject.getTrackedChanges().get().hasOtherChanges(), "no other changes expected");

        NodeID stringManipulation_9 = new NodeID(wfm.getID(), 9);
        //change node settings
        componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null); //reset changes tracker
        NodeSettings settings = new NodeSettings("settings");
        wfm.getNodeContainer(stringManipulation_9).saveSettings(settings);
        wfm.loadNodeSettings(stringManipulation_9, settings);
        assertTrue(componentProject.getTrackedChanges().get().hasOtherChanges(), "other changes expected");
        assertTrue(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "node state changes expected");

        //delete connection
        componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.removeConnection(wfm.getConnection(new ConnectionID(stringManipulation_9, 1)));
        assertTrue(componentProject.getTrackedChanges().get().hasOtherChanges(), "other changes expected");
        //node state changes expected to IDLE
        assertTrue(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "node state changes expected");

        //delete node
        componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.removeNode(stringManipulation_9);
        assertTrue(componentProject.getTrackedChanges().get().hasOtherChanges(), "other changes expected");
        assertFalse(componentProject.getTrackedChanges().get().hasNodeStateChanges(), "no node state changes expected");
    }

    /**
     * Tests global actions on a component project workflow.
     * 
     * @throws Exception
     */
    @Test
    public void testActionsOnComponentProjectWorkflow() throws Exception {
        // save and open component project
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_10);
        component.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null);
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(m_componentDir.toPath(), null));
        MetaNodeLinkUpdateResult loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();

        // bug NXT-355: NPE when calling wfm.canCancelAll()
        assertFalse(componentProject.getWorkflowManager().canCancelAll());

        // enh NXT-359: can-reset-all introduced on workflow level
        assertFalse(componentProject.getWorkflowManager().canResetAll());
    }

    private void assertComponentLoadingResult(final LoadResult lr, final int expectedNodeStateChanges) {
        int numNodeStateChanges = assertComponentLoadingResult(lr.getChildren());
        assertThat("unexpected number of node state changes", numNodeStateChanges, is(expectedNodeStateChanges));
    }

    private int assertComponentLoadingResult(final LoadResultEntry[] entries) {
        int countNodeStateChanges = 0;
        for (LoadResultEntry entry : entries) {
            if (entry.getChildren().length == 0) {
                switch (entry.getType()) {
                    case DataLoadError:
                    case Error:
                        throw new AssertionError("Component loaded with errors");
                    case Warning:
                        if (entry.getCause().isPresent()) {
                            if (entry.getCause().get().equals(LoadResultEntryCause.NodeStateChanged)) {
                                countNodeStateChanges++;
                            } else {
                                throw new AssertionError(
                                    "Component loaded with warning that are NOT due to node state changes");
                            }
                        }
                    default:

                }
            } else {
                countNodeStateChanges += assertComponentLoadingResult(entry.getChildren());
            }
        }
        return countNodeStateChanges;
    }
}
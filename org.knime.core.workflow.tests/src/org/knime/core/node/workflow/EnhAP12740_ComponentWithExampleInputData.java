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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryCause;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Tests to save components with example input data and to open them as component projects.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP12740_ComponentWithExampleInputData extends WorkflowTestCase {

    private File m_workflowDir;

    private NodeID m_component_10;

    private List<NodeID> m_loadedComponentNodeIDs = new ArrayList<>();

    /**
     * Creates and copies the workflow into a temporary directory.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
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
        File componentDir = FileUtil.createTempDir(getClass().getSimpleName());

        /* extract and open with example input data */
        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(componentDir, new ExecutionMonitor(), inputData);
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true, null);
        MetaNodeLinkUpdateResult loadResult = loadComponent(componentDir, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 8); //nodes are expected to change state their from IDLE to CONFIGURED on load
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();
        WorkflowManager wfm = componentProject.getWorkflowManager();
        wfm.executeAllAndWaitUntilDone();
        assertThat(
            "Execution of shared component failed. Node messages: "
                + wfm.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR),
            componentProject.getVirtualOutNode().getInternalState(), is(InternalNodeContainerState.EXECUTED));

        /* save and open without example input data */
        component.saveAsTemplate(componentDir, new ExecutionMonitor());
        loadResult = loadComponent(componentDir, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 0); //no state changes expected since no example data available
        componentProject = (SubNodeContainer)loadResult.getLoadedInstance();
        NativeNodeContainer componentInput = componentProject.getVirtualInNode();
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
        File componentDir = FileUtil.createTempDir(getClass().getSimpleName());
        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(componentDir, new ExecutionMonitor(), inputData);

        /* load and save a component project with data to another location, re-load and execute */
        //first load
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true, null);
        MetaNodeLinkUpdateResult loadResult = loadComponent(componentDir, new ExecutionMonitor(), loadHelper);
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();

        //save as
        File componentDir2 = FileUtil.createTempDir(getClass().getSimpleName());
        componentProject.saveAsTemplate(componentDir2, new ExecutionMonitor(), null);

        //re-load
        loadResult = loadComponent(componentDir2, new ExecutionMonitor(), loadHelper);
        assertComponentLoadingResult(loadResult, 8); //nodes are expected to change their state from IDLE to CONFIGURED on load
        SubNodeContainer componentProject2 = (SubNodeContainer)loadResult.getLoadedInstance();
        WorkflowManager wfm = componentProject2.getWorkflowManager();
        wfm.executeAllAndWaitUntilDone();
        assertThat(
            "Execution of shared component failed. Node messages: "
                + wfm.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR),
            componentProject2.getVirtualOutNode().getInternalState(), is(InternalNodeContainerState.EXECUTED));
    }

    /**
     * Tests the changes tracker for a component project in order to verify that node state changes and other changes
     * are tracked and distinguished correctly.
     *
     * @throws Exception
     */
    @Test
    public void testChangesTrackerForComponentProject() throws Exception {
        /* save a component and load a copmonent project */
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_10);
        File componentDir = FileUtil.createTempDir(getClass().getSimpleName());
        PortObject[] inputData = component.fetchInputDataFromParent();
        component.saveAsTemplate(componentDir, new ExecutionMonitor(), inputData);
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true, null);
        MetaNodeLinkUpdateResult loadResult = loadComponent(componentDir, new ExecutionMonitor(), loadHelper);
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();

        /* the actual tests */
        assertFalse("changes tracker should not be initialised", componentProject.getChangesTracker().isPresent());
        componentProject.initChangesTracker();
        WorkflowManager wfm = componentProject.getWorkflowManager();

        //execute node
        wfm.executeAllAndWaitUntilDone();
        assertFalse("no other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        assertTrue("node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //reset node
        wfm.resetAndConfigureNode(componentProject.getVirtualOutNodeID());
        assertFalse("no other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        assertTrue("node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //save
        componentProject.saveAsTemplate(componentDir, new ExecutionMonitor(), null); //should reset the changes tracker
        assertFalse("no node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //change view layout
        componentProject.setLayoutJSONString("{}");
        assertTrue("other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        assertFalse("no node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //reset all
        componentProject.saveAsTemplate(componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.resetAndConfigureAll();
        assertFalse("no other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());

        NodeID stringManipulation_9 = new NodeID(wfm.getID(), 9);
        //change node settings
        componentProject.saveAsTemplate(componentDir, new ExecutionMonitor(), null); //reset changes tracker
        NodeSettings settings = new NodeSettings("settings");
        wfm.getNodeContainer(stringManipulation_9).saveSettings(settings);
        wfm.loadNodeSettings(stringManipulation_9, settings);
        assertTrue("other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        assertTrue("node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //delete connection
        componentProject.saveAsTemplate(componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.removeConnection(wfm.getConnection(new ConnectionID(stringManipulation_9, 1)));
        assertTrue("other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        //node state changes expected to IDLE
        assertTrue("node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());

        //delete node
        componentProject.saveAsTemplate(componentDir, new ExecutionMonitor(), null); //reset changes tracker
        wfm.removeNode(stringManipulation_9);
        assertTrue("other changes expected", componentProject.getTrackedChanges().get().hasOtherChanges());
        assertFalse("no node state changes expected", componentProject.getTrackedChanges().get().hasNodeStateChanges());
    }

    /**
     * Removes loaded components from the project map.
     */
    @After
    public void closeLoadedComponents() {
        for (NodeID id : m_loadedComponentNodeIDs) {
            WorkflowManager.ROOT.removeProject(id);
        }
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

    private MetaNodeLinkUpdateResult loadComponent(final File componentDir, final ExecutionMonitor exec,
        final WorkflowLoadHelper loadHelper)
        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException {
        URI componentURI = componentDir.toURI();
        TemplateNodeContainerPersistor loadPersistor =
            loadHelper.createTemplateLoadPersistor(componentDir, componentURI);
        MetaNodeLinkUpdateResult loadResult =
            new MetaNodeLinkUpdateResult("Shared instance from \"" + componentURI + "\"");
        WorkflowManager.ROOT.load(loadPersistor, loadResult, exec, false);
        m_loadedComponentNodeIDs.add(loadResult.getLoadedInstance().getID());
        return loadResult;
    }

}

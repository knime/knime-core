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
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
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
        //TODO check load result
        SubNodeContainer componentProject = (SubNodeContainer)loadResult.getLoadedInstance();
        WorkflowManager wfm = componentProject.getWorkflowManager();
        wfm.executeAllAndWaitUntilDone();
        assertThat(
            "Execution of shared component failed. Node messages: "
                + wfm.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR),
            componentProject.getVirtualOutNode().getInternalState(), is(InternalNodeContainerState.EXECUTED));
        WorkflowManager.ROOT.removeProject(componentProject.getID());

        /* save and open without example input data */
        component.saveAsTemplate(componentDir, new ExecutionMonitor());
        loadResult = loadComponent(componentDir, new ExecutionMonitor(), loadHelper);
        //TODO check load result
        componentProject = (SubNodeContainer)loadResult.getLoadedInstance();
        NativeNodeContainer componentInput = componentProject.getVirtualInNode();
        NodeMessage nodeMessage = componentInput.getNodeMessage();
        assertThat("warning message expected", nodeMessage.getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("unexpected warning message", nodeMessage.getMessage(),
            is("No example input data stored with component"));
        WorkflowManager.ROOT.removeProject(componentProject.getID());

    }
}

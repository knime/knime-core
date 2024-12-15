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
 *   Dec 1, 2020 (hornm): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.action.MetaNodeToSubNodeResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;

/**
 * Tests methods in {@link NodeContainerParent}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeContainerParentTest {

    private List<NodeID> m_loadedComponentNodeIDs = new ArrayList<>();

    private NodeID m_projectId;

    /**
     * Tests {@link NodeContainerParent#getProjectWFM(NodeContainer)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetProjectWFM() throws Exception {
        WorkflowManager project = createEmptyWorkflow();
        m_projectId = project.getID();

        // at root level
        NativeNodeContainer nnc = addSaveWorkflowNode(project);
        checkGetProjectWorkflow(nnc, project);

        // from a metanode
        WorkflowManager metanode = project.createAndAddSubWorkflow(new PortType[0], new PortType[0], "component");
        nnc = addSaveWorkflowNode(metanode);
        checkGetProjectWorkflow(nnc, project);

        // from a component
        MetaNodeToSubNodeResult res = project.convertMetaNodeToSubNode(metanode.getID());
        SubNodeContainer component = (SubNodeContainer)project.getNodeContainer(metanode.getID());
        WorkflowManager componentWfm = component.getWorkflowManager();
        nnc = addSaveWorkflowNode(componentWfm);
        checkGetProjectWorkflow(nnc, project);

        // from a metanode in a component
        WorkflowManager nested =
            componentWfm.createAndAddSubWorkflow(new PortType[0], new PortType[0], "metanode in component");
        nnc = addSaveWorkflowNode(nested);
        checkGetProjectWorkflow(nnc, project);

        // from a component project
        File componentDir = FileUtil.createTempDir("component_project");
        component.saveAsTemplate(componentDir, new ExecutionMonitor(), null);
        SubNodeContainer componentProject = loadComponent(componentDir);
        WorkflowManager componentProjectWfm = componentProject.getWorkflowManager();
        nnc = (NativeNodeContainer)componentProjectWfm.getNodeContainer(componentProjectWfm.getID().createChild(1));
        checkGetProjectWorkflow(nnc, componentProjectWfm);
    }

    @AfterEach
    public void tearDown() {
        WorkflowManager.ROOT.removeProject(m_projectId);
        for (NodeID id : m_loadedComponentNodeIDs) {
            WorkflowManager.ROOT.removeProject(id);
        }
    }

    private static NativeNodeContainer addSaveWorkflowNode(final WorkflowManager wfm)
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        NodeID id = wfm.createAndAddNode(NodeFactoryProvider.getInstance() //
        		.getNodeFactory(PortObjectInNodeFactory.class.getCanonicalName()).get());
        return (NativeNodeContainer)wfm.getNodeContainer(id);
    }

    private static void checkGetProjectWorkflow(final NativeNodeContainer nnc, final WorkflowManager project) {
        NodeContext.pushContext(nnc);
        try {
            assertThat("not the project workflow", NodeContainerParent.getProjectWFM(nnc), sameInstance(project));
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private static WorkflowManager createEmptyWorkflow() throws IOException {
        File dir = FileUtil.createTempDir("workflow");
        File workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);
        if (workflowFile.createNewFile()) {
            return WorkflowManager.ROOT.createAndAddProject("workflow", new WorkflowCreationHelper(
                    WorkflowContextV2.forTemporaryWorkflow(workflowFile.getParentFile().toPath(), null)));
        } else {
            throw new IllegalStateException("Creating empty workflow failed");
        }
    }

    private SubNodeContainer loadComponent(final File componentDir)
        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException {
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(componentDir.toPath(), null));
        URI componentURI = componentDir.toURI();
        TemplateNodeContainerPersistor loadPersistor =
            loadHelper.createTemplateLoadPersistor(componentDir, componentURI);
        MetaNodeLinkUpdateResult loadResult =
            new MetaNodeLinkUpdateResult("Shared instance from \"" + componentURI + "\"");
        WorkflowManager.ROOT.load(loadPersistor, loadResult, new ExecutionMonitor(), false);
        m_loadedComponentNodeIDs.add(loadResult.getLoadedInstance().getID());
        return (SubNodeContainer)loadResult.getLoadedInstance();
    }
}
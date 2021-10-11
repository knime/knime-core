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
 *   Oct 4, 2021 (hornm): created
 */
package org.knime.core.node.wizard.page;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests {@link WizardPageUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class WizardPageUtilTest {

    private WorkflowManager m_wfm;

    @SuppressWarnings("javadoc")
    @Before
    public void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    /**
     * Tests {@link WizardPageUtil#isWizardPage(WorkflowManager, NodeID)}.
     */
    @Test
    public void testIsWizardPage() {

        NodeID nonExistentNode = new NodeID(4).createChild(5);
        assertFalse(WizardPageUtil.isWizardPage(m_wfm, nonExistentNode));

        NodeID metanode = m_wfm.createAndAddSubWorkflow(new PortType[0], new PortType[0], "metanode").getID();
        assertFalse(WizardPageUtil.isWizardPage(m_wfm, metanode));

        m_wfm.convertMetaNodeToSubNode(metanode);
        NodeID emptyComponent = metanode;
        assertFalse(WizardPageUtil.isWizardPage(m_wfm, emptyComponent));

        SubNodeContainer component = (SubNodeContainer)m_wfm.getNodeContainer(emptyComponent);
        WorkflowManagerUtil.createAndAddNode(component.getWorkflowManager(), new WizardNodeFactory());
        NodeID componentWithWizardNode = emptyComponent;
        assertTrue(WizardPageUtil.isWizardPage(m_wfm, componentWithWizardNode));

        NodeID componentWithNodeViewNode = m_wfm.collapseIntoMetaNode(
            new NodeID[]{WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0)).getID()},
            new WorkflowAnnotation[0], "component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentWithNodeViewNode);
        assertTrue(WizardPageUtil.isWizardPage(m_wfm, componentWithNodeViewNode));

        NodeID componentWithAComponentWithNodeView = m_wfm.collapseIntoMetaNode(new NodeID[]{componentWithNodeViewNode},
            new WorkflowAnnotation[0], "component of a component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentWithAComponentWithNodeView);
        assertTrue(WizardPageUtil.isWizardPage(m_wfm, componentWithAComponentWithNodeView));
    }

    /**
     * Tests {@link WizardPageUtil#getWizardPageNodes(WorkflowManager)} etc.
     */
    @Test
    public void testGetWizardPageNodes() {
        NodeID componentWithNodeViewNode = m_wfm.collapseIntoMetaNode(
            new NodeID[]{WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0)).getID()},
            new WorkflowAnnotation[0], "component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentWithNodeViewNode);
        WorkflowManager componentWfm =
            ((SubNodeContainer)m_wfm.getNodeContainer(componentWithNodeViewNode)).getWorkflowManager();
        List<NativeNodeContainer> wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm);
        assertThat(wizardPageNodes.size(), is(1));
        assertThat(wizardPageNodes.get(0).getName(), is("NodeView"));

        NodeID componentWithAComponentWithNodeView = m_wfm.collapseIntoMetaNode(new NodeID[]{componentWithNodeViewNode},
            new WorkflowAnnotation[0], "component of a component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentWithAComponentWithNodeView);
        componentWfm =
            ((SubNodeContainer)m_wfm.getNodeContainer(componentWithAComponentWithNodeView)).getWorkflowManager();
        wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm);
        assertTrue(wizardPageNodes.isEmpty());
        wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm, false);
        assertTrue(wizardPageNodes.isEmpty());
        wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm, true);
        assertThat(wizardPageNodes.size(), is(1));
        assertThat(wizardPageNodes.get(0).getName(), is("NodeView"));

        NodeID componentWithNodeWizardNode = m_wfm.collapseIntoMetaNode(
            new NodeID[]{WorkflowManagerUtil.createAndAddNode(m_wfm, new WizardNodeFactory()).getID()},
            new WorkflowAnnotation[0], "component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentWithNodeWizardNode);
        componentWfm = ((SubNodeContainer)m_wfm.getNodeContainer(componentWithNodeWizardNode)).getWorkflowManager();
        wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm);
        assertThat(wizardPageNodes.size(), is(1));
        assertThat(wizardPageNodes.get(0).getName(), is("Wizard"));
    }

    /**
     * Tests {@link WizardPageUtil#getAllWizardPageNodes(WorkflowManager, boolean)}.
     */
    @Test
    public void testGetAllWizardPageNodes() {
        NodeID n1 = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0)).getID();
        NodeID n2 = WorkflowManagerUtil.createAndAddNode(m_wfm, new WizardNodeFactory()).getID();

        NodeID component = m_wfm.collapseIntoMetaNode(new NodeID[]{n1, n2}, new WorkflowAnnotation[0], "component")
            .getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(component);
        WorkflowManager componentWfm = ((SubNodeContainer)m_wfm.getNodeContainer(component)).getWorkflowManager();

        List<NativeNodeContainer> wizardPageNodes = WizardPageUtil.getAllWizardPageNodes(componentWfm, false);
        assertThat(wizardPageNodes.size(), is(2));

        WizardNodeModel wnm =
            (WizardNodeModel)((NativeNodeContainer)m_wfm.findNodeContainer(component.createChild(0).createChild(2)))
                .getNodeModel();
        wnm.setHideInWizard(true);

        wizardPageNodes = WizardPageUtil.getAllWizardPageNodes(componentWfm, false);
        assertThat(wizardPageNodes.size(), is(2));

        wizardPageNodes = WizardPageUtil.getWizardPageNodes(componentWfm);
        assertThat(wizardPageNodes.size(), is(1));
    }

    /**
     * Tests {@link WizardPageUtil#createWizardPage(WorkflowManager, NodeID)}.
     */
    @Test
    public void testCreateWizardPage() {
        NodeID n1 = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0)).getID();
        NodeID n2 = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0)).getID();

        try (WorkflowLock lock = m_wfm.lock()) {
            assertThrows(IllegalArgumentException.class, () -> WizardPageUtil.createWizardPage(m_wfm, n1));
        }

        NodeID component =
            m_wfm.collapseIntoMetaNode(new NodeID[]{n1}, new WorkflowAnnotation[0], "component").getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(component);
        component = m_wfm.collapseIntoMetaNode(new NodeID[]{component, n2}, new WorkflowAnnotation[0], "component")
            .getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(component);

        WizardPage wizardPage;
        try (WorkflowLock lock = m_wfm.lock()) {
            wizardPage = WizardPageUtil.createWizardPage(m_wfm, component);
        }
        assertThat(wizardPage.getPageNodeID(), is(component));
        assertThat(wizardPage.getPageMap().size(), is(2));
        assertThat(wizardPage.getPageMap().keySet(),
            containsInAnyOrder(NodeIDSuffix.fromString("4:0:2"), NodeIDSuffix.fromString("4:0:3:0:1")));
        assertThat(wizardPage.getPageMap().values().stream().map(n -> n.getName()).collect(Collectors.toList()),
            containsInAnyOrder("NodeView", "NodeView"));

        m_wfm.getParent().removeProject(m_wfm.getID());
    }

    @SuppressWarnings("javadoc")
    @After
    public void disposeWorkflow() {
        m_wfm.getParent().removeProject(m_wfm.getID());
    }

}

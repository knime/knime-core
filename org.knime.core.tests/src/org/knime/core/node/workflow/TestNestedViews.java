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
 *   11 Oct 2018 (Christian Albrecht, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;
import org.knime.core.wizard.SinglePageManager;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.layout.bs.JSONNestedLayout;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class TestNestedViews extends WorkflowTestCase {

    private SinglePageManager m_spm;
    private NodeID m_outerSubnodeID;
    private NodeID m_topLevelTableID;
    private NodeID m_topLevelScatterID;
    private NodeID m_innerViewSubnodeID;
    private NodeID m_innerPCViewID;
    private NodeID m_noViewID;
    private NodeID m_deeplyNestedSubnodeID;
    private NodeID m_innerInnerSubnodeID;
    private NodeID m_innerInnerTableID;

    /**
     * Load workflow, setup node ids
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        loadAndSetWorkflow();
        m_spm = SinglePageManager.of(getManager());
        m_outerSubnodeID = createNodeID("6");
        m_topLevelTableID = createNodeID("6:0:3");
        m_topLevelScatterID = createNodeID("6:0:5");
        m_innerViewSubnodeID = createNodeID("6:0:9");
        m_innerPCViewID = createNodeID("6:0:9:0:8");
        m_noViewID = createNodeID("6:0:11");
        m_deeplyNestedSubnodeID = createNodeID("6:0:13");
        m_innerInnerSubnodeID = createNodeID("6:0:13:0:16");
        m_innerInnerTableID = createNodeID("6:0:13:0:16:0:15");
    }

    private NodeID createNodeID(final String suffix) {
        return NodeIDSuffix.fromString(suffix).prependParent(getManager().getID());
    }

    private NodeIDSuffix createNodeSuffix(final NodeID nodeID) {
        return NodeIDSuffix.create(getManager().getID(), nodeID);
    }

    /**
     * Simple execute test, just a sanity check
     * @throws Exception
     */
    @Test
    public void testExecuteAll() throws Exception {
        executeAllAndWait();
        final WorkflowManager wfm = getManager();
        checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

    /**
     * Test if a combined view can be created and contains immediately contained views AND nested subnodes
     * @throws Exception
     */
    @Test
    public void testExecuteAndCreateSubnodeView() throws Exception {
        //setup
        executeAllAndWait();
        SinglePageWebResourceController spc = new SinglePageWebResourceController(getManager(), m_outerSubnodeID);
        assertTrue("Should have subnode view", spc.isSubnodeViewAvailable());

        //create page content
        WizardPageContent page = spc.getWizardPage();
        assertNotNull("Page content should be available", page);
        @SuppressWarnings("rawtypes")
        Map<NodeIDSuffix, WizardNode> pageMap = page.getPageMap();
        assertNotNull("Page map should be available", pageMap);
        String layoutString = page.getLayoutInfo();
        assertNotNull("Page layout should be available", layoutString);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        JSONLayoutPage layout = mapper.readerFor(JSONLayoutPage.class).readValue(layoutString);
        assertNotNull("Layout should be deserializable", layout);

        //test if expected nodes are present
        assertTrue("Page should contain top level table view",
            pageMap.containsKey(createNodeSuffix(m_topLevelTableID)));
        assertTrue("Page should contain nested parallel coordinates plot",
            pageMap.containsKey(createNodeSuffix(m_innerPCViewID)));
        assertTrue("Page should contain deeply nested table view",
            pageMap.containsKey(createNodeSuffix(m_innerInnerTableID)));

        //test if layout contains expected nodes and nested content
        assertTrue("Layout should contain several top level rows", layout.getRows().size() > 0);
        JSONLayoutContent firstContent = layout.getRows().get(0).getColumns().get(0).getContent().get(0);
        assertTrue("First view in layout should be table view",
            firstContent instanceof JSONLayoutViewContent && ((JSONLayoutViewContent)firstContent).getNodeID()
                .equals(Integer.toString(m_topLevelTableID.getIndex())));
        JSONLayoutContent secondContent = layout.getRows().get(0).getColumns().get(1).getContent().get(0);
        assertTrue("Second view should be nested subnode", secondContent instanceof JSONNestedLayout
            && ((JSONNestedLayout)secondContent).getNodeID().equals(Integer.toString(m_innerViewSubnodeID.getIndex())));
        JSONLayoutContent thirdContent = layout.getRows().get(2).getColumns().get(0).getContent().get(0);
        assertTrue("Third view should be deeply nested subnode",
            thirdContent instanceof JSONNestedLayout && ((JSONNestedLayout)thirdContent).getNodeID()
                .equals(Integer.toString(m_deeplyNestedSubnodeID.getIndex())));
    }

    /**
     * Test if a serializable page object (view) can be created. The layout in the page needs to resolve the node ids
     * correctly and expand nested layouts correctly.
     * @throws Exception
     */
    @Test
    public void testExecuteAndCreateSerializableSubnodeView() throws Exception {
        executeAllAndWait();
        JSONWebNodePage page = m_spm.createWizardPage(m_outerSubnodeID);
        assertNotNull("Page should be created", page);
        JSONLayoutPage layout = page.getWebNodePageConfiguration().getLayout();
        assertNotNull("Layout should be present", layout);
        assertTrue("Layout should contain several top level rows", layout.getRows().size() > 0);

        //test that top level node ids resolved correctly
        JSONLayoutContent firstContent = layout.getRows().get(0).getColumns().get(0).getContent().get(0);
        assertTrue("First view in layout should be table view",
            firstContent instanceof JSONLayoutViewContent && ((JSONLayoutViewContent)firstContent).getNodeID()
                .equals(createNodeSuffix(m_topLevelTableID).toString()));
        JSONLayoutContent secondContent = layout.getRows().get(0).getColumns().get(1).getContent().get(0);
        assertTrue("Second view should be nested subnode", secondContent instanceof JSONNestedLayout
            && ((JSONNestedLayout)secondContent).getNodeID().equals(createNodeSuffix(m_innerViewSubnodeID).toString()));
        JSONLayoutContent thirdContent = layout.getRows().get(2).getColumns().get(0).getContent().get(0);
        assertTrue("Third view should be deeply nested subnode",
            thirdContent instanceof JSONNestedLayout && ((JSONNestedLayout)thirdContent).getNodeID()
                .equals(createNodeSuffix(m_deeplyNestedSubnodeID).toString()));

        //test that nested layouts are expanded
        JSONNestedLayout innerView = (JSONNestedLayout)secondContent;
        JSONLayoutPage innerLayout = innerView.getLayout();
        assertNotNull("Inner view should have layout", innerLayout);
        assertTrue("Inner view should contain rows", innerLayout.getRows() != null && innerLayout.getRows().size() > 0);
        JSONLayoutContent innerPCPlot = innerLayout.getRows().get(0).getColumns().get(0).getContent().get(0);
        assertTrue("Inner view should contain parallel coordinates plot",
            innerPCPlot instanceof JSONLayoutViewContent && ((JSONLayoutViewContent)innerPCPlot).getNodeID()
                .equals(createNodeSuffix(m_innerPCViewID).toString()));
        JSONLayoutPage deepLayout = ((JSONNestedLayout)thirdContent).getLayout();
        assertNotNull("Deeply nested view should have layout", deepLayout);
        assertTrue("Deeply nested view should contain rows",
            deepLayout.getRows() != null && deepLayout.getRows().size() > 0);
        JSONLayoutContent innerInnerView = deepLayout.getRows().get(0).getColumns().get(0).getContent().get(0);
        assertTrue("Inner inner view should be present in deeply nested subnode",
            innerInnerView instanceof JSONNestedLayout && ((JSONNestedLayout)innerInnerView).getNodeID()
                .equals(createNodeSuffix(m_innerInnerSubnodeID).toString()));
        JSONLayoutPage innerInnerLayout = ((JSONNestedLayout)innerInnerView).getLayout();
        assertNotNull("Inner inner view should have layout", innerInnerLayout);
        assertTrue("Inner inner view should contain rows",
            innerInnerLayout.getRows() != null && innerInnerLayout.getRows().size() > 0);
        JSONLayoutContent innerTable = innerInnerLayout.getRows().get(0).getColumns().get(0).getContent().get(0);
        assertTrue("Inner inner view should contain table view",
            innerTable instanceof JSONLayoutViewContent && ((JSONLayoutViewContent)innerTable).getNodeID()
            .equals(createNodeSuffix(m_innerInnerTableID).toString()));
    }

}
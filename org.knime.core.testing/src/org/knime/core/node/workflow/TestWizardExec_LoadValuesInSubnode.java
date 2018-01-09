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
 *   Jun 25, 2015 (albrecht): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.web.ValidationError;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.TestWizardExec_Loop_Simple.WizardHold;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
public class TestWizardExec_LoadValuesInSubnode extends WorkflowTestCase {

    private NodeID m_filterSubnode;
    private NodeID m_noClustersSubnode;
    private NodeID m_labelClustersSubnode;
    private NodeID m_showClustersSubnode;
    private NodeID m_loopEndNode;
    private NodeID m_colFilterInFilterSubnode;
    private NodeID m_intInputInNoClusterSubnode;
    private NodeID m_stringInputInLabelClustersSubnode;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_filterSubnode = new NodeID(baseID, 101);
        m_noClustersSubnode = new NodeID(baseID, 102);
        m_labelClustersSubnode = new NodeID(baseID, 104);
        m_loopEndNode = new NodeID(baseID, 88);
        m_showClustersSubnode = new NodeID(baseID, 105);
        m_colFilterInFilterSubnode = new NodeID(new NodeID(m_filterSubnode, 0), 17);
        m_intInputInNoClusterSubnode = new NodeID(new NodeID(m_noClustersSubnode, 0), 23);
        m_stringInputInLabelClustersSubnode = new NodeID(new NodeID(m_labelClustersSubnode, 0), 93);
    }

    @Test
    public void testExecuteAll() throws Exception {
        executeAllAndWait();
        final WorkflowManager wfm = getManager();
        checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testWizardStepThroughWithSeveralLoopIterations() throws Exception {
        final int numLoops = 3;
        final WorkflowManager wfm = getManager();
        assertTrue("Should have new wizard execution", WebResourceController.hasWizardExecution(wfm));
        checkState(m_filterSubnode, InternalNodeContainerState.CONFIGURED);
        WizardExecutionController wizardController = wfm.getWizardExecutionController();
        wizardController.stepFirst();
        waitWhile(wfm, new WizardHold(wfm));
        assertTrue("Should have steps", wizardController.hasCurrentWizardPage());
        checkState(m_colFilterInFilterSubnode, InternalNodeContainerState.EXECUTED);
        WizardPageContent currentWizardPage = wizardController.getCurrentWizardPage();
        //don't load anything here, just execute to next subnode (all columns included)
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_noClustersSubnode, InternalNodeContainerState.EXECUTED);
        currentWizardPage = wizardController.getCurrentWizardPage();
        Map<String, String> valueMap = new HashMap<String, String>();
        //setting number of clusters to be found (and loop iterations)
        String intInputID = m_noClustersSubnode.getIndex() + ":0:" + m_intInputInNoClusterSubnode.getIndex();
        valueMap.put(intInputID, "{\"integer\":" + numLoops + "}");
        Map<String, ValidationError> errorMap = wizardController.loadValuesIntoCurrentPage(valueMap);
        assertEquals("Loading number of clusters should not have caused errors", 0, errorMap.size());

        //looping over clusters
        for (int curLoop = 1; curLoop <= numLoops; curLoop++) {
            String stringInputID = m_labelClustersSubnode.getIndex() + ":0:" + m_stringInputInLabelClustersSubnode.getIndex();
            wizardController.stepNext();
            waitWhile(wfm, new WizardHold(wfm));
            checkState(m_labelClustersSubnode, InternalNodeContainerState.EXECUTED);
            checkState(m_loopEndNode, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
            currentWizardPage = wizardController.getCurrentWizardPage();
            assertEquals("Labeling page should have 3 components", 3, currentWizardPage.getPageMap().size());
            assertNotNull("Labeling page should contain string input", currentWizardPage.getPageMap().get(NodeIDSuffix.fromString(stringInputID)));
            valueMap.clear();
            //label for cluster
            valueMap.put(stringInputID, "{\"string\":\"Cluster " + curLoop + "\"}");
            errorMap = wizardController.loadValuesIntoCurrentPage(valueMap);
            assertEquals("Loading cluster label should not have caused errors", 0, errorMap.size());
        }

        //display result of labeling
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_showClustersSubnode, InternalNodeContainerState.EXECUTED);
        currentWizardPage = wizardController.getCurrentWizardPage();
        assertEquals("Result page should have 2 components", 2, currentWizardPage.getPageMap().size());

        //finish execute
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        assertFalse("Should have no more pages", wizardController.hasCurrentWizardPage());
        checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testWizardStepBackInsideLoop() throws Exception {
        final WorkflowManager wfm = getManager();
        WizardExecutionController wizardController = wfm.getWizardExecutionController();
        assertFalse("Should have no previous steps", wizardController.hasPreviousWizardPage());
        wizardController.stepFirst();
        waitWhile(wfm, new WizardHold(wfm));
        assertTrue("should have steps", wizardController.hasCurrentWizardPage());
        checkState(m_colFilterInFilterSubnode, InternalNodeContainerState.EXECUTED);

        //standard no of clusters (5)
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_noClustersSubnode, InternalNodeContainerState.EXECUTED);

        //two loop iterations
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_labelClustersSubnode, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndNode, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_labelClustersSubnode, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndNode, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);

        //step back from inside loop
        assertTrue("Should have previous steps", wizardController.hasPreviousWizardPage());
        wizardController.stepBack();
        checkState(m_noClustersSubnode, InternalNodeContainerState.EXECUTED);
        //checkState(m_labelClustersSubnode, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
        checkState(wfm, InternalNodeContainerState.IDLE);
        assertTrue("Should have page to prompt", wizardController.hasCurrentWizardPage());

        //execute all

        //checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

}

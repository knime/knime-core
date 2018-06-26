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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;



/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestWizardExec_Loop_Simple extends WorkflowTestCase {

    private NodeID m_tableCreateNode1;
    private NodeID m_subnodeQueryStringBool15;
    private NodeID m_subnodeInLOOPQueryInt14;
    private NodeID m_subnodeInLOOPQueryIntInactive17;
    private NodeID m_loopEnd11;
    private NodeID m_interactiveTable16;
    private NodeID m_qfStringIn_Subnode15_02;
    private NodeID m_qfBooleanIn_Subnode15_04;
    private NodeID m_qfIntIn_Subnode14_09;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreateNode1 = new NodeID(baseID, 1);
        m_subnodeQueryStringBool15 = new NodeID(baseID, 15);
        m_subnodeInLOOPQueryInt14 = new NodeID(baseID, 14);
        m_subnodeInLOOPQueryIntInactive17 = new NodeID(baseID, 17);
        m_loopEnd11 = new NodeID(baseID, 11);
        m_interactiveTable16 = new NodeID(baseID, 16);
        m_qfStringIn_Subnode15_02 = new NodeID(new NodeID(m_subnodeQueryStringBool15, 0), 6);
        m_qfBooleanIn_Subnode15_04 = new NodeID(new NodeID(m_subnodeQueryStringBool15, 0), 4);
        m_qfIntIn_Subnode14_09 = new NodeID(new NodeID(m_subnodeInLOOPQueryInt14, 0), 9);
    }

    @Test
    public void testExecuteAll() throws Exception {
        executeAllAndWait();
        final WorkflowManager wfm = getManager();
        checkState(wfm, InternalNodeContainerState.EXECUTED);
        final SubNodeContainer inactiveNode = wfm.getNodeContainer(
            m_subnodeInLOOPQueryIntInactive17, SubNodeContainer.class, true);
        assertTrue("Node not inactive but should", inactiveNode.getOutputObject(1) instanceof InactiveBranchPortObject);
    }

    @Test
    public void testWizardStepThrough() throws Exception {
        final WorkflowManager wfm = getManager();
        assertTrue("should have new wizard execution", WebResourceController.hasWizardExecution(wfm));
        checkState(m_tableCreateNode1, InternalNodeContainerState.CONFIGURED);
        WizardExecutionController wizardController = wfm.getWizardExecutionController();
        wizardController.stepFirst();

        waitWhile(wfm, new WizardHold(wfm));
        assertTrue("should have steps", wizardController.hasCurrentWizardPage());
        checkState(m_subnodeQueryStringBool15, InternalNodeContainerState.EXECUTED);
        WizardPageContent currentWizardPage = wizardController.getCurrentWizardPage(); // outside loop
        // TODO: load something real
        wizardController.loadValuesIntoCurrentPage(Collections.<String, String>emptyMap());
        // TODO check IDs
//        assertEquals(m_subnodeQueryStringBool15.toString(), currentWizardPage.getPageNodeID());


        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(wfm, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC,
            InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC);
        checkState(m_subnodeQueryStringBool15, InternalNodeContainerState.EXECUTED);
        checkState(m_subnodeInLOOPQueryInt14, InternalNodeContainerState.EXECUTED);
        assertTrue("should have steps (loop iteration 0)", wizardController.hasCurrentWizardPage());
        currentWizardPage = wizardController.getCurrentWizardPage(); // inside loop 1st time
//        assertEquals(m_subnodeInLOOPQueryInt14.toString(), currentWizardPage.getPageNodeID());

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_subnodeInLOOPQueryInt14, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEnd11, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
        assertTrue("should have steps (loop iteration 1)", wizardController.hasCurrentWizardPage());
        currentWizardPage = wizardController.getCurrentWizardPage(); // inside loop 2nd time
//        assertEquals(m_subnodeInLOOPQueryInt14.toString(), currentWizardPage.getPageNodeID());

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        assertFalse("should have no more pages", wizardController.hasCurrentWizardPage());
        checkState(wfm, InternalNodeContainerState.EXECUTED);

        NodeSettings settings = new NodeSettings("test");
        wizardController.save(settings);
        int[] prompted = settings.getIntArray("promptedSubnodeIDs");
        assertTrue("should have saved prompted node ids (15) to settings", Arrays.equals(new int[]{15}, prompted));
    }

    @Test
    public void testWizardStepHalfWayThrougAndBack() throws Exception {
        final WorkflowManager wfm = getManager();
        WizardExecutionController wizardController = wfm.getWizardExecutionController();
        assertFalse("should have no previous steps", wizardController.hasPreviousWizardPage());
        wizardController.stepFirst();

        waitWhile(wfm, new WizardHold(wfm));
        assertFalse("should have no previous steps", wizardController.hasPreviousWizardPage());
        checkState(m_subnodeQueryStringBool15, InternalNodeContainerState.EXECUTED);
        wizardController.getCurrentWizardPage(); // outside loop

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_subnodeInLOOPQueryInt14, InternalNodeContainerState.EXECUTED);
        assertTrue("should have previous steps", wizardController.hasPreviousWizardPage());

        wizardController.stepBack();
        checkState(m_subnodeQueryStringBool15, InternalNodeContainerState.EXECUTED);
        checkState(wfm, InternalNodeContainerState.IDLE);
        assertTrue("should have no next steps", wizardController.hasCurrentWizardPage());
        wizardController.getCurrentWizardPage(); // outside loop QF

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_subnodeInLOOPQueryInt14, InternalNodeContainerState.EXECUTED); // first iteration

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_subnodeInLOOPQueryInt14, InternalNodeContainerState.EXECUTED); // second iteration

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(wfm, InternalNodeContainerState.EXECUTED);
    }

    /**
     *
     * @author wiswedel
     */
    static final class WizardHold extends Hold {
        private final WorkflowManager m_wfm;

        WizardHold(final WorkflowManager wfm) {
            m_wfm = wfm;
        }

        @Override
        protected boolean shouldHold() {
            try (WorkflowLock lock = m_wfm.lock()) {
                return !m_wfm.getNodeContainerState().isHalted();
            }
        }
    }


}

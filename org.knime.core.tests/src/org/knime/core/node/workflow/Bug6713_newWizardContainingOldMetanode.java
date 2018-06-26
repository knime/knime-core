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
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;
import static org.knime.core.node.workflow.InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.WebResourceController.WizardPageContent;



/**
 * 6713: Metanodes cause error during execution of workflow in wrapped node wizard execution mode
 * https://bugs.knime.org/show_bug.cgi?id=6713
 *
 * The pure presence of a meta node in wizard workflow broke the wizard execution.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6713_newWizardContainingOldMetanode extends WorkflowTestCase {

    private NodeID m_subnodeFirstPage_6;
    private NodeID m_subnodeSecondPage_7;
    private NodeID m_subnodeThirdPage_9;
    private NodeID m_metaNode_8;
    private NodeID m_javaEdit_InMetaNode_8_4;
    private NodeID m_javaEdit_3;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subnodeFirstPage_6 = baseID.createChild(6);
        m_subnodeSecondPage_7 = baseID.createChild(7);
        m_subnodeThirdPage_9 = baseID.createChild(9);
        m_metaNode_8 = baseID.createChild(8);
        m_javaEdit_InMetaNode_8_4 = m_metaNode_8.createChild(4);
        m_javaEdit_3 = baseID.createChild(3);
    }

    @Test
    public void testExecuteAll() throws Exception {
        checkState(m_subnodeFirstPage_6, CONFIGURED);
        checkStateOfMany(IDLE, m_subnodeSecondPage_7, m_subnodeThirdPage_9, m_metaNode_8);
        executeAllAndWait();
        final WorkflowManager wfm = getManager();
        checkState(wfm, EXECUTED);
    }

    @Test
    public void testWizardStepThrough() throws Exception {
        final WorkflowManager wfm = getManager();
        assertTrue("should have new wizard execution", WebResourceController.hasWizardExecution(wfm));
        WizardExecutionController wizardController = wfm.getWizardExecutionController();
        wizardController.stepFirst();

        waitWhile(wfm, new WizardHold(wfm));
        assertTrue("should have steps", wizardController.hasCurrentWizardPage());
        checkState(m_subnodeFirstPage_6, EXECUTED);
        checkState(m_subnodeSecondPage_7, CONFIGURED_MARKEDFOREXEC);
        checkState(m_javaEdit_3, CONFIGURED_MARKEDFOREXEC);
        WizardPageContent currentWizardPage = wizardController.getCurrentWizardPage(); // outside loop
        // TODO: load something real
        wizardController.loadValuesIntoCurrentPage(Collections.<String, String>emptyMap());
        // TODO check IDs
//        assertEquals(m_subnodeFirstPage_6.toString(), currentWizardPage.getPageNodeID());


        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(wfm, CONFIGURED_MARKEDFOREXEC, UNCONFIGURED_MARKEDFOREXEC);
        checkStateOfMany(EXECUTED, m_subnodeFirstPage_6, m_subnodeSecondPage_7, m_javaEdit_3);
        checkStateOfMany(CONFIGURED_MARKEDFOREXEC, m_metaNode_8, m_javaEdit_InMetaNode_8_4);
        checkStateOfMany(UNCONFIGURED_MARKEDFOREXEC, m_subnodeThirdPage_9);
        assertTrue("should have steps (2nd page)", wizardController.hasCurrentWizardPage());
        currentWizardPage = wizardController.getCurrentWizardPage(); // inside loop 1st time
//        assertEquals(m_subnodeSecondPage_7.toString(), currentWizardPage.getPageNodeID());

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        checkState(m_subnodeThirdPage_9, EXECUTED);
        checkState(wfm, EXECUTED);
        assertTrue("should have steps (3rd/last page)", wizardController.hasCurrentWizardPage());
        currentWizardPage = wizardController.getCurrentWizardPage(); // inside loop 2nd time
//        assertEquals(m_subnodeSecondPage_7.toString(), currentWizardPage.getPageNodeID());

        wizardController.stepNext();
        waitWhile(wfm, new WizardHold(wfm));
        assertFalse("should have no more pages", wizardController.hasCurrentWizardPage());
        checkState(wfm, EXECUTED);
    }

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

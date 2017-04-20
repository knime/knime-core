/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   20 Apr 2017 (albrecht): created
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

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class TestSubnodeView extends WorkflowTestCase {

    private NodeID m_subnodeID;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subnodeID = new NodeID(baseID, 6);
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
     * Simple test if a combined view can succesfully be created and contains the layout and expected nodes for display
     * @throws Exception
     */
    @Test
    public void testExecuteAndCreateSubnodeView() throws Exception {
        executeAndWait(m_subnodeID);
        assertTrue("Subnode should be executed.", getManager().getNodeContainer(m_subnodeID).getNodeContainerState().isExecuted());
        SinglePageWebResourceController spc = new SinglePageWebResourceController(getManager(), m_subnodeID);
        assertTrue("Should have subnode view", spc.isSubnodeViewAvailable());
        WizardPageContent page = spc.getWizardPage();
        assertNotNull("Page content should be available", page);
        Map<NodeIDSuffix, WizardNode> pageMap = page.getPageMap();
        assertNotNull("Page map should be available", pageMap);
        assertTrue("Page should contain three nodes", pageMap.size() == 3);
        String layout = page.getLayoutInfo();
        assertNotNull("Page layout should be available", layout);
        assertTrue("Layout should contain test string", layout.contains("testString"));
    }
}

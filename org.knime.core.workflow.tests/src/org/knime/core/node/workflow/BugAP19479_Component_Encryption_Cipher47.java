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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;

/**
 * Workflow created in AP 4.7 containing a single locked component.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP19479_Component_Encryption_Cipher47 extends WorkflowTestCase {

    /**
     * Tries to load a workflow that contains a locked component created in AP 4.7.
     * This should fail for every version <= 4.6.1 and succeed with all later ones.
     */
    @Test
    public void load47LockedComponentTest() throws Exception {
        final var workflowDir = getDefaultWorkflowDirectory();
        final var result = loadWorkflow(workflowDir, new ExecutionMonitor());
        final var wfm = result.getWorkflowManager();
        setManager(wfm);

        assertFalse("Loading should not produce errors.", result.hasErrors());
        final NodeID lockedID = wfm.getID().createChild(2);
        final var locked = wfm.getNodeContainer(lockedID);
        assertTrue("Locked component should exist.", locked instanceof SubNodeContainer);
        
        final NodeID innerID = lockedID.createChild(0).createChild(1);
        final var inner = ((SubNodeContainer) locked).getWorkflowManager().getNodeContainer(innerID);
        assertTrue("Inner node should be loadable.", inner instanceof NativeNodeContainer);
        assertEquals("Table Creator", inner.getName());
    }

}

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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the handling of file store port objects, which are collected/created in loop end nodes.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug5227_FileStorePortObjectAndLoops extends WorkflowTestCase {

    private NodeID m_dataGen1;
    private NodeID m_loopStart3;
    private NodeID m_createFS2;
    private NodeID m_loopEndFS4;
    private NodeID m_fsPortToCell5;
    private NodeID m_testFS6;

    @Before
    public void setUp() throws Exception {
        // will save the workflow in one of the test ...don't write SVN folder
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen1 = new NodeID(baseID, 1);
        m_createFS2 = new NodeID(baseID, 2);
        m_loopStart3 = new NodeID(baseID, 3);
        m_loopEndFS4 = new NodeID(baseID, 4);
        m_fsPortToCell5 = new NodeID(baseID, 5);
        m_testFS6 = new NodeID(baseID, 6);
    }

    @Test
    public void testStatusAndCountAfterLoad() throws Exception {
        checkState(m_loopStart3, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndFS4, InternalNodeContainerState.EXECUTED);
        checkState(m_fsPortToCell5, InternalNodeContainerState.EXECUTED);
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        File startFSDir = getFileStoresDirectory(m_loopStart3);
        assertEquals(10 + (5 - 1) * 5, countFilesInDirectory(startFSDir));
        // there should be no other nodes having a file store w/ directory
        for (SingleNodeContainer snc : iterateSNCs(getManager(), true)) {
            final NodeID id = snc.getID();
            if (!id.equals(m_loopStart3)) {
                File fsDir = getFileStoresDirectory(id);
                assertNull(fsDir);
            }
        }
    }

    @Test
    public void testExecuteAllAndCountFileStores() throws Exception {
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        assertEquals(1, getWriteFileStoreHandlers().size());
        executeAllAndWait();
        checkState(m_testFS6, InternalNodeContainerState.EXECUTED);
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testReExecuteAll() throws Exception {
        reset(m_dataGen1);
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
        File startFSDir = getFileStoresDirectory(m_loopStart3);
        assertEquals(10 + (5 - 1) * 5, countFilesInDirectory(startFSDir));
    }

    @Test
    public void testReExecuteTestNodeAfterLoad() throws Exception {
        reset(m_testFS6);
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_testFS6);
        checkState(m_testFS6, InternalNodeContainerState.EXECUTED);
    }

}

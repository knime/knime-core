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
 *
 * History
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Bundle version is saved in 2.10.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug5207_BundleVersionInWorkflow extends WorkflowTestCase {

    private NodeID m_tableCreator1;
    private NodeID m_columnFilter2;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreator1 = new NodeID(baseID, 1);
        m_columnFilter2 = new NodeID(baseID, 2);
    }

    @Test
    public void testBundleVersionOnLoad() throws Exception {
        checkState(m_tableCreator1, InternalNodeContainerState.EXECUTED);

        // table creator is executed and must have version as from execution time (which was faked but still...)
        NativeNodeContainer tableCreatorNC = (NativeNodeContainer)getManager().getNodeContainer(m_tableCreator1);
        NodeAndBundleInformationPersistor tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(tableCreatorNC.getNodeModel().getClass());
        assertEquals("1.1.1.20140523", tableBundleInfo.getBundleVersion().get().toString());
        final String tableBundleInfoVersion =
            tableBundleInfo.getBundleVersion().isPresent() ? tableBundleInfo.getBundleVersion().get().toString() : null;
        assertFalse(tableBundle.getVersion().toString().equals(tableBundleInfoVersion)); // must not be the same
        assertEquals(tableBundle.getSymbolicName(), tableBundleInfo.getBundleSymbolicName().get());

        // col filter is not executed and has current bundle version as version information
        // (stored in workflow but ignored during load)
        NativeNodeContainer colFilterNC = (NativeNodeContainer)getManager().getNodeContainer(m_columnFilter2);
        NodeAndBundleInformationPersistor colFilterBundleInfo = colFilterNC.getNodeAndBundleInformation();
        Bundle colFilterBundle = FrameworkUtil.getBundle(colFilterNC.getNodeModel().getClass());
        assertEquals(colFilterBundle.getVersion().toString(), colFilterBundleInfo.getBundleVersion().get().toString());
        assertEquals(colFilterBundle.getSymbolicName(), colFilterBundleInfo.getBundleSymbolicName().get());
    }

    @Test
    public void testBundleVersionAfterReexecute() throws Exception {
        reset(m_tableCreator1);
        // table creator is executed and must have version as from execution time (which was faked but still...)
        NativeNodeContainer tableCreatorNC = (NativeNodeContainer)getManager().getNodeContainer(m_tableCreator1);
        NodeAndBundleInformationPersistor tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(tableCreatorNC.getNodeModel().getClass());
        assertEquals(tableBundle.getVersion().toString(), tableBundleInfo.getBundleVersion().get().toString());
        assertEquals(tableBundle.getSymbolicName(), tableBundleInfo.getBundleSymbolicName().get());

        executeAndWait(m_tableCreator1);
        tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        assertEquals(tableBundle.getVersion().toString(), tableBundleInfo.getBundleVersion().get().toString());
    }

    @Test
    public void testBundleVersionAfterCopyPaste() throws Exception {
        WorkflowCopyContent.Builder copyContent = WorkflowCopyContent.builder();
        copyContent.setNodeIDs(m_tableCreator1);
        WorkflowCopyContent pasteContent = getManager().copyFromAndPasteHere(getManager(), copyContent.build());
        NodeID pasteID = pasteContent.getNodeIDs()[0];

        // bundle version number is reset after copy & paste
        NativeNodeContainer copiedTableNC = (NativeNodeContainer)getManager().getNodeContainer(pasteID);
        NodeAndBundleInformationPersistor copiedTableBundleInfo = copiedTableNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(copiedTableNC.getNodeModel().getClass());
        assertEquals(tableBundle.getVersion().toString(), copiedTableBundleInfo.getBundleVersion().get().toString());
        assertEquals(tableBundle.getSymbolicName(), copiedTableBundleInfo.getBundleSymbolicName().get());
    }

}

/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.NodeAndBundleInformation;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Bundle version is saved in 2.10.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class Bug5207_BundleVersionInWorkflow extends WorkflowTestCase {

    private NodeID m_tableCreator1;
    private NodeID m_columnFilter2;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreator1 = new NodeID(baseID, 1);
        m_columnFilter2 = new NodeID(baseID, 2);
    }

    public void testBundleVersionOnLoad() throws Exception {
        checkState(m_tableCreator1, InternalNodeContainerState.EXECUTED);

        // table creator is executed and must have version as from execution time (which was faked but still...)
        NativeNodeContainer tableCreatorNC = (NativeNodeContainer)getManager().getNodeContainer(m_tableCreator1);
        NodeAndBundleInformation tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(tableCreatorNC.getNodeModel().getClass());
        assertEquals("1.1.1.20140523", tableBundleInfo.getBundleVersion().toString());
        assertFalse(tableBundle.getVersion() == tableBundleInfo.getBundleVersion()); // must not be the same
        assertEquals(tableBundle.getSymbolicName(), tableBundleInfo.getBundleSymbolicName());

        // col filter is not executed and has current bundle version as version information
        // (stored in workflow but ignored during load)
        NativeNodeContainer colFilterNC = (NativeNodeContainer)getManager().getNodeContainer(m_columnFilter2);
        NodeAndBundleInformation colFilterBundleInfo = colFilterNC.getNodeAndBundleInformation();
        Bundle colFilterBundle = FrameworkUtil.getBundle(colFilterNC.getNodeModel().getClass());
        assertEquals(colFilterBundle.getVersion(), colFilterBundleInfo.getBundleVersion());
        assertEquals(colFilterBundle.getSymbolicName(), colFilterBundleInfo.getBundleSymbolicName());
    }

    public void testBundleVersionAfterReexecute() throws Exception {
        reset(m_tableCreator1);
        // table creator is executed and must have version as from execution time (which was faked but still...)
        NativeNodeContainer tableCreatorNC = (NativeNodeContainer)getManager().getNodeContainer(m_tableCreator1);
        NodeAndBundleInformation tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(tableCreatorNC.getNodeModel().getClass());
        assertEquals(tableBundle.getVersion(), tableBundleInfo.getBundleVersion());
        assertEquals(tableBundle.getSymbolicName(), tableBundleInfo.getBundleSymbolicName());

        executeAndWait(m_tableCreator1);
        tableBundleInfo = tableCreatorNC.getNodeAndBundleInformation();
        assertEquals(tableBundle.getVersion(), tableBundleInfo.getBundleVersion());
    }

    public void testBundleVersionAfterCopyPaste() throws Exception {
        WorkflowCopyContent copyContent = new WorkflowCopyContent();
        copyContent.setNodeIDs(m_tableCreator1);
        WorkflowCopyContent pasteContent = getManager().copyFromAndPasteHere(getManager(), copyContent);
        NodeID pasteID = pasteContent.getNodeIDs()[0];

        // bundle version number is reset after copy & paste
        NativeNodeContainer copiedTableNC = (NativeNodeContainer)getManager().getNodeContainer(pasteID);
        NodeAndBundleInformation copiedTableBundleInfo = copiedTableNC.getNodeAndBundleInformation();
        Bundle tableBundle = FrameworkUtil.getBundle(copiedTableNC.getNodeModel().getClass());
        assertEquals(tableBundle.getVersion(), copiedTableBundleInfo.getBundleVersion());
        assertEquals(tableBundle.getSymbolicName(), copiedTableBundleInfo.getBundleSymbolicName());
    }

}

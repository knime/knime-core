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
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.NodeSettings;


/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4404_CopyPasteWithSameInstance extends WorkflowTestCase {

    private NodeID m_meta100_5;
    private NodeID m_javaSnippet200_7;
    private NodeID m_javaSnippet300_8;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_meta100_5 = new NodeID(baseID, 5);
        m_javaSnippet200_7 = new NodeID(baseID, 7);
        m_javaSnippet300_8 = new NodeID(baseID, 8);
    }

    public void testCopyAndPasteTwice() throws Exception {
        WorkflowManager manager = getManager();
        WorkflowCopyContent copyContent = new WorkflowCopyContent();
        copyContent.setNodeIDs(m_meta100_5);
        WorkflowPersistor copyPersistor = manager.copy(copyContent); // copy once but paste twice
        WorkflowCopyContent paste1 = manager.paste(copyPersistor);
        NodeID meta200 = paste1.getNodeIDs()[0];
        WorkflowCopyContent paste2 = manager.paste(copyPersistor);
        NodeID meta300 = paste2.getNodeIDs()[0];
        fixDataGeneratorSettings(meta200, 200);
        fixDataGeneratorSettings(meta300, 300);
        
        manager.addConnection(meta200, 0, m_javaSnippet200_7, 1);
        manager.addConnection(meta300, 0, m_javaSnippet300_8, 1);
        executeAllAndWait();
        checkState(m_javaSnippet200_7, InternalNodeContainerState.EXECUTED);
        checkState(m_javaSnippet300_8, InternalNodeContainerState.EXECUTED);
    }
    
    private void fixDataGeneratorSettings(final NodeID metanodeID, final int rowCount) throws Exception {
        final NodeID dataGenID = new NodeID(metanodeID, 1);
        final WorkflowManager manager = getManager();
        WorkflowManager metanode = (WorkflowManager) manager.getNodeContainer(metanodeID);
        metanode.getNodeContainer(dataGenID);
        NodeSettings ns = new NodeSettings("settings");
        metanode.saveNodeSettings(dataGenID, ns);
        NodeSettings model = ns.getNodeSettings("model");
        model.addInt("patcount", rowCount);
        metanode.loadNodeSettings(dataGenID, ns);
    }
    
}

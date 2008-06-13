/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 * History
 *   21.02.2006 (sieb): created
 */
package org.knime.workbench.editor2;

import java.util.Collections;
import java.util.List;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;


/**
 * Holds a clipboard object and additional information.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ClipboardObject {
    /**
     * The content to hold in the clipboard.
     */
    private final List<NodeID>m_nodeIDs;

    private final WorkflowManager m_sourceWorkflow;
    
    /**
     * To remember how often the object was retrieved.
     */
    private int m_retrievalCounter;

    public ClipboardObject(final WorkflowManager sourceWorkflow,
            List<NodeID>nodeIds) {
        m_sourceWorkflow = sourceWorkflow;
        m_nodeIDs = nodeIds;
        m_retrievalCounter = 0;
    }

    
    public List<NodeID>getNodeIDs(){
        return Collections.unmodifiableList(m_nodeIDs);
    }

    public WorkflowManager getSourceWorkflow() {
        return m_sourceWorkflow;
    }
    
    /**
     * @return returns the number of retrievals of this clipboard object
     */
    public int getRetrievalCounter() {
        return m_retrievalCounter;
    }

    /**
     * Increments the retrieval counter. The correct incrementation is
     * application dependent.
     */
    public void incrementRetrievalCounter() {
        m_retrievalCounter++;
    }
}

/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.core.node.workflow.WorkflowPersistor;


/**
 * Holds a workflow persistor that contains nodes and connections that were
 * copied or cut and a retrieval counter used to determine visual offsets when
 * inserting the nodes.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ClipboardObject {
    
    /** The content to hold in the clipboard. */
    private final WorkflowPersistor m_copyPersistor;

    /** To remember how often the object was retrieved.
     * Used to adjust the coordinates of a node when inserted multiple times. */
    private int m_retrievalCounter;

    /** Create new object, memorize persistor.
     * @param copyPersistor The copy persistor.
     */
    public ClipboardObject(final WorkflowPersistor copyPersistor) {
        m_copyPersistor = copyPersistor;
        m_retrievalCounter = 0;
    }

    
    /** @return the persistor. */
    public WorkflowPersistor getCopyPersistor() {
        return m_copyPersistor;
    }

    /**
     * @return the (incremented) retrieval counter. 
     */
    public int incrementAndGetRetrievalCounter() {
        return ++m_retrievalCounter;
    }
}

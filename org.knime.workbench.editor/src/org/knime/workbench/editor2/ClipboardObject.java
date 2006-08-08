/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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


/**
 * Holds a clipboard object and additional information.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ClipboardObject {
    /**
     * The content to hold in the clipboard.
     */
    private final Object m_content;

    /**
     * To remember how often the object was retrieved.
     */
    private int m_retrievalCounter;

    public ClipboardObject(final Object content) {
        m_content = content;

        m_retrievalCounter = 0;
    }

    /**
     * @return returns the content of this clipboard object
     */
    public Object getContent() {
        return m_content;
    }

    /**
     * @return returns the number of retrievals of this clipboard object
     */
    public int getRetrievalCounter() {
        return m_retrievalCounter;
    }

    /**
     * Increments the retrieval counter. The correct incrementation is
     * application dependant.
     */
    public void incrementRetrievalCounter() {
        m_retrievalCounter++;
    }
}

/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.09.2006 (gabriel): created
 */
package org.knime.core.node;

/**
 * Progress events are fired when the progress information has changed, either
 * the progress value between 0 and 1, or the progress message.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeProgressEvent {
    
    /** Keeps the progress value. */
    private final Double m_progress;
    
    /** Keeps the progress information. */
    private final String m_message;

    /**
     * Create a progress event based on progress value and message. Both 
     * arguments can be <code>null</code>.
     * @param progress The progress value or null.
     * @param message The progress message or null.
     */
    NodeProgressEvent(final Double progress, final String message) {
        m_progress = progress;
        m_message  = message;
    }
    
    /**
     * If the progress value has changed.
     * @return <code>true</code> if the value has changed.
     */
    public boolean hasProgress() {
        return m_progress != null;
    }
        
    /**
     * Current progress value or null.
     * @return The progress value between 0 and 1, or null.
     */
    public Double getProgress() {
        return m_progress;
    }
    
    /**
     * Current progress message or null.
     * @return The progress message or null.
     */
    public String getMessage() {
        return m_message;
    }
 
    /**
     * If the progress message has changed.
     * @return <code>true</code> if the progress message has changed.
     */
    public boolean hasMessage() {
        return m_message != null;
    }

}

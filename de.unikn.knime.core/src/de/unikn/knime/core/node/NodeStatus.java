/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.08.2005 (gabriel): created
 */
package de.unikn.knime.core.node;

/**
 * Keeps info about the node's status. No events are send when the message
 * changes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public final class NodeStatus {

    // ------------ The possible node states ----------------------------------
    /** Node reset. */
    public static final int RESET = 1001;

    /** Node execution started. */
    public static final int START_EXECUTE = 1002;

    /** Node execution finished. */
    public static final int END_EXECUTE = 1003;

    /** Node configured. */
    public static final int CONFIGURED = 1004;

    /** Warning during execution. */
    public static final int WARNING = 2001;

    /** Warning during execution. */
    public static final int ERROR = 3001;
    
    /** Indicates a general status change. */
    public static final int STATUS_CHANGED = 4001;
    
    /** Indicates extra infor changes of meta workflows. */
    public static final int STATUS_EXTRA_INFO_CHANGED = 5001;

    /** The status code. */
    private final int m_statusId;

    /** The internal message to show or null if not available. */
    private final String m_message;

    /**
     * Create new status object with an empty message and given id.
     * 
     * @param statusId The status id for this status object.
     */
    public NodeStatus(final int statusId) {
        this(statusId, null);
    }

    /**
     * Create new message, with Id and message.
     * 
     * @param statusId the status id for this status object
     * @param messsage free text description
     */
    public NodeStatus(final int statusId, final String messsage) {
        m_message = messsage;
        m_statusId = statusId;
    }

    /**
     * @return The current status message or null if not available.
     */
    public String getMessage() {
        return m_message;
    }

    /**
     * @return <code>true</code> if no message available.
     */
    public boolean messageAvailable() {
        return m_message != null;
    }

    /**
     * @return The status id.
     */
    public int getStatusId() {
        return m_statusId;
    }

}

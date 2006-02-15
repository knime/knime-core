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
 *   19.04.2005 (georg): created
 *   12.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import java.text.DateFormat;
import java.util.Date;

/**
 * Event-class for workflow events. Note that not all event types use all of the
 * fields, meaning that some of them might be <code>null</code> depending on
 * the type of the event (or whatever meaningless value was given to those
 * fields during construction of the event - meaning all values are always
 * specified but not all carry meaning).
 * 
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowEvent {

    /** Event: node added to workflow. */
    public static final int NODE_ADDED = 0;

    /** Event: node removed from workflow. */
    public static final int NODE_REMOVED = 1;

    /** Event: connection added to workflow. */
    public static final int CONNECTION_ADDED = 2;

    /** Event: connection removed from workflow. */
    public static final int CONNECTION_REMOVED = 3;

    /** Event: pool of executable nodes has changed. */
    public static final int EXEC_POOL_CHANGED = 4;

    /** Event: pool of executable nodes is now empty: execute wf finished. */
    public static final int EXEC_POOL_DONE = 5;

    /** Event: node was reset. */
    public static final int NODE_RESET = 10;

    /** Event: node was reset. */
    public static final int NODE_CONFIGURED = 11;
    
    /** Event: extra info attached to node has changed. */
    public static final int NODE_EXTRAINFO_CHANGED = 100;
    
    /** Event: extra info attached to connection has changed. */
    public static final int CONNECTION_EXTRAINFO_CHANGED = 200;

    /* Private fields, not always all valid */
    private int m_id;
    private long m_timestamp;
    private int m_eventType;
    private Object m_oldValue;
    private Object m_newValue;

    /**
     * Creates a new workflow event.
     * 
     * @param type event type
     * @param nodeID The ID for the affected node
     * @param oldValue value before the change (may be <code>null</code>)
     * @param newValue value after the change (may be <code>null</code>)
     */
    public WorkflowEvent(final int type, final int nodeID,
            final Object oldValue, final Object newValue) {
        m_eventType = type;
        m_id = nodeID;
        m_oldValue = oldValue;
        m_newValue = newValue;
        m_timestamp = System.currentTimeMillis();
    }

    /**
     * @return Returns the eventType.
     */
    public int getEventType() {
        return m_eventType;
    }

    /**
     * @return Returns the node ID of the affected node.
     */
    public int getID() {
        return m_id;
    }

    /**
     * @return Returns the newValue.
     */
    public Object getNewValue() {
        return m_newValue;
    }

    /**
     * @return Returns the oldValue.
     */
    public Object getOldValue() {
        return m_oldValue;
    }

    /**
     * Gives a nicer representation of this event. For debugging purposes only.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {

        return "WorkflowEvent [type=" + m_eventType + ";old=" + m_oldValue
                + ";new=" + m_newValue + ";timestamp="
                + DateFormat.getDateInstance().format(new Date(m_timestamp))
                + "]";
    }

}

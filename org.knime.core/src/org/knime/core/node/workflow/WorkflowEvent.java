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
 *   19.04.2005 (georg): created
 *   12.01.2006 (mb): clean up for code review
 */
package org.knime.core.node.workflow;

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


    /**
     * Enumeration of all workflow events that are interesting for the GUI.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public enum Type {
        /** Event: connection added to workflow. */
     CONNECTION_ADDED,
     /** Event: UI info attached to connection has changed. */
     CONNECTION_UI_CHANGED,
     /** Event: connection removed from workflow. */
     CONNECTION_REMOVED,
     /** Event: node added to workflow. */
     NODE_ADDED,
     /** Event: node was configured. */
     NODE_CONFIGURED,
     /** Event: UI info attached to node has changed. */
     NODE_UI_CHANGED,
     /** Event: node execution is finished. */
     NODE_FINISHED,
     /** Event: node removed from workflow. */
     NODE_REMOVED,
     /** Event: node was reset. */
     NODE_RESET,
     /** Event: node is being executed. */
     NODE_STARTED,
     /** Event: node is waiting for execution. */
     NODE_WAITING
    }

    private final NodeID m_id;
    private final long m_timestamp;
    private final Object m_oldValue;
    private final Object m_newValue;
    private final Type m_type;


    /**
     * Creates a new workflow event.
     *
     * @param type the type of the event
     * @param nodeID The ID for the affected node
     * @param oldValue value before the change (may be <code>null</code>)
     * @param newValue value after the change (may be <code>null</code>)
     */
    public WorkflowEvent(final Type type, final NodeID nodeID,
            final Object oldValue, final Object newValue) {
        m_id = nodeID;
        m_oldValue = oldValue;
        m_newValue = newValue;
        m_type = type;
        m_timestamp = System.currentTimeMillis();
    }

    /**
     *
     * @return type of event
     */
    public Type getType() {
        return m_type;
    }

    /**
     * @return Returns the node ID of the affected node.
     */
    public NodeID getID() {
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "WorkflowEvent [type=" + getClass().getSimpleName()
                + ";old=" + m_oldValue
                + ";new=" + m_newValue + ";timestamp="
                + DateFormat.getDateTimeInstance().format(new Date(m_timestamp))
                + "]";
    }
}

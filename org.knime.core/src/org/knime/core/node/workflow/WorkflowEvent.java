/* 
 * -------------------------------------------------------------------
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
public abstract class WorkflowEvent {
    /** Event: node added to workflow. */
    public static class NodeAdded extends WorkflowEvent {
        /**
         * Creates a new "node added" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue <code>null</code>
         * @param newValue the newly created {@link NodeContainer}
         */
        public NodeAdded(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: node removed from workflow. */
    public static class NodeRemoved extends WorkflowEvent {
        /**
         * Creates a new "node removed" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public NodeRemoved(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }

    /** Event: connection added to workflow. */
    public static class ConnectionAdded extends WorkflowEvent {
        /**
         * Creates a new "connection added" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public ConnectionAdded(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }

    /** Event: connection removed from workflow. */
    public static class ConnectionRemoved extends WorkflowEvent {
        /**
         * Creates a new "connection removed" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public ConnectionRemoved(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: node was reset. */
    public static class NodeReset extends WorkflowEvent {
        /**
         * Creates a new "node reset" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public NodeReset(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: node was reset. */
    public static class NodeConfigured extends WorkflowEvent {
        /**
         * Creates a new "node configured" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public NodeConfigured(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: extra info attached to node has changed. */
    public static class NodeExtrainfoChanged extends WorkflowEvent {
        /**
         * Creates a new "node extra info changed" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public NodeExtrainfoChanged(final int nodeID, final Object oldValue,
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: extra info attached to connection has changed. */
    public static class ConnectionExtrainfoChanged extends WorkflowEvent {
        /**
         * Creates a new "connection extra info changed" event.
         *  
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>)
         */
        public ConnectionExtrainfoChanged(final int nodeID,
                final Object oldValue, final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }

    /** Event: node is being executed. */
    public static class NodeStarted extends WorkflowEvent {
        /**
         * Creates a new "node started execution" event.
         * 
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>) 
         */
        public NodeStarted(final int nodeID,
                final Object oldValue, final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }


    /** Event: node execution is finished. */
    public static class NodeFinished extends WorkflowEvent {
        /**
         * Creates a new "node finished execution" event.
         * 
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>) 
         */
        public NodeFinished(final int nodeID,
                final Object oldValue, final Object newValue) {
            super(nodeID, oldValue, newValue);
        }        
    }
    
    /** Event: node is waiting for execution. */
    public static class NodeWaiting extends WorkflowEvent {
        /**
         * Creates a new "node waiting for execution" event.
         * 
         * @param nodeID the ID for the affected node
         * @param oldValue value before the change (may be <code>null</code>)
         * @param newValue value after the change (may be <code>null</code>) 
         */
        public NodeWaiting(final int nodeID, final Object oldValue, 
                final Object newValue) {
            super(nodeID, oldValue, newValue);
        }
    }

    private int m_id;
    private long m_timestamp;
    private Object m_oldValue;
    private Object m_newValue;

    /**
     * Creates a new workflow event.
     * 
     * @param nodeID The ID for the affected node
     * @param oldValue value before the change (may be <code>null</code>)
     * @param newValue value after the change (may be <code>null</code>)
     */
    public WorkflowEvent(final int nodeID,
            final Object oldValue, final Object newValue) {
        m_id = nodeID;
        m_oldValue = oldValue;
        m_newValue = newValue;
        m_timestamp = System.currentTimeMillis();
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
    @Override
    public String toString() {
        return "WorkflowEvent [type=" + getClass().getSimpleName()
                + ";old=" + m_oldValue
                + ";new=" + m_newValue + ";timestamp="
                + DateFormat.getDateTimeInstance().format(new Date(m_timestamp))
                + "]";
    }
}

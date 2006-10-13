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
 *   05.08.2005 (gabriel): created
 */
package org.knime.core.node;

/**
 * Keeps info about the node's status. No events are send when the message
 * changes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class NodeStatus {
    
    /** Node reset. */
    public static class Reset extends NodeStatus {
        /**
         * Creates a new "reset" node status.
         */
        public Reset() { super(); }

        /**
         * Creates a new "reset" node status.
         * 
         * @param messsage an additional message
         */
        public Reset(final String messsage) { super(messsage); }
    }
    
    /** Node queued. */
    public static class Queued extends NodeStatus {
        /**
         * Creates a new "queued" node status.
         */
        public Queued() { super(); }

        /**
         * Creates a new "queued" node status.
         * 
         * @param messsage an additional message
         */
        public Queued(final String messsage) { super(messsage); }
    }
    
    /** Node execution started. */
    public static class StartExecute extends NodeStatus {
        /**
         * Creates a new "start execute" node status.
         */
        public StartExecute() { super(); }

        /**
         * Creates a new "start execute" node status.
         * 
         * @param messsage an additional message
         */
        public StartExecute(final String messsage) { super(messsage); }
    }
    
    /** Node execution finished. */
    public static class EndExecute extends NodeStatus {
        /**
         * Creates a new "end execute" node status.
         */
        public EndExecute() { super(); }

        /**
         * Creates a new "end execute" node status.
         * 
         * @param messsage an additional message
         */
        public EndExecute(final String messsage) { super(messsage); }
    }

    /** Node configured. */
    public static class Configured extends NodeStatus {
        /**
         * Creates a new "configured" node status.
         */
        public Configured() { super(); }

        /**
         * Creates a new "configured" node status.
         * 
         * @param messsage an additional message
         */
        public Configured(final String messsage) { super(messsage); }
    }

    /** Custom name set to node. */
    public static class CustomName extends NodeStatus {
        /**
         * Creates a new "custom name" node status.
         */
        public CustomName() { super(); }

        /**
         * Creates a new "custom name" node status.
         * 
         * @param messsage an additional message
         */
        public CustomName(final String messsage) { super(messsage); }
    }

    /** Custom description set to node. */
    public static class CustomDescription extends NodeStatus {
        /**
         * Creates a new "custom description" node status.
         */
        public CustomDescription() { super(); }

        /**
         * Creates a new "custom description" node status.
         * 
         * @param messsage an additional message
         */
        public CustomDescription(final String messsage) { super(messsage); }
    }

    /** Warning during execution. */
    public static class Warning extends NodeStatus {
        /**
         * Creates a new "warning" node status.
         * 
         * @param messsage the warning message
         */
        public Warning(final String messsage) { super(messsage); }
    }

    /** Error during execution. */
    public static class Error extends NodeStatus {
        /**
         * Creates a new "error" node status.
         * 
         * @param messsage the error message
         */
        public Error(final String messsage) { super(messsage); }
    }

    /** Indicates a general status change. */
    public static class StatusChanged extends NodeStatus {
        /**
         * Creates a new "status changed" node status.
         */
        public StatusChanged() { super(); }

        /**
         * Creates a new "status changed" node status.
         * 
         * @param messsage an additional message
         */
        public StatusChanged(final String messsage) { super(messsage); }
    }

    /** Indicates extra info changes of meta workflows. */
    public static class ExtrainfoChanged extends NodeStatus {
        /**
         * Creates a new "extra info changed" node status.
         */
        public ExtrainfoChanged() { super(); }

        /**
         * Creates a new "extra info changed" node status.
         * 
         * @param messsage an additional message
         */
        public ExtrainfoChanged(final String messsage) { super(messsage); }
    }

    /** Indicates that the execution of the node has been canceled by the
     *  user. */
    public static class ExecutionCanceled extends NodeStatus {
        /**
         * Creates a new "extra info changed" node status.
         */
        public ExecutionCanceled() { super(); }

        /**
         * Creates a new "extra info changed" node status.
         * 
         * @param messsage an additional message
         */
        public ExecutionCanceled(final String messsage) { super(messsage); }
    }

    
    /** The internal message to show or null if not available. */
    private final String m_message;

    /**
     * Create new status object with an empty message. 
     */
    public NodeStatus() {
        this(null);
    }

    /**
     * Create new status object with a  message.
     * 
     * @param messsage free text description
     */
    public NodeStatus(final String messsage) {
        m_message = messsage;
    }

    /**
     * @return The current status message or null if not available.
     */
    public String getMessage() {
        return m_message;
    }
}

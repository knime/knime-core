/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.workflow;

/**
 *
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class NodeMessage {

    /** Enum for the possible types of messages, sorted by severity. */
    public enum Type {
        /** Reset - reset the warning/error. */
        RESET,
        /** Warning - not fatal . */
        WARNING,
        /** Error - fatal, node not executable. */
        ERROR
    }
    
    /** Convenience member to signal that there is no message. */
    public static final NodeMessage NONE = new NodeMessage(Type.RESET, "");

    private final String m_message;

    private final Type m_type;

    /**
     * Creates a message with the type and the message.
     *
     * @param messageType the message type (error or warning)
     * @param message the message
     */
    public NodeMessage(final Type messageType, final String message) {
        m_message = message;
        m_type = messageType;
    }

    /**
     *
     * @return the message
     */
    public String getMessage() {
        return m_message;
    }

    /**
     *
     * @return the type
     */
    public Type getMessageType() {
        return m_type;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getMessageType() + ": " + getMessage();
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_type.hashCode() ^ getMessage().hashCode();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NodeMessage)) {
            return false;
        }
        NodeMessage other = (NodeMessage)obj;
        return m_type.equals(other.m_type) && m_message.equals(other.m_message);
    }
    
    /**
     * Merges two messages. The result message will have the most severe type
     * (e.g. if m1 is WARNING and m2 is ERROR the output is ERROR) and a 
     * concatenated message string, delimited by a line break.
     * @param m1 Message 1
     * @param m2 Message 2
     * @return A merged message
     */
    public static final NodeMessage merge(
            final NodeMessage m1, final NodeMessage m2) {
        if (m1.equals(m2)) {
            return m1;
        }
        int max = Math.max(m1.m_type.ordinal(), m2.m_type.ordinal());
        Type type = Type.values()[max];
        StringBuilder message = new StringBuilder();
        message.append(m1.m_message);
        if (message.length() > 0 && m2.m_message.length() > 0) {
            message.append("\n");
        }
        message.append(m2.m_message);
        return new NodeMessage(type, message.toString());
    }

}

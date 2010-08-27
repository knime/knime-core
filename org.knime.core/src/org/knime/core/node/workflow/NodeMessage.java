/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

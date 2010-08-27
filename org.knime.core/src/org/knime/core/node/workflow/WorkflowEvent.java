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
     /** Event: connection removed from workflow. */
     CONNECTION_REMOVED,
     /** Event: node added to workflow. */
     NODE_ADDED,
     /** Event: node removed from workflow. */
     NODE_REMOVED,
     /** Event: workflow is marked as dirty. */
     WORKFLOW_DIRTY,
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
        StringBuilder b = new StringBuilder(getClass().getSimpleName());
        b.append(" [type=").append(m_type);
        b.append(";node=").append(m_id);
        b.append(";old=").append(m_oldValue);
        b.append(";new=").append(m_newValue);
        b.append(";timestamp=");
        b.append(DateFormat.getDateTimeInstance().format(
                new Date(m_timestamp)));
        b.append("]");
        return b.toString();
    }
}

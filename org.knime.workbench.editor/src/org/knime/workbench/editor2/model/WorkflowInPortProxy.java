/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.model;

import org.knime.core.node.NodeInPort;
import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowInPortProxy {

    private final NodeInPort m_wrappedPort;
    private final NodeID m_workflowManagerID;

    public WorkflowInPortProxy(final NodeInPort port, final NodeID id) {
        m_wrappedPort = port;
        m_workflowManagerID = id;
    }

    public NodeInPort getPort() {
        return m_wrappedPort;
    }

    public NodeID getID() {
        return m_workflowManagerID;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof WorkflowInPortProxy)) {
            return false;
        }
        WorkflowInPortProxy other = (WorkflowInPortProxy)obj;
        return getID() == other.getID() && getPort() == other.getPort();
    }

    @Override
    public int hashCode() {
        return getID().hashCode() + getPort().hashCode();
    }
}

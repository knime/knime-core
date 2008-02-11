/* -------------------------------------------------------------------
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
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.model;

import org.knime.core.node.NodeOutPort;
import org.knime.core.node.workflow.NodeID;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortProxy {

    private final NodeOutPort m_wrappedOutPort;
    private final NodeID m_nodeId;

    public WorkflowOutPortProxy(final NodeOutPort outPort, final NodeID id) {
        m_wrappedOutPort = outPort;
        m_nodeId = id;
    }

    public NodeOutPort getPort() {
        return m_wrappedOutPort;
    }

    public NodeID getID() {
        return m_nodeId;
    }

}

/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author mb, University of Konstanz
 */
public class WorkflowOutPort extends NodePortAdaptor implements NodeOutPort  {
    
    /** if connected, reference the outport this node is connected to.
     */
    private NodeOutPort m_underlyingPort;
    
    private final NodeInPort m_simulatedInPort;
    
    private final WorkflowManager m_workflowManager;
    
    /**
     * Creates a new output port with a fixed type and index (should unique 
     * to all other output ports of this node) for the given node.
     * 
     * @param wm The workflow manger containing this port
     * @param pType The port's type
     * @param portIndex This port index
     */
    WorkflowOutPort(final WorkflowManager wm, 
            final int portIndex, final PortType pType) {
        super(portIndex, pType);
        m_simulatedInPort = new NodeInPort(portIndex, pType);
        m_workflowManager = wm;
    }

    /**
     * Set a new underlying port - used when the connection inside this
     * workflow to this outgoing port changes. The argument is the new
     * NodeOutPort connected to the outgoing port of the WFM.
     * 
     * @param p new port
     */
    void setUnderlyingPort(final NodeOutPort p) {
        m_underlyingPort = p;
    }

    /** Return a NodeInPort for the WFM's output ports so that the Outport
     * of a node within the WFM can connect to it as an "input". Since InPorts
     * only wrap name/type this is really all it does: it wraps this information
     * as specified during WFM construction into an InPort.
     * 
     * @return fake InPort.
     */
    NodeInPort getSimulatedInPort() {
        return m_simulatedInPort;
    }
    
    /** @return the workflow manager associated with this port */
    WorkflowManager getWorkflowManager() {
        return m_workflowManager;
    }

    /**
     * {@inheritDoc}
     */
    public HiLiteHandler getHiLiteHandler() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getHiLiteHandler();
    }
    
    /**
     * {@inheritDoc}
     */
    public ScopeObjectStack getScopeContextStackContainer() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getScopeContextStackContainer();
    }

    /**
     * {@inheritDoc}
     */
    public PortObject getPortObject() {
        if (m_underlyingPort == null) {
            return null;
        }
        // Note that we can NOT test if the WFM is EXECUTED in order to be sure
        // that we want to return this object (like the SNC does). This can be
        // called twofold: inside the WFM to retrieve data from a WFM-inport
        // (then this WFMOutPort is a member of a WFMInPort) and outside the
        // WFM to retrieve data from WFM itself (when the WFM plays like a
        // normal node.)
        return m_underlyingPort.getPortObject();
    }
    
    /**
     * {@inheritDoc}
     */
    public PortObjectSpec getPortObjectSpec() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObjectSpec();
    }


    /**
     * {@inheritDoc}
     */
    public void openPortView(final String name) {
        if (m_underlyingPort == null) {
            return;
        }
        m_underlyingPort.openPortView(name);
    }

    ///////////////////////////
    // Equals/HashCode/ToString
    ///////////////////////////
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (m_underlyingPort == null) {
            return this == obj;
        }
        return m_underlyingPort.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (m_underlyingPort == null) {
            return System.identityHashCode(this);
        }
        return m_underlyingPort.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_underlyingPort == null) {
            return "<<not connected>>";
        }
        return m_underlyingPort.toString();
    }
    
}

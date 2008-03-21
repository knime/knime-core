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
    
    /** the following flag allows SingleNodeContainers/WFMs to hide
     * the PortObjects after a Node.execute() until the state of the
     * SNC/WFM has been adjusted to "EXECUTED".
     */
    private boolean m_showPortObject = true;

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
    
    void setUnderlyingPort(final NodeOutPort p) {
        m_underlyingPort = p;
    }
    
    NodeInPort getSimulatedInPort() {
        return m_simulatedInPort;
    }
    
    /** @return the workflow manager associated with this port */
    WorkflowManager getWorkflowManager() {
        return m_workflowManager;
    }

    /**
     * Disable/Enable port content - used to make sure port stays in sync
     * with EXECUTED-flag and does not provide content to the outside while
     * the node is officially not (yet) executed.
     * 
     * @param flag true if content is to be seen
     */
    public void showPortObject(final boolean flag) {
        m_showPortObject = flag;
    }
    
    /**
     * @return true if content is to be seen
     */
    public boolean isPortObjectVisible() {
        return m_showPortObject;
    }

    /**
     * @param obj
     * @return
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (m_underlyingPort == null) {
            return this == obj;
        }
        return m_underlyingPort.equals(obj);
    }

    /**
     * @return
     * @see org.knime.core.node.workflow.NodeOutPort#getHiLiteHandler()
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
     * @return
     * @see org.knime.core.node.workflow.NodeOutPort#getPortObject()
     */
    public PortObject getPortObject() {
        if (m_underlyingPort == null) {
            return null;
        }
        // the following test allows SingleNodeContainers/WFMs to hide
        // the PortObjects after a Node.execute() until the state of the
        // SNC/WFM has been adjusted to "EXECUTED"
        return isPortObjectVisible() ? m_underlyingPort.getPortObject() : null;
    }
    
    /**
     * @return
     * @see org.knime.core.node.workflow.NodeOutPort#getPortObjectSpec()
     */
    public PortObjectSpec getPortObjectSpec() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObjectSpec();
    }

    /**
     * @return
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (m_underlyingPort == null) {
            return System.identityHashCode(this);
        }
        return m_underlyingPort.hashCode();
    }

    /**
     * @param name
     * @see org.knime.core.node.workflow.NodeOutPort#openPortView(java.lang.String)
     */
    public void openPortView(String name) {
        if (m_underlyingPort == null) {
            return;
        }
        m_underlyingPort.openPortView(name);
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    public String toString() {
        if (m_underlyingPort == null) {
            return "<<not connected>>";
        }
        return m_underlyingPort.toString();
    }
    


}

/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 1, 2008 (mb/bw): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author B. Wiswedel, M. Berthold, University of Konstanz
 */
public class NodeOutPortWrapper extends NodePortAdaptor implements NodeOutPort {
    
    /** if connected, reference the outport this node is connected to.
     */
    private NodeOutPort m_underlyingPort;
    
    /**
     * Creates a new output port with a fixed type and index (should unique 
     * to all other output ports of this node) for the given node.
     * 
     * @param pType The port's type
     * @param portIndex This port index
     */
    NodeOutPortWrapper(final int portIndex, final PortType pType) {
        super(portIndex, pType);
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

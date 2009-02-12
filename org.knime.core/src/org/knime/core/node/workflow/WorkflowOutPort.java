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
 *   18.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author M. Berthold, University of Konstanz
 */
public class WorkflowOutPort extends NodeOutPortWrapper  {
    
    private final NodeInPort m_simulatedInPort;
    
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getPortObject() {
        // don't test for execution in the WFM, this will be done by
        // the individual ports
        return super.getPortObject();
    }
    
}

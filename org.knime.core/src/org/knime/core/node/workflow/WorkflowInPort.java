/*
 * ------------------------------------------------------------------ *
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
 *   26.09.2007 (mb/bw): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.PortType;

/**
 * 
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public final class WorkflowInPort extends NodeInPort {

    /** wrap the underlying port in yet another wrapper to enable
     * us to return this one as a wrapper.
     * (Needed for connection going directly from a workflow inport
     * to the same workflow's outport - ConnectionType.WFM_THROUGH)
     */
    private final NodeOutPortWrapper m_underlyingPortWrapper;

    /**
     * 
     */
    WorkflowInPort(final int index, final PortType pType) {
        super(index, pType);
        m_underlyingPortWrapper = new NodeOutPortWrapper(index, pType);
    }

    void setUnderlyingPort(final NodeOutPort port) {
        m_underlyingPortWrapper.setUnderlyingPort(port);
    }
    
    /**
     * @return the underlyingOutPort
     */
    public NodeOutPort getUnderlyingPort() {
        return m_underlyingPortWrapper;
    }

}

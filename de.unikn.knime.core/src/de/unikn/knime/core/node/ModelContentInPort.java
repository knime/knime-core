/* 
 * -------------------------------------------------------------------
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
 */
package de.unikn.knime.core.node;

/**
 * Input port used to transfere <code>ModelContent</code> objects between
 * nodes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ModelContentInPort extends NodeInPort implements
        NodePort.PredictorParamsPort {

    /**
     * Creates a new ModelContent port.
     * 
     * @param portId Unique ID for this port.
     * @param node This ModelContent port's underlying node.
     */
    ModelContentInPort(final int portId, final Node node) {
        super(portId, node);
    }

    /**
     * Returns the <code>ModelContent</code> for this port by retrieving it
     * from the connected ModelContent output port or returns null if this
     * port is not connected.
     * 
     * @return ModelContent from the connected output port.
     * 
     * @see #isConnected()
     */
    public ModelContent getPredictorParams() {
        if (!isConnected()) {
            return null;
        }
        return ((ModelContentOutPort)super.getConnectedPort())
                .getPredictorParams();
    }

    /**
     * Called by the connected output port to notify its counterparts of a new
     * ModelContent available.
     */
    void newPredictorParamsAvailable() {
        getNode().inportHasNewPredictorParams(super.getPortID());
    }

}

/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * Output port used to transfere PredictorParams objects between nodes.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
final class PredictorOutPort extends NodeOutPort implements
        NodePort.PredictorParamsPort {

    /**
     * The PredictorParams of this port or null if not set..
     */
    private PredictorParams m_predParams;

    /**
     * Creates a new output port for PredictorParams objects. The predictive
     * params object is set to null.
     * 
     * @param portId An unique port ID.
     */
    PredictorOutPort(final int portId) {
        super(portId);
        m_predParams = null;
    }

    /**
     * Sets a <code>PredictorParams</code> object in this port. The port will
     * notify all connected input ports and its view.
     * 
     * @param predParams The new <code>PredictorParams</code> object or null.
     */
    void setPredictorParams(final PredictorParams predParams) {
        m_predParams = predParams;
        for (NodeInPort inPort : super.getConnectedInPorts()) {
            if (inPort instanceof PredictorInPort) {
                ((PredictorInPort)inPort).newPredictorParamsAvailable();
            }
        }
        if (getPortView() != null) {
            ((PredictorOutPortView)getPortView())
                    .updatePredictorParams(m_predParams);
        }
    }

    /**
     * Returns the <code>PredictorParams</code> object for this port, as set
     * by the node this port is output for.
     * 
     * @return PredictorParams of this port which can be null.
     */
    public PredictorParams getPredictorParams() {
        return m_predParams;
    }

    /**
     * Opens this port's view and updates the PredictorParams object
     * representation inside this view.
     * 
     * @param name The view's name.
     * @see NodeOutPort#openPortView(java.lang.String)
     */
    public void openPortView(final String name) {
        if (getPortView() == null) {
            super.setPortView(new PredictorOutPortView(name, getPortName()));
            ((PredictorOutPortView)getPortView())
                    .updatePredictorParams(m_predParams);
        }
        getPortView().openView();
    }

}

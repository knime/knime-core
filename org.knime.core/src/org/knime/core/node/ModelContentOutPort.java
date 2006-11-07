/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node;

/**
 * Output port used to transfere ModelContent objects between nodes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ModelContentOutPort extends NodeOutPort implements
        NodePort.ModelContentPort {

    /**
     * The ModelContent of this port or null if not set..
     */
    private ModelContentRO m_predParams;

    /**
     * Creates a new output port for ModelContent objects. The predictive
     * params object is set to null.
     * 
     * @param portId An unique port ID.
     * @param node the node this port belongs to
     */
    ModelContentOutPort(final int portId, final Node node) {
        super(portId, node);
        m_predParams = null;
    }

    /**
     * Sets a <code>ModelContent</code> object in this port. The port will
     * notify all connected input ports and its view.
     * 
     * @param predParams The new <code>ModelContent</code> object or null.
     */
    void setModelContent(final ModelContentRO predParams) {
        // The order has been changed here in rev 5787 because
        // Node.isExecutable checks if the ModelContentOutPorts of its
        // predecessor(s) have a model set. It may node happen that isExecutable
        // returns true even if the model here is still being loaded. Thus
        // the model is really set into this port after the new model has been
        // fully propagated.
        for (NodeInPort inPort : super.getConnectedInPorts()) {
            if (inPort instanceof ModelContentInPort) {
              ((ModelContentInPort)inPort).newModelContentAvailable(predParams);
            }
        }
        if (getPortView() != null) {
            ((ModelContentOutPortView)getPortView())
                    .updateModelContent(predParams);
        }
        m_predParams = predParams;
    }

    /**
     * Returns the <code>ModelContent</code> object for this port, as set
     * by the node this port is output for.
     * 
     * @return ModelContent of this port which can be null.
     */
    public ModelContentRO getModelContent() {
        return m_predParams;
    }

    /**
     * Opens this port's view and updates the ModelContent object
     * representation inside this view.
     * 
     * @param name The view's name.
     * @see NodeOutPort#openPortView(java.lang.String)
     */
    @Override
    public void openPortView(final String name) {
        if (getPortView() == null) {
            super.setPortView(new ModelContentOutPortView(name, getPortName()));
            ((ModelContentOutPortView)getPortView())
                    .updateModelContent(m_predParams);
        }
        getPortView().openView();
    }

}

/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.workflow;

import org.knime.core.node.port.PortType;

/**
 * Implements a node's input port. Internally it keeps a reference to its
 * connected <code>NodeOutPort</code> if available and to its node. The node
 * gets notified, whenever the connection changes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see NodeOutPort
 */
public class NodeInPort extends NodePortAdaptor implements NodePort {

    /**
     * Creates a new input port with index and type assigned from the node.
     * 
     * @param portIndex the index of this port.
     */
    public NodeInPort(final int portIndex, final PortType type) {
        super(portIndex, type);
    }

}

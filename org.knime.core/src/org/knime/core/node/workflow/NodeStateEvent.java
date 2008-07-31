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
 * -------------------------------------------------------------------
 *
 * History
 *   20.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.workflow;

import java.util.EventObject;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class NodeStateEvent extends EventObject {

    private final NodeContainer.State m_state;

    /**
     * @param src id of the node
     * @param state the new state of the node
     */
    public NodeStateEvent(final NodeID src, final NodeContainer.State state) {
        super(src);
        m_state = state;
    }


    /**
     *
     * @return the new state of the node
     */
    public NodeContainer.State getState() {
        return m_state;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public NodeID getSource() {
        return (NodeID)super.getSource();
    }
}

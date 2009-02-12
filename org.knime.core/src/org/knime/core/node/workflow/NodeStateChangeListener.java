/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.workflow;

/**
 * Interface for clients that are interested in notifications about state
 * changes of a node.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public interface NodeStateChangeListener {


    /**
     * Callback from node, indicating that the given node has changed its state.
     * Clients may observe the node in order to get the current state.
     *
     * @param state Indicates the change of this node.
     */
    public void stateChanged(final NodeStateEvent state);

}

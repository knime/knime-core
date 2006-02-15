/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

/**
 * Interface for clients that are interested in notifications about state
 * changes of a node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public interface NodeStateListener {
    
    /**
     * TODO (tg) is the id important here?
     * Callback from node, indicating that the given node has changed its state.
     * Clients may observe the node in order to get the current state.
     * 
     * @param state Indicates the change of this node.
     * @param id A unique identifier for this node or -1 if none existend.
     */
    public void stateChanged(NodeStatus state, int id);

}

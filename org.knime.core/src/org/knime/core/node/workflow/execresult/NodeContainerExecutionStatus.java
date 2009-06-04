/* ------------------------------------------------------------------
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
 *   Jun 3, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * Status of a finished execution of a node. This information consists currently
 * of a success flag and status information for all of the node's children
 * (if it is a meta node).
 * 
 * <p>For the standard execution (local thread execution), this is either 
 * {@link #FAILURE} or {@link #SUCCESS}. This interface becomes necessary 
 * (over a simple use of a success flag) to represent the execution status of 
 * a remotely executed meta-node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface NodeContainerExecutionStatus {
    
    /** Whether the execution was successful. If the node represents a 
     * {@link SingleNodeContainer}, a successful execution brings the node into
     * the {@link State#EXECUTED} state, otherwise into {@link State#IDLE}. If
     * this object represents an execution status of a meta node, this method
     * is typically ignored and the state is determined based on the internals
     * of the meta node. 
     * @return the success status.
     */
    public boolean isSuccess();
    
    /** Query the execution status for a child given its 
     * {@linkplain NodeID#getIndex() node id suffix}. If the child is unknown,
     * the implementation should return {@link #FAILURE}.
     * @param idSuffix The child id suffix
     * @return The child execution status.
     */
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix);

    /** Represents a failed execution. */
    public static final NodeContainerExecutionStatus FAILURE = 
        new NodeContainerExecutionStatus() {

            /** @param idSuffix ignored
             * @return {@link NodeContainerExecutionStatus#FAILURE} (this) */
            @Override
            public NodeContainerExecutionStatus getChildStatus(
                    final int idSuffix) {
                return FAILURE;
            }

            /** @return false */
            @Override
            public boolean isSuccess() {
                return false;
            }
    };

    /** Represents a successful execution. */
    public static final NodeContainerExecutionStatus SUCCESS = 
        new NodeContainerExecutionStatus() {
        
        /** This method should actually not be called on a success execution.
         * @param idSuffix ignored
         * @return {@link NodeContainerExecutionStatus#SUCCESS} (this) */
        @Override
        public NodeContainerExecutionStatus getChildStatus(
                final int idSuffix) {
            assert false : "No implied success status on children";
            return SUCCESS;
        }
        
        /** @return true */
        @Override
        public boolean isSuccess() {
            return true;
        }
    };
    
}

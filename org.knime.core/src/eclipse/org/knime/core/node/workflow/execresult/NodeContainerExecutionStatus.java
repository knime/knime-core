/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

        /**
         * @param idSuffix ignored
         * @return {@link NodeContainerExecutionStatus#FAILURE} (this)
         */
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

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Failure execution status";
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
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Success execution status";
        }
    };
    
}

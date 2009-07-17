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
 */
package org.knime.core.node.workflow.execresult;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Specialized execution result for {@link WorkflowManager}. Offers access
 * to all contained node's execution result. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class WorkflowExecutionResult extends NodeContainerExecutionResult {
    
    private Map<NodeID, NodeContainerExecutionResult> m_execResultMap =
        new LinkedHashMap<NodeID, NodeContainerExecutionResult>();
    
    private final NodeID m_baseID;
    
    /**
     * Creates new workflow execution result with no particular settings.
     * @param baseID The node id of the workflow (the loading procedure in
     * the target workflow will correct the prefix).
     * @throws NullPointerException If the argument is null.
     */
    public WorkflowExecutionResult(final NodeID baseID) {
        if (baseID == null) {
            throw new NullPointerException();
        }
        m_baseID = baseID;
    }
    
    /**@return The base id of the workflow. Used to amend the node ids in
     * {@link #getExecutionResultMap()}. */
    public NodeID getBaseID() {
        return m_baseID;
    }
    
    /** @return The map containing node id to their execution result, 
     * never null. */
    public Map<NodeID, NodeContainerExecutionResult> getExecutionResultMap() {
        return m_execResultMap;
    }

    /**
     * Adds the execution result for a child node.
     * @param id The node id of the child, it must have the "correct" prefix.
     * @param execResult The execution result for the child
     * @return <code>true</code> if this map did not contain an entry for
     *         this child before.
     * @throws IllegalArgumentException If the id prefix is invalid
     * @throws NullPointerException If either argument is null 
     */
    public boolean addNodeExecutionResult(final NodeID id, 
            final NodeContainerExecutionResult execResult) {
        if (execResult == null || id == null) {
            throw new NullPointerException();
        }
        if (!id.hasSamePrefix(m_baseID)) {
            throw new IllegalArgumentException("Invalid prefix: " + id
                    + ", expected " + m_baseID);
        }
        return m_execResultMap.put(id, execResult) == null;
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus getChildStatus(final int idSuffix) {
        NodeID id = new NodeID(m_baseID, idSuffix);
        NodeContainerExecutionStatus status = m_execResultMap.get(id);
        if (status == null) {
            getLogger().debug("No execution status for node with suffix " 
                    + idSuffix + "; return FAILURE");
            status = NodeContainerExecutionStatus.FAILURE;
        }
        return status;
    }
}

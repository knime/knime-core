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
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.node.workflow.SingleNodeContainer;

/**
 * Specialized execution result for {@link SingleNodeContainer}. Offers access
 * to the node's execution result (containing port objects and possibly 
 * internals).
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SingleNodeContainerExecutionResult 
    extends NodeContainerExecutionResult {
    
    private NodeExecutionResult m_nodeExecutionResult;

    /** @return The execution result for the node. */
    public NodeExecutionResult getNodeExecutionResult() {
        return m_nodeExecutionResult;
    }
    
    /**
     * @param nodeExecutionResult the result to set
     * @throws NullPointerException If argument is null.
     */
    public void setNodeExecutionResult(
            final NodeExecutionResult nodeExecutionResult) {
        if (nodeExecutionResult == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_nodeExecutionResult = nodeExecutionResult;
    }
    
}

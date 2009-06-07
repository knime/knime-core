/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.core.node.workflow.execresult;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeMessage;

/**
 * Remote execution result. Derived classes define specialized access 
 * methods for SingleNodeContainer and WorkflowManager.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class NodeContainerExecutionResult 
    implements NodeContainerExecutionStatus {
    
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    
    private NodeMessage m_message;
    private boolean m_needsResetAfterLoad;
    
    private boolean m_isSuccess;
    
    /** Get a node message that was set during execution. 
     * @return The node message. */
    public NodeMessage getNodeMessage() {
        return m_message;
    }
    
    /** Set a node message.
     * @param message the message to set
     */
    public void setMessage(final NodeMessage message) {
        m_message = message;
    }
    
    /** Request a reset of the node after loading the result. The node is 
     * allowed to trigger a reset if the loading process causes errors that 
     * invalidate the computed result. */
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }
    
    /** @return true when the node needs to be reset after loading the results.
     * @see #setNeedsResetAfterLoad()
     */
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /**
     * @param isSuccess the isSuccess to set
     */
    public void setSuccess(final boolean isSuccess) {
        m_isSuccess = isSuccess;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSuccess() {
        return m_isSuccess;
    }
    
    /** @return the logger (never null). */
    protected NodeLogger getLogger() {
        return m_logger;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": "
            + (isSuccess() ? "success" : "failure");
    }

}

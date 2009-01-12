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

import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * Remote execution result. Derived classes define specialized access 
 * methods for SingleNodeContainer and WorkflowManager.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class NodeContainerExecutionResult {
    
    private State m_state;
    private boolean m_stateShouldBeLoaded;
    private NodeMessage m_message;
    private boolean m_needsResetAfterLoad;
    
    /** Get State of this node after (remote) execution.
     * @return The state of this accompanying node
     */
    public State getState() {
        return m_state;
    }
    
    /** Set state of the accompanying node.
     * @param state the state to set
     */
    public void setState(final State state) {
        if (state == null) {
            throw new NullPointerException();
        }
        m_state = state;
    }
    
    /**
     * Set whether the state of the target node is to be loaded. This
     * typically is true if the target node is a child of a meta node that
     * is executed remotely and false if only a single node is executed (the
     * state change in it will happen automatically through the respective
     * post-execute method in the local workflow). 
     * @param stateShouldBeLoaded the stateShouldBeLoaded to set
     */
    public void setStateShouldBeLoaded(final boolean stateShouldBeLoaded) {
        m_stateShouldBeLoaded = stateShouldBeLoaded;
    }
    
    /**
     * See {@link #shouldStateBeLoaded()} for details.
     * @return the stateShouldBeLoaded
     */
    public boolean shouldStateBeLoaded() {
        return m_stateShouldBeLoaded;
    }
    
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
    
}

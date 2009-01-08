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
 * General interface for a remote execution result. Derived interfaces define
 * specialized access methods for SingleNodeContainer and WorkflowManager.
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface NodeContainerExecutionResult {
    
    /** Get State of this node after (remote) execution.
     * @return The state of this accompanying node
     */
    public State getState();
    
    /** Get a node message that was set during execution. 
     * @return The node message. */
    public NodeMessage getNodeMessage();
    
    /** Request a reset of the node after loading the result. The node is 
     * allowed to trigger a reset if the loading process causes errors that 
     * invalidate the computed result. */
    public void setNeedsResetAfterLoad();
    
    /** @return true when the node needs to be reset after loading the results.
     * @see #setNeedsResetAfterLoad()
     */
    public boolean needsResetAfterLoad();
    
}

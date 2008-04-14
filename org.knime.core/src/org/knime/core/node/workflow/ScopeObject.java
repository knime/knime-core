/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * 
 * History
 *   15.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

/** Object holding base information for a loop context object: the head
 * and tail IDs of the loop's "control" node.
 * 
 * @author M. Berthold, University of Konstanz
 */
abstract class ScopeObject {

    private NodeID m_headNode;
    private NodeID m_tailNode;
    
    void setHeadNode(final NodeID head) {
        m_headNode = head;
    }
    
    public NodeID getHeadNode() {
        return m_headNode;
    }
    
    void setTailNode(final NodeID tail) {
        m_tailNode = tail;
    }
    
    public NodeID getTailNode() {
        return m_tailNode;
    }
}

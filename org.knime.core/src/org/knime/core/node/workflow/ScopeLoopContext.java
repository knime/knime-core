/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;



/**
 * ScopeContext interface holding loop information.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class ScopeLoopContext extends ScopeObject {

    private NodeID m_tailNode;

    public NodeID getHeadNode() {
        return super.getOwner();
    }
    
    public void setTailNode(final NodeID tail) {
        m_tailNode = tail;
    }
    
    public NodeID getTailNode() {
        return m_tailNode;
    }
    
    /** {@inheritDoc} */
    @Override
    protected ScopeObject cloneAndUnsetOwner() {
        ScopeLoopContext clone = (ScopeLoopContext)super.cloneAndUnsetOwner();
        clone.setTailNode(null);
        return clone;
    }
    
    public static class RestoredScopeLoopContext extends ScopeLoopContext {
        
    }
}

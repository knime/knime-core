/*
 * ------------------------------------------------------------------ *
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
abstract class ScopeObject implements Cloneable {

    private NodeID m_owner;
    
    void setOwner(final NodeID owner) {
        m_owner = owner;
    }
    
    NodeID getOwner() {
        return m_owner;
    }
    
    /** {@inheritDoc} */
    @Override
    protected ScopeObject clone() {
        try {
            return (ScopeObject)super.clone();
        } catch (CloneNotSupportedException e) {
            InternalError error = new InternalError(
                    "Unexpected exception, object clone failed");
            error.initCause(e);
            throw error;
        }
    }
    
    protected ScopeObject cloneAndUnsetOwner() {
        ScopeObject clone = clone();
        clone.setOwner(null);
        return clone;
    }
}

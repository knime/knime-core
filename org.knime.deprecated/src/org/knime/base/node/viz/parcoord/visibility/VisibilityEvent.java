/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jun 27, 2005 (tg): created
 */
package org.knime.base.node.viz.parcoord.visibility;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.RowKey;



/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class VisibilityEvent extends EventObject {

    private final HashSet<RowKey> m_keys;
    
    /** 
     * Creates a new event with the underlying source and one data cell.
     * @param  src The object on which the event initially occurred.
     * @param  key A <code>DataCell</code> for which this event is created.
     * @throws NullPointerException If the key is <code>null</code>.
     * @throws IllegalArgumentException If the source is <code>null</code>.
     * 
     * @see java.util.EventObject#EventObject(Object)
     */
    public VisibilityEvent(final Object src, final RowKey key) {
        super(src);
        m_keys = new HashSet<RowKey>();
        m_keys.add(key);
    }
    
    /** 
     * Creates a new event with the underlying source and a set of row keys.
     * @param  src  The object on which the event initially occurred.
     * @param  keys A set of <code>DataCell</code> row keys for which the 
     *         event is created.
     * @throws NullPointerException If keys are <code>null</code>.
     * @throws IllegalArgumentException If the source is <code>null</code>.
     *
     * @see java.util.EventObject#EventObject(Object)
     */
    public VisibilityEvent(final Object src, final Set<RowKey> keys) {
        super(src);
        m_keys = new HashSet<RowKey>(keys);
    }

    /** 
     * Returns the set of <code>DataCell</code> row keys on which the event 
     * initially occurred.
     * @return A set of row keys.
     */
    public Set<RowKey> keys() { 
        return m_keys;
    }
}

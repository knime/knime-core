/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;

/**
 * Event object that is fired when registered listener need to update its
 * properties. An event keeps an unmodifiable set of row keys as 
 * <code>DataCell</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class KeyEvent extends EventObject {

    /** 
     * Internal unmodifiable set of row IDs.
     */
    private final Set<DataCell> m_keys;

    /** 
     * Creates a new event with the underlying source and one data cell.
     * @param  src The object on which the event initially occurred.
     * @param  id A <code>DataCell</code> for which this event is created.
     * @throws NullPointerException If the key is <code>null</code>.
     * @throws IllegalArgumentException If the source is <code>null</code>.
     * 
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final DataCell... id) {
        super(src);
        Set<DataCell> set = new LinkedHashSet<DataCell>(Arrays.asList(id));
        m_keys = Collections.unmodifiableSet(set);
    }
    
    /** 
     * Creates a new event with the underlying source and a set of row keys.
     * @param  src  The object on which the event initially occurred.
     * @param  ids A set of <code>DataCell</code> row IDs for which the 
     *         event is created.
     * @throws NullPointerException If ids are <code>null</code>.
     * @throws IllegalArgumentException If the source is <code>null</code>.
     *
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final Set<DataCell> ids) {
        super(src);
        m_keys = Collections.unmodifiableSet(ids);
    }

    /** 
     * Returns the set of <code>DataCell</code> row keys on which the event 
     * initially occured.
     * @return A set of row IDs.
     */
    public Set<DataCell> keys() { 
        return m_keys;
    }
        
}   // KeyEvent

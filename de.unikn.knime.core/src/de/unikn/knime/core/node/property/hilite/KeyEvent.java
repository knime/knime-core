/*
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
 * 
 * 2006-06-08 (tm): reviewed
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
 * {@link DataCell}.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class KeyEvent extends EventObject {
    private static final long serialVersionUID = -5555018973664128867L;
    
    /** Internal unmodifiable set of row IDs. */
    private final Set<DataCell> m_keys;

    /** 
     * Creates a new event with the underlying source and one data cell.
     * 
     * @param src the object on which the event initially occurred
     * @param ids an array of  <code>DataCell</code> elements for which this 
     *         event is created.
     * @throws IllegalArgumentException if the source is <code>null</code> or
     *         the ids empty
     * 
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final DataCell... ids) {
        super(src);
        if (ids.length == 0) {
            throw new IllegalArgumentException("KeyEvent can not be empty.");
        }
        Set<DataCell> set = new LinkedHashSet<DataCell>(Arrays.asList(ids));
        m_keys = Collections.unmodifiableSet(set);
    }
    
    /** 
     * Creates a new event with the underlying source and a set of row keys.
     * 
     * @param src the object on which the event initially occurred
     * @param ids a set of <code>DataCell</code> row IDs for which the 
     *         event is created.
     * @throws IllegalArgumentException if the source is <code>null</code>, or
     *         the ids are <code>null</code> or empty
     *
     * @see java.util.EventObject#EventObject(Object)
     */
    public KeyEvent(final Object src, final Set<DataCell> ids) {
        super(src);
        if (ids == null || ids.size() == 0) {
            throw new IllegalArgumentException("KeyEvent can not be empty.");
        }       
        m_keys = Collections.unmodifiableSet(new LinkedHashSet<DataCell>(ids));
    }

    /** 
     * Returns the set of <code>DataCell</code> row keys on which the event 
     * initially occured.
     * 
     * @return a set of row IDs
     */
    public Set<DataCell> keys() { 
        return m_keys;
    }
}   // KeyEvent

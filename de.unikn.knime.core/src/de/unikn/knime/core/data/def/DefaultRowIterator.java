/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
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
package de.unikn.knime.core.data.def;

import java.util.Arrays;
import java.util.Iterator;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowIterator;

/** 
 * Specific implementation for a <code>RowIterator</code> that iterates over a
 * generic <code>DataRow</code> container.
 * @author Bernd Wiswedel, University Konstanz
 */
public class DefaultRowIterator extends RowIterator {
    
    /** To get next rows from. */
    private final Iterator<DataRow> m_iterator;

    /** Constructs a new iterator based on a <code>Iterable</code>. 
     * @param iterable The underlying iterable container.
     * @throws NullPointerException If argument is null.
     */
    public DefaultRowIterator(final Iterable<DataRow> iterable) {
        m_iterator = iterable.iterator();
    }
    
    /** Constructs a new iterator that traverses an array of DataRow. 
     * @param rows The array to iterate over.
     * @throws NullPointerException If argument is null.
     */
    public DefaultRowIterator(final DataRow... rows) {
        // TODO: Shouldn't we rather make a private non-changeable copy? PO.
        this(Arrays.asList(rows));
    }
    
    /**
     * @see de.unikn.knime.core.data.RowIterator#hasNext()
     */
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#next()
     */
    public DataRow next() {
        return m_iterator.next();
    }

}

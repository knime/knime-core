/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;


/** 
 * Specific implementation for a <code>RowIterator</code> that iterates over a
 * generic <code>DataTable</code>. It delegates to a given 
 * <code>Iterator&lt;DataRow&gt</code>; but disallows the invocation of 
 * the <code>remove</code> method. 
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
        // prevent the caller from changing the array underneath.
        this(new ArrayList<DataRow>(Arrays.asList(rows)));
    }
    
    /**
     * @see org.knime.core.data.RowIterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * @see org.knime.core.data.RowIterator#next()
     */
    @Override
    public DataRow next() {
        return m_iterator.next();
    }

}

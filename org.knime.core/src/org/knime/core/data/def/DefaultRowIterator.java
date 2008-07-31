/*
 * --------------------------------------------------------------------- *
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
 * Specific implementation for a {@link RowIterator} that iterates over a
 * generic {@link org.knime.core.data.DataTable DataTable}. It delegates to a
 * given <code>Iterator&lt;DataRow&gt</code>; but disallows the invocation of
 * the <code>remove</code> method.
 * 
 * @author Bernd Wiswedel, University Konstanz
 */
public class DefaultRowIterator extends RowIterator {

    /** The wrapped iterator to get next rows from. */
    private final Iterator<DataRow> m_iterator;

    /**
     * Constructs a new iterator based on an {@link Iterable}.
     * 
     * @param iterable the underlying iterable row container.
     * @throws NullPointerException if the argument is null.
     */
    public DefaultRowIterator(final Iterable<DataRow> iterable) {
        m_iterator = iterable.iterator();
    }

    /**
     * Constructs a new iterator that traverses an array of {@link DataRow}.
     * 
     * @param rows the array to iterate over.
     * @throws NullPointerException if the argument is null.
     */
    public DefaultRowIterator(final DataRow... rows) {
        // prevents the caller from changing the array underneath.
        this(new ArrayList<DataRow>(Arrays.asList(rows)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        return m_iterator.next();
    }

}

/*
 * ------------------------------------------------------------------
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
 *   21.06.2006 (bw & po): reviewed
 *   25.10.2006 (tg): cleanup
 *   29.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Iterator;

/**
 * Classes extending this class iterate over the rows of a {@link DataTable}.
 * Each DataTable has its specific <code>RowIterator</code>, which returns
 * the rows one by one. A <code>RowIterator</code> must return the rows always
 * in the same order.
 * 
 * <p>
 * Use RowIterators as follows:
 * 
 * <pre>
 *     DataTable table = ...;
 *     for (RowIterator it = table.getRowIterator(); it.hasNext();) {
 *         DataRow row = it.next();
 *         ...
 *     }
 * </pre>
 * 
 * <p>
 * or, if you don't need access to the iterator:
 * 
 * <pre>
 *     DataTable table =...;
 *     for (DataRow row : table) {
 *       // access the row here
 *     }
 * </pre>
 * 
 * <p>
 * Note, the difference of this class to a generic Iterator&lt;DataRow&gt; is
 * that it does not allow to remove elements.
 * 
 * @see DataRow
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class RowIterator implements Iterator<DataRow> {

    /**
     * Returns <code>true</code> if there are more rows and <code>false</code>
     * otherwise.
     * 
     * @see RowIterator#next()
     * @return <code>true</code> if the iterator has more elements, otherwise
     *         <code>false</code>
     */
    public abstract boolean hasNext();

    /**
     * Returns the next <code>DataRow</code>.
     * 
     * @return the next row in the <code>DataTable</code>
     * @throws java.util.NoSuchElementException if there are no more rows
     */
    public abstract DataRow next();

    /**
     * NOT supported by the DataTable iterator! DataTables are immutable
     * read-only objects after their creation. Do not call this method, it will
     * throw an exception.
     * 
     * @throws UnsupportedOperationException if the <tt>remove</tt>
     *                operation is not supported by this Iterator.
     */
    public final void remove() {
        throw new UnsupportedOperationException("Can't remove row from table."
                + " Data tables are read-only.");
    }
}

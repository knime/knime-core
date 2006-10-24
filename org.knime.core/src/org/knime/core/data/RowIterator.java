/*
 * ------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

import java.util.Iterator;

/** 
 * Classes extending this class will iterate over the rows of a 
 * {@link DataTable}. Each DataTable generates and returns its specific
 * <code>RowIterator</code>. <code>RowIterator</code>s return rows of DataTables
 * one by one and always in the same order.
 *
 * <p>
 * Use RowIterators as follows: 
 * <pre>
 * DataTable table = ...;
 * for (RowIterator it = table.getRowIterator(); it.hasNext();) {
 *     DataRow row = it.next();
 *     ...
 * }
 * </pre>
 * 
 * <p>
 * or, if you don't need access to the iterator:
 * <pre>
 * DataTable table =...;
 * for (DataRow row : table) {
 *   // access the row here
 * }
 * </pre>
 * 
 * <p>
 * Note, the difference of this class to a generic Iterator&lt;DataRow&gt;
 * is that it does not allow to remove elements. 
 * 
 * @see DataRow
 * @see RowKey
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class RowIterator implements Iterator<DataRow> {

    /** 
     * Returns <code>true</code> if the iterator reached the end of the table
     * or <code>false</code> if <code>next()</code> will return at least one 
     * more row.
     * @see     RowIterator#next()
     * @return  <code>true</code> if the iterator has more elements, otherwise 
     *          <code>false</code>.
     */
     public abstract boolean hasNext();
    
    /** 
     * Iterates over a collection of <code>DataRow</code> elements and always 
     * returns the next element of the iteration.
     * @return the next row in the <code>DataTable</code>.
     * @throws java.util.NoSuchElementException if there are no more rows.
     */
    public abstract DataRow next();
    
    /**
     * Method of the Java Iterator. NOT supported by the DataTable iterator!
     * DataTables are read-only objects after their creation.
     * Do not call this method, it will throw an exception.
     * 
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *        operation is not supported by this Iterator.
     */
    public final void remove() {
        throw new UnsupportedOperationException("Can't remove row from table."
                + " Data tables are read-only.");
    }

}

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
 *   09.01.2006(all): reviewed
 */
package org.knime.core.data;


/**  
 * Container interface for a vector of <code>DataCell</code>s with a key
 * identifier.
 * 
 * <p>Each <code>DataRow</code> represents one row of a <code>DataTable</code> 
 * and contains a fixed number of <code>DataCell</code> elements which are 
 * directly accessible and read-only. In addition, each <code>DataRow</code> 
 * vector contains a unique identifier key (which is not part of the data 
 * vector).
 * <p>A <code>DataRow</code> must not contain a <code>null</code> element or
 * a <code>null</code> key.
 * 
 * <p>This <code>DataRow</code> interface extends the 
 * <code>java.util.Iterable</code> interface but does not allow the removal of 
 * <code>DataCell</code>. Implementors must therefore throw an 
 * <code>UnsupportedOperationException</code> in the Iterators remove method.
 *   
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataTable
 * @see DataCell
 * @see RowIterator
 * @see RowKey
 */
public interface DataRow extends Iterable<DataCell> {
    
    /** 
     * Returns the length of this row, i.e. the number of columns of the
     * DataTable (not including the row identifier).
     * @return Length of this row.
     */
    int getNumCells();
    
    /** 
     * Returns the identifier key of this row.
     * @return Identifier key.
     */
    RowKey getKey();

    /** 
     * Returns the <code>DataCell</code> at the provided index within this row.
     * @param   index In this row.
     * @return  DataCell at the provided index.
     * @throws  IndexOutOfBoundsException If index out of range.
     */
    DataCell getCell(final int index);
    
}

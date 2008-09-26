/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.core.data.collection;

import java.util.Iterator;

import org.knime.core.data.DataCell;

/**
 * Iterator on a collection of {@link DataCell} objects, permitting
 * the access on underlying 
 * {@link org.knime.core.data.container.BlobWrapperDataCell BlobWrapperDataCell}
 * if applicable.
 * 
 * <p>
 * Instances of this iterator are returned by the 
 * {@link CollectionDataValue#iterator()} method when the collection contains
 * wrapped blobs. The framework will handle such cases with care, that is, 
 * by accessing the wrapper cell, not the actual blob (which might be 
 * unnecessary and expensive).  
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface BlobSupportDataCellIterator extends Iterator<DataCell> {
    
    /** Get the next DataCell, not unwrapping the blob when it is a 
     * {@link org.knime.core.data.container.BlobWrapperDataCell 
     * BlobWrapperDataCell}.
     * @return The next cell.
     */
    public DataCell nextWithBlobSupport();

}

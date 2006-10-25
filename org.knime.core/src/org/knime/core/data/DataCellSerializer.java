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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface for proxies that can read/write specific <code>DataCell</code>
 * implementations. Using DataCellSerializer is considerably faster than using 
 * ordinary Java serialization. Objects of this class are returned in a static 
 * method in a <code>DataCell</code> implementation. For further details see the
 * {@link org.knime.core.data.DataCell DataCell description} and the 
 * <a href="doc-files/newtypes.html#newtypes">manual</a> on how to define 
 * new types in KNIME.  
 * 
 * @param <T> A DataCell implementation being read/written.
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellSerializer<T extends DataCell> {

    /** Saves <code>cell</code> to the stream.
     * @param cell The cell to save.
     * @param out The place to write to.
     * @throws IOException If writing fails for an unknown (or known) reason.
     */
    void serialize(final T cell, DataOutput out) throws IOException;

    /** Loads a new instance of a <code>DataCell</code> from a DataInput. 
     * @param input The source to load from, never <code>null</code>.
     * @return A new DataCell instance.
     * @throws IOException If loading fails.
     */
    T deserialize(final DataInput input) throws IOException;
    
    
}

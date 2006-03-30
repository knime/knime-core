/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface that most <code>DataType</code> implementations will implement.
 * It allows to save and load a <code>DataCell</code> to/from a 
 * DataOutput/DataInput. <code>DataCell</code> objects whose type implements
 * this interface are tremendously faster written and read from a stream than
 * using the slow java serialization technique. 
 * 
 * <p>Implementors can safely assume that <code>DataCell</code> objects
 * being passed to the serialize/deserialize method can be parsed as native
 * <code>DataValue</code>. These methods are never called with missing cells.
 *   
 * @author wiswedel, University of Konstanz
 */
public interface DataCellSerializer {

    /** Saves <code>cell</code> to the stream.
     * @param cell The cell to save.
     * @param out The place to write to.
     * @throws IOException If writing fails for an unknown reason.
     */
    void serialize(final DataCell cell, DataOutput out) throws IOException;

    /** Loads a new instance of a <code>DataCell</code> from a DataInput. 
     * The returned value must be a <code>DataCell</code> compatible to the 
     * DataType, which implements this interface.
     * @param input The source to load from, never <code>null</code>.
     * @return A new DataCell instance.
     * @throws IOException If loading fails.
     */
    DataCell deserialize(final DataInput input) throws IOException;
    
    
}

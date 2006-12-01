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
 * Interface for classes that can read or write specific 
 * {@link org.knime.core.data.DataCell}
 * implementations. Using <code>DataCellSerializer</code> implementations are
 * normally considerably faster than ordinary Java serialization. Objects of
 * this class are returned by a static method in a 
 * {@link org.knime.core.data.DataCell} implementation. For further details see 
 * the {@link org.knime.core.data.DataCell} description and the <a
 * href="doc-files/newtypes.html#newtypes">manual</a> on how to define new
 * types.
 * 
 * @param <T> a {@link org.knime.core.data.DataValue} implementation being read 
 * or written
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellSerializer<T extends DataCell> {

    /**
     * Saves <code>cell</code> to the output stream.
     * 
     * @param cell the cell to save
     * @param out the place to write to
     * @throws IOException if writing fails
     */
    void serialize(final T cell, DataOutput out) throws IOException;

    /**
     * Loads a new instance of {@link org.knime.core.data.DataCell} of type 
     * <code>T</code> from the input stream, which represents a former 
     * serialized content.
     * 
     * @param input the source to load from, never <code>null</code>
     * @return a new {@link org.knime.core.data.DataValue} instance of type 
     * <code>T</code> representing a former serialized cell
     * @throws IOException if loading fails
     */
    T deserialize(final DataInput input) throws IOException;
 
}

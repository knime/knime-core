/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Aug 6, 2008 (wiswedel): created
 */
package org.knime.core.data;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Extended {@link DataOutput}, which also allows the serialization of
 * {@link DataCell} objects. It also overcomes the 64k limitation when 
 * serializing Strings in UTF format.
 * @see DataCellDataInput
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellDataOutput extends DataOutput {
    
    /** Writes a UTF String as described in {@link DataOutput#writeUTF(String)}
     * except for the 64k limitation. 
     * {@inheritDoc} */
    @Override
    public void writeUTF(final String s) throws IOException;
    
    /** Writes a given {@link DataCell} to the output stream.
     * @param cell The cell to write.
     * @throws IOException If any IO problem occur
     * @throws NullPointerException If the argument is null. */
    public void writeDataCell(final DataCell cell) throws IOException;

}

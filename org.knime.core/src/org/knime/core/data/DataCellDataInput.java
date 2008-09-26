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
 *   Aug 6, 2008 (wiswedel): created
 */
package org.knime.core.data;

import java.io.DataInput;
import java.io.IOException;

/**
 * Extended {@link DataInput}, which enables de-serialization of 
 * {@link DataCell} objects.
 * @see DataCellDataOutput  
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellDataInput extends DataInput {

    /** Reads a {@link DataCell} as written by the accompanying  
     * {@link DataCellDataOutput#writeUTF(String) write method} in class 
     * {@link DataCellDataOutput}.
     * @return The cell being desiralized from the stream.
     * @throws IOException If IO problems occur or the cell instance can't be
     * instantiated (for instance because of NoClassDefFoundError) */
    public DataCell readDataCell() throws IOException;
}

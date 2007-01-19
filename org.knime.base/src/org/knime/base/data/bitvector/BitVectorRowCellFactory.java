/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.01.2007 (dill): created
 */
package org.knime.base.data.bitvector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.container.SingleCellFactory;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BitVectorRowCellFactory  extends SingleCellFactory {
    
    private int m_nrOfProcessedRows;
    
    /**
     * 
     * @param newColumnSpec column spec of the newly created column
     */
    public BitVectorRowCellFactory(final DataColumnSpec newColumnSpec) {
        super(newColumnSpec);
    }
    
    /**
     * Increments the number of processed rows.
     *
     */
    public void incrementNrOfRows() {
        m_nrOfProcessedRows++;
    }
    
    /**
     * Returns the number of processed rows.
     * 
     * @return the number of processed rows.
     */
    public int getNrOfProcessedRows() {
        return m_nrOfProcessedRows;
    }
    
    /**
     * 
     * @return the number of set bits.
     */
    public abstract int getNumberOfSetBits();
    
    /**
     * 
     * @return the number of not set bits.
     */
    public abstract int getNumberOfNotSetBits();
    
    
    /**
     * 
     * @return true if at least one conversion was successful, false otherwise.
     */
    public abstract boolean wasSuccessful();
}

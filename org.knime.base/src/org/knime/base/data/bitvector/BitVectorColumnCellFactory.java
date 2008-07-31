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
 * -------------------------------------------------------------------
 * 
 * History
 *   15.08.2006 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import org.knime.core.data.DataColumnSpec;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BitVectorColumnCellFactory 
    extends BitVectorCellFactory {
    
    private int m_columnIndex;
    
    /** 
     * Create new cell factory that provides one column given by newColSpec.
     *  
     * @param columnSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     */
    public BitVectorColumnCellFactory(final DataColumnSpec columnSpec,    
            final int columnIndex) {
        super(columnSpec);
        m_columnIndex = columnIndex;
    }
    
    
    /**
     * 
     * @return index of the column to replace. 
     */
    public int getColumnIndex() {
        return m_columnIndex;
    }
    


}

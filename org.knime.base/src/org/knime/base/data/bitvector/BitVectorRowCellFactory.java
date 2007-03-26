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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   18.01.2007 (dill): created
 */
package org.knime.base.data.bitvector;

import java.util.List;

import org.knime.core.data.DataColumnSpec;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BitVectorRowCellFactory  extends BitVectorCellFactory {
    
    private final List<String> m_nameMapping;
    
    /**
     * 
     * @param newColumnSpec column spec of the newly created column
     * @param nameMapping optional mapping of the bit positions to strings
     */
    public BitVectorRowCellFactory(final DataColumnSpec newColumnSpec, 
            final List<String> nameMapping) {
        super(newColumnSpec);
        m_nameMapping = nameMapping;
    }
    
    /**
     * 
     * @return the optional bitposition column name mapping
     */
    public List<String> getNameMapping() {
        return m_nameMapping;
    }

}

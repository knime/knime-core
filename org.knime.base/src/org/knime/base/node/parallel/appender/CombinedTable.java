/* Created on 23.01.2007 15:47:28 by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.parallel.appender;

import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * Simples helper table whose iterator just concatenates all the tables passed
 * in the constructor. <b>The implementation does not check if the row keys
 * in the passed tables are unique, so you have to assure that by yourself!</b>
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
class CombinedTable implements DataTable {
    private final DataTable[] m_tables;

    /**
     * Creates a new combined table.
     * 
     * @param tables the tables to combine
     */
    public CombinedTable(final DataTable... tables) {
        if ((tables == null) || (tables.length < 1)) {
            throw new IllegalArgumentException("At least one table must be "
                    + "given");
        }
        m_tables = tables;

        for (int i = 1; i < m_tables.length; i++) {
            if (!m_tables[0].getDataTableSpec().equalStructure(
                    m_tables[i].getDataTableSpec())) {
                throw new IllegalArgumentException("The table at index " + i
                        + " has an incompatible spec");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_tables[0].getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new RowIterator() {            
            private RowIterator m_it = m_tables[0].iterator();
            private int m_nextTableIndex = 1;
            
            @Override
            public boolean hasNext() {
                if (m_it.hasNext()) {
                    return true;
                }
                
                if (m_nextTableIndex >= m_tables.length) {
                    return false;
                }
                
                m_it = m_tables[m_nextTableIndex++].iterator();
                return m_it.hasNext();
            }

            @Override
            public DataRow next() {
                if (hasNext()) {
                    return m_it.next();
                } else {
                    throw new NoSuchElementException("No more rows");
                }
            }            
        };
    }
}

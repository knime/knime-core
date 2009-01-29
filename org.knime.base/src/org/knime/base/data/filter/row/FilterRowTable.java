/*
 * ---------------------------------------------------------------------
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
 */
package org.knime.base.data.filter.row;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * This class filters a given number of rows from a
 * {@link org.knime.core.data.DataTable} using the
 * {@link FilterRowGenerator} interface to
 * check the criteria.
 * 
 * @see FilterRowGenerator
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FilterRowTable implements DataTable {
    /*
     * Underlying table to filter.
     */
    private final DataTable m_data;

    /*
     * Used to check for included/excluded rows.
     */
    private final FilterRowGenerator m_gen;

    /**
     * Creates a new row filter table by wraping the given data table. The
     * filter row generator is used to check if each row from the iteration
     * belongs is included in this table.
     * 
     * @param data the underlying data table
     * @param gen the filter row generator
     * @throws NullPointerException if one of the arguments is <code>null</code>
     */
    public FilterRowTable(final DataTable data, final FilterRowGenerator gen) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (gen == null) {
            throw new NullPointerException();
        }
        m_data = data;
        m_gen = gen;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_data.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new FilterRowIterator(m_data.iterator(), m_gen);
    }
}

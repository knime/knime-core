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

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

/**
 * This class wraps the {@link org.knime.core.data.RowIterator} which
 * includes only {@link org.knime.core.data.DataRow}s which satify the
 * {@link FilterRowGenerator} criteria.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class FilterRowIterator extends RowIterator {
    /*
     * The underlying data's row iterator.
     */
    private final RowIterator m_it;

    /*
     * Row filter to check data rows with.
     */
    private final FilterRowGenerator m_gen;

    /*
     * The current data row to return or null if the iterator is at end.
     */
    private DataRow m_row;

    /**
     * Creates a new filter row iterator wrapping a row iterator and using the
     * filter row generator for checking each row.
     * 
     * @param it the row iterator to retrieve each row
     * @param gen the filter row generator
     */
    FilterRowIterator(final RowIterator it, final FilterRowGenerator gen) {
        m_it = it;
        m_gen = gen;
        m_row = null;
        // to retrieve the first row
        next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return (m_row != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        // keep current one to return it
        DataRow row = m_row;
        m_row = null;
        // try to find next
        while (m_it.hasNext()) {
            m_row = m_it.next();
            // for which the row filter returns true
            if (m_gen.isIn(m_row)) {
                break;
            }
            m_row = null;
        }
        // currently saved one
        return row;
    }
}

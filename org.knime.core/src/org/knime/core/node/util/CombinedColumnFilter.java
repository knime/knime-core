/*
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
 * -------------------------------------------------------------------
 * 
 * History
 *    15.03.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import org.knime.core.data.DataColumnSpec;


/**
 * This {@link ColumnFilter} implementation filters all columns which
 * are filter by at least one of the given filters. It returns only columns
 * which pass ALL of the given filter. The filters are applied in the
 * order they are added. Thus the most restrictive filter should be the first.
 * @author Tobias Koetter, University of Konstanz
 */
public class CombinedColumnFilter implements ColumnFilter {

    /**The filters to check.*/
    private final ColumnFilter[] m_filters;
    
    
    /**Constructor for class CombinedColumnFilter.
     * @param filters the filters to use
     */
    public CombinedColumnFilter(final ColumnFilter... filters) {
        if (filters == null || filters.length < 1) {
            throw new NullPointerException("Filters must not be null or empty");
        }
        m_filters = filters;
    }
    
    /**
     * {@inheritDoc}
     */
    public String allFilteredMsg() {
        final StringBuilder buf = new StringBuilder();
        for (ColumnFilter filter : m_filters) {
            buf.append(filter.allFilteredMsg());
        }
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean includeColumn(final DataColumnSpec colSpec) {
        for (ColumnFilter filter : m_filters) {
            if (!filter.includeColumn(colSpec)) {
                return false;
            }
        }
        return true;
    }
}

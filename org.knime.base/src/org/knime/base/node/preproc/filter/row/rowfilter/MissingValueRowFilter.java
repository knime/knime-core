/*
 * ------------------------------------------------------------------
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
 *   04.02.2008 (ohl): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Filters rows with a missing value in a certain column.<br>
 * NOTE: Before the filter instance is applied it must be configured to find the
 * column index to the specified column name.
 *
 * @author ohl, University of Konstanz
 */
public class MissingValueRowFilter extends AttrValueRowFilter {

    /**
     * Creates a row filter that includes or excludes (depending on the
     * corresponding argument) rows with a missing value in the specified
     * column.
     *
     *
     * @param colName the column name of the cell to match
     * @param include if true, matching rows are included, if false, they are
     *            excluded.
     *
     */
    public MissingValueRowFilter(final String colName, final boolean include) {
        super(colName, include);
    }

    /**
     * Don't use created filter without loading settings before.
     */
    MissingValueRowFilter() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        // if this goes off, configure was probably not called after
        // loading filter's settings
        assert getColIdx() >= 0;

        DataCell theCell = row.getCell(getColIdx());
        boolean match = theCell.isMissing();
        return ((getInclude() && match) || (!getInclude() && !match));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MissingValueFilter: ColName='" + getColName()
                + (getInclude() ? " includes" : "excludes") + " rows.";
    }

}

/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   25.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import java.util.Calendar;
import java.util.Locale;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.StringCell;

/**
 * Extracts the month name (in English) from the passed {@link DateAndTimeValue}
 * and returns it as a string.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class MonthStringExtractorCellFactory extends SingleCellFactory {

    private final int m_colIdx;
    
    /**
     * 
     * @param colName name of the column containing the names of the months
     * @param columnIndex index of the column containing the 
     *  {@link DateAndTimeValue}
     */
    public MonthStringExtractorCellFactory(final String colName, 
            final int columnIndex) {
        super(new DataColumnSpecCreator(colName, StringCell.TYPE).createSpec());
        m_colIdx = columnIndex;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        DataCell cell = row.getCell(m_colIdx);
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        DateAndTimeValue value = (DateAndTimeValue)cell;
        if (value.hasDate()) {
            return new StringCell(value.getUTCCalendarClone().getDisplayName(
                    Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        }
        // no date set
        return DataType.getMissingCell();
    }

}

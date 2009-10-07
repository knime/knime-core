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
 *   05.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.StringCell;

/**
 * Cell Factory to extract a string representation of a {@link DateAndTimeValue}
 * from, whereby it can be safely assumed that the values is checked for missing
 * values and that it contains a date. 
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractTimeExtractorStringCellFactory extends
        AbstractTimeExtractorCellFactory {

    /**
     * 
     * @param colName
     *            name of the column containing the names of the months
     * @param colIdx
     *            index of the column containing the {@link DateAndTimeValue}
     * @param checkTime
     *            <code>true</code> if the time fields should be checked,
     *            <code>false</code> if the date fields should be checked
     * 
     * @see DateAndTimeValue#hasTime()
     * @see DateAndTimeValue#hasDate()
     */
    public AbstractTimeExtractorStringCellFactory(final String colName, 
            final int colIdx, final boolean checkTime) {
        super(colName, colIdx, StringCell.TYPE, checkTime);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        DataCell cell = row.getCell(getColumnIndex());
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        DateAndTimeValue value = (DateAndTimeValue)cell;
        if (checkTime() && value.hasTime()) {
            producedValidValue();
            return new StringCell(extractTimeField(value));
        }
        if (checkDate() && value.hasDate()) {
            producedValidValue();
            return new StringCell(extractTimeField(value));
        }
        // no date set
        increaseMissingValueCount();
        return DataType.getMissingCell();
    }

    /**
     * 
     * @param value the date and time value to extract a string representation 
     *  from
     * @return the string representation of the referring date field
     */
    protected abstract String extractTimeField(DateAndTimeValue value);
    
}

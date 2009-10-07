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
 *   06.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.IntCell;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractTimeExtractorIntCellFactory extends
        AbstractTimeExtractorCellFactory {

    /**
     * 
     * @param colIdx index of the column to extract the value from
     * @param colName new column name
     * @param checkTime <code>true</code> if the time should be checked, 
     * <code>false</code> if the date should be checked 
     */
    public AbstractTimeExtractorIntCellFactory(
            final String colName, final int colIdx, final boolean checkTime) {
        super(colName, colIdx, IntCell.TYPE, checkTime);
    }
    
    /**
     * 
     * @param value the time value which has already been tested to be not 
     *  missing and to have time fields set
     * @return the value of the referring time field
     */
    protected abstract int extractTimeField(final DateAndTimeValue value);
    
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
            return new IntCell(extractTimeField(value));
        }
        if (checkDate() && value.hasDate()) {
            producedValidValue();
            return new IntCell(extractTimeField(value));
        }
        // no date set
        increaseMissingValueCount();
        return DataType.getMissingCell();
    }

}

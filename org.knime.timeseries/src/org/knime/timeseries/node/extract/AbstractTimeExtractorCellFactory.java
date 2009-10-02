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
 *   24.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.IntCell;

/**
 * Abstract implementation for the {@link TimeFieldExtractorNodeModel} which 
 * checks for missing values and if the date or the time fields are set before
 * passing them to the actual implementation.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
abstract class AbstractTimeExtractorCellFactory 
    extends SingleCellFactory {

    private final int m_colIdx;

    private final boolean m_checkTime;
    
    /**
     * 
     * @param colName name of the resulting column
     * @param colIdx index of the column containing the DateAndTime value to 
     *  extract values from
     * @param checkTime <code>true</code> if {@link DateAndTimeValue#hasTime()} 
     * should be checked before calling 
     * {@link #extractTimeField(DateAndTimeValue)}, <code>false</code> if 
     * {@link DateAndTimeValue#hasDate()} should be checked  
     */
    AbstractTimeExtractorCellFactory(final String colName, final int colIdx, 
            final boolean checkTime) {
        super(new DataColumnSpecCreator(colName, 
                IntCell.TYPE).createSpec());
        m_colIdx = colIdx;
        m_checkTime = checkTime;
    }
        
    /**
     * 
     * @param value the time value which has already been tested to be not 
     *  missing and to have time fields set
     * @return the value of the referring time field
     */
    abstract int extractTimeField(final DateAndTimeValue value);
    
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
        if (m_checkTime && value.hasTime()) {
            return new IntCell(extractTimeField(value));
        }
        if (!m_checkTime && value.hasDate()) {
            return new IntCell(extractTimeField(value));
        }
        // no date set
        return DataType.getMissingCell();
    }

}

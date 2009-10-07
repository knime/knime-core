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

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.IntCell;
import org.knime.timeseries.node.extract.date.DateFieldExtractorNodeModel;
import org.knime.timeseries.node.extract.time.TimeFieldExtractorNodeModel;

/**
 * Abstract implementation for the {@link TimeFieldExtractorNodeModel} and 
 * {@link DateFieldExtractorNodeModel} which checks for missing values and if 
 * the date or the time fields are set before passing them to the actual 
 * implementation.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractTimeExtractorCellFactory 
    extends SingleCellFactory {

    private final int m_colIdx;

    private final boolean m_checkTime;
    
    private boolean m_onlyMissingValues;
    
    private int m_nrMissingValues;
    
    /**
     * 
     * @param colName name of the new column
     * @param colIdx index of the column containing the 
     *  {@link DateAndTimeValue}s to extract the fields from  
     * @param type either int or String for time field names such as month or 
     * day of week
     * @param checkTime <code>true</code> if the {@link DateAndTimeValue} should
     * be checked for time ({@link DateAndTimeValue#hasTime()}), 
     * <code>false</code> if they should be checked for date fields 
     * ({@link DateAndTimeValue#hasDate()})
     */
    public AbstractTimeExtractorCellFactory(final String colName, 
            final int colIdx, final DataType type, final boolean checkTime) {
        super(new DataColumnSpecCreator(colName, type).createSpec());
        m_colIdx = colIdx;
        m_checkTime = checkTime;
    }
    
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
    public AbstractTimeExtractorCellFactory(final String colName, 
            final int colIdx, final boolean checkTime) {
        super(new DataColumnSpecCreator(colName, 
                IntCell.TYPE).createSpec());
        m_colIdx = colIdx;
        m_checkTime = checkTime;
        m_onlyMissingValues = true;
        m_nrMissingValues = 0;
    }
    
    /**
     * Increases the number of actually produced (not found in input table) 
     * missing values.
     */
    protected void increaseMissingValueCount() {
        m_nrMissingValues++;
    }
    
    /**
     * At least one valid values could be produced.
     */
    protected void producedValidValue() {
        m_onlyMissingValues = false;
    }
    
    /**
     * 
     * @return the number of actually produced missing values (due to missing 
     * time field information in {@link DateAndTimeValue}
     */
    public int getNumberMissingValues() {
        return m_nrMissingValues;
    }
    
    /**
     * 
     * @return <code>false</code> if at least one valid value could be produced,
     * false otherwise
     */
    public boolean producedOnlyMissingValues() {
        return m_onlyMissingValues;
    }
    
    /**
     * 
     * @return <code>true</code> if the date should be checked with 
     *  {@link DateAndTimeValue#hasDate()}
     */
    protected boolean checkDate() {
        return !m_checkTime;
    }

    /**
     * 
     * @return <code>true</code> if the time should be checked with
     * {@link DateAndTimeValue#hasTime()}
     */
    protected boolean checkTime() {
        return m_checkTime;
    }
    
    /**
     * 
     * @return the index of the {@link DateAndTimeValue} column to extract the 
     * fields from
     */
    protected int getColumnIndex() {
        return m_colIdx;
    }

}

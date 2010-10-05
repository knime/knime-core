/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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

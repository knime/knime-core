/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

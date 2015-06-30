/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.timeseries.node.timemissvaluehandler.tshandler;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * This class is used to handle time series missing values by using the a linear interpolation of the value.
 *
 * @author Iris Adae, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 *
 */
@Deprecated
public class TSLinearHandler extends TSMissVHandler {

    private DataCell m_previous;

    private DataCell m_next;

    private int m_numberofsteps;

    /**
     * Constructor.
     */
    public TSLinearHandler() {
        super();
        m_previous = DataType.getMissingCell();
        m_next = DataType.getMissingCell();
        m_numberofsteps = 0;
    }

    @Override
    public void incomingValue(final RowKey key, final DataCell newCell) {
        if (newCell.isMissing()) {
            // set it on the waiting list
            addToWaiting(key, newCell);
            m_numberofsteps++;
        } else if (!getWaitingList().isEmpty()) {
            // we do have new values waiting and found a next
            if (m_previous.isMissing()) {
                // we don't have a previous, so we use the next only
                m_previous = newCell;
            }
            m_next = newCell;

            // there are values which wait for this entry.
            int i = 1;

            for (RowKey k : getWaitingList()) {
                addToDetected(k, getLinearInterpolation(i, m_numberofsteps + 1));
                i++;
            }
            clearWaiting();

            m_previous = newCell;
            m_numberofsteps = 0;
        } else {
            // final case
            m_previous = newCell;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // set the remaining waiting ones to the last seen value
        for (RowKey k : getWaitingList()) {
            addToDetected(k, m_previous);
        }
    }

    private DataCell getLinearInterpolation(final double stepnumber, final double stepcount) {
        if (m_previous instanceof DoubleValue
                || m_previous instanceof IntValue
                || m_previous instanceof LongValue)  {
            double prev = ((DoubleValue)m_previous).getDoubleValue();
            double next = ((DoubleValue)m_next).getDoubleValue();
            double lin = prev + 1.0 * stepnumber / (1.0 * stepcount) * (next - prev);
            if (m_previous instanceof IntValue) {
                // get an int, create an int
                return new IntCell((int)Math.round(lin));
            }
            if (m_previous instanceof LongValue) {
                // get an long, create an long
                return new LongCell(Math.round(lin));
            }
            return new DoubleCell(lin);
        }
        if (m_previous instanceof DateAndTimeValue) {
            // get an int, create an int
            DateAndTimeValue dataCell1 = (DateAndTimeValue)m_previous;
            DateAndTimeValue dataCell2 = ((DateAndTimeValue)m_next);
            boolean hasDate = dataCell1.hasDate() | dataCell2.hasDate();
            boolean hasTime = dataCell1.hasTime() | dataCell2.hasTime();
            boolean hasMilis = dataCell1.hasMillis() | dataCell2.hasMillis();
            double prev = dataCell1.getUTCTimeInMillis();
            double next = dataCell2.getUTCTimeInMillis();
            double d = prev + stepnumber / stepcount * (next - prev);
            return new DateAndTimeCell((long)d, hasDate, hasTime, hasMilis);
        }
        return DataType.getMissingCell();
    }
}

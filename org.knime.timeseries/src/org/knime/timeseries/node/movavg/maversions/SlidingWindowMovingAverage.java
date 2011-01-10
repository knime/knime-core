/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.timeseries.node.movavg.maversions;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

/**
 * This is the base class for sliding window moving averages.
 *
 * @author Adae, University of Konstanz
 */
public abstract class SlidingWindowMovingAverage extends MovingAverage {

    /** the length of the window.*/
    private int m_winLength;

    /** the values taken into account in the current window. */
    private final double[] m_values;

    private int m_indexOldestValue = 0;
    private int m_indexNewestValue = 0;

    private double m_avg = 0.0;
    /** the number of values in the window. can be less than the needed in
     * the beginning. */
    private int m_nrofValues = 0;
    /** if true, the window, if complete, and m_winlength == m_nrofvalue.*/
    private boolean m_enoughValues = false;

    /**
     * Constructor. Builds MA array with specified number of items.
     *
     * @param winLength
     *            MA window length
     */
    public SlidingWindowMovingAverage(final int winLength) {
        m_winLength = winLength;
        m_values = new double[getWinLength()];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getMeanandUpdate(final double newValue) {
        if (!m_enoughValues) {
            m_avg = updateMean(newValue, m_nrofValues);
            m_values[m_indexNewestValue] = newValue;
            m_indexNewestValue++;

            m_nrofValues++;
            m_enoughValues = (m_nrofValues == getWinLength());

            if (!m_enoughValues) {
                return DataType.getMissingCell();
            }
            DoubleCell dc = new DoubleCell(m_avg);
            return dc;
        }
        m_avg = updateMean(newValue);

        m_indexNewestValue = m_indexOldestValue;
        m_values[m_indexNewestValue] = newValue;
        m_indexOldestValue++;
        if (m_indexOldestValue >= getWinLength()) {
            m_indexOldestValue = 0;
        }

        DoubleCell dc = new DoubleCell(m_avg);
        return dc;
    }

    /**
     * @return the currently saved mean value
     */
    protected double getMean() {
        return m_avg;
    }

    /**
     * @return the value currently the first in the list (so its the
     * next to be deleted).
     */
    protected double getFirst() {
        return m_values[m_indexOldestValue];
    }

    /**
     * @return the value currently the last in the list (so its the last
     * which got in).
     */
    protected double getLast() {
        return m_values[m_indexNewestValue];
    }

    /**This method is updating the currently saved mean, based on
     * the new value coming in.
     * @param value the new incoming value.
     * @return the updated average.
     */
    protected abstract double updateMean(final double value);

    /** This method is updating the currently saved mean, based on
     * the new value coming in, if the window is not already full.
     * to handle, the underfull window, the windowsize is given.
     *
     * @param value the new value in the window.
     * @param curWinSize the current number of values in the window
     *(before this new one was inserted)
     * @return the average of the columns so far.
     */
    protected abstract double updateMean(final double value,
            final int curWinSize);


    /**
     * @return the winLength
     */
    public int getWinLength() {
        return m_winLength;
    }

}

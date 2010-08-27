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
 *   Jan 17, 2007 (rs): created
 */
package org.knime.timeseries.node.movavg;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

/**
 * Implements Moving Average on a time series.
 * 
 * @author Rosaria Silipo
 * 
 */
public class MovingAverage {

    /** Default length of MA window. */
    public static final int DEFAULT_WINLENGTH = 21;

    /** enum constants for weight functions. */
    enum WEIGHT_FUNCTIONS {
        /** no weight function. */
        Simple,
        /** weight function for exponential moving average. */
        Exponential
    }

    private int m_winLength = -1;
    private final double[] m_originalValues;
    private final double[] m_weights;
    private double m_expWeight = 0.0;

    private String m_weightFunction = null;

    private int m_indexOldestValue = 0;
    private int m_indexNewestValue = 0;
    private double m_avg = 0.0;
    private int m_initialValues = 0;
    private boolean m_enoughValues = false;

    /**
     * Constructor. Builds MA array with specified number of items.
     * 
     * @param winLength
     *            MA window length
     * @param weights
     *            weight function to overlap to MA window
     */
    public MovingAverage(final int winLength, final String weights) {

        m_winLength = winLength;

        m_originalValues = new double[m_winLength];
        m_weights = new double[m_winLength];

        defineWeights(weights);
    }

    /**
     * Constructor. Builds MA array with specified number of items. Window
     * Length is 21 items (default)
     * 
     * @param weights
     *            weight function to overlap to MA window
     */
    public MovingAverage(final String weights) {

        m_originalValues = new double[m_winLength];
        m_weights = new double[m_winLength];

        defineWeights(weights);
    }

    /**
     * Constructor. Builds MA array with specified number of items.
     * 
     * @param winLength
     *            MA window length No weight function
     */
    public MovingAverage(final int winLength) {

        m_winLength = winLength;

        m_originalValues = new double[m_winLength];
        m_weights = new double[m_winLength];

        defineWeights("none");

    }

    /**
     * Constructor. Builds MA array with specified number of items. Window
     * Length is 21 items (default) No weight function
     */
    public MovingAverage() {

        m_originalValues = new double[m_winLength];
        m_weights = new double[m_winLength];

        defineWeights("none");
    }

    /**
     * Implements moving average algorithm using the weights defined by the
     * weight function.
     * 
     * @param newValue
     *            new value from time series
     * @return m_avg current moving average value
     */

    public DataCell maValue(final double newValue) {

        double previousAvg = m_avg;
        boolean previousEnoughValues = m_enoughValues;
        DataCell dc = simpleMA(newValue);
        WEIGHT_FUNCTIONS wf = WEIGHT_FUNCTIONS.valueOf(m_weightFunction);
        if (WEIGHT_FUNCTIONS.Exponential.equals(wf)) {
            if (previousEnoughValues) {
                dc = new DoubleCell(newValue * m_expWeight + previousAvg
                        * (1 - m_expWeight));
            } else {
                return DataType.getMissingCell();
            }
        }
        return dc;
    }

    private DataCell simpleMA(final double newValue) {
        if (!m_enoughValues) {
            m_avg += newValue * m_weights[m_indexNewestValue];
            m_originalValues[m_indexNewestValue] = newValue;
            m_indexNewestValue++;

            m_initialValues++;
            m_enoughValues = (m_initialValues == m_winLength);

            if (!m_enoughValues) {
                return DataType.getMissingCell();
            }
            DoubleCell dc = new DoubleCell(m_avg);
            return dc;
        } 
        m_avg = m_avg - (m_originalValues[m_indexOldestValue] 
                                          * m_weights[m_indexOldestValue])
                + (newValue * m_weights[m_indexOldestValue]);

        m_indexNewestValue = m_indexOldestValue;
        m_originalValues[m_indexNewestValue] = newValue;
        m_indexOldestValue++;
        if (m_indexOldestValue >= m_winLength) {
            m_indexOldestValue = 0;
        }

        DoubleCell dc = new DoubleCell(m_avg);
        return dc;
    }

    private void defineWeights(final String weights) {

        m_weightFunction = weights;

        for (int i = 0; i < m_winLength; i++) {
            m_weights[i] = 1.0 / m_winLength;
        }

        if (WEIGHT_FUNCTIONS.Exponential.equals(WEIGHT_FUNCTIONS
                .valueOf(m_weightFunction))) {
            m_expWeight = 2.0 / (m_winLength + 1);
        }

    }
}

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
 * This is the old behavior of the exponential smoothing.
 *
 * it calculates the following
 * k = windowsize, s_n the simple MA and exp_n the smoothing value of this
 * method, v_n the value of the timeseries
 *
 * alpha = 2(k+1)
 *
 * than s_n = 1/k * sum (t = n ... n-k) v_n
 * and exp_n = alpha * v_n + (1-alpha) * s_n-1
 *
 * @author Rosaria Silipo
 */
public class ExponentialOldMA extends MovingAverage {

        private int m_winLength = -1;
        private final double[] m_originalValues;
        private final double[] m_weights;
        private double m_expWeight = 0.0;

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
         */
        public ExponentialOldMA(final int winLength) {

            m_winLength = winLength;

            m_originalValues = new double[m_winLength];
            m_weights = new double[m_winLength];
            defineWeights();
        }


        /**
         * Implements moving average algorithm using the weights defined by the
         * weight function.
         *
         * @param newValue
         *            new value from time series
         * @return m_avg current moving average value
         */

        @Override
        public DataCell getMeanandUpdate(final double newValue) {

            double previousAvg = m_avg;
            boolean previousEnoughValues = m_enoughValues;
            DataCell dc = simpleMA(newValue);

               if (previousEnoughValues) {
                    dc = new DoubleCell(newValue * m_expWeight + previousAvg
                            * (1 - m_expWeight));
                } else {
                    return DataType.getMissingCell();
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

        private void defineWeights() {

            for (int i = 0; i < m_winLength; i++) {
                m_weights[i] = 1.0 / m_winLength;
            }
            m_expWeight = 2.0 / (m_winLength + 1);

        }


        /**
         * {@inheritDoc}
         */
        @Override
       public double getMean() {
            return m_avg;
        }
    }

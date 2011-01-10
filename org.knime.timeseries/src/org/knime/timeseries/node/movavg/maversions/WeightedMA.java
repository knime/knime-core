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
import org.knime.timeseries.util.SlidingWindow;

/**
 * calculates a freely weighted time series.
 * Every part of the sliding window is weighted individually.
 *
 * @author Adae, University of Konstanz
 */
public class WeightedMA extends MovingAverage {

    private SlidingWindow<Double> m_window;
    private double[] m_weights;

    /**
     * @param weights the weights of the sliding window.
     * they should sum up to 1 (if not... you get bad results, thats all)
     */
    public WeightedMA(final double[] weights) {
        m_window = new SlidingWindow<Double>(weights.length);
        m_weights = new double[weights.length];
        System.arraycopy(weights, 0, m_weights, 0, weights.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getMeanandUpdate(final double newValue) {
        Double oldVal = m_window.addandget(newValue);
        if (oldVal == null) {
            return DataType.getMissingCell();
        }
        double avg = 0;
        int counter = 0;
        for (Double d : m_window.getList()) {
            avg += d.doubleValue() * m_weights[counter];
            counter++;
        }
        return new DoubleCell(avg);
    }

    /**
     * @param windowsize the size of the sliding window
     * @param mean the mean of the distribution
     * @param variance the variance of the Gaussian distribution
     * @return a MovingAverage window containing the Gaussian weighting.
     */
    public static WeightedMA getGaussianWeightedInstance(
            final int windowsize,
            final double mean,
            final double variance) {
        double[] weights = new double[windowsize];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.exp((-0.5) * (i - mean) * (i - mean) / variance);
        }
        // sum over all values
        double totalsum = 0;
        for (double d : weights) {
            totalsum += d;
        }

        // and normalize Gaussian window
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= totalsum;
        }
        return new WeightedMA(weights);
    }

    /**
     * @param windowsize the size of the sliding window
     * @return a MovingAverage window containing the Gaussian weighting.
     */
    public static WeightedMA getGaussianWeightedInstance(
            final int windowsize) {
        double sigma =  (windowsize - 1) / 4.0;
        double mean = (windowsize - 1) * 0.5;
        return getGaussianWeightedInstance(windowsize, mean, sigma * sigma);
    }

    /**
     * A backward Gaussian weighted instance. e.g. the position 0
     * will be weighted the  less, and the end the most
     *
     * @param windowsize the size of the sliding window
     * @return a MovingAverage window containing the Gaussian weighting.
     */
    public static WeightedMA getBackwardGaussianWeightedInstance(
            final int windowsize) {
        double sigma =  (windowsize - 1) / 4.0;
        return getGaussianWeightedInstance(windowsize, windowsize - 1,
                sigma * sigma);
    }

    /**
     * A forward Gaussian weighted instance. e.g. the position 0
     * will be weighted the  most, and the end the less
     *
     * @param windowsize the size of the sliding window
     * @return a MovingAverage window containing the Gaussian weighting.
     */
    public static WeightedMA getForwardGaussianWeightedInstance(
            final int windowsize) {
        double sigma =  (windowsize - 1) / 4.0;
        return getGaussianWeightedInstance(windowsize, 0, sigma * sigma);
    }

}

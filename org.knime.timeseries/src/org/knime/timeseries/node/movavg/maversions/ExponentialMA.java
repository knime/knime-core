/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
import org.knime.core.data.def.DoubleCell;

/**
 *  calculates an exponential smoothing of the time series.
 *  the mean therefore is calculated using
 *
 *  s_n = alpha * x_n + (1-alpha) * s_n-1
 *
 *  we use the windowsize k as initialization and for the alpha value
 *
 * alpha = 2(k+1)
 *
 * @author Adae, University of Konstanz
 */
public class ExponentialMA extends MovingAverage {

    private double m_alpha;
    private double m_avg = 0;
    private int m_nrofValues = 0;
    private int m_winLength;

    /**
     * @param winLength the length of the window.
     */
    public ExponentialMA(final int winLength) {
        m_winLength = 1;
        m_nrofValues = 0;
        m_alpha = 2.0 / (winLength + 1);

        m_avg = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getMeanandUpdate(final double newValue) {

        // till the predefined window size is reached, we calculate the mean
        if (m_nrofValues < m_winLength) {
            m_avg = m_avg + ((newValue - m_avg) / (m_winLength + 1));
            m_nrofValues++;
        } else {
            m_avg = m_alpha * newValue + m_avg * (1 - m_alpha);
        }
        return new DoubleCell(m_avg);
    }

    /**
     * @return the current mean.
     */
    protected double getMean() {
        return m_avg;
    }

}

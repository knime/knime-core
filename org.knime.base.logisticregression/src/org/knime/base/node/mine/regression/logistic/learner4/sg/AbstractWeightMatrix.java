/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Arrays;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;

/**
 * Abstract implementation of a WeightVector that implements the updating logic for the
 * weights.
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractWeightMatrix <T extends TrainingRow> implements WeightMatrix<T> {

    protected final double[][] m_data;
    private final boolean m_fitIntercept;

    public AbstractWeightMatrix(final int nFets, final int nCats, final boolean fitIntercept) {
        m_data = new double[nCats][nFets];
        m_fitIntercept = fitIntercept;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final WeightVectorConsumer2 func,
        final boolean includeIntercept, final TrainingRow row/*final IndexCache indexCache*/) {
        boolean updateIntercept = m_fitIntercept && includeIntercept;
            for (FeatureIterator iter = row.getFeatureIterator(); iter.next();) {
                int i = iter.getFeatureIndex();
                double featureValue = iter.getFeatureValue();
                if (!updateIntercept && i == 0) {
                    continue;
                }
                for (int c = 0; c < m_data.length; c++) {
                    applyFunc(c, i, featureValue, func);
            }
        }
    }

    private void applyFunc(final int c, final int i, final WeightVectorConsumer1 func) {
        double val = func.calculate(m_data[c][i], c, i);
        assert Double.isFinite(val);
        m_data[c][i] = val;
    }

    private void applyFunc(final int c, final int i, final double featureValue, final WeightVectorConsumer2 func) {
        double val = func.calculate(m_data[c][i], c, i, featureValue);
        assert Double.isFinite(val);
        m_data[c][i] = val;
    }


    @Override
    public void update(final WeightVectorConsumer1 func, final boolean includeIntercept) {
        // if we decided to not fit the intercept at all, we never touch the intercept weight
        int startIdx = m_fitIntercept && includeIntercept ? 0 : 1;
        for (int c = 0; c < m_data.length; c++) {
            for (int i = startIdx; i < m_data[c].length; i++) {
                applyFunc(c, i, func);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] getWeightVector() {
        return m_data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNVariables() {
        return m_data[0].length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNVectors() {
        return m_data.length;
    }


    @Override
    public String toString() {
        return "beta: " + Arrays.deepToString(m_data);
    }


}

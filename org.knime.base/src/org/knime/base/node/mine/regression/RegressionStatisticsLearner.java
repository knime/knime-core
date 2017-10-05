/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * Created on 2014.01.28. by gabor
 */
package org.knime.base.node.mine.regression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.UpdatingMultipleLinearRegression;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;


/**
 * Base class for the linear and polynomial regression statistics learner methods (@link
 * org.knime.base.node.mine.regression.linear2.learner.Learner} and
 * {@link org.knime.base.node.mine.regression.polynomial.learner2.Learner}). (It was based on these classes previous
 * versions.)
 *
 * @author Gabor Bakos
 * @author Adrian Nembach, KNIME.com
 * @since 2.10
 */
public abstract class RegressionStatisticsLearner {

    /** The output {@link PMMLPortObjectSpec}. */
    protected final PMMLPortObjectSpec m_outSpec;

    /** If true an exception is thrown when a missing cell is observed. */
    protected final boolean m_failOnMissing;

    /** whether to include the constant term during the estimation process. */
    protected final boolean m_includeConstant;

    /** offset value (a user defined intercept). */
    protected double m_offsetValue;

    /**
     * Constructor for the learners.
     *
     * @param spec The output spec.
     * @param failOnMissing Should fail on missing values?
     * @param includeConstant include a constant automatically
     */
    public RegressionStatisticsLearner(final PMMLPortObjectSpec spec, final boolean failOnMissing,
        final boolean includeConstant) {
        super();
        this.m_outSpec = spec;
        this.m_failOnMissing = failOnMissing;
        this.m_includeConstant = includeConstant;
    }

    /**
     * @param data The data table.
     * @param exec The execution context used for reporting progress.
     * @return An object which holds the results.
     * @throws CanceledExecutionException When method is cancelled
     * @throws InvalidSettingsException When settings are inconsistent with the data
     */
    public abstract RegressionContent perform(final BufferedDataTable data, final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException;

    /**
     * Initialize/create the objects to compute the statistics.
     *
     * @param regressorCount The size of the statistics. ({@code numOfColumns * maxExponent})
     * @param stats The statistics array (with size {@code regressorCount}).
     * @return The new {@link UpdatingMultipleLinearRegression} object.
     */
    protected UpdatingMultipleLinearRegression
        initStatistics(final int regressorCount, final SummaryStatistics[] stats) {
        UpdatingMultipleLinearRegression regr = new MillerUpdatingRegression(regressorCount, m_includeConstant);
        for (int i = 0; i < regressorCount; i++) {
            stats[i] = new SummaryStatistics();
        }
        return regr;
    }

    /**
     * Reads the table and performs the necessary updates on the statistics ({@code stats} and {@code regr}).
     * @param exec An {@link ExecutionMonitor}.
     * @param trainingData The training data.
     * @param stats Array of statistics for each (column, exponent) pair.
     * @param regr An {@link UpdatingMultipleLinearRegression} object.
     *
     * @throws CanceledExecutionException Processing was cancelled.
     */
    protected abstract void processTable(final ExecutionMonitor exec, RegressionTrainingData trainingData,
        SummaryStatistics[] stats, UpdatingMultipleLinearRegression regr)
        throws CanceledExecutionException;

    /**
     * Creates the covariance matrix from the {@link RegressionResults}.
     *
     * @param result A {@link RegressionResults} object.
     * @return The covariance {@link RealMatrix}.
     * @since 3.4
     */
    protected RealMatrix createCovarianceMatrix(final RegressionResults result) {
        // The covariance matrix
        int dim = result.getNumberOfParameters();
        RealMatrix covMat = MatrixUtils.createRealMatrix(dim, dim);
        for (int i = 0; i < dim; i++) {
            for (int k = 0; k < dim; k++) {
                covMat.setEntry(i, k, result.getCovarianceOfParameters(i, k));
            }
        }
        return covMat;
    }

    /**
     * Collect the factors (nominal columns) and the covariate (numeric columns) column names.
     *
     * @param data Input data.
     * @param trainingData Same {@code data} wrapped as {@link RegressionTrainingData} to ease handling.
     * @param factorList The modifiable {@link List} of selected nominal column names (should be empty).
     * @return The {@link List} of selected numeric column names.
     */
    protected List<String> createCovariateListAndFillFactors(final DataTable data,
        final RegressionTrainingData trainingData, final List<String> factorList) {
        List<String> covariateList = new ArrayList<String>();
        //This seems to be unused.
        Map<String, List<DataCell>> factorDomainValues = new HashMap<String, List<DataCell>>();
        for (int i : trainingData.getActiveCols()) {
            if (trainingData.getIsNominal().get(i)) {
                String factor = data.getDataTableSpec().getColumnSpec(i).getName();
                factorList.add(factor);
                List<DataCell> values = trainingData.getDomainValues().get(i);
                factorDomainValues.put(factor, values);
            } else {
                covariateList.add(data.getDataTableSpec().getColumnSpec(i).getName());
            }
        }
        return covariateList;
    }
}

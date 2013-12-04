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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

import Jama.Matrix;

/**
 * A Logistic Regression Learner.
 *
 * @author Heiko Hofer
 */
final class Learner {
    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger
          .getLogger(Learner.class);

    private final PMMLPortObjectSpec m_outSpec;

    private final int m_maxIter;

    private final double m_eps;
    private Matrix A;
    private Matrix b;
    private double m_penaltyTerm;


    /** the target reference category, if not set it is the last category. */
    private DataCell m_targetReferenceCategory;
    /** true when target categories should be sorted. */
    private boolean m_sortTargetCategories;
    /** true when categories of nominal data in the include list should be sorted. */
    private boolean m_sortFactorsCategories;

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param targetReferenceCategory the target reference category, if not set it is the last category
     * @param sortTargetCategories true when target categories should be sorted
     * @param sortFactorsCategories true when categories of nominal data in the include list should be sorted
     */
    Learner(final PMMLPortObjectSpec spec,
        final DataCell targetReferenceCategory,
        final boolean sortTargetCategories,
        final boolean sortFactorsCategories) {
        this(spec, targetReferenceCategory, sortTargetCategories, sortFactorsCategories, 30, 1e-14);
    }

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param targetReferenceCategory the target reference category, if not set it is the last category
     * @param sortTargetCategories true when target categories should be sorted
     * @param sortFactorsCategories true when categories of nominal data in the include list should be sorted
     * @param maxIter the maximum number of iterations
     * @param eps threshold used to identify convergence
     */
    Learner(final PMMLPortObjectSpec spec,
            final DataCell targetReferenceCategory,
            final boolean sortTargetCategories,
            final boolean sortFactorsCategories,
            final int maxIter, final double eps) {
        m_outSpec = spec;
        m_targetReferenceCategory = targetReferenceCategory;
        m_sortTargetCategories = sortTargetCategories;
        m_sortFactorsCategories = sortFactorsCategories;
        m_maxIter = maxIter;
        m_eps = eps;
        m_penaltyTerm = 0.0;
    }

    /**
     * @param data The data table.
     * @param exec The execution context used for reporting progress.
     * @return An object which holds the results.
     * @throws CanceledExecutionException When method is cancelled
     * @throws InvalidSettingsException When settings are inconsistent with the data
     */
    public LogisticRegressionContent perform(final DataTable data,
            final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        exec.checkCanceled();
        int iter = 0;
        boolean converged = false;

        RegressionTrainingData trainingData = new RegressionTrainingData(data, m_outSpec, true,
            m_targetReferenceCategory, m_sortTargetCategories, m_sortFactorsCategories);
        int targetIndex = data.getDataTableSpec().findColumnIndex(m_outSpec.getTargetCols().get(0).getName());
        int tcC = trainingData.getDomainValues().get(targetIndex).size();
        int rC = trainingData.getRegressorCount();

        Matrix beta = new Matrix(1, (tcC - 1) * (rC + 1));

        double loglike = 0;
        double loglikeOld = 0;

        // main loop
        while (iter < m_maxIter && !converged) {
            Matrix betaOld = new Matrix(beta.getArrayCopy());
            loglikeOld = loglike;

            irlsRls(trainingData.iterator(), beta, rC, tcC);
            exec.checkCanceled();
            loglike = likelihood(trainingData.iterator(), beta, rC, tcC);
            if (Double.isInfinite(loglike) || Double.isNaN(loglike)) {
                throw new RuntimeException("Log-likelihood is not a number.");
            }
            exec.checkCanceled();
            // test for decreasing likelihood
            while ((Double.isInfinite(loglike) || Double.isNaN(loglike)
                    || loglike < loglikeOld) && iter > 0) {
                converged = true;
                for (int k = 0; k < beta.getRowDimension(); k++) {
                    if (abs(beta.get(k, 0) - betaOld.get(k, 0)) > m_eps
                            * abs(betaOld.get(k, 0))) {
                        converged = false;
                        break;
                    }
                }
                if (converged) {
                    break;
                }
                // half the step size of beta
                beta = (beta.plus(betaOld)).times(0.5);
                exec.checkCanceled();
                loglike = likelihood(trainingData.iterator(), beta, rC, tcC);
                exec.checkCanceled();
            }

            // test for convergence
            converged = true;
            for (int k = 0; k < beta.getRowDimension(); k++) {
                if (abs(beta.get(k, 0) - betaOld.get(k, 0)) > m_eps
                        * abs(betaOld.get(k, 0))) {
                    converged = false;
                    break;
                }
            }
            iter++;

            LOGGER.debug("#Iterations: " + iter);
            LOGGER.debug("Log Likelihood: " + loglike);
            StringBuilder betaBuilder = new StringBuilder();
            for (int i = 0; i < beta.getRowDimension() - 1; i++) {
                betaBuilder.append(Double.toString(beta.get(i, 0)));
                betaBuilder.append(", ");
            }
            if (beta.getRowDimension() > 0) {
              betaBuilder.append(Double.toString(beta.get(
                      beta.getRowDimension() - 1, 0)));
            }
            LOGGER.debug("beta: " + betaBuilder.toString());

            exec.checkCanceled();
            exec.setMessage("#Iterations: " + iter + " | Log-likelihood: "
                    + DoubleFormat.formatDouble(loglike));
        }
        // The covariance matrix
        Matrix covMat = A.inverse().times(-1);

        List<String> factorList = new ArrayList<String>();
        List<String> covariateList = new ArrayList<String>();
        Map<String, List<DataCell>> factorDomainValues =
            new HashMap<String, List<DataCell>>();
        for (int i : trainingData.getActiveCols()) {
            if (trainingData.getIsNominal().get(i)) {
                String factor =
                    data.getDataTableSpec().getColumnSpec(i).getName();
                factorList.add(factor);
                List<DataCell> values = trainingData.getDomainValues().get(i);
                factorDomainValues.put(factor, values);
            } else {
                covariateList.add(
                        data.getDataTableSpec().getColumnSpec(i).getName());
            }
        }

        // create content
        LogisticRegressionContent content =
            new LogisticRegressionContent(m_outSpec,
                    factorList, covariateList,
                    m_targetReferenceCategory, m_sortTargetCategories, m_sortFactorsCategories,
                    beta, loglike, covMat, iter);
        return content;
    }

    /**
     * Do a irls step. The result is stored in beta.
     *
     * @param iter iterator over trainings data.
     * @param beta parameter vector
     * @param rC regressors count
     * @param tcC target category count
     */
    private void irlsRls(final Iterator<RegressionTrainingRow> iter, final Matrix beta,
            final int rC, final int tcC) {
        int rowCount = 0;
        int dim = (rC + 1) * (tcC - 1);
        Matrix xTwx = new Matrix(dim, dim);
        Matrix xTyu = new Matrix(dim, 1);

        Matrix x = new Matrix(1, rC + 1);
        Matrix eBetaTx = new Matrix(1, tcC - 1);
        Matrix pi = new Matrix(1, tcC - 1);
        while (iter.hasNext()) {
            rowCount++;
            RegressionTrainingRow row = iter.next();
            x.set(0, 0, 1);
            x.setMatrix(0, 0, 1, rC, row.getParameter());

            for (int k = 0; k < tcC - 1; k++) {
                Matrix betaITx = x.times(
                    beta.getMatrix(0, 0,
                            k * (rC + 1), (k + 1) * (rC + 1) - 1).transpose());
                eBetaTx.set(0, k, Math.exp(betaITx.get(0, 0)));
            }

            double sumEBetaTx = 0;
            for (int k = 0; k < tcC - 1; k++) {
                sumEBetaTx += eBetaTx.get(0, k);
            }

            for (int k = 0; k < tcC - 1; k++) {
                double pik = eBetaTx.get(0, k) / (1 + sumEBetaTx);
                pi.set(0, k, pik);
            }

            // fill the diagonal blocks of matrix xTwx (k = k')
            for (int k = 0; k < tcC - 1; k++) {
                for (int i = 0; i < rC + 1; i++) {
                    for (int ii = i; ii < rC + 1; ii++) {
                        int o = k * (rC + 1);
                        double v = xTwx.get(o + i, o + ii);
                        double w = pi.get(0, k) * (1 - pi.get(0, k));
                        v += x.get(0, i) * w * x.get(0, ii);
                        xTwx.set(o + i, o + ii, v);
                        xTwx.set(o + ii, o + i, v);
                    }
                }
            }
            // fill the rest of xTwx (k != k')
            for (int k = 0; k < tcC - 1; k++) {
                for (int kk = k + 1; kk < tcC - 1; kk++) {
                    for (int i = 0; i < rC + 1; i++) {
                        for (int ii = i; ii < rC + 1; ii++) {
                            int o1 = k * (rC + 1);
                            int o2 = kk * (rC + 1);
                            double v = xTwx.get(o1 + i, o2 + ii);
                            double w = -pi.get(0, k) * pi.get(0, kk);
                            v += x.get(0, i) * w * x.get(0, ii);
                            xTwx.set(o1 + i, o2 + ii, v);
                            xTwx.set(o1 + ii, o2 + i, v);
                            xTwx.set(o2 + ii, o1 + i, v);
                            xTwx.set(o2 + i, o1 + ii, v);
                        }
                    }
                }
            }

            int g = (int)row.getTarget();
            // fill matrix xTyu
            for (int k = 0; k < tcC - 1; k++) {
                for (int i = 0; i < rC + 1; i++) {
                    int o = k * (rC + 1);
                    double v = xTyu.get(o + i, 0);
                    double y = k == g ? 1 : 0;
                    v += (y - pi.get(0, k)) * x.get(0, i);
                    xTyu.set(o + i, 0, v);
                }
            }


        }

        if (m_penaltyTerm > 0.0) {
            Matrix stdError = getStdErrorMatrix(xTwx);
            // do not penalize the constant terms
            for (int i = 0; i < tcC - 1; i++) {
                stdError.set(i * (rC + 1), i * (rC + 1), 0);
            }
            xTwx.minusEquals(stdError.times(0.00001));
        }
        b = xTwx.times(beta.transpose()).plus(xTyu);
        A = xTwx;
        if (rowCount < A.getColumnDimension()) {
            throw new IllegalStateException("The dataset must have at least "
                    + A.getColumnDimension() + " rows, but it has only "
                    + rowCount + " rows. It is recommended to use a "
                    + "larger dataset in order to increase accuracy.");
        }
        Matrix betaNew = A.solve(b);
        beta.setMatrix(0, 0, 0, (tcC - 1) * (rC + 1) - 1, betaNew.transpose());
    }

    private Matrix getStdErrorMatrix(final Matrix xTwx) {
        Matrix covMat = xTwx.inverse().times(-1);
        // the standard error estimate
        Matrix stdErr = new Matrix(covMat.getColumnDimension(),
                covMat.getRowDimension());
        for (int i = 0; i < covMat.getRowDimension(); i++) {
            stdErr.set(i, i, sqrt(abs(covMat.get(i, i))));
        }
        return stdErr;
    }

    /**
     * Compute the likelihood at given beta.
     *
     * @param iter iterator over trainings data.
     * @param beta parameter vector
     * @param rC regressors count
     * @param tcC target category count
     */
    private double likelihood(final Iterator<RegressionTrainingRow> iter,
            final Matrix beta,
            final int rC, final int tcC) {
        double loglike = 0;

        Matrix x = new Matrix(1, rC + 1);
        while (iter.hasNext()) {
            RegressionTrainingRow row = iter.next();

            x.set(0, 0, 1);
            x.setMatrix(0, 0, 1, rC, row.getParameter());

            double sumEBetaTx = 0;
            for (int i = 0; i < tcC - 1; i++) {
                Matrix betaITx = x.times(
                    beta.getMatrix(0, 0,
                            i * (rC + 1), (i + 1) * (rC + 1) - 1).transpose());
                sumEBetaTx += Math.exp(betaITx.get(0, 0));
            }

            int y = (int)row.getTarget();
            double yBetaTx = 0;
            if (y < tcC - 1) {
                yBetaTx = x.times(
                    beta.getMatrix(0, 0,
                            y * (rC + 1), (y + 1) * (rC + 1) - 1).transpose()
                            ).get(0, 0);
            }
            loglike += yBetaTx - Math.log(1 + sumEBetaTx);
        }

        return loglike;
    }
}

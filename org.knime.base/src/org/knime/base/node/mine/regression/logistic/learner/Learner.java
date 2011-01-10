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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
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

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     */
    Learner(final PMMLPortObjectSpec spec) {
        this(spec, 30, 1e-8);
    }

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param maxIter the maximum number of iterations
     * @param eps threshold used to identify convergence
     */
    Learner(final PMMLPortObjectSpec spec,
            final int maxIter, final double eps) {
        m_outSpec = spec;
        m_maxIter = maxIter;
        m_eps = eps;
        m_penaltyTerm = 0.0;
    }

    /**
     * @param data The data table.
     * @param exec The execution context used for reporting progress.
     * @return An object which holds the results.
     * @throws CanceledExecutionException When method is cancelled
     */
    public LogisticRegressionContent perform(final DataTable data,
            final ExecutionContext exec) throws CanceledExecutionException {
        exec.checkCanceled();
        int iter = 0;
        boolean converged = false;

        TrainingData trainingData = new TrainingData(data, m_outSpec);
        int tcC = trainingData.getTargetCategoryCount();
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
        for (int i : trainingData.getLearningCols()) {
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
                    factorList,
                    covariateList, beta, loglike, covMat, iter);
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
    private void irlsRls(final Iterator<TrainingRow> iter, final Matrix beta,
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
            TrainingRow row = iter.next();
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

            int g = row.getTarget();
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
                    + rowCount + " rows. However it is recommended to use a "
                    + "larger dataset in order to increas accuracy.");
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
    private double likelihood(final Iterator<TrainingRow> iter,
            final Matrix beta,
            final int rC, final int tcC) {
        double loglike = 0;

        Matrix x = new Matrix(1, rC + 1);
        while (iter.hasNext()) {
            TrainingRow row = iter.next();

            x.set(0, 0, 1);
            x.setMatrix(0, 0, 1, rC, row.getParameter());

            double sumEBetaTx = 0;
            for (int i = 0; i < tcC - 1; i++) {
                Matrix betaITx = x.times(
                    beta.getMatrix(0, 0,
                            i * (rC + 1), (i + 1) * (rC + 1) - 1).transpose());
                sumEBetaTx += Math.exp(betaITx.get(0, 0));
            }

            int y = row.getTarget();
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

    /** This class is a decorator for a DataTable.*/
    private static class TrainingData implements Iterable<TrainingRow> {
        private DataTable m_data;
        private List<Integer> m_learningCols;
        private Map<Integer, Boolean> m_isNominal;
        private Map<Integer, List<DataCell>> m_domainValues;

        private int m_targetIndex;
        private List<DataCell> m_targetDomainValues;

        private int m_parameterCount;

        /**
         * @param data training data.
         * @param spec port object spec.
         */
        public TrainingData(final DataTable data,
                final PMMLPortObjectSpec spec) {
            m_data = data;
            m_learningCols = new ArrayList<Integer>();
            m_isNominal = new HashMap<Integer, Boolean>();
            m_domainValues = new HashMap<Integer, List<DataCell>>();

            DataTableSpec inSpec = data.getDataTableSpec();
            m_parameterCount = 0;
            for (DataColumnSpec colSpec : spec.getLearningCols()) {
                int i = inSpec.findColumnIndex(colSpec.getName());
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    // Create Design Variables
                    m_learningCols.add(i);
                    m_isNominal.put(i, true);
                    List<DataCell> valueList = new ArrayList<DataCell>();
                    valueList.addAll(colSpec.getDomain().getValues());
                    Collections.sort(valueList,
                            colSpec.getType().getComparator());
                    m_domainValues.put(i, valueList);
                    m_parameterCount += valueList.size() - 1;
                } else {
                    m_learningCols.add(i);
                    m_isNominal.put(i, false);
                    m_domainValues.put(i, null);
                    m_parameterCount++;
                }
            }

            String target = spec.getTargetFields().get(0);
            m_targetIndex = inSpec.findColumnIndex(target);
            m_targetDomainValues = new ArrayList<DataCell>();
            DataColumnSpec targetSpec = inSpec.getColumnSpec(target);
            m_targetDomainValues.addAll(targetSpec.getDomain().getValues());
            Collections.sort(m_targetDomainValues,
                    targetSpec.getType().getComparator());
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<TrainingRow> iterator() {
            return new TrainingDataIterator(m_data.iterator(),
                    m_parameterCount, m_learningCols,
                    m_isNominal, m_domainValues,
                    m_targetIndex, m_targetDomainValues);
        }

        /**
         * @return the regressorCount
         */
        public int getRegressorCount() {
            return m_parameterCount;
        }

        /**
         * @return the targetCategoryCount
         */
        public int getTargetCategoryCount() {
            return m_targetDomainValues.size();
        }

        /**
         * @return the indices
         */
        public List<Integer> getLearningCols() {
            return m_learningCols;
        }

        /**
         * @return the isDesignVariable
         */
        public Map<Integer, Boolean> getIsNominal() {
            return m_isNominal;
        }

        /**
         * @return the values
         */
        public Map<Integer, List<DataCell>> getDomainValues() {
            return m_domainValues;
        }

        /**
         * @return the targetIndex
         */
        public int getTargetIndex() {
            return m_targetIndex;
        }

        /**
         * @return the targetValues
         */
        public List<DataCell> getTargetValues() {
            return m_targetDomainValues;
        }

    }

    /** This is a decorator for a iterator over DataRows.*/
    private static class TrainingDataIterator implements Iterator<TrainingRow> {
        private Iterator<DataRow> m_iter;

        private int m_parameterCount;
        private List<Integer> m_learningCols;
        private Map<Integer, Boolean> m_isNominal;
        private Map<Integer, List<DataCell>> m_domainValues;

        private int m_target;
        private List<DataCell> m_targetDomainValues;

        /**
         * @param iter the underlying iterator
         * @param parameterCount number of parameters which will be generated
         * from the learning columns
         * @param learningCols indices of the learning columns
         * @param isNominal whether a learning column is nominal
         * @param domainValues the domain values of the nominal learning columns
         * @param target the index of the target value
         * @param targetDomainValues the domain values of the target
         */
        public TrainingDataIterator(final Iterator<DataRow> iter,
                final int parameterCount,
                final List<Integer> learningCols,
                final Map<Integer, Boolean> isNominal,
                final Map<Integer, List<DataCell>> domainValues,
                final int target,
                final List<DataCell> targetDomainValues) {
            m_iter = iter;
            m_parameterCount = parameterCount;
            m_learningCols = learningCols;
            m_isNominal = isNominal;
            m_domainValues = domainValues;
            m_target = target;
            m_targetDomainValues = targetDomainValues;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return m_iter.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public TrainingRow next() {
            return new TrainingRow(m_iter.next(), m_parameterCount,
                    m_learningCols,
                    m_isNominal, m_domainValues,
                    m_target, m_targetDomainValues);
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** A decorator for a data row. */
    private static class TrainingRow {
        private int m_target;
        private Matrix m_parameter;

        /**
         * @param row The underlying row
         * @param parameterCount number of parameters which will be generated
         * from the learning columns
         * @param learningCols indices of the learning columns
         * @param isNominal whether a learning column is nominal
         * @param domainValues the domain values of the nominal learning columns
         * @param target the index of the target value
         * @param targetDomainValues the domain values of the target
         */
        public TrainingRow(final DataRow row,
                final int parameterCount,
                final List<Integer> learningCols,
                final Map<Integer, Boolean> isNominal,
                final Map<Integer, List<DataCell>> domainValues,
                final int target,
                final List<DataCell> targetDomainValues) {
            m_parameter = new Matrix(1, parameterCount);
            int c = 0;
            for (int i : learningCols) {
                if (isNominal.get(i)) {
                    DataCell cell = row.getCell(i);
                    checkMissing(cell);
                    int index = domainValues.get(i).indexOf(cell);
                    if (index < 0) {
                        throw new IllegalStateException("DataCell \""
                        + cell.toString()
                        + "\" is not in the DataColumnDomain. Please apply a "
                        + "Domain Calculator on the columns with nominal "
                        + "values.");
                    }
                    for (int k = 1; k < domainValues.get(i).size(); k++) {
                        if (k == index) {
                            m_parameter.set(0, c, 1.0);
                        } else {
                            m_parameter.set(0, c, 0.0);
                        }
                        c++;
                    }
                } else {
                    DataCell cell = row.getCell(i);
                    checkMissing(cell);
                    DoubleValue value = (DoubleValue)cell;
                    m_parameter.set(0, c, value.getDoubleValue());
                    c++;
                }
            }

            DataCell targetCell = row.getCell(target);
            checkMissing(targetCell);
            m_target = targetDomainValues.indexOf(targetCell);
            if (m_target < 0) {
                throw new IllegalStateException("DataCell \""
                + row.getCell(target).toString()
                + "\" is not in the DataColumnDomain of target column. "
                + "Please apply a "
                + "Domain Calculator on the target column.");
            }
        }


        private void checkMissing(final DataCell cell) {
            if (cell.isMissing()) {
                throw new IllegalStateException("Missing values are not "
                        + "supported by this node.");
            }
        }

        /**
         * The value of the target for this row.
         * @return the value of the target.
         */
        public int getTarget() {
            return m_target;
        }

        /**
         * Returns a {@link Matrix} with values of the parameters retrieved
         * from the learning columns.
         * @return the parameters
         */
        public Matrix getParameter() {
            return m_parameter;
        }
    }
}

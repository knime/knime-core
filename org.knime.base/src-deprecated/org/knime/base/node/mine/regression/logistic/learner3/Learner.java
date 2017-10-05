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
 * ------------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner3;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.knime.base.node.mine.regression.RegressionTrainingData;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.util.ThreadPool;



/**
 * A Logistic Regression Learner.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 */
final class Learner {
    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Learner.class);

    private final PMMLPortObjectSpec m_outSpec;

    private final int m_maxIter;

    private final double m_eps;
    private RealMatrix A;
    private RealMatrix b;
    private double m_penaltyTerm;

    private static final String FAILING_MSG = "The logistic regression model cannot be computed. "
            + "See section \"Potential Errors and Error Handling\" in the node description for possible error "
            + "causes and fixes";


    /** the target reference category, if not set it is the last category. */
    private DataCell m_targetReferenceCategory;
    /** true when target categories should be sorted. */
    private boolean m_sortTargetCategories;
    /** true when categories of nominal data in the include list should be sorted. */
    private boolean m_sortFactorsCategories;

    private List<DataColumnSpec> m_specialColumns;

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param specialColumns The special columns that are learning columns, but cannot be represented as PMML columns (vectors).
     * @param targetReferenceCategory the target reference category, if not set it is the last category
     * @param sortTargetCategories true when target categories should be sorted
     * @param sortFactorsCategories true when categories of nominal data in the include list should be sorted
     */
    Learner(final PMMLPortObjectSpec spec,
        final List<DataColumnSpec> specialColumns, final DataCell targetReferenceCategory,
        final boolean sortTargetCategories,
        final boolean sortFactorsCategories) {
        this(spec, specialColumns, targetReferenceCategory, sortTargetCategories, sortFactorsCategories, 30, 1e-14);
    }

    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param specialColumns The special columns that are learning columns, but cannot be represented as PMML columns (vectors).
     * @param targetReferenceCategory the target reference category, if not set it is the last category
     * @param sortTargetCategories true when target categories should be sorted
     * @param sortFactorsCategories true when categories of nominal data in the include list should be sorted
     * @param maxIter the maximum number of iterations
     * @param eps threshold used to identify convergence
     */
    Learner(final PMMLPortObjectSpec spec,
            final List<DataColumnSpec> specialColumns, final DataCell targetReferenceCategory,
            final boolean sortTargetCategories,
            final boolean sortFactorsCategories,
            final int maxIter, final double eps) {
        m_outSpec = spec;
        m_specialColumns = new ArrayList<>(specialColumns);
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
     * @throws CanceledExecutionException when method is cancelled
     * @throws InvalidSettingsException When settings are inconsistent with the data
     */
    public LogisticRegressionContent perform(final BufferedDataTable data,
            final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        exec.checkCanceled();
        int iter = 0;
        boolean converged = false;

        final RegressionTrainingData trainingData = new RegressionTrainingData(data, m_outSpec, m_specialColumns, true,
            m_targetReferenceCategory, m_sortTargetCategories, m_sortFactorsCategories);
        int targetIndex = data.getDataTableSpec().findColumnIndex(m_outSpec.getTargetCols().get(0).getName());
        final int tcC = trainingData.getDomainValues().get(targetIndex).size();
        final int rC = trainingData.getRegressorCount();

        final RealMatrix beta = new Array2DRowRealMatrix(1, (tcC - 1) * (rC + 1));

        Double loglike = 0.0;
        Double loglikeOld = 0.0;

        exec.setMessage("Iterative optimization. Processing iteration 1.");
        // main loop
        while (iter < m_maxIter && !converged) {
            RealMatrix betaOld = beta.copy();
            loglikeOld = loglike;

            // Do heavy work in a separate thread which allows to interrupt it
            // note the queue may block if no more threads are available (e.g. thread count = 1)
            // as soon as we stall in 'get' this thread reduces the number of running thread
            Future<Double> future = ThreadPool.currentPool().enqueue(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    final ExecutionMonitor progMon = exec.createSubProgress(1.0 / m_maxIter);
                    irlsRls(trainingData, beta, rC, tcC, progMon);
                    progMon.setProgress(1.0);
                    return likelihood(trainingData.iterator(), beta, rC, tcC, exec);
                }
            });

            try {
                loglike = future.get();
            } catch (InterruptedException e) {
              future.cancel(true);
              exec.checkCanceled();
              throw new RuntimeException(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }

            if (Double.isInfinite(loglike) || Double.isNaN(loglike)) {
                throw new RuntimeException(FAILING_MSG);
            }
            exec.checkCanceled();
            // test for decreasing likelihood
            while ((Double.isInfinite(loglike) || Double.isNaN(loglike)
                    || loglike < loglikeOld) && iter > 0) {
                converged = true;
                for (int k = 0; k < beta.getRowDimension(); k++) {
                    if (abs(beta.getEntry(k, 0) - betaOld.getEntry(k, 0)) > m_eps
                            * abs(betaOld.getEntry(k, 0))) {
                        converged = false;
                        break;
                    }
                }
                if (converged) {
                    break;
                }
                // half the step size of beta
                beta.setSubMatrix((beta.add(betaOld)).scalarMultiply(0.5).getData(), 0, 0);
                exec.checkCanceled();
                loglike = likelihood(trainingData.iterator(), beta, rC, tcC, exec);
                exec.checkCanceled();
            }

            // test for convergence
            converged = true;
            for (int k = 0; k < beta.getRowDimension(); k++) {
                if (abs(beta.getEntry(k, 0) - betaOld.getEntry(k, 0)) > m_eps
                        * abs(betaOld.getEntry(k, 0))) {
                    converged = false;
                    break;
                }
            }
            iter++;

            LOGGER.debug("#Iterations: " + iter);
            LOGGER.debug("Log Likelihood: " + loglike);
            StringBuilder betaBuilder = new StringBuilder();
            for (int i = 0; i < beta.getRowDimension() - 1; i++) {
                betaBuilder.append(Double.toString(beta.getEntry(i, 0)));
                betaBuilder.append(", ");
            }
            if (beta.getRowDimension() > 0) {
              betaBuilder.append(Double.toString(beta.getEntry(
                      beta.getRowDimension() - 1, 0)));
            }
            LOGGER.debug("beta: " + betaBuilder.toString());

            exec.checkCanceled();
            exec.setMessage("Iterative optimization. #Iterations: " + iter + " | Log-likelihood: "
                    + DoubleFormat.formatDouble(loglike) + ". Processing iteration " +  (iter + 1) + ".");
        }
        // The covariance matrix
        RealMatrix covMat = new QRDecomposition(A).getSolver().getInverse().scalarMultiply(-1);

        List<String> factorList = new ArrayList<String>();
        List<String> covariateList = new ArrayList<String>();
        Map<String, List<DataCell>> factorDomainValues =
            new HashMap<String, List<DataCell>>();
        for (int i : trainingData.getActiveCols()) {
            DataColumnSpec columnSpec = data.getDataTableSpec().getColumnSpec(i);
            if (trainingData.getIsNominal().get(i)) {
                String factor =
                    columnSpec.getName();
                factorList.add(factor);
                List<DataCell> values = trainingData.getDomainValues().get(i);
                factorDomainValues.put(factor, values);
            } else {
                if (columnSpec.getType().isCompatible(BitVectorValue.class) || columnSpec.getType().isCompatible(ByteVectorValue.class) ) {
                    int length = trainingData.getVectorLengths().getOrDefault(i, 0).intValue();
                    for (int j = 0; j < length; ++j) {
                        covariateList.add(columnSpec.getName() + "[" + j + "]");
                    }
                } else {
                    covariateList.add(
                        columnSpec.getName());
                }
            }
        }

        final Map<? extends Integer, Integer> vectorIndexLengths = trainingData.getVectorLengths();
        final Map<String, Integer> vectorLengths = new LinkedHashMap<String, Integer>();
        for (DataColumnSpec spec: m_specialColumns) {
            int colIndex = data.getSpec().findColumnIndex(spec.getName());
            if (colIndex >= 0) {
                vectorLengths.put(spec.getName(), vectorIndexLengths.get(colIndex));
            }
        }
        // create content
        LogisticRegressionContent content =
            new LogisticRegressionContent(m_outSpec,
                    factorList, covariateList, vectorLengths,
                    m_targetReferenceCategory, m_sortTargetCategories, m_sortFactorsCategories,
                    beta, loglike, covMat, iter);
        return content;
    }

    /**
     * Do a irls step. The result is stored in beta.
     *
     * @param data over trainings data.
     * @param beta parameter vector
     * @param rC regressors count
     * @param tcC target category count
     * @throws CanceledExecutionException when method is cancelled
     */
    private void irlsRls(final RegressionTrainingData data, final RealMatrix beta,
        final int rC, final int tcC, final ExecutionMonitor exec)
                throws CanceledExecutionException {
        Iterator<RegressionTrainingRow> iter = data.iterator();
        long rowCount = 0;
        int dim = (rC + 1) * (tcC - 1);
        RealMatrix xTwx = new Array2DRowRealMatrix(dim, dim);
        RealMatrix xTyu = new Array2DRowRealMatrix(dim, 1);

        RealMatrix x = new Array2DRowRealMatrix(1, rC + 1);
        RealMatrix eBetaTx = new Array2DRowRealMatrix(1, tcC - 1);
        RealMatrix pi = new Array2DRowRealMatrix(1, tcC - 1);
        final long totalRowCount = data.getRowCount();
        while (iter.hasNext()) {
            rowCount++;
            RegressionTrainingRow row = iter.next();
            exec.checkCanceled();
            exec.setProgress(rowCount / (double)totalRowCount, "Row " + rowCount + "/" + totalRowCount);
            x.setEntry(0, 0, 1);
            x.setSubMatrix(row.getParameter().getData(), 0, 1);

            for (int k = 0; k < tcC - 1; k++) {
                RealMatrix betaITx = x.multiply(beta.getSubMatrix(0, 0,
                            k * (rC + 1), (k + 1) * (rC + 1) - 1).transpose());
                eBetaTx.setEntry(0, k, Math.exp(betaITx.getEntry(0, 0)));
            }

            double sumEBetaTx = 0;
            for (int k = 0; k < tcC - 1; k++) {
                sumEBetaTx += eBetaTx.getEntry(0, k);
            }

            for (int k = 0; k < tcC - 1; k++) {
                double pik = eBetaTx.getEntry(0, k) / (1 + sumEBetaTx);
                pi.setEntry(0, k, pik);
            }

            // fill the diagonal blocks of matrix xTwx (k = k')
            for (int k = 0; k < tcC - 1; k++) {
                for (int i = 0; i < rC + 1; i++) {
                    for (int ii = i; ii < rC + 1; ii++) {
                        int o = k * (rC + 1);
                        double v = xTwx.getEntry(o + i, o + ii);
                        double w = pi.getEntry(0, k) * (1 - pi.getEntry(0, k));
                        v += x.getEntry(0, i) * w * x.getEntry(0, ii);
                        xTwx.setEntry(o + i, o + ii, v);
                        xTwx.setEntry(o + ii, o + i, v);
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
                            double v = xTwx.getEntry(o1 + i, o2 + ii);
                            double w = -pi.getEntry(0, k) * pi.getEntry(0, kk);
                            v += x.getEntry(0, i) * w * x.getEntry(0, ii);
                            xTwx.setEntry(o1 + i, o2 + ii, v);
                            xTwx.setEntry(o1 + ii, o2 + i, v);
                            xTwx.setEntry(o2 + ii, o1 + i, v);
                            xTwx.setEntry(o2 + i, o1 + ii, v);
                        }
                    }
                }
            }

            int g = (int)row.getTarget();
            // fill matrix xTyu
            for (int k = 0; k < tcC - 1; k++) {
                for (int i = 0; i < rC + 1; i++) {
                    int o = k * (rC + 1);
                    double v = xTyu.getEntry(o + i, 0);
                    double y = k == g ? 1 : 0;
                    v += (y - pi.getEntry(0, k)) * x.getEntry(0, i);
                    xTyu.setEntry(o + i, 0, v);
                }
            }


        }

        if (m_penaltyTerm > 0.0) {
            RealMatrix stdError = getStdErrorMatrix(xTwx);
            // do not penalize the constant terms
            for (int i = 0; i < tcC - 1; i++) {
                stdError.setEntry(i * (rC + 1), i * (rC + 1), 0);
            }
            xTwx = xTwx.add(stdError.scalarMultiply(-0.00001));
        }
        exec.checkCanceled();
        b = xTwx.multiply(beta.transpose()).add(xTyu);
        A = xTwx;
        if (rowCount < A.getColumnDimension()) {
            throw new IllegalStateException("The dataset must have at least "
                    + A.getColumnDimension() + " rows, but it has only "
                    + rowCount + " rows. It is recommended to use a "
                    + "larger dataset in order to increase accuracy.");
        }
        DecompositionSolver solver = new SingularValueDecomposition(A).getSolver();
        //boolean isNonSingular = solver.isNonSingular();
//        if (isNonSingular) {
            RealMatrix betaNew = solver.solve(b);
            beta.setSubMatrix(betaNew.transpose().getData(), 0, 0);
//        } else {
//            throw new RuntimeException(FAILING_MSG);
//        }
    }

    private RealMatrix getStdErrorMatrix(final RealMatrix xTwx) {
        RealMatrix covMat = new QRDecomposition(xTwx).getSolver().getInverse().scalarMultiply(-1);
        // the standard error estimate
        RealMatrix stdErr = new Array2DRowRealMatrix(covMat.getColumnDimension(),
                covMat.getRowDimension());
        for (int i = 0; i < covMat.getRowDimension(); i++) {
            stdErr.setEntry(i, i, sqrt(abs(covMat.getEntry(i, i))));
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
     * @throws CanceledExecutionException when method is cancelled
     */
    private double likelihood(final Iterator<RegressionTrainingRow> iter,
            final RealMatrix beta,
            final int rC, final int tcC,
            final ExecutionContext exec) throws CanceledExecutionException {
        double loglike = 0;

        RealMatrix x = new Array2DRowRealMatrix(1, rC + 1);
        while (iter.hasNext()) {
            exec.checkCanceled();
            RegressionTrainingRow row = iter.next();

            x.setEntry(0, 0, 1);
            x.setSubMatrix(row.getParameter().getData(), 0, 1);

            double sumEBetaTx = 0;
            for (int i = 0; i < tcC - 1; i++) {
                RealMatrix betaITx = x.multiply(beta.getSubMatrix(0, 0,
                        i * (rC + 1), (i + 1) * (rC + 1) - 1).transpose());
                sumEBetaTx += Math.exp(betaITx.getEntry(0, 0));
            }

            int y = (int)row.getTarget();
            double yBetaTx = 0;
            if (y < tcC - 1) {
                yBetaTx = x.multiply(beta.getSubMatrix(0, 0,
                            y * (rC + 1), (y + 1) * (rC + 1) - 1).transpose()
                            ).getEntry(0, 0);
            }
            loglike += yBetaTx - Math.log(1 + sumEBetaTx);
        }

        return loglike;
    }
}

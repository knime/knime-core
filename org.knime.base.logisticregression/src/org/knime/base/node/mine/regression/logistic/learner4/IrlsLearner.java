/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
package org.knime.base.node.mine.regression.logistic.learner4;

import static java.lang.Math.abs;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.util.ThreadPool;



/**
 * A Logistic Regression Learner based on the iteratively reweighted least squares algorithm.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 */
final class IrlsLearner implements LogRegLearner {
    /** Logger to print debug info to. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(IrlsLearner.class);

    private final int m_maxIter;

    private final double m_eps;
    private RealMatrix A;
    private RealMatrix b;
//    private double m_penaltyTerm;
    private final boolean m_calcCovMatrix;

    private String m_warning;

    private static final String FAILING_MSG = "The logistic regression model cannot be computed. "
            + "See section \"Potential Errors and Error Handling\" in the node description for possible error "
            + "causes and fixes";


    /**
     * @param spec The {@link PMMLPortObjectSpec} of the output table.
     * @param specialColumns The special columns that are learning columns, but cannot be represented as PMML columns (vectors).
     * @param targetReferenceCategory the target reference category, if not set it is the last category
     * @param sortTargetCategories true when target categories should be sorted
     * @param sortFactorsCategories true when categories of nominal data in the include list should be sorted
     * @param maxIter the maximum number of iterations
     * @param eps threshold used to identify convergence
     */
    IrlsLearner(final int maxIter, final double eps,
            final boolean calcCovMatrix) {
        m_maxIter = maxIter;
        m_eps = eps;
//        m_penaltyTerm = 0.0;
        m_calcCovMatrix = calcCovMatrix;
    }


    /**
     * Do an irls step. The result is stored in beta.
     *
     * @param data over trainings data.
     * @param beta parameter vector
     * @param rC regressors count
     * @param tcC target category count
     * @throws CanceledExecutionException when method is cancelled
     */
    private void irlsRls(final TrainingData<ClassificationTrainingRow> data, final RealMatrix beta,
        final int rC, final int tcC, final ExecutionMonitor exec)
                throws CanceledExecutionException {
        long rowCount = 0;
        int dim = (rC + 1) * (tcC - 1);
        RealMatrix xTwx = MatrixUtils.createRealMatrix(dim, dim);
        RealMatrix xTyu = MatrixUtils.createRealMatrix(dim, 1);

        double[] eBetaTx = new double[tcC - 1];
        double[] pi = new double[tcC - 1];
        final long totalRowCount = data.getRowCount();
        for(ClassificationTrainingRow row : data) {
            rowCount++;
            exec.checkCanceled();
            exec.setProgress(rowCount / (double)totalRowCount, "Row " + rowCount + "/" + totalRowCount);

            for (int k = 0; k < tcC - 1; k++) {
                double z = 0.0;
                for (FeatureIterator iter = row.getFeatureIterator(); iter.next();) {
                    double featureVal = iter.getFeatureValue();
                    int featureIdx = iter.getFeatureIndex();
                    z += featureVal * beta.getEntry(0, k * (rC + 1) + featureIdx);
                }
                eBetaTx[k] = Math.exp(z);
            }

            double sumEBetaTx = 0;
            for (int k = 0; k < tcC - 1; k++) {
                sumEBetaTx += eBetaTx[k];
            }

            for (int k = 0; k < tcC - 1; k++) {
                double pik = eBetaTx[k] / (1 + sumEBetaTx);
                pi[k] = pik;
            }

            // fill xTwx (aka the hessian of the loglikelihood)
            for (FeatureIterator outer = row.getFeatureIterator(); outer.next();) {
                int i = outer.getFeatureIndex();
                double outerVal = outer.getFeatureValue();
                for (FeatureIterator inner = outer.spawn(); inner.next();) {
                    int ii = inner.getFeatureIndex();
                    double innerVal = inner.getFeatureValue();
                    for (int k = 0; k < tcC - 1; k++) {
                        for (int kk = k; kk < tcC - 1; kk++) {
                            int o1 = k * (rC + 1);
                            int o2 = kk * (rC + 1);
                            double v = xTwx.getEntry(o1 + i, o2 + ii);
                            if (k == kk) {
                                double w = pi[k] * (1 - pi[k]);
                                v += outerVal * w * innerVal;
                                assert o1 == o2;
                            } else {
                                double w = -pi[k] * pi[kk];
                                v += outerVal * w * innerVal;
                            }
                            xTwx.setEntry(o1 + i, o2 + ii, v);
                            xTwx.setEntry(o1 + ii, o2 + i, v);
                            if (k != kk) {
                                xTwx.setEntry(o2 + ii, o1 + i, v);
                                xTwx.setEntry(o2 + i, o1 + ii, v);
                            }
                        }
                    }
                }
            }


            int g = row.getCategory();
            // fill matrix xTyu
            for (FeatureIterator iter = row.getFeatureIterator(); iter.next();) {
                int idx = iter.getFeatureIndex();
                double val = iter.getFeatureValue();
                for (int k = 0; k < tcC - 1; k++) {
                    int o = k * (rC + 1);
                    double v = xTyu.getEntry(o + idx, 0);
                    double y = k == g ? 1 : 0;
                    v += (y - pi[k]) * val;
                    xTyu.setEntry(o + idx, 0, v);
                }
            }


        }

        // currently not used but could become interesting in the future
//        if (m_penaltyTerm > 0.0) {
//            RealMatrix stdError = getStdErrorMatrix(xTwx);
//            // do not penalize the constant terms
//            for (int i = 0; i < tcC - 1; i++) {
//                stdError.setEntry(i * (rC + 1), i * (rC + 1), 0);
//            }
//            xTwx = xTwx.add(stdError.scalarMultiply(-0.00001));
//        }
        exec.checkCanceled();
        b = xTwx.multiply(beta.transpose()).add(xTyu);
        A = xTwx;
        if (rowCount < A.getColumnDimension()) {
            // fall back check: This case should already be handled on a higher level
            // but it's important to ensure this property
            throw new IllegalStateException("The dataset must have at least "
                    + A.getColumnDimension() + " rows, but it has only "
                    + rowCount + " rows. It is recommended to use a "
                    + "larger dataset in order to increase accuracy.");
        }
        DecompositionSolver solver = new SingularValueDecomposition(A).getSolver();
        RealMatrix betaNew = solver.solve(b);
        beta.setSubMatrix(betaNew.transpose().getData(), 0, 0);
    }

//    private RealMatrix getStdErrorMatrix(final RealMatrix xTwx) {
//        RealMatrix covMat = new QRDecomposition(xTwx).getSolver().getInverse().scalarMultiply(-1);
//        // the standard error estimate
//        RealMatrix stdErr = MatrixUtils.createRealMatrix(covMat.getColumnDimension(),
//                covMat.getRowDimension());
//        for (int i = 0; i < covMat.getRowDimension(); i++) {
//            stdErr.setEntry(i, i, sqrt(abs(covMat.getEntry(i, i))));
//        }
//        return stdErr;
//    }


    /**
     * Compute the likelihood at given beta.
     *
     * @param iter iterator over trainings data.
     * @param beta parameter vector
     * @param rC regressors count
     * @param tcC target category count
     * @throws CanceledExecutionException when method is cancelled
     */
    private double likelihood(final Iterator<ClassificationTrainingRow> iter,
            final RealMatrix beta,
            final int rC, final int tcC,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        double loglike = 0;

        RealMatrix x = MatrixUtils.createRealMatrix(1, rC + 1);
        while (iter.hasNext()) {
            exec.checkCanceled();
            ClassificationTrainingRow row = iter.next();

            fillXFromRow(x, row);

            double sumEBetaTx = 0;
            for (int i = 0; i < tcC - 1; i++) {
                RealMatrix betaITx = x.multiply(beta.getSubMatrix(0, 0,
                        i * (rC + 1), (i + 1) * (rC + 1) - 1).transpose());
                sumEBetaTx += Math.exp(betaITx.getEntry(0, 0));
            }

            int y = row.getCategory();
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

    private static void fillXFromRow(final RealMatrix x, final ClassificationTrainingRow row) {
        FeatureIterator iter = row.getFeatureIterator();
        boolean hasNext = iter.next();
        for (int i = 0; i < x.getColumnDimension(); i++) {
            double val = 0.0;
            if (hasNext && iter.getFeatureIndex() == i) {
                val = iter.getFeatureValue();
                hasNext = iter.next();
            }
            x.setEntry(0, i, val);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getWarningMessage() {
        return m_warning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogRegLearnerResult learn(final TrainingData<ClassificationTrainingRow> trainingData, final ExecutionMonitor exec)
        throws CanceledExecutionException, InvalidSettingsException {
        exec.checkCanceled();
        int iter = 0;
        boolean converged = false;

        final int tcC = trainingData.getTargetDimension() + 1;
        final int rC = trainingData.getFeatureCount() - 1;

        final RealMatrix beta = MatrixUtils.createRealMatrix(1, (tcC - 1) * (rC + 1));

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
                for (int k = 0; k < beta.getColumnDimension(); k++) {
                    if (abs(beta.getEntry(0, k) - betaOld.getEntry(0, k)) > m_eps
                            * abs(betaOld.getEntry(0, k))) {
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
            for (int k = 0; k < beta.getColumnDimension(); k++) {
                if (abs(beta.getEntry(0, k) - betaOld.getEntry(0, k)) > m_eps
                        * abs(betaOld.getEntry(0, k))) {
                    converged = false;
                    break;
                }
            }
            iter++;

            LOGGER.debug("#Iterations: " + iter);
            LOGGER.debug("Log Likelihood: " + loglike);
            StringBuilder betaBuilder = new StringBuilder();
            for (int i = 0; i < beta.getColumnDimension() - 1; i++) {
                betaBuilder.append(Double.toString(beta.getEntry(0, i)));
                betaBuilder.append(", ");
            }
            if (beta.getColumnDimension() > 0) {
              betaBuilder.append(Double.toString(beta.getEntry(0,
                      beta.getColumnDimension() - 1)));
            }
            LOGGER.debug("beta: " + betaBuilder.toString());

            exec.checkCanceled();
            exec.setMessage("Iterative optimization. #Iterations: " + iter + " | Log-likelihood: "
                    + DoubleFormat.formatDouble(loglike) + ". Processing iteration " +  (iter + 1) + ".");
        }
        StringBuilder warnBuilder = new StringBuilder();
        if (iter >= m_maxIter) {
            warnBuilder.append("The algorithm did not reach convergence after the specified number of epochs. "
                    + "Setting the epoch limit higher might result in a better model.");
        }
        // The covariance matrix
        RealMatrix covMat = null;
        if (m_calcCovMatrix) {
            try {
                covMat = new QRDecomposition(A).getSolver().getInverse().scalarMultiply(-1);
            } catch (SingularMatrixException sme) {
                if (warnBuilder.length() > 0) {
                    warnBuilder.append("\n");
                }
                warnBuilder.append("The covariance matrix could not be calculated because the"
                    + " observed fisher information matrix was singular.");
            }
        }
        RealMatrix betaMat = MatrixUtils.createRealMatrix(tcC - 1, rC + 1);
        for (int i = 0; i < beta.getColumnDimension(); i++) {
            int r = i / (rC + 1);
            int c = i % (rC + 1);
            betaMat.setEntry(r, c, beta.getEntry(0, i));
        }
        m_warning = warnBuilder.length() > 0 ? warnBuilder.toString() : null;
        return new LogRegLearnerResult(betaMat, covMat, iter, loglike);
    }
}

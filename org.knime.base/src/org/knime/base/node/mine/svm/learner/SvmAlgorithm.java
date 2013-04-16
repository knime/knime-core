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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.mine.svm.learner;

import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * This class is the implementation of a binary SVM learning algorithm.
 *
 * The main algorithm is described in:
 *
 * Sequential Minimal Optimization: A Fast Algorithm for Training Support Vector
 * Machines, by John C. Platt. This source code also contains the improvements
 * to this algorithm presented in: Improvements to Platt's SMO Algorithm for SVM
 * Classifier Design, by Keerthi a.o.
 *
 * In order to understand this code, you should read the above papers (which
 * describe the algorithm used) and possibly other documents about SVMs in
 * general.
 *
 * The variable names used in this class follow the notations from the papers.
 *
 * @author Stefan, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class SvmAlgorithm {

    /*
     * NodeLogger for this class.
     */
//    private static final NodeLogger LOGGER =
//            NodeLogger.getLogger(SvmAlgorithm.class);
//
    /*
     * the input data.
     */
    private DoubleVector[] m_inputData;

    /*
     * if a vector has this class, it is considered a 'positive' example. all
     * other classes are considered 'negative'.
     */
    private String m_positiveClass;

    /*
     * a pointer to the kernel to use for the computation.
     */
    private Kernel m_kernel;

    /*
     * the C parameter (upper bound for alpha's) -- needed for when the input
     * data is not separable.
     */
    private double m_paramC;

    /*
     * the Lagrange coefficients.
     */
    private double[] m_alpha;

    /*
     * the error cache.
     */
    private double[] m_fcache;

    /*
     * the offsets, as described in the 2nd paper.
     */
    private double m_bUp, m_bLow, m_b;

    /*
     * the indices of m_bUp and m_bLow, as described in the 2nd paper.
     */
    private int m_iUp, m_iLow;

    /*
     * the five sets described in the 2nd paper.
     */
    private Set<Integer> m_i0, m_i1, m_i2, m_i3, m_i4;

    /*
     * the tolerance level for optimality.
     */
    private static final double TOLERANCE = 1.0e-3;

    /*
     * the difference up to which two doubles are considered equal.
     */
    private static final double EPSILON = 1.0e-12;

    /**
     * The main constructor.
     *
     * @param inputData the input vectors
     * @param positiveClass the class value for which to consider an input
     *            vector a 'positive' example. if input vectors have other class
     *            values, they are considered 'negative'
     * @param paramC the "C" from the problem constraints
     * @param kernel the kernel to use in the algorithm
     */
    public SvmAlgorithm(final DoubleVector[] inputData,
            final String positiveClass, final Kernel kernel,
            final double paramC) {
        m_inputData = inputData;
        m_positiveClass = positiveClass;
        m_kernel = kernel;
        m_paramC = paramC;
        m_alpha = new double[m_inputData.length];
    }

    /**
     * test if the parameter is very close to zero.
     *
     * @param x the number to check
     * @return true if the number is close to zero false otherwise
     */
    private static boolean zero(final double x) {
        return -EPSILON <= x && x <= EPSILON;
    }

    /**
     * test if the two parameters are very close to each other.
     *
     * @param x the first parameter to check for equality
     * @param y the second parameter to check for equality
     * @return true if they are almost equal false otherwise
     */
    private static boolean equal(final double x, final double y) {
        return zero(x - y);
    }

    /**
     * determine the output for a given input vector.
     *
     * @return -1 or 1, depending if the input is a 'positive' example or not
     * @param i the index of the input vector
     */
    private double target(final int i) {
        if (m_inputData[i].getClassValue().equals(m_positiveClass)) {
            return 1.0;
        } else {
            return -1.0;
        }
    }

    /**
     * compute the predicted value of a vector by using the current SVM.
     *
     * @param i1 the index of the vector
     * @return the predicted value (-1 or 1)
     */
    private double computeSvmOutput(final int i1) {
        double result = 0;

        for (int i2 = 0; i2 < m_alpha.length; ++i2) {
            if (!zero(m_alpha[i2])) {
                double alpha = m_alpha[i2];
                double targ = target(i2);
                double kern =
                        m_kernel.evaluate(m_inputData[i1], m_inputData[i2]);
                result += alpha * targ * kern;
            }
        }
        result -= m_b;
        return result;
    }

    /**
     * given an index, add it to one of I0, I1, I2, I3, I4, taking into account
     * the conditions. see 2nd paper.
     */
    private void addToCorrectSet(final int i) {
        if (target(i) == -1.0) {
            if (equal(m_alpha[i], m_paramC)) {
                m_i2.add(i);
            } else if (equal(m_alpha[i], 0.0)) {
                m_i4.add(i);
            } else {
                m_i0.add(i);
            }
        } else {
            if (equal(m_alpha[i], m_paramC)) {
                m_i3.add(i);
            } else if (equal(m_alpha[i], 0.0)) {
                m_i1.add(i);
            } else {
                m_i0.add(i);
            }
        }
    }

    /**
     * update the sets knowing only i1 and i2 may have changed.
     *
     * @param i1 ...
     * @param i2 ...
     */
    private void updateSets(final int i1, final int i2) {
        m_i0.remove(i1);
        m_i1.remove(i1);
        m_i2.remove(i1);
        m_i3.remove(i1);
        m_i4.remove(i1);

        m_i0.remove(i2);
        m_i1.remove(i2);
        m_i2.remove(i2);
        m_i3.remove(i2);
        m_i4.remove(i2);

        addToCorrectSet(i1);
        addToCorrectSet(i2);
    }

    /**
     * after a successful optimization step, make sure each element is in the
     * set it belongs to.
     *
     * @param i1 the first Lagrange coefficient optimized
     * @param i2 the second Lagrange coefficient optimized
     */
    private void repairSets(final int i1, final int i2) {
        m_bLow = -Double.MAX_VALUE;
        m_bUp = Double.MAX_VALUE;
        m_iLow = -1;
        m_iUp = -1;
        for (int j : m_i0) {
            if (m_fcache[j] < m_bUp) {
                m_bUp = m_fcache[j];
                m_iUp = j;
            }
            if (m_fcache[j] > m_bLow) {
                m_bLow = m_fcache[j];
                m_iLow = j;
            }
        }
        if (!m_i0.contains(i1)) {
            if (m_i3.contains(i1) || m_i4.contains(i1)) {
                if (m_fcache[i1] > m_bLow) {
                    m_bLow = m_fcache[i1];
                    m_iLow = i1;
                }
            } else {
                if (m_fcache[i1] < m_bUp) {
                    m_bUp = m_fcache[i1];
                    m_iUp = i1;
                }
            }
        }
        if (!m_i0.contains(i2)) {
            if (m_i3.contains(i2) || m_i4.contains(i2)) {
                if (m_fcache[i2] > m_bLow) {
                    m_bLow = m_fcache[i2];
                    m_iLow = i2;
                }
            } else {
                if (m_fcache[i2] < m_bUp) {
                    m_bUp = m_fcache[i2];
                    m_iUp = i2;
                }
            }
        }

        assert m_iLow != -1;
        assert m_iUp != -1;
    }

    /**
     * Given two examples, optimize their Lagrange coefficients.
     *
     * see Sequential Minimal Optimization: A Fast Algorithm for Training
     * Support Vector Machines by John C. Platt for pseudocode.
     *
     * @param i1 first index
     * @param i2 second index
     * @return was the optimization successful?
     */
    private boolean takeStep(final int i1, final int i2) {
        if (i1 == i2) {
            return false;
        }
        double alpha1 = m_alpha[i1];
        double alpha2 = m_alpha[i2];
        double y1 = target(i1);
        double y2 = target(i2);
        double f1 = m_fcache[i1];
        double f2 = m_fcache[i2];
        double s = y1 * y2;
        double low, high; // L, H -- equations (13), (14) from the paper above
        if (y1 != y2) {
            low = Math.max(0, alpha2 - alpha1);
            high = Math.min(m_paramC, m_paramC + alpha2 - alpha1);
        } else {
            low = Math.max(0, alpha2 + alpha1 - m_paramC);
            high = Math.min(m_paramC, alpha2 + alpha1);
        }
        if (Math.abs(low - high) < EPSILON) {
            return false;
        }
        double k11 = m_kernel.evaluate(m_inputData[i1], m_inputData[i1]);
        double k12 = m_kernel.evaluate(m_inputData[i1], m_inputData[i2]);
        double k22 = m_kernel.evaluate(m_inputData[i2], m_inputData[i2]);
        double eta = k11 + k22 - 2.0 * k12; // value of second derivative
        double a2;
        if (eta > 0) {
            a2 = alpha2 + y2 * (f1 - f2) / eta;
            if (a2 < low) {
                a2 = low;
            } else if (a2 > high) {
                a2 = high;
            }
        } else {
            return false;
        }
        if (a2 < EPSILON) {
            a2 = 0.0;
        } else if (a2 > m_paramC - EPSILON) {
            a2 = m_paramC;
        }
        if (Math.abs(a2 - alpha2) < EPSILON * (a2 + alpha2 + EPSILON)) {
            return false;
        }
        double a1 = alpha1 + s * (alpha2 - a2);
        m_alpha[i1] = a1;
        m_alpha[i2] = a2;
        updateSets(i1, i2);
        for (int i : m_i0) {
            if ((i != i1) && (i != i2)) {
                m_fcache[i] +=
                        y1
                                * (a1 - alpha1)
                                * m_kernel.evaluate(m_inputData[i1],
                                        m_inputData[i])
                                + y2
                                * (a2 - alpha2)
                                * m_kernel.evaluate(m_inputData[i2],
                                        m_inputData[i]);
            }
        }
        m_fcache[i1] += y1 * (a1 - alpha1) * k11 + y2 * (a2 - alpha2) * k12;
        m_fcache[i2] += y1 * (a1 - alpha1) * k12 + y2 * (a2 - alpha2) * k22;
        repairSets(i1, i2);
        m_b = (m_bLow + m_bUp) * 0.5;
        return true;
    }

    /**
     * Given one of the example with which to optimize, find another convenient
     * example and optimize the alpha's of the two examples.
     *
     * see Sequential Minimal Optimization: A Fast Algorithm for Training
     * Support Vector Machines by John C. Platt and also Improvements to Platt's
     * SMO Algorithm for SVM Classifier Design
     */
    private boolean examineExample(final int i2) {
        int i1 = -1;
        double y2 = target(i2);
        double f2;
        if (m_i0.contains(i2)) {
            f2 = m_fcache[i2];
        } else {
            f2 = computeSvmOutput(i2) + m_b - y2;
            m_fcache[i2] = f2;
            if ((m_i1.contains(i2) || m_i2.contains(i2)) && (f2 < m_bUp)) {
                m_bUp = f2;
                m_iUp = i2;
            } else if ((m_i3.contains(i2) || m_i4.contains(i2))
                    && (f2 > m_bLow)) {
                m_bLow = f2;
                m_iLow = i2;
            }
        }
        boolean optimality = true;
        if (m_i0.contains(i2) || m_i1.contains(i2) || m_i2.contains(i2)) {
            if (m_bLow - f2 > 2.0 * TOLERANCE) {
                optimality = false;
                i1 = m_iLow;
            }
        }
        if (m_i0.contains(i2) || m_i3.contains(i2) || m_i4.contains(i2)) {
            if (f2 - m_bUp > 2.0 * TOLERANCE) {
                optimality = false;
                i1 = m_iUp;
            }
        }
        if (optimality) {
            return false;
        }
        if (m_i0.contains(i2)) {
            if (m_bLow - f2 > f2 - m_bUp) {
                i1 = m_iLow;
            } else {
                i1 = m_iUp;
            }
        }
        assert i1 != -1;
        return takeStep(i1, i2);
    }

    /**
     * Check the amount by which the KKT conditions for the i'th example are
     * violated.
     *
     * @param i the index of the example to examine
     * @return the amount of the violation
     */
//    private double kktViolation(final int i) {
//        double result = 0;
//        if (zero(m_alpha[i])) {
//            double delta = target(i) * computeSvmOutput(i) - (1.0 - EPSILON);
//            if (delta < 0.0) {
//                result = -delta;
//            }
//        } else if (equal(m_alpha[i], m_paramC)) {
//            double delta = target(i) * computeSvmOutput(i) - (1.0 - EPSILON);
//            if (delta > 0.0) {
//                result = delta;
//            }
//        } else {
//            double delta = target(i) * computeSvmOutput(i) - (1.0 - EPSILON);
//            result = Math.abs(delta);
//        }
//        return result;
//    }

    /**
     * check the Karush-Kuhn-Tucker conditions. If these conditions are met, we
     * have reached the optimal solution. Otherwise, we screwed up somewhere in
     * the algorithm (or we are not done yet).
     */
//    private double kktGlobalViolation() {
//        double result = 0.0;
//        for (int i = 0; i < m_alpha.length; ++i) {
//            result = Math.max(result, kktViolation(i));
//        }
//        return result;
//    }

    /**
     * Implements the main algorithm (Sequential minimal optimization). see
     * Sequential Minimal Optimization: A Fast Algorithm for Training Support
     * Vector Machines by John C. Platt for pseudocode.
     *
     * @param exec report progress is reported here
     */
    private void mainAlgorithm(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        int numChanged = 0;
        boolean examineAll = true;

        m_fcache = new double[m_alpha.length];
        for (int i = 0; i < m_alpha.length; ++i) {
            m_fcache[i] = 0.0;
            m_alpha[i] = 0.0;
        }
        m_b = 0.0;
        m_bUp = -1.0;
        m_bLow = 1.0;
        m_iUp = -1;
        m_iLow = -1;
        for (int i = 0; i < m_alpha.length; ++i) {
            if (target(i) == 1.0) {
                m_iUp = i;
            } else {
                m_iLow = i;
            }
        }
        assert m_iUp != -1 : "Input data doesn't contain two classes";
        assert m_iLow != -1 : "Input data doesn't contain two classes";
        m_fcache[m_iLow] = 1;
        m_fcache[m_iUp] = -1;

        m_i0 = new HashSet<Integer>();
        m_i1 = new HashSet<Integer>();
        m_i2 = new HashSet<Integer>();
        m_i3 = new HashSet<Integer>();
        m_i4 = new HashSet<Integer>();

        for (int i = 0; i < m_alpha.length; ++i) {
            if (target(i) == 1.0) {
                m_i1.add(i);
            } else {
                m_i4.add(i);
            }
        }
//        double maximalViolation = kktGlobalViolation();
//        int steps = 0;

        while (numChanged > 0 || examineAll) {
//            steps++;
//            double currentViolation = kktGlobalViolation();
//            if (currentViolation > maximalViolation) {
//                maximalViolation = currentViolation;
//            }
//            double progress = 1.0 - currentViolation / maximalViolation;
//            progress = Math.min(progress, 1.0);
//            progress = Math.max(progress, 0.0);
//            if (steps > 2) {
//                // don't show the progress from the very beginning
//                // wait some steps for everything to stabilize
//                exec.setProgress(progress);
//            }
            exec.checkCanceled();
            numChanged = 0;
            if (examineAll) {
                for (int i = 0; i < m_inputData.length; ++i) {
                    exec.checkCanceled();
                    if (examineExample(i)) {
                        numChanged++;
                    }
                }
            } else {
                Set<Integer> i0 = new HashSet<Integer>(m_i0);
                for (int i : i0) {
                    exec.checkCanceled();
                    if (examineExample(i)) {
                        numChanged++;
                    }
                    if (m_bUp > m_bLow - 2.0 * TOLERANCE) {
                        numChanged = 0;
                        break;
                    }
                }
            }

            if (examineAll) {
                examineAll = false;
            } else if (numChanged == 0) {
                examineAll = true;
            }
        }
        exec.setProgress(1.0);
//        LOGGER.debug("Final KKT Violation: " + kktGlobalViolation());
        final double half = 0.5;
        m_b = (m_bLow + m_bUp) * half;
    }

    /**
     * Runs the main algorithm and return the resulting SVM.
     *
     * @param exec progress is reported here
     * @return the resulting SVM
     * @throws CanceledExecutionException if the algorithm is canceled.
     * @throws Exception if the algorithm is not able to find support vectors.
     */
    public Svm run(final ExecutionMonitor exec)
            throws CanceledExecutionException, Exception {
        for (int i = 0; i < m_alpha.length; ++i) {
            m_alpha[i] = 0.0;
        }
        m_b = 0;

        mainAlgorithm(exec);
        int countSupportVectors = 0;
        for (int i = 0; i < m_alpha.length; ++i) {
            if (!zero(m_alpha[i])) {
                countSupportVectors++;
            }
        }
        if (countSupportVectors == 0) {
            throw new Exception("No support vectors could be found "
                    + "for class " + m_positiveClass + ". Consider using a"
                    + " different kernel.");
        }
        DoubleVector[] supportVectors = new DoubleVector[countSupportVectors];
        double[] supportAlphas = new double[countSupportVectors];
        for (int i = 0, j = 0; i < m_alpha.length; ++i) {
            if (!zero(m_alpha[i])) {
                supportVectors[j] = m_inputData[i];
                supportAlphas[j] = m_alpha[i];
                ++j;
            }
        }
        return new Svm(supportVectors, supportAlphas, m_positiveClass, m_b,
                m_kernel);
    }
}

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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.mine.svm;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * This class represents a (binary) support vector machine.
 * It works by remembering the support vectors and the
 * corresponding alpha values.
 *
 * @author Stefan, University of Konstanz
 * @author cebron, University of Konstanz
 */
public class Svm {

    /* the keys under which we save the parameters. */
    private static final String KEY_CATEGORY = "Category";
    private static final String KEY_THRESHOLD = "Threshold";
    private static final String KEY_ALPHAS = "Alphas";
    private static final String KEY_SV_COUNT = "Number of support vectors";
    private static final String KEY_SV = "Support vector";
    private static final String KEY_KERNELTYPE = "Kernel type";
    private static final String KEY_KERNELPARAMS = "Kernel parameters";

    /* the support vectors. */
    private final DoubleVector[] m_supportVectors;

    /* the corresponding alpha values. */
    private final double [] m_alpha;

    /*
     * the 'positive' class. objects of this class are
     * classified as 1, other objects as -1
     */
    private final String m_positive;

    /*
     * the threshold.
     */
    private double m_b;

    /*
     * the kernel type to use.
     */
    private KernelType m_kernelType;

    /*
     * the actual kernel.
     */
    private Kernel m_kernel;

    /**
     * Constructor.
     *
     * @param supportVectors the support vectors that define the SVM
     * @param alpha the corresponding Lagrange coefficients
     * @param positive the class for which SVM should yield 1
     * @param b the threshold
     * @param kernel the kernel to use
     */
    public Svm(final DoubleVector[] supportVectors, final double[] alpha,
            final String positive, final double b, final Kernel kernel) {

        /*
         * We should have as many support vectors as Lagrange coefficients and
         * we should have both a positive and a negative class.
         */
        assert supportVectors.length == alpha.length;
//        boolean foundPositive = false;
//        boolean foundNegative = false;
//        for (int i = 0; i < supportVectors.length; ++i) {
//            if (supportVectors[i].getClassValue().equals(positive)) {
//                foundPositive = true;
//            } else {
//                foundNegative = true;
//            }
//        }
   //     assert foundPositive && foundNegative;

        m_supportVectors = supportVectors;
        m_alpha = alpha;
        m_positive = positive;
        m_b = b;
        m_kernel = kernel;
        m_kernelType = KernelFactory.getType(m_kernel);
    }

    /**
     * Determines the output for the support vector at the given index.
     * @param i the index of the support vector
     * @return -1 or 1, depending if the input is a 'positive' example or not
     */
    private double target(final int i) {
        if (m_supportVectors[i].getClassValue().equals(m_positive)) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Computes the distance from the hyperplane in the kernel induced
     * hyperspace.
     * @param vector the vector to predict
     * @return the distance from the hyperplane
     */
    public double distance(final DoubleVector vector) {
        double result = 0;
        for (int i = 0; i < m_alpha.length; ++i) {
            result += m_alpha[i] * target(i)
                     * m_kernel.evaluate(vector, m_supportVectors[i]);
        }
        result -= m_b;
        return result;
    }

    /**
     * Computes the predicted value of a vector by using the current SVM.
     * @param vector the vector for which to predict the class
     * @return the predicted value (-1 or 1)
     */
    public double predict(final DoubleVector vector) {
        double result = distance(vector);
        if (result < 0) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * The margin of a SVM is the minimum distance from all support
     * vectors to the decision hyperplane.
     * @return margin value of SVM.
     */
    public double getMargin() {
        double margin = Double.MAX_VALUE;
        for (int i = 0; i < m_supportVectors.length; i++) {
            margin = Math.abs(Math.min(margin, distance(m_supportVectors[i])));
        }
        return margin;
    }



    /**
     * Save the Support Vector Machine for later use.
     * @param predParams where the SVM will be saved.
     * @param id unique identifier for this SVM.
     */
    public void saveToPredictorParams(final ModelContentWO predParams,
            final String id) {
        predParams.addString(id + KEY_CATEGORY, m_positive);
        predParams.addDouble(id + KEY_THRESHOLD, m_b);
        predParams.addDoubleArray(id + KEY_ALPHAS, m_alpha);
        predParams.addInt(id + KEY_SV_COUNT, m_supportVectors.length);
        for (int i = 0; i < m_supportVectors.length; ++i) {
            m_supportVectors[i].saveTo(predParams, id + KEY_SV + i);
        }
        predParams.addString(id + KEY_KERNELTYPE, m_kernelType.toString());
        int count = m_kernel.getNumberParameters();
        double [] kernelParams = new double [ count ];
        for (int i = 0; i < count; ++i) {
            kernelParams[i] = m_kernel.getParameter(i);
        }
        predParams.addDoubleArray(id + KEY_KERNELPARAMS, kernelParams);
    }

    /**
     * Loads a binary SVM from a ModelContent object.
     * @param predParams the object to read the SVM configuration from.
     * @param id the unique identifier
     * @throws InvalidSettingsException if the required keys are not present
     */
    public Svm(final ModelContentRO predParams, final String id)
            throws InvalidSettingsException {
        m_positive = predParams.getString(id + KEY_CATEGORY);
        m_b = predParams.getDouble(id + KEY_THRESHOLD);
        m_alpha = predParams.getDoubleArray(id + KEY_ALPHAS);
        m_supportVectors =
                new DoubleVector[predParams.getInt(id + KEY_SV_COUNT)];
        for (int i = 0; i < m_supportVectors.length; ++i) {
            m_supportVectors[i] = new DoubleVector(predParams, id + KEY_SV + i);
        }

        m_kernelType =
                KernelType.valueOf(predParams.getString(id + KEY_KERNELTYPE));
        m_kernel = KernelFactory.getKernel(m_kernelType);
        int count = m_kernel.getNumberParameters();
        double[] kernelParams =
                predParams.getDoubleArray(id + KEY_KERNELPARAMS);
        assert count == kernelParams.length;
        for (int i = 0; i < count; ++i) {
            m_kernel.setParameter(i, kernelParams[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "Support vectors (bias = " + m_b + "):\n";
        for (int i = 0; i < m_supportVectors.length; ++i) {
            result = result + m_supportVectors[i].toString()
                        + " (alpha: " + m_alpha[i] + ")\n";
        }
        return result;
    }

    /**
     * @return the "positive" class value.
     */
    public String getPositive() {
        return m_positive;
    }

    /**
     * @return the supportVectors
     */
    public DoubleVector[] getSupportVectors() {
        return m_supportVectors;
    }

    /**
     * @return the alpha coefficients of the SVM.
     */
    public double[] getAlphas() {
        return m_alpha;
    }

    /**
     * @return the threshold value b of the SVM.s
     */
    public double getThreshold() {
        return m_b;
    }

}

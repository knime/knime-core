/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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

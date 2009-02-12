/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.mine.svm.kernel;

import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;

/**
 * RBF Kernel.
 *
 * @author Stefan Ciobaca, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class RBFKernel implements Kernel {
    /* the only kernel parameter. */
    private double m_sigma;

    /** evaluate the kernel.
     * @param a first vector
     * @param b second vector
     * @return the result
     * */
    public double evaluate(final DoubleVector a, final DoubleVector b) {
        double result = 0;
        assert a.getNumberValues() == b.getNumberValues();
        for (int i = 0; i < a.getNumberValues(); ++i) {
            double dif = a.getValue(i) - b.getValue(i);
            result = result + dif * dif;
        }
        return Math.pow(Math.E, -result / 2.0 / m_sigma / m_sigma);
    }

    /**
     * {@inheritDoc}
     */
    public double evaluate(final double[] a, final double[] b) {
        double result = 0;
        assert a.length == b.length;
        for (int i = 0; i < a.length; ++i) {
            double dif = a[i] - b[i];
            result = result + dif * dif;
        }
        return Math.pow(Math.E, -result / 2.0 / m_sigma / m_sigma);
    }

    /**
     * just 1 parameter (sigma).
     * @return 1
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getNumberParameters()
     */
    public int getNumberParameters() {
        return 1;
    }

    /**
     * get the name of the parameter.
     * @param index must be 0
     * @return the name of the parameter
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getParameterName(int)
     */
    public String getParameterName(final int index) {
        assert index == 0;
        return "sigma";
    }

    /**
     * are these parameters ok to be used?
     * @return validity
     * @param params the bias and power
     * @see org.knime.base.node.mine.svm.kernel.Kernel#areValid(double[])
     */
    public boolean areValid(final double[] params) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public double getDefaultParameter(final int index) {
        if (index == 0) {
            return 0.1;
        }
        return 0;
    }

    /**
     * set the given parameter.
     * @param index must be 0
     * @param value which value
     * @see org.knime.base.node.mine.svm.kernel.Kernel#setParameter(int, double)
     */
    public void setParameter(final int index, final double value) {
        assert index == 0;
        m_sigma = value;
    }

    /**
     * return the given parameter.
     * @param index must be 0
     * @return value of kernel parameter
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getParameter(int)
     */
    public double getParameter(final int index) {
        assert index == 0;
        return m_sigma;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KernelType getType() {
        return KernelType.RBF;
    }
}

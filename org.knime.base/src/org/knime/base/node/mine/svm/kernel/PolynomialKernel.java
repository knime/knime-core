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
package org.knime.base.node.mine.svm.kernel;

import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.DoubleVector;

/**
 * Polynomial kernel of the form:
 * (x * y + bias) ^ power.
 * @author Stefan Ciobaca, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class PolynomialKernel implements Kernel {
    /* bias. */
    private double m_bias;

    /* power. */
    private double m_power;

    /* gamma. */
    private double m_gamma;

    /** evaluate the kernel.
     * @param a first vector
     * @param b second vector
     * @return the result
     * */
    public double evaluate(final DoubleVector a, final DoubleVector b) {
        assert a.getNumberValues() == b.getNumberValues();
        double result = 0;
        for (int i = 0; i < a.getNumberValues(); ++i) {
            double oldresult = result;
            result = oldresult + a.getValue(i) * b.getValue(i);
        }
        result = m_gamma * result;
        result = result + m_bias;
        return Math.pow(result, m_power);
    }

    /**
     *
     * {@inheritDoc}
     */
    public double evaluate(final double[] a, final double[] b) {
        assert a.length == b.length;
        double result = 0;
        for (int i = 0; i < a.length; ++i) {
            double oldresult = result;
            result = oldresult + a[i] * b[i];
        }
        result = m_gamma * result;
        result = result + m_bias;
        return Math.pow(result, m_power);
    }

    /**
     * 2 parameters (bias and power).
     * @return 2
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getNumberParameters()
     */
    public int getNumberParameters() {
        return 3;
    }

    /**
     * get the names of the 2 parameters.
     * @param index first or second parameter?
     * @return the name of the parameter
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getParameterName(int)
     */
    public String getParameterName(final int index) {
        if (index == 0) {
            return "Bias";
        } else if (index == 1) {
            return "Power";
        } else if (index == 2) {
            return "Gamma";
        }
        assert false : "Parameter index out of range";
        return "";
    }

    /**
     * @return validity
     * @param params the bias and power
     * @see org.knime.base.node.mine.svm.kernel.Kernel#areValid(double[])
     */
    public boolean areValid(final double[] params) {
        assert params.length == 3;
        return true;
    }

    /**
     * @param index first or second
     * @param value which value
     * @see org.knime.base.node.mine.svm.kernel.Kernel#setParameter(int, double)
     */
    public void setParameter(final int index, final double value) {
        if (index == 0) {
            m_bias = value;
        } else if (index == 1) {
            m_power = value;
        } else if (index == 2) {
            m_gamma = value;
        } else {
            assert false : "Trying to set nonexistant parameter";
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDefaultParameter(final int index) {
       if (index == 1) {
           return 1.0;
       } else if (index == 2) {
           return 1.0;
       }
       return 0;
    }

    /**
     * return the given parameter.
     * @param index first or second?
     * @return value of kernel parameter
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getParameter(int)
     */
    public double getParameter(final int index) {
        if (index == 0) {
            return m_bias;
        } else if (index == 1) {
            return m_power;
        } else if (index == 2) {
            return m_gamma;
        } else {
            assert false : "Trying to set nonexistant parameter";
        }
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KernelType getType() {
        return KernelType.Polynomial;
    }
}

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
 * Hypertangent kernel.
 *
 * @author Stefan Ciobaca, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class HyperTangentKernel implements Kernel {

    /* first parameter. */
    private double m_kappa;

    /* second parameter. */
    private double m_delta;

    /** evaluate the kernel.
     * @param a first vector
     * @param b second vector
     * @return the result
     * */
    public double evaluate(final DoubleVector a, final DoubleVector b) {
        assert a.getNumberValues() == b.getNumberValues();
        double result = 0;
        for (int i = 0; i < a.getNumberValues(); ++i) {
            result += a.getValue(i) * b.getValue(i);
        }
        return Math.tanh(m_kappa * result + m_delta);
    }

    /**
     * {@inheritDoc}
     */
    public double evaluate(final double[] a, final double[] b) {
        assert a.length == b.length;
        double result = 0;
        for (int i = 0; i < a.length; ++i) {
            result += a[i] * b[i];
        }
        return Math.tanh(m_kappa * result + m_delta);
    }

    /**
     * 2 parameters (kappa and delta).
     * @return 2
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getNumberParameters()
     */
    public int getNumberParameters() {
        return 2;
    }

    /**
     * parameters are "kappa" and "delta".
     * @param index the parameter index.
     * @return parameter name
     * @see org.knime.base.node.mine.svm.kernel.Kernel#getParameterName(int)
     */
    public String getParameterName(final int index) {
        if (index == 0) {
            return "kappa";
        } else if (index == 1) {
            return "delta";
        }
        assert false : "Parameter index out of range";
        return "";
    }

    /**
     * check if the parameters are valid.
     * @param params which parameters
     * @return validity
     * @see org.knime.base.node.mine.svm.kernel.Kernel#areValid(double[])
     */
    public boolean areValid(final double[] params) {
        if (params.length == 2) {
            return true;
        }
        assert false : "Wrong number of parameters!";
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setParameter(final int index, final double value) {
        if (index == 0) {
            m_kappa = value;
        } else if (index == 1) {
            m_delta = value;
        } else {
            assert false : "Trying to set nonexistant parameter";
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDefaultParameter(final int index) {
        if (index == 0) {
            return .1;
        }
        return .5;
    }

    /**
     * {@inheritDoc}
     */
    public double getParameter(final int index) {
        if (index == 0) {
            return m_kappa;
        } else if (index == 1) {
            return m_delta;
        } else {
            assert false : "Trying to get nonexistant parameter";
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KernelType getType() {
        return KernelType.HyperTangent;
    }
}

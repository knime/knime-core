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
       if (index == 0) {
           return 1.0;
       } else if (index == 1) {
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

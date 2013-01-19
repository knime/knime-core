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

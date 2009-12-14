/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * The kernel interface to be used by any class that wants to implement
 * a kernel.
 *
 * @author Stefan Ciobaca, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public interface Kernel {

    /**
     * @return the {@link KernelType} of the Kernel
     */
    public abstract KernelType getType();

    /**
     * returns the value of the kernel given vectors a and b.
     * @param a the first vector
     * @param b the second vector
     * @return the result of the kernel in a and b
     * @see DoubleVector
     */
    public abstract double evaluate(DoubleVector a, DoubleVector b);

    /**
     * returns the value of the kernel given vectors a and b.
     * @param a the first vector
     * @param b the second vector
     * @return the result of the kernel in a and b
     */
    public abstract double evaluate(double[] a, double[] b);

    /**
     * Kernel parameters are constants which modify the
     * way the kernel works. For example, the degree p of the
     * polynomial kernel (x * y + 1) ^ p, is a parameter.
     * @return the number of kernel parameters
     */
    public abstract int getNumberParameters();

    /**
     * return the name of the parameter at the given index. index must
     * be between 0 and getNumberParameters() - 1.
     * @param index the parameter index.
     * @return the parameter's name (e.g. p or gamma)
     */
    public abstract String getParameterName(final int index);

    /**
     * test if the given combination of parameters is valid
     * for this kernel.
     * @param params the parameters. must have length = getNumberParameters()
     * @return whether parameter combination is valid or not.
     */
    public abstract boolean areValid(final double [] params);

    /**
     * sets the given parameter.
     * @param index the index of the parameter to set
     * @param value the value to put into the parameter
     */
    public abstract void setParameter(final int index, final double value);

    /**
     * returns the parameter at given index.
     * @return the value of the parameter
     * @param index the index of the parameter to get
     */
    public abstract double getParameter(final int index);

    /**
     * return the default parameter at the given index. index must
     * be between 0 and getNumberParameters() - 1.
     * @param index the parameter index.
     * @return the parameter's default value
     */
    public abstract double getDefaultParameter(final int index);
}

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

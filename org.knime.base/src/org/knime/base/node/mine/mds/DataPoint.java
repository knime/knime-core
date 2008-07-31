/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.03.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds;

/**
 * Represents a data point consisting of a values for each dimension. The values
 * of each dimension can be accessed by get and set methods. The dimension
 * of the data point has to be set when creating an instance by calling the
 * constructor and can not be changed afterwards.
 * To create instances of <code>DataPoint</code> see {@link MDSManager}.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class DataPoint {

    private double[] m_vector;
    
    /**
     * Creates a new instance of <code>DataPoint</code> with given dimension.
     * The dimension can not be changed afterwards.
     * 
     * @param dimension The dimension to set. Only positive values greater than
     * zero are allowed here.
     * @throws IllegalArgumentException If dimension is smaller or equals zero.
     */
    public DataPoint(final int dimension) throws IllegalArgumentException {
        if (dimension <= 0) {
            throw new IllegalArgumentException(
                    "Dimension must not be smaller 1!");
        }
        m_vector = new double[dimension];
    }
    
    /**
     * Returns the value of the <code>DataPoint</code> at the given index
     * or dimension respectively.
     * 
     * @param index The index to return the value of the data point at.
     * @return The value of the data point at the given index.
     * @throws IndexOutOfBoundsException If the index is less zero or greater 
     * than the dimension of the data point.
     */
    public double getElementAt(final int index) 
    throws IndexOutOfBoundsException {
        if (index >= m_vector.length || index < 0) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds!");
        }
        return m_vector[index];
    }
    
    /**
     * Sets the given value at the given index of the data point.
     * 
     * @param index The index to set the value at.
     * @param value The value to set at the specified index.
     * @throws IndexOutOfBoundsException If the given index is less than zero
     * or greater than the data points dimension. 
     */
    public void setElementAt(final int index, final double value) 
    throws IndexOutOfBoundsException {
        if (index >= m_vector.length || index < 0) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds!");
        }
        m_vector[index] = value;
    }
    
    /**
     * @return Returns the size or dimension, respectively of the data point.
     */
    public int size() {
        return m_vector.length;
    }
}

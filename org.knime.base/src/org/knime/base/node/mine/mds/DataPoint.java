/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

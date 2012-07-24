/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * History
 *   04.10.2006 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * This class describes an eigenvalue - eigenvector pair, comparable by absolute
 * of eigenvalue.
 */
public class EigenValue implements Comparable<EigenValue> {
    /** eigenvalue. */
    private final Double m_value;

    /** eigenvector. */
    private final Matrix m_vector;

    /** position. */
    private final int m_position;

    /**
     * Create pair.
     *
     * @param position original position in eigenvalue matrix
     * @param value eigenvalue
     * @param vector eigenvector
     */
    EigenValue(final int position, final double value, final Matrix vector) {
        if (vector == null) {
            throw new IllegalArgumentException("vector must be !=null");
        }
        m_position = position;
        m_value = value;
        m_vector = vector;
    }

    /**
     * @return original position in ev-matrix
     */
    public int getPosition() {

        return m_position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final EigenValue o) {
        return -Double.compare(Math.abs(m_value), Math.abs(o.m_value));
    }

    /**
     * @return the value
     */
    public Double getValue() {
        return m_value;
    }

    /**
     * @return the vector
     */
    public Matrix getVector() {
        return m_vector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" + m_position + "," + m_value + "]";
    }

    /**
     * extract the vector of eigenvalues.
     *
     * @param eig eigenvalue decomposition
     * @return vector of eigenvalues
     */
    public static double[] extractEVVector(final EigenvalueDecomposition eig) {
        final double[] evs = new double[eig.getD().getRowDimension()];
        for (int i = 0; i < evs.length; i++) {
            evs[i] = eig.getD().get(i, i);
        }
        return evs;
    }

    /**
     * Constructs a matrix consisting of the first <code>number</code>
     * eigenvectors of <code>m</code>, where first refers to the corresponding
     * eigenvalues sorted by absolute value.<br>
     * <b>Take care that the matrix is symmetric!</b>
     *
     * @param eigenVectors list of eigenvectors
     * @param eigenvalues list of eigenvalues
     *
     * @param number number of eigenvectors in return matrix
     * @return matrix with eigenvectors columnwise
     */
    public static Matrix getSortedEigenVectors(final double[][] eigenVectors,
            final double[] eigenvalues, final int number) {

        final List<EigenValue> list = createSortedList(eigenVectors,
				eigenvalues);
        final double[][] rm = new double[eigenvalues.length][number];
        for (int i = 0; i < number; i++) {
            final double[][] t = list.get(i).m_vector.getArray();
            for (int j = 0; j < t.length; j++) {
                rm[j][i] = t[j][0];
            }
        }
        return new Matrix(rm);
    }
    /**
     * create list of {@link EigenValue}s sorted by absolute value
     * @param eigenVectors matrix of eigenvector (in columns)
     * @param eigenvalues eigenvalues, same order as columns of eigenVectors
     * @return sorted list of {@link EigenValue}s
     */
	public static List<EigenValue> createSortedList(
			final double[][] eigenVectors, final double[] eigenvalues) {
		final int[] rowindices = new int[eigenvalues.length];
        final Matrix v = new Matrix(eigenVectors);
        for (int i = 0; i < rowindices.length; i++) {
            rowindices[i] = i;
        }
        final LinkedList<EigenValue> list = new LinkedList<EigenValue>();
        for (int i = 0; i < eigenvalues.length; i++) {

            list.add(new EigenValue(i, eigenvalues[i], v.getMatrix(rowindices,
                    new int[]{i})));
        }
        Collections.sort(list);
		return list;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_value == null) ? 0 : m_value.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EigenValue other = (EigenValue)obj;
        if (m_value == null) {
            if (other.m_value != null) {
                return false;
            }
        } else if (!m_value.equals(other.m_value)) {
            return false;
        }
        return true;
    }
}

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
 * History
 *   04.10.2006 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.util.Collections;
import java.util.LinkedList;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * This class describes an eigenvalue, eigenvector pair, comparable by absolute
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
    public int compareTo(final EigenValue o) {

        return -new Double(Math.abs(m_value)).compareTo(Math.abs(o.m_value));
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
        final double[][] rm = new double[eigenvalues.length][number];
        for (int i = 0; i < number; i++) {
            final double[][] t = list.get(i).m_vector.getArray();
            for (int j = 0; j < t.length; j++) {
                rm[j][i] = t[j][0];
            }
        }
        return new Matrix(rm);
    }

}
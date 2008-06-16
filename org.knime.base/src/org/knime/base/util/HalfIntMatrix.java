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
 *   04.06.2007 (thor): created
 */
/* Created on 06.02.2007 16:32:34 by thor
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * This stores half a matrix of ints efficiently in just one array. The access
 * function {@link #get(int, int)} works symmetrically. Upon creating the matrix
 * you can choose if place for the diagonal should be reserved or not.
 * 
 * It is also possible to save the contents of the matrix into a node settings
 * object and load it again from there afterwards.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class HalfIntMatrix {
    private final boolean m_withDiagonal;

    private final int[] m_matrix;

    /**
     * Creates a new half-matrix of ints.
     * 
     * @param rows the number of rows (and columns) in the matrix
     * @param withDiagonal <code>true</code> if the diagonal should be stored
     *            too, <code>false</code> otherwise
     */
    public HalfIntMatrix(final int rows, final boolean withDiagonal) {
        m_withDiagonal = withDiagonal;
        if (withDiagonal) {
            m_matrix = new int[(rows * rows + rows) / 2];
        } else {
            m_matrix = new int[(rows * rows - rows) / 2];
        }
    }

    /**
     * Loads a half int matrix from the given node settings object.
     * 
     * @param config a node settings object
     * @throws InvalidSettingsException if the passed node settings do not
     *             contain valid settings
     */
    public HalfIntMatrix(final ConfigRO config) throws InvalidSettingsException {
        m_withDiagonal = config.getBoolean("withDiagonal");
        m_matrix = config.getIntArray("array");
    }

    /**
     * Sets a value in the matrix. This function works symmetrically, i.e.
     * <code>set(i, j, 1)</code> is the same as <code>set(j, i, 1)</code>.
     * 
     * @param row the value's row
     * @param col the value's column
     * @param value the value
     */
    public void set(final int row, final int col, final int value) {
        if (!m_withDiagonal && row == col) {
            throw new IllegalArgumentException("Can't set value in diagonal "
                    + "of the matrix (no space reserved)");
        }
        if (row > col) {
            if (m_withDiagonal) {
                m_matrix[row * (row + 1) / 2 + col] = value;
            } else {
                m_matrix[row * (row - 1) / 2 + col] = value;
            }
        } else {
            if (m_withDiagonal) {
                m_matrix[col * (col + 1) / 2 + row] = value;
            } else {
                m_matrix[col * (col - 1) / 2 + row] = value;
            }
        }
    }

    /**
     * Returns a value in the matrix. This function works symmetrically, i.e.
     * <code>get(i, j)</code> is the same as <code>get(j, i)</code>.
     * 
     * @param row the value's row
     * @param col the value's column
     * @return the value
     */
    public int get(final int row, final int col) {
        if (!m_withDiagonal && row == col) {
            throw new IllegalArgumentException("Can't read value in diagonal "
                    + "of the matrix (not saved)");
        }
        if (row > col) {
            if (m_withDiagonal) {
                return m_matrix[row * (row + 1) / 2 + col];
            } else {
                return m_matrix[row * (row - 1) / 2 + col];
            }
        } else {
            if (m_withDiagonal) {
                return m_matrix[col * (col + 1) / 2 + row];
            } else {
                return m_matrix[col * (col - 1) / 2 + row];
            }
        }
    }

    /**
     * Fills the matrix with the given value.
     * 
     * @param value any value
     */
    public void fill(final int value) {
        for (int i = 0; i < m_matrix.length; i++) {
            m_matrix[i] = value;
        }
    }

    /**
     * Saves the matrix directly into the passed node settings object.
     * 
     * @param config a node settings object.
     */
    public void save(final ConfigWO config) {
        config.addBoolean("withDiagonal", m_withDiagonal);
        config.addIntArray("array", m_matrix);
    }

    /**
     * Returns if the half matrix also stores the diagonal or not.
     * 
     * @return <code>true</code> if the diagonal is stored, <code>false</code>
     *         otherwise
     */
    public boolean storesDiagonal() {
        return m_withDiagonal;
    }

    /**
     * Returns the number of rows the half matrix has.
     * 
     * @return the number of rows
     */
    public int getRowCount() {
        if (m_withDiagonal) {
            return (-1 + (int)Math.sqrt(1 + 8 * m_matrix.length)) / 2;
        } else {
            return (1 + (int)Math.sqrt(1 + 8 * m_matrix.length)) / 2;
        }
    }

    /**
     * Permutes the matrix based on the permutation given in the parameter.
     * 
     * @param permutation an array in which at position <i>i</i> is the index
     *            where the old row <i>i</i> is moved to, i.e.
     *            <code>{ 2, 3, 0, 1 }</code> means that rows (and columns) 0
     *            and 2 are swapped and rows 1 and 3
     */
    public void permute(final int[] permutation) {
        final int rc = getRowCount();
        if (permutation.length != rc) {
            throw new IllegalArgumentException(
                    "Size of permutation array does not match the matrix row "
                            + "count: " + permutation.length + " vs. " + rc);
        }

        HalfIntMatrix temp = new HalfIntMatrix(rc, m_withDiagonal);
        for (int i = 0; i < rc; i++) {
            final int newI = permutation[i];

            for (int j = 0; j <= i; j++) {
                final int newJ = permutation[j];

                if (m_withDiagonal || (j != i)) {
                    temp.set(newI, newJ, get(i, j));
                }
            }
        }

        System.arraycopy(temp.m_matrix, 0, m_matrix, 0, m_matrix.length);
    }
}

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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.06.2007 (thor): created
 */
/* Created on 06.02.2007 16:32:34 by thor
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
     * Adds a value in the matrix. See also {@link #set(int, int, int)} for
     * details on the arguments.
     *
     * @param row the value's row
     * @param col the value's column
     * @param value the value to add to the previous value
     * @since 2.6
     */
    public void add(final int row, final int col, final int value) {
        if (!m_withDiagonal && row == col) {
            throw new IllegalArgumentException("Can't set value in diagonal "
                    + "of the matrix (no space reserved)");
        }
        if (row > col) {
            if (m_withDiagonal) {
                m_matrix[row * (row + 1) / 2 + col] += value;
            } else {
                m_matrix[row * (row - 1) / 2 + col] += value;
            }
        } else {
            if (m_withDiagonal) {
                m_matrix[col * (col + 1) / 2 + row] += value;
            } else {
                m_matrix[col * (col - 1) / 2 + row] += value;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        int maxDigits = 0;
        for (int i = 0; i < m_matrix.length; i++) {
            int m = m_matrix[i];
            while (m > 0) {
                m /= 10;
            }
            maxDigits = m > maxDigits ? m : maxDigits;
        }

        String zeros = "################".substring(maxDigits);
        NumberFormat nf = new DecimalFormat(" " + zeros + ";-" + zeros);

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < getRowCount(); i++) {
            for (int k = 0; k < i; k++) {
                buf.append(nf.format(get(i, k))).append(' ');
            }
            if (m_withDiagonal) {
                buf.append(nf.format(get(i, i))).append(' ');
            }
            buf.append('\n');
        }

        return buf.toString();
    }
}

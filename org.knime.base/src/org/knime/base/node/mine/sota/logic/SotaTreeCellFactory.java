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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.core.data.FuzzyIntervalValue;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaTreeCellFactory {
    private static SotaTreeCellFactory cellFactory = null;

    private int m_dimension;

    /**
     * Initialises the SotaTreeCellFactory instance. Since the
     * SotaTreeCellFactory constructor is private an instance can just be
     * accessed via this method.
     * 
     * @param dimension dimension of the cells data vector
     * @return the instance of SotaTreeCellFactory
     */
    public static SotaTreeCellFactory initCellFactory(final int dimension) {
        if (cellFactory == null) {
            cellFactory = new SotaTreeCellFactory(dimension);
        } else if (cellFactory.getDimension() != dimension) {
            cellFactory = new SotaTreeCellFactory(dimension);
        }
        return cellFactory;
    }

    /**
     * Initialises the SotaTreeCellFactory instance. Since the
     * SotaTreeCellFactory constructor is private an instance can just be
     * accessed via this method. If no cellFactory instance is created yet,
     * <code>null</code> is returned.
     * 
     * @return the instance of SotaTreeCellFactory or <code>null</code>
     */
    public static SotaTreeCellFactory initCellFactory() {
        if (cellFactory == null) {
            return null;
        }
        return cellFactory;
    }

    /**
     * Creates new instance of SotaTreeCellFactory with given dimension.
     * 
     * @param dim dimension of the cells data vector
     */
    private SotaTreeCellFactory(final int dim) {
        this.m_dimension = dim;
    }

    /**
     * Returns the dimension of the Cells to create.
     * 
     * @return The dimension of the Cells to create.
     */
    public int getDimension() {
        return m_dimension;
    }

    /**
     * Creates new instance of Cell with the factorys dimension and returns it.
     * 
     * @return the created cell
     */
    public SotaTreeCell createCell() {
        return new SotaTreeCell(m_dimension, true);
    }

    /**
     * Creates new instance of Cell with the factorys dimension and the given
     * level and returns it.
     * 
     * @param level level of hierarchy to set
     * @return the created cell
     */
    public SotaTreeCell createCell(final int level) {
        return new SotaTreeCell(m_dimension, level, true);
    }

    /**
     * Creates new instance of Cell with given data and level and returns it. If
     * length of the data array and the factorys dimension does not fit
     * <code>null</code> is returned.
     * 
     * @param data data to set to the Cell
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createCell(final SotaCell[] data, final int level) {
        if (data.length == m_dimension) {
            return new SotaTreeCell(data, level, true);
        }
        return null;
    }

    /**
     * Creates new instance of Cell with given data and level and returns it. If
     * length of the data array and the factorys dimension does not fit
     * <code>null</code> is returned.
     * 
     * @param data data to set to the Cell
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createCell(final double[] data, final int level) {
        if (data.length == m_dimension) {
            SotaCell[] dat = new SotaCell[data.length];
            for (int i = 0; i < data.length; i++) {
                dat[i] = new SotaDoubleCell(data[i]);
            }
            return new SotaTreeCell(dat, level, true);
        }
        return null;
    }

    /**
     * Creates new insatnce of Cell with given FuzzyIntervalValue data and level
     * and returns it. If length of the data array and the factorys dimension
     * does not fit <code>null</code> is returned.
     * 
     * @param data FuzzyIntervalValue data to set to the Cell
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createCell(final FuzzyIntervalValue[] data,
            final int level) {
        if (data.length == m_dimension) {
            SotaCell[] dat = new SotaCell[data.length];
            for (int i = 0; i < data.length; i++) {
                dat[i] = new SotaFuzzyCell(data[i].getMinSupport(), data[i]
                        .getMinCore(), data[i].getMaxCore(), data[i]
                        .getMaxSupport());
            }
            return new SotaTreeCell(dat, level, true);
        }
        return null;
    }

    /**
     * Creates new insatnce of Cell with given min, max Core and Support data
     * and level and returns it. If length of the arrays and the factorys
     * dimension does not fit <code>null</code> is returned.
     * 
     * @param minSupp array with minimal support values
     * @param minCore array with minimal core values
     * @param maxCore array with maxmal core values
     * @param maxSupp array with maxmal support values
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createCell(final double[] minSupp,
            final double[] minCore, final double[] maxCore,
            final double[] maxSupp, final int level) {
        if (minSupp.length == m_dimension && minCore.length == m_dimension
                && maxCore.length == m_dimension
                && maxSupp.length == m_dimension) {

            SotaCell[] dat = new SotaCell[m_dimension];
            for (int i = 0; i < m_dimension; i++) {
                dat[i] = new SotaFuzzyCell(minSupp[i], minCore[i], maxCore[i],
                        maxSupp[i]);
            }
            return new SotaTreeCell(dat, level, true);
        }
        return null;
    }

    /**
     * Creates new instance of Cell with the factorys dimension and returns it.
     * The isCell flag is set to <code>false</code>, since a Node shell be
     * created.
     * 
     * @return the created Cell
     */
    public SotaTreeCell createNode() {
        return new SotaTreeCell(m_dimension, false);
    }

    /**
     * Creates new instance of Cell with the factorys dimension and given level
     * and returns it. The isCell flag is set to false, since a Node shell be
     * created.
     * 
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createNode(final int level) {
        return new SotaTreeCell(m_dimension, level, false);
    }

    /**
     * Creates new instance of Cell with given data and level and returns it. If
     * length of the data array and the factorys dimension does not fit null is
     * returned. The isCell flag is set to false, since a Node shell be created.
     * 
     * @param data data to set to the Cell
     * @param level level of hierarchy to set
     * @return the created Cell
     */
    public SotaTreeCell createNode(final SotaCell[] data, final int level) {
        if (data.length == m_dimension) {
            return new SotaTreeCell(data, level, false);
        }
        return null;
    }
}

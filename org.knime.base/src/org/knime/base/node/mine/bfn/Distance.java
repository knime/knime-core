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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

/**
 * Computes the Euclidean distance between two vectors.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class Distance {
    
    /* 
     * TODO introduce more general class and distance package
     */
    
    /**
     * Hidden, empty constructor.
     */
    private Distance() {
        // empty
    }

    /**
     * Returns a new <code>Distance</code> object.
     * 
     * @return a distance object
     */
    public static final Distance getInstance() {
        return new Distance();
    }

    /**
     * Computes the Euclidean distance between two normalized rows.
     * 
     * @param x a row
     * @param y another row
     * @return Euclidean distance between <code>x</code> and <code>y</code>.
     * @throws NullPointerException if one of the given rows is
     *             <code>null</code>
     */
    public final double compute(final DataRow x, final DataRow y) {
        assert (x.getNumCells() == y.getNumCells());
        final int length = x.getNumCells();
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            DataCell xCell = x.getCell(i);
            DataCell yCell = y.getCell(i);
            if (xCell.isMissing() || yCell.isMissing()) {
                continue;
            }
            double xd = ((DoubleValue)xCell).getDoubleValue();
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = ((DoubleValue)yCell).getDoubleValue();
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Computes the Euclidean distance between two normalized vectors.
     * 
     * @param x an array of double cells
     * @param y a row
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>
     * @throws NullPointerException if one of the given rows is
     *             <code>null</code>
     */
    public final double compute(final DoubleValue[] x, final DataRow y) {
        assert (x.length == y.getNumCells());
        final int length = x.length;
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            DataCell yCell = y.getCell(i);
            if (yCell.isMissing()) {
                continue;
            }
            double xd = x[i].getDoubleValue();
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = ((DoubleValue)yCell).getDoubleValue();
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Computes the Euclidean distance between two normalized vectors.
     * 
     * @param x an array of doubles
     * @param y a row
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>
     * @throws NullPointerException if one of the given rows is
     *             <code>null</code>
     */
    public final double compute(final double[] x, final DataRow y) {
        assert (x.length == y.getNumCells());
        final int length = x.length;
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            DataCell yCell = y.getCell(i);
            if (yCell.isMissing()) {
                continue;
            }
            double xd = x[i];
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = ((DoubleValue)yCell).getDoubleValue();
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Computes the Euclidean distance between two normalized vectors.
     * 
     * @param x an array of doubles
     * @param y an array of doubles
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>.
     * @throws NullPointerException if one of the given arrays is
     *             <code>null</code>
     */
    public final double compute(final double[] x, final double[] y) {
        assert (x.length == y.length);
        final int length = x.length;
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            double xd = x[i];
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = y[i];
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Computes the Euclidean distance between two normalized vectors.
     * 
     * @param x an array of doubles
     * @param y an array of doubles
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>.
     * @throws NullPointerException if one of the given arrays is
     *             <code>null</code>
     */
    public final double computeSquaredEuclidean(final double[] x, 
            final double[] y) {
        assert (x.length == y.length);
        final int length = x.length;
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            double xd = x[i];
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = y[i];
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return result;
    }    
    
    
    
    /**
     * Computes the Euclidean distance between two normalized vectors.
     * 
     * @param x an array of doubles
     * @param y an array of DoubleValues
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>
     * @throws NullPointerException if one of the given rows is
     *             <code>null</code>
     */
    public final double compute(final double[] x, final DoubleValue[] y) {
        assert (x.length == y.length);
        final int length = x.length;
        double result = 0.0;
        for (int i = 0; i < length; i++) {
            double xd = x[i];
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = y[i].getDoubleValue();
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Computes the Euclidean distance between the two normalized arrays.
     * 
     * @param x an array
     * @param y another array
     * @return the Euclidean distance between <code>x</code> and
     *         <code>y</code>
     * @throws NullPointerException if one of the given arrays is
     *             <code>null</code>
     */
    public final double compute(final DoubleValue[] x, final DoubleValue[] y) {
        assert (x.length == y.length);
        final int length = x.length;
        double result = 0;
        for (int i = 0; i < length; i++) {
            double xd = x[i].getDoubleValue();
            if (Double.isNaN(xd)) {
                continue;
            }
            double yd = y[i].getDoubleValue();
            if (Double.isNaN(yd)) {
                continue;
            }
            double diff = xd - yd;
            result += (diff * diff);
        }
        assert result >= 0.0 : "result=" + result;
        return Math.sqrt(result);
    }

    /**
     * Returns string representation <tt>Euclidean</tt>.
     * 
     * @return <tt>Euclidean</tt>
     */
    @Override
    public String toString() {
        return "Euclidean";
    }
}

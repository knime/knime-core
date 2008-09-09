/*
 * --------------------------------------------------------------------- *
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

/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   29.07.2005 (bernd): created
 */
package org.knime.base.data.normalize;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;


/**
 * RowIterator that wraps another iterator and performs an affine
 * transformation, i.e. y = a*x + b where a and be b are parameters, x the input
 * value and y the transformed output.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AffineTransRowIterator extends RowIterator {
    private final RowIterator m_it;

    private final double[] m_scales;

    private final double[] m_translations;

    /**
     * Convenience constructor for package scope classes that make sure that all
     * sanity checks are met.
     * 
     * @param it the row iterator to wrap
     * @param scales the scales for each dimension, make sure that it has the
     *            same length as a row has cells, non double columns MUST have
     *            {@link Double#NaN} as value in this argument
     * @param translations the translation operator, similar to
     *            <code>scales</code>
     */
    AffineTransRowIterator(final RowIterator it, final double[] scales,
            final double[] translations) {
        m_it = it;
        m_scales = scales;
        m_translations = translations;
    }

    /**
     * Creates new row iterator given "some" parameters.
     * 
     * @param it the row iterator to wrap
     * @param spec the spec from the iterators table (need for type check)
     * @param names the names of the column to scale
     * @param scales the scale parameters (same order as <code>names</code>)
     * @param translations the translation parameters
     * @throws NullPointerException if any argument is <code>null</code>
     * @throws IllegalArgumentException if the arrays don't have the same
     *             length, the names are not contained in the spec, the double
     *             arrays contain NaN, the target columns are not
     *             {@link DoubleValue} compatible
     */
    public AffineTransRowIterator(final RowIterator it,
            final DataTableSpec spec, final String[] names,
            final double[] scales, final double[] translations) {
        if (it == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (names.length != scales.length
                || names.length != translations.length) {
            throw new IllegalArgumentException("Lengths must match: "
                    + names.length + " vs. " + scales.length + " vs. "
                    + translations.length);
        }
        for (int i = 0; i < scales.length; i++) {
            if (Double.isNaN(scales[i]) || Double.isNaN(translations[i])) {
                throw new IllegalArgumentException("Cannot transform with NaN");
            }
        }
        double[] myScales = new double[spec.getNumColumns()];
        double[] myTrans = new double[spec.getNumColumns()];
        Arrays.fill(myScales, Double.NaN);
        Arrays.fill(myTrans, Double.NaN);
        for (int i = 0; i < names.length; i++) {
            int index = spec.findColumnIndex(names[i]);
            if (index < 0) {
                throw new IllegalArgumentException("No such column: "
                        + names[i]);
            }
            DataType type = spec.getColumnSpec(index).getType();
            // do we need to support IntValue also?
            if (!type.isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException("Not supported: " + type);
            }
            myScales[index] = scales[i];
            myTrans[index] = translations[i];
        }
        m_it = it;
        m_scales = myScales;
        m_translations = myTrans;
    }

    /**
     * @see org.knime.core.data.RowIterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return m_it.hasNext();
    }

    /**
     * @see org.knime.core.data.RowIterator#next()
     */
    @Override
    public DataRow next() {
        final DataRow in = m_it.next();
        final DataCell[] cells = new DataCell[in.getNumCells()];
        for (int i = 0; i < cells.length; i++) {
            final DataCell oldCell = in.getCell(i);
            if (oldCell.isMissing() || Double.isNaN(m_scales[i])) {
                cells[i] = oldCell;
            } else {
                double oldDouble = ((DoubleValue)oldCell).getDoubleValue();
                double newDouble = m_scales[i] * oldDouble + m_translations[i];
                cells[i] = new DoubleCell(newDouble);
            }
        }
        return new DefaultRow(in.getKey(), cells);
    }
}

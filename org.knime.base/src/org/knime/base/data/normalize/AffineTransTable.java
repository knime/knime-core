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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.data.normalize;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;


/**
 * Table that performs an affine transformation, i.e. y = a*x + b where a and be
 * b are parameters, x the input value and y the transformed output.
 * 
 * <p>
 * The transformation is only applied to a given set of ({@link DoubleValue} -
 * compatible) columns. Other columns are copied.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AffineTransTable implements DataTable {
    private final DataTable m_table;

    private final DataTableSpec m_spec;

    private final double[] m_scales;

    private final double[] m_translations;

    /**
     * Convenience constructor for package scope classes that make sure that all
     * sanity checks are met.
     * 
     * @param table the table to wrap
     * @param scales the scales for each dimension, make sure that it has the
     *            same length as a row has cells, non double columns MUST have
     *            {@link Double#NaN} as value in this argument
     * @param translations the translation operator, similar to
     *            <code>scales</code>
     */
    AffineTransTable(final DataTable table, final double[] scales,
            final double[] translations) {
        m_table = table;
        m_scales = scales;
        m_translations = translations;
        m_spec = generateNewSpec();
    }

    /**
     * Creates new table given "some" parameters.
     * 
     * @param table the Table to wrap
     * @param names the names of the column to scale
     * @param scales the scale parameters (same order as <code>names</code>)
     * @param translations the translation parameters
     * @throws NullPointerException if any argument is <code>null</code>
     * @throws IllegalArgumentException if the arrays don't have the same
     *             length, the names are not contained in the spec, the double
     *             arrays contain NaN, the target columns are not
     *             {@link DoubleValue} compatible
     */
    public AffineTransTable(final DataTable table, final String[] names,
            final double[] scales, final double[] translations) {
        final DataTableSpec spec = table.getDataTableSpec();
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
        m_table = table;
        m_scales = myScales;
        m_translations = myTrans;
        m_spec = generateNewSpec();
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new AffineTransRowIterator(m_table.iterator(), m_scales,
                m_translations);
    }

    /**
     * Creates a new DataTableSpec. The target column's type is set to
     * DoubleType, the domain is adjusted.
     * 
     * @return DataTableSpec for this table
     */
    private DataTableSpec generateNewSpec() {
        DataTableSpec tabSpec = m_table.getDataTableSpec();
        DataColumnSpec[] specs = new DataColumnSpec[tabSpec.getNumColumns()];
        for (int i = 0; i < m_scales.length; i++) {
            DataColumnSpec colSpec = tabSpec.getColumnSpec(i);
            DataColumnDomain colDomain = tabSpec.getColumnSpec(i).getDomain();
            if (Double.isNaN(m_scales[i])) {
                specs[i] = colSpec;
            } else {
                // determine domain
                DataCell up = null;
                DataCell oldUp = colDomain.getUpperBound();
                if (oldUp != null && !oldUp.isMissing()) {
                    double oldVal = ((DoubleValue)oldUp).getDoubleValue();
                    double newVal = m_scales[i] * oldVal + m_translations[i];
                    up = new DoubleCell(newVal);
                }
                DataCell low = null;
                DataCell oldLow = colDomain.getLowerBound();
                if (oldLow != null && !oldLow.isMissing()) {
                    double oldVal = ((DoubleValue)oldLow).getDoubleValue();
                    double newVal = m_scales[i] * oldVal + m_translations[i];
                    low = new DoubleCell(newVal);
                }
                DataColumnDomain dom = new DataColumnDomainCreator(low, up)
                        .createDomain();
                DataType type = DoubleCell.TYPE;
                DataColumnSpecCreator c = new DataColumnSpecCreator(colSpec);
                // IntType must be converted to DoubleType!
                c.setType(type);
                c.setDomain(dom);
                specs[i] = c.createSpec();
            }
        }
        return new DataTableSpec(specs);
    }
}

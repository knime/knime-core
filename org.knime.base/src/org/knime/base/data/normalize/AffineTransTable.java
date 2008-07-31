/* 
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


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
    
    /**
     * A very small number.
     */
    public static final double VERY_SMALL = 1E-10;
    
    private final DataTable m_table;

    private final DataTableSpec m_spec;

    private final double[] m_scales;

    private final double[] m_translations;
    
    private final String[] m_includeNames;
    
    private final double[] m_min;
    
    private final double[] m_max;
    
    private String m_errormessage;

    /**
     * Creates new table given the following parameters.
     * 
     * @param table the Table to wrap
     * @param names the names of the column to scale
     * @param scales the scale parameters (same order as <code>names</code>)
     * @param translations the translation parameters
     * @param min the minimum values (for sanity checks). If the normalized
     *            value is slightly off the desired minimum value because of
     *            rounding errors, it is set to this bounding value. Can be
     *            {@link Double#NaN}, then no checks are performed.
     * @param max the maximum values (for sanity checks). If the normalized
     *            value is slightly off the desired maximum value because of
     *            rounding errors, it is set to this bounding value. Can be
     *            {@link Double#NaN}, then no checks are performed.
     * @throws NullPointerException if any argument is <code>null</code>
     * @throws IllegalArgumentException if the arrays don't have the same
     *             length, the names are not contained in the spec, the double
     *             arrays of scales and translations contain NaN, the target
     *             columns are not {@link DoubleValue} compatible
     */
    public AffineTransTable(final DataTable table, final String[] names,
            final double[] scales, final double[] translations,
            final double[] min, final double[] max) {
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
        double[] myMin = new double[spec.getNumColumns()];
        double[] myMax = new double[spec.getNumColumns()];
        String[] myIncludes = new String[spec.getNumColumns()];
        Arrays.fill(myScales, Double.NaN);
        Arrays.fill(myTrans, Double.NaN);
        Arrays.fill(myMin, Double.NaN);
        Arrays.fill(myMax, Double.NaN);
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
            myMin[index] = min[i];
            myMax[index] = max[i];
            myIncludes[index] = names[i];
        }
        m_includeNames = myIncludes;
        m_table = table;
        m_scales = myScales;
        m_translations = myTrans;
        m_min = myMin;
        m_max = myMax;
        m_spec = generateNewSpec(table.getDataTableSpec(), m_scales,
                        m_translations, m_min, m_max);
        m_errormessage = null;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new AffineTransRowIterator(m_table, this);
    }

    /**
     * Creates a new DataTableSpec. The target column's type is set to
     * DoubleType, the domain is adjusted.
     */
    private static DataTableSpec generateNewSpec(
            final DataTableSpec tabSpec, 
            final double[] scales,
            final double[] translations,
            final double[] newmin,
            final double[] newmax) {
        DataColumnSpec[] specs = new DataColumnSpec[tabSpec.getNumColumns()];
        for (int i = 0; i < scales.length; i++) {
            DataColumnSpec colSpec = tabSpec.getColumnSpec(i);
            DataColumnDomain colDomain = tabSpec.getColumnSpec(i).getDomain();
            if (Double.isNaN(scales[i])) {
                specs[i] = colSpec;
            } else {
                // determine domain
                double interval = newmax[i] - newmin[i];
                DataCell up = null;
                DataCell oldUp = colDomain.getUpperBound();
                if (oldUp != null && !oldUp.isMissing()) {
                    double oldVal = ((DoubleValue)oldUp).getDoubleValue();
                    double newVal = scales[i] * oldVal + translations[i];
                    if (!Double.isNaN(newmax[i])) {
                        if (newVal > newmax[i]
                                && ((newVal - newmax[i]) 
                                        / interval) < VERY_SMALL) {
                            newVal = newmax[i];
                        }
                    }
                    up = new DoubleCell(newVal);
                }
                DataCell low = null;
                DataCell oldLow = colDomain.getLowerBound();
                if (oldLow != null && !oldLow.isMissing()) {
                    double oldVal = ((DoubleValue)oldLow).getDoubleValue();
                    double newVal = scales[i] * oldVal + translations[i];
                    if (!Double.isNaN(newmin[i])) {
                        if (newVal < newmin[i]
                                && ((newmin[i] - newVal) 
                                        / interval) < VERY_SMALL) {
                            newVal = newmin[i];
                        }
                    }
                    low = new DoubleCell(newVal);
                }
                DataColumnDomain dom =
                        new DataColumnDomainCreator(low, up).createDomain();
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

    private static final String CFG_NAME = "column_name";

    private static final String CFG_SCALE = "column_scale";

    private static final String CFG_TRANSLATE = "column_translate";

    private static final String CFG_MIN = "column_min";

    private static final String CFG_MAX = "column_max";

    /**
     * Saves internals to the argument settings object. This object is supposed
     * to be the only one writing to this (sub)setting object.
     * 
     * @param settings To write to.
     */
    public void save(final ModelContentWO settings) {
        for (int i = 0; i < m_includeNames.length; i++) {
            String name = m_includeNames[i];
            double scale = m_scales[i];
            double transl = m_translations[i];
            double min = m_min[i];
            double max = m_max[i];
            if (name == null) {
                assert Double.isNaN(scale) && Double.isNaN(transl);
                continue;
            }
            ModelContentWO sub = settings.addModelContent(name);
            sub.addString(CFG_NAME, name);
            sub.addDouble(CFG_SCALE, scale);
            sub.addDouble(CFG_TRANSLATE, transl);
            sub.addDouble(CFG_MIN, min);
            sub.addDouble(CFG_MAX, max);
        }
    }
    
    /** Reads the meta information from the settings object and constructs
     * a AffineTransTable based on this information and the given DataTable.
     * @param table The table to which the normalization is applied.
     * @param settings The normalization information.
     * @return A new table wrapping <code>table</code> but normalized according
     * to <code>settings</code>.
     * @throws InvalidSettingsException If the settings are incomplete 
     * or cannot be applied to spec.
     */
    public static AffineTransTable load(final DataTable table,
            final ModelContentRO settings) throws InvalidSettingsException {
        /*
         * Note: this is not very nice here. I copied code from the createSpec
         * method. A better way would be to have a object containing the scales,
         * translations, etc. information.
         */
        int colCount = settings.keySet().size();
        double[] scales = new double[colCount];
        double[] translations = new double[colCount];
        double[] mins = new double[colCount];
        double[] maxs = new double[colCount];
        Arrays.fill(scales, Double.NaN);
        Arrays.fill(translations, Double.NaN);
        Arrays.fill(mins, Double.NaN);
        Arrays.fill(maxs, Double.NaN);
        String[] names = new String[colCount];
        int index = 0;
        for (String key : settings.keySet()) {
            ModelContentRO sub = settings.getModelContent(key);
            String name = sub.getString(CFG_NAME);
            double scale = sub.getDouble(CFG_SCALE);
            double trans = sub.getDouble(CFG_TRANSLATE);
            double min = sub.getDouble(CFG_MIN);
            mins[index] = min;
            double max = sub.getDouble(CFG_MAX);
            maxs[index] = max;
            scales[index] = scale;
            translations[index] = trans;
            names[index] = name;
            index++;
        }
        return new AffineTransTable(table, names, scales, translations, mins,
                maxs);
    }
    
    /** Reads the meta information from the settings object and constructs
     * the DataTableSpec, which would be the outcome when a table complying with
     * <code>spec</code> were fet to the load method.
     * @param spec The original input spec.
     * @param settings The normalization information.
     * @return The DataTableSpec of the normalized table.
     * @throws InvalidSettingsException If the settings are incomplete 
     * or cannot be applied to spec.
     */
    public static DataTableSpec createSpec(final DataTableSpec spec, 
            final ModelContentRO settings) throws InvalidSettingsException {
        double[] scales = new double[spec.getNumColumns()];
        double[] translations = new double[spec.getNumColumns()];
        double[] mins = new double[spec.getNumColumns()];
        double[] maxs = new double[spec.getNumColumns()];
        Arrays.fill(scales, Double.NaN);
        Arrays.fill(translations, Double.NaN);
        Arrays.fill(mins, Double.NaN);
        Arrays.fill(maxs, Double.NaN);
        for (String key : settings.keySet()) {
            ModelContentRO sub = settings.getModelContent(key);
            String name = sub.getString(CFG_NAME);
            int index = spec.findColumnIndex(name);
            if (index < 0) {
                throw new InvalidSettingsException("Can't apply setting to " 
                        + "input table spec, no such column: " + name);
            }
            DataColumnSpec old = spec.getColumnSpec(index);
            if (!old.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Can't apply setting to " 
                        + "input table spec, column '" + name 
                        + "' is not double compatible.");
            }
            double scale = sub.getDouble(CFG_SCALE);
            double trans = sub.getDouble(CFG_TRANSLATE);
            double min = sub.getDouble(CFG_MIN);
            double max = sub.getDouble(CFG_MAX);
            scales[index] = scale;
            translations[index] = trans;
            mins[index] = min;
            maxs[index] = max;
        }
        return generateNewSpec(spec, scales, translations, mins, maxs);
    }
    
    /**
     * @return maximum values for each column.
     */
    double[] getMax() {
        return m_max;
    }

    /**
     * @return minimum values for each column.
     */
    double[] getMin() {
        return m_min;
    }

    /**
     * @return scales for each column.
     */
    double[] getScales() {
        return m_scales;
    }

    /**
     * @return translations for each column.
     */
    double[] getTranslations() {
        return m_translations;
    }
    
    /**
     * Sets an error message, if something went wrong during normalization. 
     * @param message the message to set.
     */
    void setErrorMessage(final String message) {
        if (m_errormessage == null) {
            m_errormessage = message;
        }
    }
    
    /**
     * @return error message if something went wrong, <code>null</code>
     * otherwise.
     */
    public String getErrorMessage() {
        return m_errormessage;
    }
}

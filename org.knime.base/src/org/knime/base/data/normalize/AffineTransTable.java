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
import java.util.HashMap;

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
    
    /** A very small number. */
    public static final double VERY_SMALL = 1E-10;
    
    private final DataTable m_table;

    private final DataTableSpec m_spec;

    private String m_errormessage;
    
    private final AffineTransConfiguration m_configuration;
    private final int[] m_indicesInConfiguration;

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
     * @deprecated Create {@link AffineTransConfiguration} object and then
     * use the constructor 
     * {@link #AffineTransTable(DataTable, AffineTransConfiguration)}. 
     */
    @Deprecated
    public AffineTransTable(final DataTable table, final String[] names,
            final double[] scales, final double[] translations,
            final double[] min, final double[] max) {
        this(table, new AffineTransConfiguration(
                names, scales, translations, min, max, null));
    }
    
    /** Creates new table, normalizing <code>table</code> with the configuration
     * given by <code>configuration</code>.
     * @param table To be normalized
     * @param configuration Normalization parameters.
     * @throws NullPointerException If either arg is null.
     * @throws IllegalArgumentException If target cols in table are not
     * numeric.
     */
    public AffineTransTable(final DataTable table, 
            final AffineTransConfiguration configuration) {
        DataTableSpec spec = table.getDataTableSpec();
        m_configuration = configuration;
        m_indicesInConfiguration = new int[spec.getNumColumns()];
        Arrays.fill(m_indicesInConfiguration, -1);
        String[] names = configuration.getNames();
        HashMap<String, Integer> hash = new HashMap<String, Integer>();
        for (int i = 0; i < names.length; i++) {
            hash.put(names[i], i);
        }
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec col = spec.getColumnSpec(i);
            Integer index = hash.remove(col.getName());
            if (index != null) {
                DataType type = col.getType();
                // do we need to support IntValue also?
                if (!type.isCompatible(DoubleValue.class)) {
                    throw new IllegalArgumentException(
                            "Not supported: " + type);
                }
                m_indicesInConfiguration[i] = index;
            }
        }
        if (!hash.isEmpty()) {
            int size = hash.size();
            setErrorMessage("Normalization was not applied to " + size 
                    + " column(s) as they do not exist in the table: " 
                    + Arrays.toString(hash.keySet().toArray()));
        }
        m_spec = generateNewSpec(spec, m_configuration);
        m_table = table;
    }
    
    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }
    
    /**
     * @return the configuration
     */
    public AffineTransConfiguration getConfiguration() {
        return m_configuration;
    }
    
    /**
     * @return the indicesInConfiguration
     */
    int[] getIndicesInConfiguration() {
        return m_indicesInConfiguration;
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
            final DataTableSpec spec, 
            final AffineTransConfiguration configuration) {
        String[] names = configuration.getNames();
        HashMap<String, Integer> hash = new HashMap<String, Integer>();
        for (int i = 0; i < names.length; i++) {
            hash.put(names[i], i);
        }
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec col = spec.getColumnSpec(i);
            Integer index = hash.get(col.getName());
            if (index != null) {
                DataType type = col.getType();
                // do we need to support IntValue also?
                if (!type.isCompatible(DoubleValue.class)) {
                    throw new IllegalArgumentException(
                            "Not supported: " + type);
                }
            }
        }
        
        DataColumnSpec[] specs = new DataColumnSpec[spec.getNumColumns()];
        double[] newmin = configuration.getMin();
        double[] newmax = configuration.getMax();
        double[] scales = configuration.getScales();
        double[] translations = configuration.getTranslations();
        for (int i = 0; i < specs.length; i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DataColumnDomain colDomain = colSpec.getDomain();
            Integer indexObject = hash.get(colSpec.getName()); 
            if (indexObject == null) {
                specs[i] = colSpec;
            } else {
                int index = indexObject.intValue();
                assert !Double.isNaN(scales[index]);
                // determine domain
                double interval = newmax[index] - newmin[index];
                DataCell up = null;
                DataCell oldUp = colDomain.getUpperBound();
                if (oldUp != null && !oldUp.isMissing()) {
                    double oldVal = ((DoubleValue)oldUp).getDoubleValue();
                    double newVal = 
                        scales[index] * oldVal + translations[index];
                    if (!Double.isNaN(newmax[index])) {
                        if (newVal > newmax[index]
                                && ((newVal - newmax[index]) 
                                        / interval) < VERY_SMALL) {
                            newVal = newmax[index];
                        }
                    }
                    up = new DoubleCell(newVal);
                }
                DataCell low = null;
                DataCell oldLow = colDomain.getLowerBound();
                if (oldLow != null && !oldLow.isMissing()) {
                    double oldVal = ((DoubleValue)oldLow).getDoubleValue();
                    double newVal = 
                        scales[index] * oldVal + translations[index];
                    if (!Double.isNaN(newmin[index])) {
                        if (newVal < newmin[index]
                                && ((newmin[index] - newVal) 
                                        / interval) < VERY_SMALL) {
                            newVal = newmin[index];
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

    /**
     * Saves internals to the argument settings object. This object is supposed
     * to be the only one writing to this (sub)setting object.
     * 
     * @param settings To write to.
     */
    public void save(final ModelContentWO settings) {
        m_configuration.save(settings);
    }
    
    /** Reads the meta information from the settings object and constructs
     * a AffineTransTable based on this information and the given DataTable.
     * @param table The table to which the normalization is applied.
     * @param sets The normalization information.
     * @return A new table wrapping <code>table</code> but normalized according
     * to <code>settings</code>.
     * @throws InvalidSettingsException If the settings are incomplete 
     * or cannot be applied to spec.
     */
    public static AffineTransTable load(final DataTable table,
            final ModelContentRO sets) throws InvalidSettingsException {
        AffineTransConfiguration config = AffineTransConfiguration.load(sets);
        return new AffineTransTable(table, config);
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
        return generateNewSpec(spec, AffineTransConfiguration.load(settings));
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

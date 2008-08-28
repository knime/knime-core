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
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear;

import org.knime.base.node.mine.regression.RegressionPortObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;
import org.knime.core.node.port.AbstractSimplePortObject;


/**
 * Utility class that carries out the loading and saving of linear regression
 * models. It is used by the learner node model and the predictor node model.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class LinearRegressionPortObject extends AbstractSimplePortObject 
    implements RegressionPortObject {
    
    /** convenience access member for port type. */
    public static final PortType TYPE = 
        new PortType(LinearRegressionPortObject.class);

    private static final String CFG_OFFSET = "offset";
    private static final String CFG_MULTIPLIER = "multipliers";
    private static final String CFG_MEANS = "means";


    /** Offset value. */
    private double m_offset;
    
    /** Multipliers for regression evaluation. */
    private double[] m_multipliers;
    /** Mean values of all included columns used for visualization. (The view
     * shows the 2D-regression line on one input variable. We use the mean
     * values of the remaining variables to determine the two points that define
     * the regression line.) */
    private double[] m_means;
    
    private DataTableSpec m_spec;
    
    /** Public no arg constructor as required by super class. */
    public LinearRegressionPortObject() {
    }

    /**
     * Create new object with the given parameters.
     * 
     * @param spec The table spec of the variables
     * @param offset The fixed (constant) offset
     * @param multipliers multiplier values
     * @param means means of all variables (used for 2D plot approximation)
     */
    public LinearRegressionPortObject(final DataTableSpec spec, final double offset,
            final double[] multipliers, final double[] means) {
        if (multipliers == null || means == null) {
            throw new NullPointerException();
        }
        int expectedLength = spec.getNumColumns() - 1;
        if (expectedLength != means.length) {
            throw new IllegalArgumentException(
                    "Confusing array length: " + means.length + ", expected "
                    + expectedLength);
        }
        if (expectedLength != multipliers.length) {
            throw new IllegalArgumentException(
                    "Confusing array length: " + multipliers.length 
                    + ", expected " + expectedLength);
        }
        m_offset = offset;
        m_multipliers = multipliers;
        m_means = means;
        m_spec = spec;
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getSpec() {
        return m_spec;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return null;
    }
    
    /**
     * Get the name of the response column, i.e. the prediction column.
     * 
     * @return the name of the response column
     */
    public String getTargetColumnName() {
        return m_spec.getColumnSpec(m_spec.getNumColumns() - 1).getName();
    }
    
    /** @return the offset */
    public double getOffset() {
        return m_offset;
    }
    
    /** @return the multipliers */
    public double[] getMultipliers() {
        return m_multipliers;
    }

    /**
     * Does a prediction when the given variable has the value v and all other
     * variables have their mean value. Used to determine the line in a 2D plot.
     * 
     * @param variable the variable currently shown on x
     * @param v its value
     * @return the value of the linear regression line
     */
    public double getApproximationFor(final String variable, final double v) {
        double sum = m_offset;
        boolean isFound = false;
        // only iterate to last but one element (last is response column)
        for (int i = 0; i < m_spec.getNumColumns() - 1; i++) {
            DataColumnSpec col = m_spec.getColumnSpec(i);
            double val;
            if (col.getName().equals(variable)) {
                isFound = true;
                val = v;
            } else {
                val = m_means[i];
            }
            sum += m_multipliers[i] * val;
        }
        if (!isFound) {
            throw new IllegalArgumentException("No such column: " + variable);
        }
        return sum;
    }

    /** {@inheritDoc} */
    @Override
    public DataCell predict(final DataRow row) {
        double sum = m_offset;
        for (int i = 0; i < row.getNumCells(); i++) {
            DataCell c = row.getCell(i);
            if (c.isMissing()) {
                return DataType.getMissingCell();
            }
            double d = ((DoubleCell)c).getDoubleValue();
            sum += m_multipliers[i] * d;
        }
        return new DoubleCell(sum);
    }
    
    /** {@inheritDoc} */
    @Override
    public void save(final ModelContentWO par, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        par.addDouble(CFG_OFFSET, m_offset);
        par.addDoubleArray(CFG_MULTIPLIER, m_multipliers);
        par.addDoubleArray(CFG_MEANS, m_means);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void load(final ModelContentRO par, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException {
        m_offset = par.getDouble(CFG_OFFSET);
        m_multipliers = par.getDoubleArray(CFG_MULTIPLIER);
        m_means = par.getDoubleArray(CFG_MEANS);
        m_spec = (DataTableSpec)spec;
        // exclude last element (response column)
        int expLength = m_spec.getNumColumns() - 1;
        if (m_means.length != expLength) {
            throw new InvalidSettingsException("Unexpected array length: "
                    + m_means.length + ", expected " + expLength);
        }
        if (m_multipliers.length != expLength) {
            throw new InvalidSettingsException("Unexpected array length: "
                    + m_multipliers.length + ", expected " + expLength);
        }
    }
    
    public static LinearRegressionPortObject instantiateAndLoad(
            final ModelContentRO par, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException {
        LinearRegressionPortObject result = new LinearRegressionPortObject();
        result.load(par, spec, exec);
        return result;
    }
}

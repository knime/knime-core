/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Sep 9, 2008 (wiswedel): created
 */
package org.knime.base.data.normalize;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Port Object that is passed along a normalizer and a normalizer apply node.
 * 
 * <p>This class is not official API, it may change without prior notice.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class NormalizerPortObject extends AbstractSimplePortObject {
    
    /** Convenience accessor for the port type. */
    public static final PortType TYPE = 
        new PortType(NormalizerPortObject.class);
    
    private DataTableSpec m_spec;
    private AffineTransConfiguration m_configuration;
    
    /** Empty constructor required by super class, should not be used. */
    public NormalizerPortObject() {
    }

    /** Create new port object given the arguments.
     * @param spec Spec of this port object (contains columns to be normalized).
     * @param configuration The normalization configuration.
     * @throws NullPointerException If either arg is null
     * @throws IllegalArgumentException If columns to be normalized don't
     * exist or are not numeric.
     */
    public NormalizerPortObject(final DataTableSpec spec, 
            final AffineTransConfiguration configuration) {
        if (spec == null || configuration == null) {
            throw new NullPointerException("Args must not be null");
        }
        for (String s : configuration.getNames()) {
            DataColumnSpec col = spec.getColumnSpec(s);
            if (col == null) {
                throw new IllegalArgumentException("Spec does not match " 
                        + "normalizer configuration, no column such in spec: " 
                        + s);
            }
            if (!col.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "Column is not double compatible: " + s);
            }
        }
        m_spec = spec;
        m_configuration = configuration;
    }
    
    /** {@inheritDoc} */
    @Override
    public DataTableSpec getSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return m_configuration.getSummary();
    }
    
    /**
     * @return the configuration
     */
    public AffineTransConfiguration getConfiguration() {
        return m_configuration;
    }

    /** {@inheritDoc} */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_spec = (DataTableSpec)spec;
        m_configuration = AffineTransConfiguration.load(model);
    }

    /** {@inheritDoc} */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_configuration.save(model);
    }

}

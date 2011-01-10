/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Sep 4, 2008 (wiswedel): created
 */
package org.knime.core.node.port.viewproperty;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * PortObject used to represent visual properties such as color, shape and
 * size information attached to a table. The content of this object resides in 
 * the accompanying {@link DataTableSpec}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class ViewPropertyPortObject extends AbstractSimplePortObject {
    
    private DataTableSpec m_spec;
    private String m_summary;
    
    /** Public no arg constructor required by super class. 
     * <p>
     * <b>This constructor should only be used by the framework.</b> */
    protected ViewPropertyPortObject() {
    }
    
    /** Constructor used to instantiate this object during a node's execute
     * method.
     * @param spec The accompanying spec
     * @param portSummary A summary returned in the {@link #getSummary()}
     * method.
     * @throws NullPointerException If spec argument is <code>null</code>.
     */
    protected ViewPropertyPortObject(final DataTableSpec spec, 
            final String portSummary) {
        if (spec == null) {
            throw new NullPointerException("Spec must not be null");
        }
        m_spec = spec;
        m_summary = portSummary;
    }

    /** {@inheritDoc} */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_summary = model.getString("summary");
        m_spec = (DataTableSpec)spec;
    }

    /** {@inheritDoc} */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        model.addString("summary", m_summary);
    }

    /** {@inheritDoc} */
    @Override
    public final DataTableSpec getSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public final String getSummary() {
        return m_summary;
    }

}

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
 *   16.09.2008 (thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Provides the spec of the sota port model and a validation method, to 
 * validate if a certain data table spec is compatible to the model spec.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPortObjectSpec extends AbstractSimplePortObjectSpec {    
    
    private DataTableSpec m_spec;
    
    private int m_indexOfClassCol;
    
    /**
     * 
     */
    public SotaPortObjectSpec() { }
    
    /**
     * Creates a new instance of <code>SotaPortObjectSpec</code> with the given
     * data table spec, as the main part of the model spec and the index of the
     * class column.
     * 
     * @param spec The data table spec to store.
     * @param indexOfClassCol The index of the class column.
     */
    public SotaPortObjectSpec(final DataTableSpec spec,
            final int indexOfClassCol) {
        m_spec = spec;
        m_indexOfClassCol = indexOfClassCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) 
    throws InvalidSettingsException {
        ModelContentRO subContent = model.getModelContent(
                SotaPortObject.CFG_KEY_SPEC);
        m_spec = DataTableSpec.load(subContent);
        m_indexOfClassCol = model.getInt(SotaPortObject.CFG_KEY_CLASSCOL_INDEX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        if (m_spec != null) {
            ModelContentWO subContent = model.addModelContent(
                    SotaPortObject.CFG_KEY_SPEC);
            m_spec.save(subContent);
            model.addInt(SotaPortObject.CFG_KEY_CLASSCOL_INDEX, 
                    m_indexOfClassCol);
        }
    }

    /**
     * @return the spec
     */
    public DataTableSpec getSpec() {
        return m_spec;
    }
    
    /**
     * Validates if the given data table spec is compatible to the model port 
     * spec and returns <code>true</code> if so, otherwise <code>false</code>.
     * 
     * @param spec The data table spec to validate.
     * @return <code>true</code> if given spec is compatible to the model port 
     * spec, otherwise <code>false</code>.
     */
    public boolean validateSpec(final DataTableSpec spec) {
        if (m_spec == null || spec == null) {
            return false;
        }
        if (spec.getNumColumns() > m_spec.getNumColumns()
                || spec.getNumColumns() < m_spec.getNumColumns() - 1) {
            return false;
        }
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            if (spec.getNumColumns() == m_spec.getNumColumns()) {
                if (!m_spec.getColumnSpec(i).getType().equals(
                        spec.getColumnSpec(i).getType())) {
                    return false;
                }
            } else {
                if (i < spec.getNumColumns()) {
                    if (!m_spec.getColumnSpec(i).getType().equals(
                            spec.getColumnSpec(i).getType())) {
                        return false;
                    }
                }                
            }
        }
        
        return true;
    }
    
    /**
     * @return <code>true</code> if internal data table spec contains a column
     * which is compatible to string value.
     */
    public boolean hasClassColumn() {
        if (m_spec == null) {
            return false;
        }
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            if (m_spec.getColumnSpec(i).getType().isCompatible(
                    StringValue.class)) {
                return true;
            }
        }
        return false;
    }
}

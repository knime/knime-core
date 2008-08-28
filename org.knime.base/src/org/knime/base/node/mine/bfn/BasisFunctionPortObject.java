/* 
 * ------------------------------------------------------------------
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
 * 
 * History
 *   14.03.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.port.AbstractSimplePortObject;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPortObject extends AbstractSimplePortObject {
    
    
    private BasisFunctionModelContent m_content;
    
    /**
     * Creates a new abstract <code>BasisFunctionPortObject</code>.
     */
    public BasisFunctionPortObject() {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final DataTableSpec getSpec() {
        return m_content.getSpec();
    }
    
    /**
     * @return basisfunctions rules by class
     */
    public final Map<DataCell, 
            List<BasisFunctionPredictorRow>> getBasisFunctions() {
        return m_content.getBasisFunctions();
    }
    
    /**
     * Creates a new basis function model object.
     * @param cont basisfunction model content containing rules and spec
     */
    public BasisFunctionPortObject(final BasisFunctionModelContent cont) {
        m_content = cont;
    }
    
    /**
     * Creator used to instantiate basisfunction predictor rows.
     */
    public interface Creator {
        /**
         * Return specific predictor row for the given 
         * <code>ModelContent</code>.
         * 
         * @param pp the content the read the predictive row from
         * @return a new predictor row
         * @throws InvalidSettingsException if the rule can be read from model
         *             content
         */
        BasisFunctionPredictorRow createPredictorRow(
                ModelContentRO pp) throws InvalidSettingsException;
    }
    
    /**
     * Create a new basisfunction port object given the model content.
     * @param content basisfunction model content with spec and rules.
     * @return a new basisfunction port object
     */
    public abstract BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content);
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return m_content.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_content.save(model);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_content = new BasisFunctionModelContent(model, getCreator());
    }
    
    /**
     * @return a creator for the desired basisfunction rows
     */
    public abstract Creator getCreator(); 
}

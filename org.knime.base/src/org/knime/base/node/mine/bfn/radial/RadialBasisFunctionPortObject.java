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
 *   20.02.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn.radial;

import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.bfn.BasisFunctionModelContent;
import org.knime.base.node.mine.bfn.BasisFunctionPortObject;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RadialBasisFunctionPortObject 
        extends BasisFunctionPortObject {

    /** The <code>PortType</code> for basisfunction models. */
    public static final PortType TYPE = new PortType(
            RadialBasisFunctionPortObject.class);
    
    private BasisFunctionModelContent m_content;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getSpec() {
        return m_content.getSpec();
    }
    
    /**
     * @return basisfunctions rules by class
     */
    @Override
    public Map<DataCell, List<BasisFunctionPredictorRow>> getBasisFunctions() {
        return m_content.getBasisFunctions();
    }
    
    /**
     * 
     */
    public RadialBasisFunctionPortObject() {
        
    }
    
    /**
     * Creates a new basis function model object.
     * @param cont basisfunction model content containing rules and spec
     */
    public RadialBasisFunctionPortObject(final BasisFunctionModelContent cont) {
        m_content = cont;
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
        m_content = new BasisFunctionModelContent(model, new RadialCreator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content) {
        return new RadialBasisFunctionPortObject(content);
    }
    
    /**
     * Used to create PNN predictor rows.
     */
    public static class RadialCreator implements Creator {
        /**
         * {@inheritDoc}
         */
        public BasisFunctionPredictorRow createPredictorRow(
                final ModelContentRO pp)
                throws InvalidSettingsException {
            return new RadialBasisFunctionPredictorRow(pp);
        }
    }
    
}

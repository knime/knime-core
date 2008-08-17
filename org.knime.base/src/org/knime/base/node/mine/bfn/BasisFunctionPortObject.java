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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.portobject.AbstractSimplePortObject;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPortObject extends AbstractSimplePortObject {
    
    /**
     * Creates a new abstract <code>BasisFunctionPortObject</code>.
     */
    public BasisFunctionPortObject() {
        
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
    
    /**
     * {@inheritDoc}
     */
    public abstract DataTableSpec getSpec();
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return null;
    }
    
    /**
     * @return basisfunction rules by class label
     */
    public abstract 
        Map<DataCell, List<BasisFunctionPredictorRow>> getBasisFunctions();
}

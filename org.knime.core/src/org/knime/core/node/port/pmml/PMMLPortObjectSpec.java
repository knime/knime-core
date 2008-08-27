/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.port.pmml;

import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.PortObjectSpec;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpec implements PortObjectSpec {
    
    private final DataTableSpec m_dataTableSpec;
    
    private final Set<DataColumnSpec>m_learningCols;
    
    private final Set<DataColumnSpec> m_ignoredCols;
    
    private final Set<DataColumnSpec> m_targetCols;
    
    /**
     * 
     * @param dataDictionary {@link DataTableSpec} describing the training data
     * @param learningCols columns used for learning of the model
     * @param ignoredCols columns ignored while learning the model
     * @param targetCols columns to be predicted
     */
    public PMMLPortObjectSpec(final DataTableSpec dataDictionary, 
            final Set<DataColumnSpec> learningCols, 
            final Set<DataColumnSpec> ignoredCols, 
            final Set<DataColumnSpec> targetCols) {
        m_dataTableSpec = dataDictionary;
        m_learningCols = learningCols;
        m_ignoredCols = ignoredCols;
        m_targetCols = targetCols;
    }
    
    /**
     * 
     * @return the {@link DataTableSpec} describing the training data  
     */
    public DataTableSpec getDataDictionary() {
        return m_dataTableSpec;
    }
    
    /**
     * 
     * @return those columns used for learning of the model
     */
    public Set<DataColumnSpec> getLearningFields() {
        return m_learningCols;
    }
    
    /**
     * 
     * @return those columns ignored while learning the model
     */
    public Set<DataColumnSpec> getIgnoredFields() {
        return m_ignoredCols;
    }

    /**
     * 
     * @return by the model predicted columns 
     */
    public Set<DataColumnSpec> getTargetFields() {
        return m_targetCols;
    }
    
}

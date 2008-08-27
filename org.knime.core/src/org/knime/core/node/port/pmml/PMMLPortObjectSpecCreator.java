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

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpecCreator {
    
    private final DataTableSpec m_dataTableSpec;
    
    private Set<DataColumnSpec>m_learningCols;
    
    private Set<DataColumnSpec> m_ignoredCols;
    
    private Set<DataColumnSpec> m_targetCols;
    
    /**
     * 
     * @param tableSpec equivalent to the data dictionary
     */
    public PMMLPortObjectSpecCreator(final DataTableSpec tableSpec) {
        m_dataTableSpec = tableSpec;
    }

    /**
     * @param learningCols the learningCols to set
     */
    public void setLearningColsNames(final Set<String> learningCols) {
        m_learningCols = new HashSet<DataColumnSpec>();
        for (String colName : learningCols) {
            DataColumnSpec colSpec = m_dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                m_learningCols = new HashSet<DataColumnSpec>();
                throw new IllegalArgumentException("Column with name " 
                        + colName + " is not in underlying DataTableSpec!");
            }
            m_learningCols.add(colSpec);
        }
    }
    
    /**
     * 
     * @param learningCols column used for training
     */
    public void setLearningCols(final Set<DataColumnSpec> learningCols) {
        // TODO: sanity checks . != null, etc.
        m_learningCols = learningCols;
    }
    

    /**
     * @param ignoredCols the ignoredCols to set
     */
    public void setIgnoredColsNames(final Set<String> ignoredCols) {
        m_ignoredCols = new HashSet<DataColumnSpec>();
        for (String colName : ignoredCols) {
            DataColumnSpec colSpec = m_dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                m_ignoredCols = new HashSet<DataColumnSpec>();
                throw new IllegalArgumentException("Column with name " 
                        + colName + " is not in underlying DataTableSpec!");
            }
            m_ignoredCols.add(colSpec);
        }
    }

    
    /**
     * 
     * @param ignoredCols columns ignored during learning
     */
    public void setIgnoredCols(final Set<DataColumnSpec>ignoredCols) {
        // TODO: sanity checks != null, etc.
        m_ignoredCols = ignoredCols;
    }
    /**
     * @param targetCols the targetCols to set
     */
    public void setTargetColsNames(final Set<String> targetCols) {
        m_targetCols = new HashSet<DataColumnSpec>();
        for (String colName : targetCols) {
            DataColumnSpec colSpec = m_dataTableSpec.getColumnSpec(colName);
            if (colSpec == null) {
                m_targetCols = new HashSet<DataColumnSpec>();
                throw new IllegalArgumentException("Column with name " 
                        + colName + " is not in underlying DataTableSpec!");
            }
            m_targetCols.add(colSpec);
        }
    }
    
    /**
     * 
     * @param targetCols predicted columns
     */
    public void setTargetCols(final Set<DataColumnSpec>targetCols) {
        // TODO: sanity checks != null, etc.
        m_targetCols = targetCols;
    }

    /**
     * Creates a new {@link PMMLPortObjectSpec} based on the internal attributes
     * of this creator.
     * 
     * @return created spec based upon the set attributes
     */
    public PMMLPortObjectSpec createSpec() {
        return new PMMLPortObjectSpec(
                m_dataTableSpec,
                m_learningCols,
                m_ignoredCols,
                m_targetCols);
    }
    

}

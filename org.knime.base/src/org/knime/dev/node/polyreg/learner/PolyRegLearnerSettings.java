/*
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.dev.node.polyreg.learner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the polynomial regression learner node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLearnerSettings {
    private int m_degree = 2;
    private int m_maxRowsForView = 10000;
    
    private String m_targetColumn;

    /**
     * Returns the maximum degree that polynomial used for regression should
     * have.
     * 
     * @return the maximum degree
     */
    public int getDegree() {
        return m_degree;
    }

    /**
     * Returns the name of the target column that holds the dependant variable.
     * 
     * @return the target column's name
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    
    /**
     * Loads the settings from the node settings object.
     * 
     * @param settings the node settings
     * 
     * @throws InvalidSettingsException if one of the settings is missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_degree = settings.getInt("degree");
        m_targetColumn = settings.getString("targetColumn");
        m_maxRowsForView = settings.getInt("maxViewRows");
    }

    
    /**
     * Saves the settings to the node settings object.
     * 
     * @param settings the node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt("degree", m_degree);
        settings.addString("targetColumn", m_targetColumn);
        settings.addInt("maxViewRows", m_maxRowsForView);
    }

    /**
     * Sets the maximum degree that polynomial used for regression should have.
     * 
     * @param degree the maximum degree
     */
    public void setDegree(final int degree) {
        m_degree = degree;
    }

    /**
     * Sets the name of the target column that holds the dependant variable.
     * 
     * @param targetColumn the target column's name
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    
    /**
     * Returns the maximum number of rows that are shown in the curve view.
     * 
     * @return the maximum number of rows
     */
    public int getMaxRowsForView() {
        return m_maxRowsForView;
    }

    /**
     * Sets the maximum number of rows that are shown in the curve view.
     * 
     * @param maxRowsForView the maximum number of rows
     */
    public void setMaxRowsForView(final int maxRowsForView) {
        m_maxRowsForView = maxRowsForView;
    }
}

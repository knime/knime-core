/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.mine.regression.polynomial.learner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

    private final Set<String> m_selectedColumnNames = 
        new LinkedHashSet<String>();
    
    private boolean m_includeAll = false;

    private final Set<String> m_unmodSelectedColumnNames = Collections
            .unmodifiableSet(m_selectedColumnNames);

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
     * Returns the name of the target column that holds the dependent variable.
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
        m_includeAll = settings.getBoolean("includeAll", false); // added v2.1
        m_selectedColumnNames.clear();
        if (!m_includeAll) {
            for (String s : settings.getStringArray("selectedColumns")) {
                m_selectedColumnNames.add(s);
            }
        }
    }

    /**
     * Saves the settings to the node settings object.
     * 
     * @param settings the node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_targetColumn != null) {
            settings.addInt("degree", m_degree);
            settings.addString("targetColumn", m_targetColumn);
            settings.addInt("maxViewRows", m_maxRowsForView);
            settings.addBoolean("includeAll", m_includeAll);
            if (!m_includeAll) {
                settings.addStringArray("selectedColumns", m_selectedColumnNames
                        .toArray(new String[m_selectedColumnNames.size()]));
            }
        }
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
     * Sets the name of the target column that holds the dependent variable.
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

    /**
     * Sets the names of the columns that should be used for the regression. The
     * target column name must not be among these columns!
     * 
     * @param columnNames a set with the selected column names
     */
    public void setSelectedColumns(final Set<String> columnNames) {
        m_selectedColumnNames.clear();
        for (String s : columnNames) {
            m_selectedColumnNames.add(s);
        }
    }

    /**
     * Returns an (unmodifieable) set of the select column names.
     * 
     * @return a set with the selectec column names
     */
    public Set<String> getSelectedColumns() {
        return m_unmodSelectedColumnNames;
    }
    
    /**
     * @return the includeAll
     */
    public boolean isIncludeAll() {
        return m_includeAll;
    }
    
    /**
     * @param includeAll the includeAll to set
     */
    public void setIncludeAll(final boolean includeAll) {
        m_includeAll = includeAll;
    }
}

/* Created on Dec 5, 2006 8:17:28 PM by thor
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
package org.knime.base.node.mine.knn;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class stores the settings for the kNN node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class KnnSettings {
    private String m_classColumn;

    private int m_k = 3;

    private boolean m_weightByDistance;
    
    
    /**
     * Returns if the nearest neighbours should be weighted by their distance
     * to the query pattern.
     * 
     * @return <code>true</code> if the neighbours should be weighted by their
     * distance, <code>false</code> otherwise
     */
    public boolean weightByDistance() {
        return m_weightByDistance;
    }
    
    
    /**
     * Sets if the nearest neighbours should be weighted by their distance
     * to the query pattern.
     * 
     * @param b <code>true</code> if the neighbours should be weighted by their
     * distance, <code>false</code> otherwise
     */
    public void weightByDistance(final boolean b) {
        m_weightByDistance = b;
    }
    
    /**
     * Returns the number of neighbours to consider.
     * 
     * @return the number of neighbours
     */
    public int k() {
        return m_k;
    }

    /**
     * Sets the number of neighbours to consider.
     * 
     * @param k the number of neighbours
     */
    public void k(final int k) {
        this.m_k = k;
    }

    /**
     * Returns the name of the column with the class labels.
     * 
     * @return the class column's name
     */
    public String classColumn() {
        return m_classColumn;
    }

    /**
     * sets the name of the column with the class labels.
     * 
     * @param classColumn the class column's name
     */
    public void classColumn(final String classColumn) {
        m_classColumn = classColumn;
    }

    /**
     * Saves the settings into the given node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("classColumn", m_classColumn);
        settings.addInt("k", m_k);
        settings.addBoolean("weightByDistance", m_weightByDistance);
    }

    /**
     * Loads the settings from the given node settings object.
     * 
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing or invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classColumn = settings.getString("classColumn");
        m_k = settings.getInt("k");
        m_weightByDistance = settings.getBoolean("weightByDistance");
    }
}

/* Created on 27.03.2007 14:38:58 by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.valcount;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the value counter node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ValueCounterSettings {
    private String m_colName;
    private boolean m_hiliting;
    

    /**
     * Sets the column name.
     * 
     * @param columnName a column name
     */
    public void columnName(final String columnName) {
        m_colName = columnName;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    public String columnName() {
        return m_colName;
    }

    
    /**
     * @param b enable or disable hiliting
     */
    public void hiliting(final boolean b) {
        m_hiliting = b;
    }
    
    
    /**
     * @return boolean for hilting 
     */
    public boolean hiliting() {
        return m_hiliting;
    }
    
    /**
     * Save the settings into the node settings object.
     * 
     * @param settings node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("columnName", m_colName);
        settings.addBoolean("hiliting", m_hiliting);
    }

    /**
     * Loads the settings from the node settings object.
     * 
     * @param settings node settings
     * @throws InvalidSettingsException if settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_colName = settings.getString("columnName");
        m_hiliting = settings.getBoolean("hiliting");
    }
}

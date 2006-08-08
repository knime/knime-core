/* Created on Jun 9, 2006 12:17:56 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Simples class for managing the cross validation settings.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateSettings {
    private short m_validations = 10;
    private boolean m_randomSampling;
    private String m_classColumnName;
    

    /**
     * Returns the name of the class column.
     * 
     * @return the class column name
     */
    public String classColumnName() {
        return m_classColumnName;
    }


    /**
     * Sets the name of the class column.
     * 
     * @param classColumnName the name
     */
    public void classColumnName(final String classColumnName) {
        m_classColumnName = classColumnName;
    }


    /**
     * Writes the settings into the node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addShort("validations", m_validations);
        settings.addBoolean("randomSampling", m_randomSampling);
        settings.addString("classColumnName", m_classColumnName);
    }

    
    /**
     * Loads the settings from the node settings object.
     * 
     * @param settings a node settings object
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) {
        m_validations = settings.getShort("validations", (short) 10);
        m_randomSampling = settings.getBoolean("randomSampling", false);
        m_classColumnName = settings.getString("classColumnName", null);
    }


    /**
     * Returns if the rows of the input table should be sampled randomly.
     * 
     * @return <code>true</code> if the should be sampled randomly,
     * <code>false</code> otherwise
     */
    public boolean randomSampling() {
        return m_randomSampling;
    }


    /**
     * Sets if the rows of the input table should be sampled randomly.
     * 
     * @param randomSampling <code>true</code> if the should be sampled
     * randomly, <code>false</code> otherwise
     */
    public void randomSampling(final boolean randomSampling) {
        m_randomSampling = randomSampling;
    }

    /**
     * Returns the number of validation runs that should be performed.
     * 
     * @return the number of validation runs
     */
    public short validations() {
        return m_validations;
    }


    /**
     * Sets the number of validation runs that should be performed.
     * 
     * @param validations the number of validation runs
     */    
    public void validations(final short validations) {
        m_validations = validations;
    }
}

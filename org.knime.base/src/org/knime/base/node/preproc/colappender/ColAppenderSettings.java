/* Created on Oct 25, 2006 2:50:34 PM by thor
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.colappender;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains the settings for the column appender node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColAppenderSettings {
    /**
     * Enum for the three methods how duplicate column names in both tables
     * should be handled.
     * 
     * @author Thorsten Meinl, University of Konstanz
     */
    public enum DuplicateHandling {
        /** Do not execute the node if two columns have the same name. */
        FAIL,
        /** Filter out columns with the same name from the right table. */
        FILTER,
        /** Append a suffix to duplicate column in the right table. */
        APPEND_SUFFIX
    }
    
    private String m_leftColumn, m_rightColumn;
    private String m_duplicateSuffix = "";
    private DuplicateHandling m_duplicateHandling = DuplicateHandling.FAIL;
    private boolean m_sortInMemory, m_innerJoin;
    
    /**
     * Sets if inner joins should be performed.
     * 
     * @param b <code>true</code> if inner joins should be performed,
     * <code>false</code> otherwise
     */
    public void innerJoin(final boolean b) {
        m_innerJoin = b;
    }
    
    
    /**
     * Returns if inner joins should be performed.
     * 
     * @return <code>true</code> if inner joins should be performed,
     * <code>false</code> otherwise
     */
    public boolean innerJoin() {
        return m_innerJoin;
    }
    
    
    /**
     * Sets if the two tables should be sorted completely in memory.
     * 
     * @param b <code>true</code> if memory sorting should be performed,
     * <code>false</code> otherwise
     */
    public void sortInMemory(final boolean b) {
        m_sortInMemory = b;
    }
    

    /**
     * Returns if the two tables should be sorted completely in memory.
     * 
     * @return <code>true</code> if memory sorting should be performed,
     * <code>false</code> otherwise
     */
    public boolean sortInMemory() {
        return m_sortInMemory;
    }
    
    /**
     * Sets the name of the column in the left table that should be used for
     * joining.
     * 
     * @param name the column name
     */
    public void leftColumn(final String name) {
        m_leftColumn = name;
    }
    
    /**
     * Returns the name of the column in the left table that should be used for
     * joining.
     * 
     * @return the column name
     */
    public String leftColumn() {
        return m_leftColumn;
    }
    
    /**
     * Sets the name of the column in the right table that should be used for
     * joining.
     * 
     * @param name the column name
     */
    public void rightColumn(final String name) {
        m_rightColumn = name;
    }
    
    /**
     * Returns the name of the column in the right table that should be used for
     * joining.
     * 
     * @return the column name
     */
    public String rightColumn() {
        return m_rightColumn;
    }
     
    /**
     * Sets the suffix that should be appended to duplicate columns in the right
     * table if {@link #duplicateHandling(DuplicateHandling)} is set to
     * {@link DuplicateHandling#APPEND_SUFFIX}.
     * 
     * @param suffix the suffix
     */
    public void duplicateSuffix(final String suffix) {
        m_duplicateSuffix = suffix;
    }
    
    /**
     * Returns the suffix that should be appended to duplicate columns in the
     * right table if {@link #duplicateHandling(DuplicateHandling)} is set to
     * {@link DuplicateHandling#APPEND_SUFFIX}.
     * 
     * @return the suffix
     */
    public String duplicateSuffix() {
        return m_duplicateSuffix;
    }
    
    /**
     * Sets how columns with duplicate names in the right table should be
     * handled.
     * 
     * @param handling the handling method
     */
    public void duplicateHandling(final DuplicateHandling handling) {
        m_duplicateHandling = handling;
    }
    
    /**
     * Returns how columns with duplicate names in the right table should be
     * handled.
     * 
     * @return the handling method
     */
    public DuplicateHandling duplicateHandling() {
        return m_duplicateHandling;
    }
    
    /**
     * Save the options to the given settintgs object.
     * 
     * @param settings a settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("leftColumn", m_leftColumn);
        settings.addString("rightColumn", m_rightColumn);
        settings.addString("duplicateHandling", m_duplicateHandling.toString());
        settings.addString("duplicateSuffix", m_duplicateSuffix);
        settings.addBoolean("sortInMemory", m_sortInMemory);
        settings.addBoolean("innerJoin", m_innerJoin);
    }


    /**
     * Reads the options from the settings object.
     * 
     * @param settings a settings object
     */
    public void loadSettings(final NodeSettingsRO settings) {
        m_leftColumn = settings.getString("leftColumn", null);
        m_rightColumn = settings.getString("rightColumn", null);
        
        m_duplicateHandling = DuplicateHandling.valueOf(
                settings.getString("duplicateHandling",
                        DuplicateHandling.FAIL.toString()));
        m_duplicateSuffix = settings.getString("duplicateSuffix", "_dup");
        m_sortInMemory = settings.getBoolean("sortInMemory", false);
        m_innerJoin = settings.getBoolean("innerJoin", false);
    }
}

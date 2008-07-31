/* 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface SotaCell {

    /**
     * Adjusts the cells value related to the given cell with given
     * learningrate.
     * 
     * @param cell cell to adjust SotaCell with
     * @param learningrate learningrate to adjust cell value with
     */
    public void adjustCell(final DataCell cell, final double learningrate);
    
    
    /**
     * Returns a double value of the cell.
     * 
     * @return a double value of the cell
     */
    public double getValue();

    /**
     * Clones the SotaCell instance and returns the clone.
     * 
     * @return the clone of the SotaCell instance
     */
    public SotaCell clone();
    
    /**
     * Saves the value of the <code>SotaCell</code> to the given 
     * <code>ModelContentWO</code>.
     * 
     * @param modelContent The <code>ModelContentWO</code> to save the values
     * to. 
     */
    public abstract void saveTo(final ModelContentWO modelContent);
    
    
    /**
     * Loads the values from the given <code>ModelContentWO</code>.
     * 
     * @param modelContent The <code>ModelContentWO</code> to load the values 
     * from.
     * 
     * @throws InvalidSettingsException If setting to load is not valid.
     */
    public abstract void loadFrom(final ModelContentRO modelContent) 
    throws InvalidSettingsException;    
    
    
    /**
     * @return Returns the cells type.
     */
    public abstract String getType();
}

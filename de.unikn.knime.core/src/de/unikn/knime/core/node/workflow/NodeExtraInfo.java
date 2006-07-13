/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.03.2005 (mb): created
 *   09.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/** Interface for an object that's held within a NodeContainer and
 * allows loading and saving of information. Usually such objects
 * will hold information about the Node's position in some layout.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface NodeExtraInfo {

    /**
     * Stores all contained information into the given configuration.
     * 
     * @param config The configuration to write the current settings into.
     * @see #load
     */
    public void save(final NodeSettingsWO config);
    
    /**
     * Reads the information from the NodeSettings object.
     * 
     * @param config Retrieve the data from. 
     * @throws InvalidSettingsException If the required keys are not
     *         available in the NodeSettings.
     * 
     * @see #save
     */
    public void load(final NodeSettingsRO config) throws InvalidSettingsException;
    
    /**
     * Checks if all information for this extra info is set properly.
     * 
     * @return true if infos are set properly
     */
    public boolean isFilledProperly();
    
    /**
     * Changes the position according to
     * the given moving distance.
     * 
     * @param moveDist the distance to change position
     */
    public void changePosition(final int moveDist);
    
    /**
     * @see Object#clone()
     */
    public Object clone() throws CloneNotSupportedException;
}

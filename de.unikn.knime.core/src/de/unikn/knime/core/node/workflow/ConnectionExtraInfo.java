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
 *   22.03.2005 (mb): created
 *   10.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/** Interface for an object that's held within a ConnectionContainer and
 * allows loading and saving of information. Usually such objects
 * will hold information about the Edge's position(s) in some layout.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface ConnectionExtraInfo {
    /**
     * Stores all contained information into the given configuration.
     * 
     * @param config The configuration to write the current settings into.
     * @see #load
     */
    void save(final NodeSettings config);
    
    /**
     * Reads the information from the NodeSettings object.
     * 
     * @param config Retrieve the data from. 
     * @throws InvalidSettingsException If the required keys are not
     *         available in the NodeSettings.
     * 
     * @see #save
     */
    void load(final NodeSettings config) throws InvalidSettingsException;
}

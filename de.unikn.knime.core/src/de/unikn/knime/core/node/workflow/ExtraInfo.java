/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   20.07.2006 (sieb): created
 */
package de.unikn.knime.core.node.workflow;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * Basic interface for extra information.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public interface ExtraInfo {

    /**
     * Stores all contained information into the given configuration.
     * 
     * @param config The configuration to write the current settings into.
     * @see #load
     */
    void save(final NodeSettingsWO config);

    /**
     * Reads the information from the NodeSettings object.
     * 
     * @param config Retrieve the data from.
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings.
     * 
     * @see #save
     */
    void load(final NodeSettingsRO config) throws InvalidSettingsException;

}

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
 *   20.07.2006 (sieb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Basic interface for extra information.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public interface UIInformation extends Cloneable {

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
    
    /** UIInformation objects are cloneable without further restriction.
     * {@inheritDoc} */
    public UIInformation clone();
    
}

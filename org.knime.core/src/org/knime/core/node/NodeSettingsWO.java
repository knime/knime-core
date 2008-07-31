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
 *   12.07.2006 (gabriel): created
 */
package org.knime.core.node;

import org.knime.core.node.config.ConfigWO;

/**
 * Write-only <code>NodeSettingsWO</code> interface.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface NodeSettingsWO extends ConfigWO {

    /**
     * Creates new <code>NodeSettingsWO</code> object for the given key and
     * returns it.
     * @param key The identifier for the given config.
     * @return A new <code>NodeSettingsWO</code> object.
     */
    NodeSettingsWO addNodeSettings(String key);
    
    /**
     * Add the given <code>NodeSettings</code> object to this Config using the 
     * key of the argument's <code>NodeSettings</code>.
     * @param settings The object to add to this <code>Config</code>.
     */
    void addNodeSettings(NodeSettings settings);
    
}

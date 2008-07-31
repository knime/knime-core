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
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the looper node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ForLoopHeadSettings {
    private int m_loops = 10;
    
    /**
     * Sets the number of times the inner workflow should be executed.
     * @param loops the number loops, which must be > 0
     */
    public void loops(final int loops) {
        m_loops = loops;
    }
    
    /**
     * Returns the number of loops.
     * @return the number of loops.
     */
    public int loops() {
        return m_loops;
    }
    
    
    /**
     * Loads the settings from the node settings object.
     * 
     * @param settings a node settings object
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) {
        m_loops = settings.getInt("loops", 10);
    }

    
    /**
     * Writes the settings into the node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt("loops", m_loops);
    }
}

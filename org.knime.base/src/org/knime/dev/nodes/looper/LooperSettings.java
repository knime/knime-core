/* Created on Aug 7, 2006 7:56:21 AM by thor
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
 */
package org.knime.dev.nodes.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the looper node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class LooperSettings {
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

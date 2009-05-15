/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.05.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the generic loop end node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndNodeSettings {
    private boolean m_addIterationColumn = true;

    /**
     * Sets if a column containing the iteration number should be appended to
     * the output table.
     *
     * @param add <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     */
    public void addIterationColumn(final boolean add) {
        m_addIterationColumn = add;
    }

    /**
     * Returns if a column containing the iteration number should be appended to
     * the output table.
     *
     * @return <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     */
    public boolean addIterationColumn() {
        return m_addIterationColumn;
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("addIterationColumn", m_addIterationColumn);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    public void loadSettings(final NodeSettingsRO settings) {
        m_addIterationColumn = settings.getBoolean("addIterationColumn", true);
    }
}

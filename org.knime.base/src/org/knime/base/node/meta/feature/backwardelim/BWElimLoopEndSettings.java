/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   26.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains th settings for the elimination loop's tail node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimLoopEndSettings {
    private String m_predictionColumn;
    private String m_targetColumn;

    /**
     * Returns the target column's name.
     *
     * @return the column's name
     */
    public String targetColumn() {
        return m_targetColumn;
    }

    /**
     * Sets the target column's name.
     *
     * @param targetColumn the column's name
     */
    public void targetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }



    /**
     * Returns the prediction column's name.
     *
     * @return the column's name
     */
    public String predictionColumn() {
        return m_predictionColumn;
    }

    /**
     * Sets the prediction column's name.
     *
     * @param targetColumn the column's name
     */
    public void predictionColumn(final String targetColumn) {
        m_predictionColumn = targetColumn;
    }

    /**
     * Saves the settings from this object into the passed node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("predictionColumn", m_predictionColumn);
        settings.addString("targetColumn", m_targetColumn);
    }

    /**
     * Loads the settings from passed node settings object into this object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if a settings is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn = settings.getString("predictionColumn");
        m_targetColumn = settings.getString("targetColumn");
    }

    /**
     * Loads the settings from passed node settings object into this object
     * using default values if a settings is missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_predictionColumn = settings.getString("predictionColumn", null);
        m_targetColumn = settings.getString("targetColumn", null);
    }
}

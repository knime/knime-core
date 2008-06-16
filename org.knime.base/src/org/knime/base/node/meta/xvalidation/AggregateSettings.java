/*
 * ------------------------------------------------------------------
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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the aggregation node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class AggregateSettings {
    private String m_targetColumn;

    private String m_predictionColumn;

    /**
     * Returns the target column containing the real class values.
     *
     * @return a column name
     */
    public String targetColumn() {
        return m_targetColumn;
    }

    /**
     * sets the target column containing the real class values.
     *
     * @param targetCol column name
     */
    public void targetColumn(final String targetCol) {
        m_targetColumn = targetCol;
    }


    /**
     * Returns the prediction column containing the predicted class values.
     *
     * @return a column name
     */
    public String predictionColumn() {
        return m_predictionColumn;
    }

    /**
     * Sets the prediction column containing the predicted class values.
     *
     * @param predictionCol column name
     */
    public void predictionColumn(final String predictionCol) {
        m_predictionColumn = predictionCol;
    }


    /**
     * Saves this object's settings to the given node settings.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("predictionColumn", m_predictionColumn);
        settings.addString("targetColumn", m_targetColumn);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn = settings.getString("predictionColumn");
        m_targetColumn = settings.getString("targetColumn");
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_predictionColumn = settings.getString("predictionColumn", null);
        m_targetColumn = settings.getString("targetColumn", null);
    }
}

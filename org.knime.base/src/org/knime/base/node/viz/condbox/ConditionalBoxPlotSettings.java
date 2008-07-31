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
 *   Feb 25, 2008 (sellien): created
 */
package org.knime.base.node.viz.condbox;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for conditional box plot.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class ConditionalBoxPlotSettings {
    private String m_nominalColumn;

    private String m_numericColumn;

    private boolean m_showMissingValues = false;

    /**
     * Sets the nominal column.
     * 
     * @param nominalColumn the nominal column
     */
    public void nominalColumn(final String nominalColumn) {
        m_nominalColumn = nominalColumn;
    }

    /**
     * Returns the nominal column.
     * 
     * @return the nominal column
     */
    public String nominalColumn() {
        return m_nominalColumn;
    }

    /**
     * Sets the numeric column.
     * 
     * @param numericColumn the numeric column
     */
    public void numericColumn(final String numericColumn) {
        m_numericColumn = numericColumn;
    }

    /**
     * Returns the numeric column.
     * 
     * @return the numeric column
     */
    public String numericColumn() {
        return m_numericColumn;
    }

    /**
     * Sets whether missing values should be shown.
     * 
     * @param showMissingValues true, if missing values should be shown
     */
    public void showMissingValues(final boolean showMissingValues) {
        m_showMissingValues = showMissingValues;
    }

    /**
     * Returns whether missing values should be shown.
     * 
     * @return whether missing values should be shown.
     */
    public boolean showMissingValues() {
        return m_showMissingValues;
    }

    /**
     * Load settings from given NodeSettings.
     * 
     * @param settings the given settings
     * @throws InvalidSettingsException if setting missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_nominalColumn = settings.getString("nominalColumn");
        m_numericColumn = settings.getString("numericColumn");
        m_showMissingValues = settings.getBoolean("showMissingValues");
    }

    /**
     * Save settings in NodeSettings.
     * 
     * @param settings the settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("nominalColumn", m_nominalColumn);
        settings.addString("numericColumn", m_numericColumn);
        settings.addBoolean("showMissingValues", m_showMissingValues);
    }
}

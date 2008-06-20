/*
 * ------------------------------------------------------------------ *
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
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains the settings for the feature elimination filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimFilterSettings {
    private int m_nrOfFeatures;

    private final List<String> m_includedColumns = new ArrayList<String>();

    private final List<String> m_unmodList =
            Collections.unmodifiableList(m_includedColumns);

    private boolean m_includeTargetColumn;

    /**
     * Returns if the target column should be included in the output table.
     *
     * @return <code>true</code> if it should be included, <code>false</code>
     *         otherwise
     */
    public boolean includeTargetColumn() {
        return m_includeTargetColumn;
    }

    /**
     * sets if the target column should be included in the output table.
     *
     * @param b <code>true</code> if it should be included, <code>false</code>
     *            otherwise
     */
    public void includeTargetColumn(final boolean b) {
        m_includeTargetColumn = b;
    }

    /**
     * Returns a list with the columns that should be included in the output
     * table. This list also includes the target column if it should be
     * included.
     *
     * @return an unmodifiable list with column names
     *
     * @see #includeTargetColumn()
     */
    public List<String> includedColumns() {
        return m_unmodList;
    }

    /**
     * Clears the list with included columns.
     */
    public void clearColumns() {
        m_includedColumns.clear();
    }

    /**
     * Adds a column to the list of included columns.
     *
     * @param name the column's name
     */
    public void addColumn(final String name) {
        m_includedColumns.add(name);
    }

    /**
     * Returns the number of included feature for the selected level. This is
     * not necessarily the same as the size of {@link #includedColumns()} as the
     * latter only contains columns that are present in the input table while
     * the number of features is the "level" that comes out from the elimination
     * loop.
     *
     * @return the number of included features
     */
    public int nrOfFeatures() {
        return m_nrOfFeatures;
    }

    /**
     * Sets the number of included feature for the selected level. This is not
     * necessarily the same as the size of {@link #includedColumns()} as the
     * latter only contains columns that are present in the input table while
     * the number of features is the "level" that comes out from the elimination
     * loop.
     *
     * @param number the number of included features
     */
    public void nrOfFeatures(final int number) {
        m_nrOfFeatures = number;
    }

    /**
     * Saves the settings from this object into the passed node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray("includedColumns", m_includedColumns
                .toArray(new String[m_includedColumns.size()]));
        settings.addInt("nrOfFeatures", m_nrOfFeatures);
        settings.addBoolean("includeTargetColumn", m_includeTargetColumn);
    }

    /**
     * Loads the settings from passed node settings object into this object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if a settings is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.clear();
        for (String t : settings.getStringArray("includedColumns")) {
            m_includedColumns.add(t);
        }
        m_nrOfFeatures = settings.getInt("nrOfFeatures");
        m_includeTargetColumn = settings.getBoolean("includeTargetColumn");
    }

    /**
     * Loads the settings from passed node settings object into this object
     * using default values if a settings is missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_includedColumns.clear();
        for (String t : settings.getStringArray("includedColumns",
                new String[0])) {
            m_includedColumns.add(t);
        }
        m_nrOfFeatures = settings.getInt("nrOfFeatures", -1);
        m_includeTargetColumn =
                settings.getBoolean("includeTargetColumn", false);
    }
}

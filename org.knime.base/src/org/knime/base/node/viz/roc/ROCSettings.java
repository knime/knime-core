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
 *   11.02.2008 (thor): created
 */
package org.knime.base.node.viz.roc;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the ROC curve view.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ROCSettings {
    private final ArrayList<String> m_curves = new ArrayList<String>();

    private String m_classColumn;

    private DataCell m_positiveClass;

    /**
     * Returns the list of class probability columns that should be shown
     * in the ROC view.
     *
     * @return a list with column names
     */
    public List<String> getCurves() {
        return m_curves;
    }

    /**
     * Returns the name of the class column.
     *
     * @return the class column's name
     */
    public String getClassColumn() {
        return m_classColumn;
    }

    /**
     * Sets the value from the class column that represents the "positive"
     * class.
     *
     * @param value any value
     */
    public void setPositiveClass(final DataCell value) {
        m_positiveClass = value;
    }

    /**
     * Returns the value from the class column that represents the "positive"
     * class.
     *
     * @return any value
     */
    public DataCell getPositiveClass() {
        return m_positiveClass;
    }

    /**
     * Sets the name of the class column.
     *
     * @param colName the class column's name
     */
    public void setClassColumn(final String colName) {
        m_classColumn = colName;
    }

    /**
     * Saves this object's settings to the given node settings.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("classColumn", m_classColumn);
        settings.addDataCell("positiveClass", m_positiveClass);
        settings.addStringArray("curves", m_curves.toArray(new String[0]));
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classColumn = settings.getString("classColumn");
        m_positiveClass = settings.getDataCell("positiveClass");
        m_curves.clear();

        for (String s : settings.getStringArray("curves")) {
            m_curves.add(s);
        }
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_classColumn = settings.getString("classColumn", null);
        m_positiveClass = settings.getDataCell("positiveClass", null);
        m_curves.clear();

        for (String s : settings.getStringArray("curves", new String[0])) {
            m_curves.add(s);
        }
    }
}

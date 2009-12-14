/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

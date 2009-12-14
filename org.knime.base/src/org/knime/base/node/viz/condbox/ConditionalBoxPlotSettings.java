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

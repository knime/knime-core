/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   08.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.preproc.pmml.missingval.handlers.DoNothingMissingCellHandlerFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Alexander Fillbrunn
 */
public class MVColumnSettings {

    private static final String COLUMN_NAMES_KEY = "colNames";
    private static final String SETTINGS_KEY = "settings";

    private Set<String> m_columns;
    private MVIndividualSettings m_settings;

    /**
     * Initializes a new MVColumnSettings object without columns and a do nothing factory.
     */
    MVColumnSettings() {
        m_columns = new HashSet<String>();
        m_settings = new MVIndividualSettings(DoNothingMissingCellHandlerFactory.getInstance());
    }

    /**
     * Constructor for column settings that initializes the settings with no columns and the given factory.
     * @param factory the factory that is assigned to the columns that are configured with this settings object
     */
    public MVColumnSettings(final MissingCellHandlerFactory factory) {
        m_columns = new HashSet<String>();
        m_settings = new MVIndividualSettings(factory);
    }

    /**
     * Loads settings from a NodeSettings object.
     * @param settings the settings to load from
     * @param repair if true, missing factories are replaced by the do nothing factory, else an exception is thrown
     * @return the MVColumnSettings stored in the NodeSettingsRO object
     * @throws InvalidSettingsException if the settings cannot be loaded
     */
    public String loadSettings(final NodeSettingsRO settings, final boolean repair) throws InvalidSettingsException {
        this.m_columns.clear();

        m_settings = new MVIndividualSettings();
        String warning = m_settings.loadSettings(settings.getNodeSettings(SETTINGS_KEY), repair);
        for (String col : settings.getStringArray(COLUMN_NAMES_KEY)) {
            m_columns.add(col);
        }
        return warning;
    }

    /**
     * Saves the settings to a NodeSettings object.
     * @param settings the settings to save to
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray(COLUMN_NAMES_KEY, m_columns.toArray(new String[0]));
        NodeSettingsWO s = settings.addNodeSettings(SETTINGS_KEY);
        m_settings.saveSettings(s);
    }

    /**
     * @return the columns
     */
    public Set<String> getColumns() {
        return m_columns;
    }

    /**
     * @return the settings
     */
    public MVIndividualSettings getSettings() {
        return m_settings;
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(final MVIndividualSettings settings) {
        m_settings = settings;
    }
}

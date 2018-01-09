/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains all settings for the business rule node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineSettings {
    private static final String APPEND_COLUMN = "append-column";

    private static final String REPLACE_COLUMN_NAME = "replace-column-name";

    private final List<String> m_rules = new ArrayList<String>();

    private String m_newColName = RuleEngineNodeDialog.NEW_COL_NAME;
    private String m_replaceColumn = "";
    private boolean m_appendColumn = true;
    /** since 3.2 the node can produce long output ($$ROWINDEX$$) but old instances of that node in old workflows
     * will map that to int ... will force that using this (hidden) setting. */
    private boolean m_disallowLongOutputForCompatibility = false;

    /**
     * Sets the name of the new column containing the matching rule's outcome.
     *
     * @param newcolName the column's name
     */
    public void setNewcolName(final String newcolName) {
        m_newColName = newcolName;
    }

    /**
     * Returns the name of the new column containing the matching rule's outcome.
     *
     * @return the column's name
     */
    public String getNewColName() {
        return m_newColName;
    }

    /**
     * @return the disallowLongOutputForCompatibility
     * @since 3.2
     */
    public boolean isDisallowLongOutputForCompatibility() {
        return m_disallowLongOutputForCompatibility;
    }

    /**
     * Adds a rule.
     *
     * @param rule the rule string
     */
    public void addRule(final String rule) {
        m_rules.add(rule);
    }

    /**
     * Removes all rules.
     */
    public void clearRules() {
        m_rules.clear();
    }

    /**
     * Returns an iterable over all rules.
     *
     * @return an iterable over all rules
     */
    public Iterable<String> rules() {
        return m_rules;
    }



    /**
     * @return the replaceColumn
     */
    public String getReplaceColumn() {
        return m_replaceColumn;
    }

    /**
     * @param replaceColumn the replaceColumn to set
     */
    public void setReplaceColumn(final String replaceColumn) {
        this.m_replaceColumn = replaceColumn;
    }

    /**
     * @return the appendColumn
     */
    public boolean isAppendColumn() {
        return m_appendColumn;
    }

    /**
     * @param appendColumn the appendColumn to set
     */
    public void setAppendColumn(final boolean appendColumn) {
        this.m_appendColumn = appendColumn;
    }

    /**
     * Loads the settings from the settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        String[] rules = settings.getStringArray("rules");

        m_rules.clear();
        for (String r : rules) {
            m_rules.add(r);
        }

        m_newColName = settings.getString("new-column-name");
        m_replaceColumn = settings.getString(REPLACE_COLUMN_NAME, "");
        m_appendColumn = settings.getBoolean(APPEND_COLUMN, true);
        // added in 3.2
        m_disallowLongOutputForCompatibility = settings.getBoolean("disallowLongOutputForCompatibility", true);
    }

    /**
     * Loads the settings from the settings object for use in the dialog, i.e. default values are used for missing
     * settings.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        String[] rules = settings.getStringArray("rules", new String[0]);

        m_rules.clear();
        for (String r : rules) {
            m_rules.add(r);
        }
        m_newColName = settings.getString("new-column-name", RuleEngineNodeDialog.NEW_COL_NAME);
        m_replaceColumn = settings.getString(REPLACE_COLUMN_NAME, "");
        m_appendColumn = settings.getBoolean(APPEND_COLUMN, true);
        m_disallowLongOutputForCompatibility = settings.getBoolean("disallowLongOutputForCompatibility", false);
    }

    /**
     * Saves the setting into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray("rules", m_rules.toArray(new String[m_rules.size()]));
        settings.addString("new-column-name", m_newColName);
        settings.addString(REPLACE_COLUMN_NAME, m_replaceColumn);
        settings.addBoolean(APPEND_COLUMN, m_appendColumn);
        settings.addBoolean("disallowLongOutputForCompatibility", m_disallowLongOutputForCompatibility);
    }
}

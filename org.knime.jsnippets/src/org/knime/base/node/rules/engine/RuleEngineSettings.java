/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules.engine;

import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.rules.engine.Rule.OutcomeKind;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains all settings for the business rule node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.8
 */
public class RuleEngineSettings {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RuleEngineSettings.class);

    /** Default label settings key. */
    @Deprecated
    static final String CFG_DEFAULT_LABEL = "default-label";

    private final List<String> m_rules = new ArrayList<String>();

    private String m_newColName = RuleEngineNodeDialog.NEW_COL_NAME;

    /**
     * Sets the name of the new column containing the matching rule's outcome.
     *
     * @param newcolName the column's name
     */
    public void setNewcolName(final String newcolName) {
        m_newColName = newcolName;
    }

    /**
     * Sets the label that should be used if no rule matches.
     *
     * @param defaultLabel the default label
     * @deprecated Use a rule like {@code TRUE=>...}.
     */
    @Deprecated
    public void setDefaultLabel(final String defaultLabel) {
        LOGGER.coding("Invoked deprecated setDefaultLabel");
    }

    /**
     * @param b If {@code true} {@link OutcomeKind#Column} will be the default outcome kind, else the previous value.
     * @see #setDefaultOutputType(OutcomeKind)
     */
    @Deprecated
    public void setDefaultLabelIsColumn(final boolean b) {
        LOGGER.coding("Invoked deprecated setDefaultLabelIsColumn");
    }

    /**
     * @param defaultOutputType the defaultOutputType to set
     * @deprecated Cannot set, use a {@code TRUE=>...} rule with the preferred outcome.
     */
    @Deprecated
    public void setDefaultOutputType(final Rule.OutcomeKind defaultOutputType) {
        LOGGER.coding("Invoked deprecated setDefaultOutputType");
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
     * Returns the label that should be used if no rule matches.
     *
     * @return the default label
     * @deprecated use a {@code TRUE=>...} rule instead.
     */
    @Deprecated
    public String getDefaultLabel() {
        LOGGER.coding("Invoked deprecated getDefaultLabel");
        throw new IllegalStateException("Deprecated");
    }

    /**
     * @return default output kind is {@link OutcomeKind#Column}
     * @see #getDefaultOutputType()
     */
    @Deprecated
    public boolean getDefaultLabelIsColumn() {
        LOGGER.coding("Invoked deprecated getDefaultLabelIsColumn");
        throw new IllegalStateException("Deprecated");
    }

    /**
     * @return the defaultOutputType
     * @deprecated Should not be used any longer.
     */
    @Deprecated
    public Rule.OutcomeKind getDefaultOutputType() {
        LOGGER.coding("Invoked deprecated getDefaultOutputType");
        throw new IllegalStateException("Deprecated");
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
    }

    /**
     * Saves the setting into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray("rules", m_rules.toArray(new String[m_rules.size()]));
        settings.addString("new-column-name", m_newColName);
    }
}

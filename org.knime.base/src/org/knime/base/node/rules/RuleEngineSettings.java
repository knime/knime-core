/* ------------------------------------------------------------------
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import java.util.ArrayList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class contains all settings for the business rule node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class RuleEngineSettings {
    static final String CFG_DEFAULT_LABEL = "default-label";

    private static final String CFG_NEW_COL_NAME = "new-column-name";

    private final ArrayList<String> m_rules = new ArrayList<String>();

    private String m_defaultLabel = RuleEngineNodeDialog.DEFAULT_LABEL;

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
     */
    public void setDefaultLabel(final String defaultLabel) {
        m_defaultLabel = defaultLabel;
    }

    /**
     * Returns the name of the new column containing the matching rule's
     * outcome.
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
     */
    public String getDefaultLabel() {
        return m_defaultLabel;
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
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String[] rules = settings.getStringArray("rules");

        m_rules.clear();
        for (String r : rules) {
            m_rules.add(r);
        }

        m_defaultLabel = settings.getString(CFG_DEFAULT_LABEL);
        m_newColName = settings.getString(CFG_NEW_COL_NAME);
    }

    /**
     * Loads the settings from the settings object for use in the dialog, i.e.
     * default values are used for missing settings.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        String[] rules = settings.getStringArray("rules", new String[0]);

        m_rules.clear();
        for (String r : rules) {
            m_rules.add(r);
        }
        m_defaultLabel =
                settings.getString(CFG_DEFAULT_LABEL,
                        RuleEngineNodeDialog.DEFAULT_LABEL);
        m_newColName =
                settings.getString(CFG_NEW_COL_NAME,
                        RuleEngineNodeDialog.NEW_COL_NAME);
    }

    /**
     * Saves the setting into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray("rules", m_rules.toArray(new String[m_rules
                .size()]));
        settings.addString(CFG_DEFAULT_LABEL, m_defaultLabel);
        settings.addString(CFG_NEW_COL_NAME, m_newColName);
    }
}

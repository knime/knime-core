/*
 * ------------------------------------------------------------------------
 *
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
 *   30 May 2015 (Gabor): created
 */
package org.knime.base.node.rules.engine.totable;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Settings for controlling the transformation of a RuleSet model to a table.
 *
 * @author Gabor Bakos
 */
public class RulesToTableSettings {

    /** By default do not split the rules to two columns. */
    static final boolean DEFAULT_SPLIT_RULES = false;
    /** By default do not provide confidence and weights columns. */
    static final boolean DEFAULT_CONFIDENCE_AND_WEIGHT = false;
    /** By default provide statistics. */
    static final boolean DEFAULT_STATISTICS = true;
    /** By default simplify rules. */
    static final boolean DEFAULT_ADDITIONAL_PARENTHESES = false;
    /** By default do not generate score distribution record counts for table. */
    static final boolean DEFAULT_TABLE_SCORE_RECORD_COUNT = false;
    /** Prefix for score distribution record counts in the table. */
    static final String DEFAULT_TABLE_SCORE_RECORD_COUNT_PREFIX = "Record count ";
    /** By default do not generate score distribution probabilities for table. */
    static final boolean DEFAULT_TABLE_SCORE_PROBABILITY = false;
    /** Prefix for score distribution probabilities in the table. */
    static final String DEFAULT_TABLE_SCORE_PROBABILITY_PREFIX = "Probability ";
    private SettingsModelBoolean m_splitRules = new SettingsModelBoolean("split.rules", DEFAULT_SPLIT_RULES);
    private SettingsModelBoolean m_confidenceAndWeight = new SettingsModelBoolean("confidence.and.weight", DEFAULT_CONFIDENCE_AND_WEIGHT);
    private SettingsModelBoolean m_provideStatistics = new SettingsModelBoolean("statistics", DEFAULT_STATISTICS);
    private SettingsModelBoolean m_useAdditionalParentheses = new SettingsModelBoolean("additional.parentheses", DEFAULT_ADDITIONAL_PARENTHESES);
    private SettingsModelBoolean m_scoreTableRecordCount = new SettingsModelBoolean("table.score.recordCount", DEFAULT_TABLE_SCORE_RECORD_COUNT);
    private SettingsModelString m_scoreTableRecordCountPrefix = new SettingsModelString("table.score.recordCount.prefix", DEFAULT_TABLE_SCORE_RECORD_COUNT_PREFIX);
    private SettingsModelBoolean m_scoreTableProbability = new SettingsModelBoolean("table.score.probability", DEFAULT_TABLE_SCORE_PROBABILITY);
    private SettingsModelString m_scoreTableProbabilityPrefix = new SettingsModelString("table.score.probability.prefix", DEFAULT_TABLE_SCORE_PROBABILITY_PREFIX);

    /**
     * Constructs default settings.
     */
    public RulesToTableSettings() {
        super();
    }

    /**
     * @return the splitRules
     */
    public SettingsModelBoolean getSplitRules() {
        return m_splitRules;
    }

    /**
     * @return the confidenceAndWeight
     */
    public SettingsModelBoolean getConfidenceAndWeight() {
        return m_confidenceAndWeight;
    }

    /**
     * @return the provideStatistics
     */
    public SettingsModelBoolean getProvideStatistics() {
        return m_provideStatistics;
    }

    /**
     * @return the useAdditionalParentheses
     */
    public SettingsModelBoolean getAdditionalParentheses() {
        return m_useAdditionalParentheses;
    }

    /**
     * @return the scoreTableRecordCount
     */
    public SettingsModelBoolean getScoreTableRecordCount() {
        return m_scoreTableRecordCount;
    }

    /**
     * @return the scoreTableRecordCountPrefix
     */
    public SettingsModelString getScoreTableRecordCountPrefix() {
        return m_scoreTableRecordCountPrefix;
    }

    /**
     * @return the scoreTableProbability
     */
    public SettingsModelBoolean getScoreTableProbability() {
        return m_scoreTableProbability;
    }

    /**
     * @return the scoreTableProbabilityPrefix
     */
    public SettingsModelString getScoreTableProbabilityPrefix() {
        return m_scoreTableProbabilityPrefix;
    }

    /**
     * Loads settings for the model, using {@code settings}.
     *
     * @param settings The {@link NodeSettingsRO}.
     * @throws InvalidSettingsException Something is missing.
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_splitRules.loadSettingsFrom(settings);
        m_confidenceAndWeight.loadSettingsFrom(settings);
        m_provideStatistics.loadSettingsFrom(settings);
        m_useAdditionalParentheses.loadSettingsFrom(settings);
        loadWithDefaults(settings);
    }

    /**
     * Loads settings with defaults (those that were introduced later). Assuming the defaults are the same as previous
     * behaviour.
     *
     * @param settings A {@link NodeSettingsRO}.
     */
    protected void loadWithDefaults(final NodeSettingsRO settings) {
        try {
            //New in 3.2
            m_scoreTableRecordCount.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scoreTableRecordCount.setBooleanValue(DEFAULT_TABLE_SCORE_RECORD_COUNT);
        }
        try {
            //New in 3.2
            m_scoreTableRecordCountPrefix.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scoreTableRecordCountPrefix.setStringValue(DEFAULT_TABLE_SCORE_RECORD_COUNT_PREFIX);
        }
        try {
            //New in 3.2
            m_scoreTableProbability.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scoreTableProbability.setBooleanValue(DEFAULT_TABLE_SCORE_PROBABILITY);
        }
        try {
            //New in 3.2
            m_scoreTableProbabilityPrefix.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scoreTableProbabilityPrefix.setStringValue(DEFAULT_TABLE_SCORE_PROBABILITY_PREFIX);
        }
    }

    /**
     * Loads settings for the dialog, using {@code settings} with defaults.
     *
     * @param settings The {@link NodeSettingsRO}.
     * @param pmmlPort The input PMML port spec.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final PMMLPortObjectSpec pmmlPort) {
        try {
            m_splitRules.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_splitRules.setBooleanValue(DEFAULT_SPLIT_RULES);
        }
        try {
            m_confidenceAndWeight.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_confidenceAndWeight.setBooleanValue(DEFAULT_CONFIDENCE_AND_WEIGHT);
        }
        try {
            m_provideStatistics.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_provideStatistics.setBooleanValue(DEFAULT_STATISTICS);
        }
        try {
            m_useAdditionalParentheses.loadSettingsFrom(settings);
        } catch(InvalidSettingsException e) {
            m_useAdditionalParentheses.setBooleanValue(DEFAULT_ADDITIONAL_PARENTHESES);
        }
        loadWithDefaults(settings);
    }

    /**
     * Saves the current settings state to {@code settings}.
     * @param settings The container to save the settings.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_splitRules.saveSettingsTo(settings);
        m_confidenceAndWeight.saveSettingsTo(settings);
        m_provideStatistics.saveSettingsTo(settings);
        m_useAdditionalParentheses.saveSettingsTo(settings);
        m_scoreTableRecordCount.saveSettingsTo(settings);
        m_scoreTableRecordCountPrefix.saveSettingsTo(settings);
        m_scoreTableProbability.saveSettingsTo(settings);
        m_scoreTableProbabilityPrefix.saveSettingsTo(settings);
    }

}
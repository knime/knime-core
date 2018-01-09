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
 *   2015 m�j. 22 (G�bor): created
 */
package org.knime.base.node.rules.engine.twoports;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod.Criterion;
import org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod.Criterion.Enum;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings used for the Rule Engine (Dictionary) node.
 *
 * @author Gabor Bakos
 */
class RuleEngine2PortsSettings extends RuleEngine2PortsSimpleSettings {
    private static final String APPEND_COLUMN = "append.column";

    private static final String REPLACE_COLUMN = "replace.column";

    private static final String IS_REPLACED_COLUMN = "is.replaced.column";

    private static final String IS_PMML_RULESET = "is.pmml.ruleset";

    private static final String RULE_SELECTION_METHOD = "rule.selection.method";

    private static final String HAS_DEFAULT_SCORE = "has.default.score";

    private static final String DEFAULT_SCORE = "default.score";

    private static final String HAS_DEFAULT_CONFIDENCE = "has.default.confidence";

    private static final String DEFAULT_CONFIDENCE = "default.confidence";

    private static final String CONFIDENCE_COLUMN = "rule.confidence.column";

    private static final String HAS_DEFAULT_WEIGHT = "has.default.weight";

    private static final String DEFAULT_WEIGHT = "default.weight";

    private static final String WEIGHT_COLUMN = "rule.weight.column";

    private static final String COMPUTE_CONFIDENCE = "compute.confidence";

    private static final String PREDICTION_CONFIDENCE_COLUMN = "prediction.confidence.column";

    private static final String PROVIDE_STATISTICS = "provide.statistics";

    private static final String VALIDATE_COLUMN = "validate.column";

    /** The default column name when appending the prediction column */
    static final String DEFAULT_APPEND_COLUMN = "Prediction";

    /** By default append prediction */
    static final boolean DEFAULT_IS_REPLACED_COLUMN = false;

    /** By default do not restrict rules */
    static final boolean DEFAULT_IS_PMML_RULESET = false;

    /** By default {@link RuleSelectionMethod#FirstHit}. */
    static final RuleSelectionMethod DEFAULT_RULE_SELECTION_METHOD = RuleSelectionMethod.FirstHit;

    /** The supported rule selection methods. */
    static final List<RuleSelectionMethod> POSSIBLE_RULE_SELECTION_METHODS = Collections.unmodifiableList(Arrays
        .asList(RuleSelectionMethod.values()));

    /** By default there is no default score (value when nothing matches) */
    static final boolean DEFAULT_HAS_DEFAULT_SCORE = false;

    /** By default an empty string is the default score value in the dialog */
    static final String DEFAULT_DEFAULT_SCORE = "";

    /** By default there is no (enabled) default confidence value */
    static final boolean DEFAULT_HAS_DEFAULT_CONFIDENCE = false;

    /** The default confidence value in the dialog */
    static final double DEFAULT_DEFAULT_CONFIDENCE = 1.0;

    /** By default there is no (enabled) default weight value */
    static final boolean DEFAULT_HAS_DEFAULT_WEIGHT = false;

    /** The default weight value in the dialog */
    static final double DEFAULT_DEFAULT_WEIGHT = 1.0;

    /** By default no confidence is computed */
    static final boolean DEFAULT_COMPUTE_CONFIDENCE = false;

    /** The default confidence column name in the dialog */
    static final String DEFAULT_PREDICTION_CONFIDENCE_COLUMN = "Confidence";

    /** By default provide statistics ({@code recordCount}) */
    static final boolean DEFAULT_IS_PROVIDE_STATISTICS = true;

    private String m_appendColumn = DEFAULT_APPEND_COLUMN;

    private String m_replaceColumn = null;

    private boolean m_isReplaceColumn = DEFAULT_IS_REPLACED_COLUMN;

    private boolean m_isPMMLRuleSet = DEFAULT_IS_PMML_RULESET;

    private RuleSelectionMethod m_ruleSelectionMethod = DEFAULT_RULE_SELECTION_METHOD;

    private boolean m_hasDefaultScore = DEFAULT_HAS_DEFAULT_SCORE;

    private String m_defaultScore = DEFAULT_DEFAULT_SCORE;

    private boolean m_hasDefaultConfidence = DEFAULT_HAS_DEFAULT_CONFIDENCE;

    private double m_defaultConfidence = DEFAULT_DEFAULT_CONFIDENCE;

    private String m_ruleConfidenceColumn = null;

    private boolean m_hasDefaultWeight = DEFAULT_HAS_DEFAULT_WEIGHT;

    private double m_defaultWeight = DEFAULT_DEFAULT_WEIGHT;

    private String m_ruleWeightColumn = null;

    private boolean m_computeConfidence = DEFAULT_COMPUTE_CONFIDENCE;

    private String m_predictionConfidenceColumn = DEFAULT_PREDICTION_CONFIDENCE_COLUMN;

    private boolean m_provideStatistics = DEFAULT_IS_PROVIDE_STATISTICS;

    private String m_validateColumn = null;

    /** An interface to specify the name of the PMML option. */
    interface PMMLName {
        /** @return Name of the PMML option. */
        String getPMMLName();
    }

    /** Possible {@link org.dmg.pmml.RuleSelectionMethodDocument.RuleSelectionMethod}s. */
    enum RuleSelectionMethod implements PMMLName, StringValue {
        /** First matching rule is selected to provide outcome. */
        FirstHit,
        /**From all matching rules group them by their outcome, select the one with the highest weighted sum. */
        WeightedSum,
        /** From all matching rules select the one with the highest weight. */
        WeightedMax;

        /**
         * {@inheritDoc}
         */
        @Override
        public String getStringValue() {
            switch (this) {
                case FirstHit:
                    return "First hit";
                case WeightedMax:
                    return "Highest matching weight";
                case WeightedSum:
                    return "Maximal matching weighted sum";
                default:
                    throw new UnsupportedOperationException("Not supported: " + this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPMMLName() {
            switch (this) {
                case FirstHit:
                    return "firstHit";
                case WeightedMax:
                    return "weightedMax";
                case WeightedSum:
                    return "weightedSum";
                default:
                    throw new UnsupportedOperationException("Not supported: " + this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getStringValue();
        }

        /**
         * @return The {@link Criterion} value for PMML document.
         */
        public Enum asCriterion() {
            return Enum.forString(getPMMLName());
        }
    }

    /**
     * Constructs the default settings.
     */
    public RuleEngine2PortsSettings() {
        super();
    }

    /**
     * @return the appendColumn
     */
    final String getAppendColumn() {
        return m_appendColumn;
    }

    /**
     * @param appendColumn the appendColumn to set
     */
    final void setAppendColumn(final String appendColumn) {
        this.m_appendColumn = appendColumn;
    }

    /**
     * @return the replaceColumn
     */
    final String getReplaceColumn() {
        return m_replaceColumn;
    }

    /**
     * @param replaceColumn the replaceColumn to set
     */
    final void setReplaceColumn(final String replaceColumn) {
        this.m_replaceColumn = replaceColumn;
    }

    /**
     * @return the isReplaceColumn
     */
    final boolean isReplaceColumn() {
        return m_isReplaceColumn;
    }

    /**
     * @param isReplaceColumn the isReplaceColumn to set
     */
    final void setReplaceColumn(final boolean isReplaceColumn) {
        this.m_isReplaceColumn = isReplaceColumn;
    }

    /**
     * @return the isPMMLRuleSet
     */
    final boolean isPMMLRuleSet() {
        return m_isPMMLRuleSet;
    }

    /**
     * @param isPMMLRuleSet the isPMMLRuleSet to set
     */
    final void setPMMLRuleSet(final boolean isPMMLRuleSet) {
        this.m_isPMMLRuleSet = isPMMLRuleSet;
    }

    /**
     * @return the ruleSelectionMethod
     */
    final RuleSelectionMethod getRuleSelectionMethod() {
        return m_ruleSelectionMethod;
    }

    /**
     * @param ruleSelectionMethod the ruleSelectionMethod to set
     */
    final void setRuleSelectionMethod(final RuleSelectionMethod ruleSelectionMethod) {
        this.m_ruleSelectionMethod = ruleSelectionMethod;
    }

    /**
     * @return the hasDefaultScore
     */
    final boolean isHasDefaultScore() {
        return m_hasDefaultScore;
    }

    /**
     * @param hasDefaultScore the hasDefaultScore to set
     */
    final void setHasDefaultScore(final boolean hasDefaultScore) {
        this.m_hasDefaultScore = hasDefaultScore;
    }

    /**
     * @return the defaultScore
     */
    final String getDefaultScore() {
        return m_defaultScore;
    }

    /**
     * @param defaultScore the defaultScore to set
     */
    final void setDefaultScore(final String defaultScore) {
        this.m_defaultScore = defaultScore;
    }

    /**
     * @return the hasDefaultConfidence
     */
    final boolean isHasDefaultConfidence() {
        return m_hasDefaultConfidence;
    }

    /**
     * @param hasDefaultConfidence the hasDefaultConfidence to set
     */
    final void setHasDefaultConfidence(final boolean hasDefaultConfidence) {
        this.m_hasDefaultConfidence = hasDefaultConfidence;
    }

    /**
     * @return the defaultConfidence
     */
    final double getDefaultConfidence() {
        return m_defaultConfidence;
    }

    /**
     * @param defaultConfidence the defaultConfidence to set
     */
    final void setDefaultConfidence(final double defaultConfidence) {
        this.m_defaultConfidence = defaultConfidence;
    }

    /**
     * @return the ruleConfidenceColumn
     */
    final String getRuleConfidenceColumn() {
        return m_ruleConfidenceColumn;
    }

    /**
     * @param ruleConfidenceColumn the ruleConfidenceColumn to set
     */
    final void setRuleConfidenceColumn(final String ruleConfidenceColumn) {
        this.m_ruleConfidenceColumn = ruleConfidenceColumn;
    }

    /**
     * @return the hasDefaultWeight
     */
    final boolean isHasDefaultWeight() {
        return m_hasDefaultWeight;
    }

    /**
     * @param hasDefaultWeight the hasDefaultWeight to set
     */
    final void setHasDefaultWeight(final boolean hasDefaultWeight) {
        this.m_hasDefaultWeight = hasDefaultWeight;
    }

    /**
     * @return the defaultWeight
     */
    final double getDefaultWeight() {
        return m_defaultWeight;
    }

    /**
     * @param defaultWeight the defaultWeight to set
     */
    final void setDefaultWeight(final double defaultWeight) {
        this.m_defaultWeight = defaultWeight;
    }

    /**
     * @return the ruleWeightColumn
     */
    final String getRuleWeightColumn() {
        return m_ruleWeightColumn;
    }

    /**
     * @param ruleWeightColumn the ruleWeightColumn to set
     */
    final void setRuleWeightColumn(final String ruleWeightColumn) {
        this.m_ruleWeightColumn = ruleWeightColumn;
    }

    /**
     * @return the computeConfidence
     */
    final boolean isComputeConfidence() {
        return m_computeConfidence;
    }

    /**
     * @param computeConfidence the computeConfidence to set
     */
    final void setComputeConfidence(final boolean computeConfidence) {
        this.m_computeConfidence = computeConfidence;
    }

    /**
     * @return the predictionConfidenceColumn
     */
    final String getPredictionConfidenceColumn() {
        return m_predictionConfidenceColumn;
    }

    /**
     * @param predictionConfidenceColumn the predictionConfidenceColumn to set
     */
    final void setPredictionConfidenceColumn(final String predictionConfidenceColumn) {
        this.m_predictionConfidenceColumn = predictionConfidenceColumn;
    }

    /**
     * @return the provideStatistics
     */
    final boolean isProvideStatistics() {
        return m_provideStatistics;
    }

    /**
     * @param provideStatistics the provideStatistics to set
     */
    final void setProvideStatistics(final boolean provideStatistics) {
        this.m_provideStatistics = provideStatistics;
    }

    /**
     * @return the validateColumn
     */
    final String getValidateColumn() {
        return m_validateColumn;
    }

    /**
     * @param validateColumn the validateColumn to set
     */
    final void setValidateColumn(final String validateColumn) {
        this.m_validateColumn = validateColumn;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input data spec
     * @param secondSpec Rules table spec
     */
    @Override
    protected void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec, final DataTableSpec secondSpec) {
        super.loadSettingsDialog(settings, inSpec, secondSpec);
        m_appendColumn = settings.getString(APPEND_COLUMN, null);
        String defaultStringColumn = inSpec == null ? null : columnNameOrNull(inSpec, StringValue.class);
        m_replaceColumn = settings.getString(REPLACE_COLUMN, defaultStringColumn);
        m_isReplaceColumn = settings.getBoolean(IS_REPLACED_COLUMN, DEFAULT_IS_REPLACED_COLUMN);
        m_isPMMLRuleSet = settings.getBoolean(IS_PMML_RULESET, DEFAULT_IS_PMML_RULESET);

        m_ruleSelectionMethod =
            RuleSelectionMethod
                .valueOf(settings.getString(RULE_SELECTION_METHOD, DEFAULT_RULE_SELECTION_METHOD.name()));
        m_hasDefaultScore = settings.getBoolean(HAS_DEFAULT_SCORE, DEFAULT_HAS_DEFAULT_SCORE);
        m_defaultScore = settings.getString(DEFAULT_SCORE, DEFAULT_DEFAULT_SCORE);
        m_hasDefaultConfidence = settings.getBoolean(HAS_DEFAULT_CONFIDENCE, DEFAULT_HAS_DEFAULT_CONFIDENCE);
        m_defaultConfidence = settings.getDouble(DEFAULT_CONFIDENCE, DEFAULT_DEFAULT_CONFIDENCE);
        m_ruleConfidenceColumn = settings.getString(CONFIDENCE_COLUMN, columnNameOrNull(secondSpec, DoubleValue.class));
        m_hasDefaultWeight = settings.getBoolean(HAS_DEFAULT_WEIGHT, DEFAULT_HAS_DEFAULT_WEIGHT);
        m_defaultWeight = settings.getDouble(DEFAULT_WEIGHT, DEFAULT_DEFAULT_WEIGHT);
        m_ruleWeightColumn = settings.getString(WEIGHT_COLUMN, columnNameOrNull(secondSpec, DoubleValue.class));
        m_computeConfidence = settings.getBoolean(COMPUTE_CONFIDENCE, DEFAULT_COMPUTE_CONFIDENCE);
        m_predictionConfidenceColumn =
            settings.getString(PREDICTION_CONFIDENCE_COLUMN, DEFAULT_PREDICTION_CONFIDENCE_COLUMN);

        m_provideStatistics = settings.getBoolean(PROVIDE_STATISTICS, DEFAULT_IS_PROVIDE_STATISTICS);
        m_validateColumn = settings.getString(VALIDATE_COLUMN, null);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    @Override
    protected void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsModel(settings);
        m_appendColumn = settings.getString(APPEND_COLUMN);
        m_replaceColumn = settings.getString(REPLACE_COLUMN);
        m_isReplaceColumn = settings.getBoolean(IS_REPLACED_COLUMN);
        m_isPMMLRuleSet = settings.getBoolean(IS_PMML_RULESET);

        m_ruleSelectionMethod = RuleSelectionMethod.valueOf(settings.getString(RULE_SELECTION_METHOD));
        m_hasDefaultScore = settings.getBoolean(HAS_DEFAULT_SCORE);
        m_defaultScore = settings.getString(DEFAULT_SCORE);
        m_hasDefaultConfidence = settings.getBoolean(HAS_DEFAULT_CONFIDENCE);
        m_defaultConfidence = settings.getDouble(DEFAULT_CONFIDENCE);
        m_ruleConfidenceColumn = settings.getString(CONFIDENCE_COLUMN);
        m_hasDefaultWeight = settings.getBoolean(HAS_DEFAULT_WEIGHT);
        m_defaultWeight = settings.getDouble(DEFAULT_WEIGHT);
        m_ruleWeightColumn = settings.getString(WEIGHT_COLUMN);
        m_computeConfidence = settings.getBoolean(COMPUTE_CONFIDENCE);
        m_predictionConfidenceColumn = settings.getString(PREDICTION_CONFIDENCE_COLUMN);

        m_provideStatistics = settings.getBoolean(PROVIDE_STATISTICS);
        m_validateColumn = settings.getString(VALIDATE_COLUMN);

    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addString(APPEND_COLUMN, m_appendColumn);
        settings.addString(REPLACE_COLUMN, m_replaceColumn);
        settings.addBoolean(IS_REPLACED_COLUMN, m_isReplaceColumn);
        settings.addBoolean(IS_PMML_RULESET, m_isPMMLRuleSet);

        settings.addString(RULE_SELECTION_METHOD, m_ruleSelectionMethod.name());
        settings.addBoolean(HAS_DEFAULT_SCORE, m_hasDefaultScore);
        settings.addString(DEFAULT_SCORE, m_defaultScore);
        settings.addBoolean(HAS_DEFAULT_CONFIDENCE, m_hasDefaultConfidence);
        settings.addDouble(DEFAULT_CONFIDENCE, m_defaultConfidence);
        settings.addString(CONFIDENCE_COLUMN, m_ruleConfidenceColumn);
        settings.addBoolean(HAS_DEFAULT_WEIGHT, m_hasDefaultWeight);
        settings.addDouble(DEFAULT_WEIGHT, m_defaultWeight);
        settings.addString(WEIGHT_COLUMN, m_ruleWeightColumn);
        settings.addBoolean(COMPUTE_CONFIDENCE, m_computeConfidence);
        settings.addString(PREDICTION_CONFIDENCE_COLUMN, m_predictionConfidenceColumn);

        settings.addBoolean(PROVIDE_STATISTICS, m_provideStatistics);
        settings.addString(VALIDATE_COLUMN, m_validateColumn);
    }
}

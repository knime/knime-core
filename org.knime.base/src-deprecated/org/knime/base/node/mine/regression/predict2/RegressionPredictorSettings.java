/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   25.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class hold the settings for the General Regression Predictor node.
 * <p>Despite being public no official API.
 * @author Heiko Hofer
 */
public final class RegressionPredictorSettings {
    /** If custom name for the prediction column should be used. */
    private boolean m_hasCustomPredictionName;

    /** The custom prediction name. */
    private String m_customPredictionName;

    /** If probability columns should be added to the output table. */
    private boolean m_includeProbabilities;

    /** The suffix for the probability columns. */
    private String m_propColumnSuffix;

    /**
     * Create instance with default values.
     */
    public RegressionPredictorSettings() {
        m_hasCustomPredictionName = false;
        m_customPredictionName = null;
        m_includeProbabilities = false;
        m_propColumnSuffix = "";
    }

    private static final String CFG_HAS_CUSTOM_PREDICTION_NAME = "has_custom_predicition_name";
    private static final String CFG_CUSTOM_PREDICTION_NAME = "custom_prediction_name";
    private static final String CFG_INCLUDE_PROBABILITIES = "include_probabilites";
    private static final String CFG_PROP_COLUMN_SUFFIX = "propability_columns_suffix";

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_hasCustomPredictionName = settings.getBoolean(CFG_HAS_CUSTOM_PREDICTION_NAME);
        m_customPredictionName = settings.getString(CFG_CUSTOM_PREDICTION_NAME);
        m_includeProbabilities = settings.getBoolean(CFG_INCLUDE_PROBABILITIES);
        m_propColumnSuffix = settings.getString(CFG_PROP_COLUMN_SUFFIX);
    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_hasCustomPredictionName = settings.getBoolean(CFG_HAS_CUSTOM_PREDICTION_NAME, false);
        m_customPredictionName = settings.getString(CFG_CUSTOM_PREDICTION_NAME, null);
        m_includeProbabilities = settings.getBoolean(CFG_INCLUDE_PROBABILITIES, false);
        m_propColumnSuffix = settings.getString(CFG_PROP_COLUMN_SUFFIX, "");
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_HAS_CUSTOM_PREDICTION_NAME, m_hasCustomPredictionName);
        settings.addString(CFG_CUSTOM_PREDICTION_NAME, m_customPredictionName);
        settings.addBoolean(CFG_INCLUDE_PROBABILITIES, m_includeProbabilities);
        settings.addString(CFG_PROP_COLUMN_SUFFIX, m_propColumnSuffix);
    }


    /**
     * @return the includeProbabilities
     */
    public boolean getIncludeProbabilities() {
        return m_includeProbabilities;
    }

    /**
     * @param includeProbabilities the includeProbabilities to set
     */
    public void setIncludeProbabilities(final boolean includeProbabilities) {
        m_includeProbabilities = includeProbabilities;
    }

    /**
     * @return the hasCustomPredictionName
     */
    public boolean getHasCustomPredictionName() {
        return m_hasCustomPredictionName;
    }

    /**
     * @param hasCustomPredictionName the hasCustomPredictionName to set
     */
    public void setHasCustomPredictionName(final boolean hasCustomPredictionName) {
        this.m_hasCustomPredictionName = hasCustomPredictionName;
    }

    /**
     * @return the customPredictionName
     */
    public String getCustomPredictionName() {
        return m_customPredictionName;
    }

    /**
     * @param customPredictionName the customPredictionName to set
     */
    public void setCustomPredictionName(final String customPredictionName) {
        this.m_customPredictionName = customPredictionName;
    }

    /**
     * @return the propColumnSuffix
     */
    public String getPropColumnSuffix() {
        return m_propColumnSuffix;
    }

    /**
     * @param propColumnSuffix the propColumnSuffix to set
     */
    public void setPropColumnSuffix(final String propColumnSuffix) {
        this.m_propColumnSuffix = propColumnSuffix;
    }


}

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
package org.knime.base.node.rules.engine.decisiontree;

import org.knime.base.node.rules.engine.totable.RulesToTableSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * Settings for controlling the transformation of a RuleSet model to a table.
 *
 * @author Gabor Bakos
 */
final class FromDecisionTreeSettings extends RulesToTableSettings {
    /** By default do not generate score distribution for PMML. */
    static final boolean DEFAULT_PMML_SCORE_RECORD_COUNT = false;
    /** By default do not generate score distribution probability for PMML. */
    static final boolean DEFAULT_PMML_SCORE_PROBABILITY = false;
    SettingsModelBoolean m_scorePmmlRecordCount = new SettingsModelBoolean("pmml.score.recordCount", DEFAULT_PMML_SCORE_RECORD_COUNT);
    SettingsModelBoolean m_scorePmmlProbability = new SettingsModelBoolean("pmml.score.probability", DEFAULT_PMML_SCORE_PROBABILITY);
    /**
     * Constructs default settings.
     */
    public FromDecisionTreeSettings() {
    }

    /**
     * @return the scorePmmlRecordCount
     */
    public SettingsModelBoolean getScorePmmlRecordCount() {
        return m_scorePmmlRecordCount;
    }

    /**
     * @return the scorePmmlProbability
     */
    public SettingsModelBoolean getScorePmmlProbability() {
        return m_scorePmmlProbability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadWithDefaults(final NodeSettingsRO settings) {
        try {
            //New in 3.2
            m_scorePmmlRecordCount.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scorePmmlRecordCount.setBooleanValue(DEFAULT_PMML_SCORE_RECORD_COUNT);
        }
        try {
            //New in 3.2
            m_scorePmmlProbability.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_scorePmmlProbability.setBooleanValue(DEFAULT_PMML_SCORE_PROBABILITY);
        }
        super.loadWithDefaults(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        m_scorePmmlRecordCount.saveSettingsTo(settings);
        m_scorePmmlProbability.saveSettingsTo(settings);
    }
}

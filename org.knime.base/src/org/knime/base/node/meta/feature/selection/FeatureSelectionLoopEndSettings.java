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
 *   16.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionLoopEndSettings {
    private static final String CFG_SCORE_VARIABLE = "scoreVariable";
    private static final String CFG_IS_MINIMIZE = "isMinimize";

    private String m_scoreVariableName;
    private boolean m_isMinimize;

    /**
     *
     */
    public FeatureSelectionLoopEndSettings() {
        m_isMinimize = false;
        m_scoreVariableName = "";
    }

    /**
     * @return the name of the score flow variable
     */
    public String getScoreVariableName() {
        return m_scoreVariableName;
    }

    /**
     * @param scoreVariableName the name of the score flow variable
     */
    public void setScoreVariableName(final String scoreVariableName) {
        m_scoreVariableName = scoreVariableName;
    }

    /**
     * @return true if the score should be minimized
     */
    public boolean isMinimize() {
        return m_isMinimize;
    }

    /**
     * @param isMinimize true if the score should be minimized
     */
    public void setIsMinimize(final boolean isMinimize) {
        m_isMinimize = isMinimize;
    }

    /**
     * @param settings the settings to save to
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_SCORE_VARIABLE, m_scoreVariableName);
        settings.addBoolean(CFG_IS_MINIMIZE, m_isMinimize);
    }

    /**
     * Load method for the usage in the NodeModel
     *
     * @param settings the settings to load from
     * @throws InvalidSettingsException thrown if any of the keys is missing in <b>settings</b>
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_scoreVariableName = settings.getString(CFG_SCORE_VARIABLE);
        m_isMinimize = settings.getBoolean(CFG_IS_MINIMIZE);
    }

    /**
     * Load method for the usage in the dialog
     *
     * @param settings the settings to load from
     */
    public void loadInDialog(final NodeSettingsRO settings) {
        m_scoreVariableName = settings.getString(CFG_SCORE_VARIABLE, "");
        m_isMinimize = settings.getBoolean(CFG_IS_MINIMIZE, false);
    }
}

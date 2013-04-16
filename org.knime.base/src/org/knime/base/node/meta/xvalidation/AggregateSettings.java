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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the aggregation node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class AggregateSettings {
    private String m_targetColumn;

    private String m_predictionColumn;

    private boolean m_addFoldId;

    /**
     * Sets if the output table should contain an additional column for each row
     * with the id of the fold in which the result was produced.
     *
     * @param b <code>true</code> if the additional column should be added,
     *            <code>false</code> otherwise
     * @since 2.4
     */
    public void addFoldId(final boolean b) {
        m_addFoldId = b;
    }

    /**
     * Returns if the output table should contain an additional column for each row
     * with the id of the fold in which the result was produced.
     *
     * @return <code>true</code> if the additional column should be added,
     *            <code>false</code> otherwise
     * @since 2.4
     */
    public boolean addFoldId() {
        return m_addFoldId;
    }

    /**
     * Returns the target column containing the real class values.
     *
     * @return a column name
     */
    public String targetColumn() {
        return m_targetColumn;
    }

    /**
     * sets the target column containing the real class values.
     *
     * @param targetCol column name
     */
    public void targetColumn(final String targetCol) {
        m_targetColumn = targetCol;
    }

    /**
     * Returns the prediction column containing the predicted class values.
     *
     * @return a column name
     */
    public String predictionColumn() {
        return m_predictionColumn;
    }

    /**
     * Sets the prediction column containing the predicted class values.
     *
     * @param predictionCol column name
     */
    public void predictionColumn(final String predictionCol) {
        m_predictionColumn = predictionCol;
    }

    /**
     * Saves this object's settings to the given node settings.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("predictionColumn", m_predictionColumn);
        settings.addString("targetColumn", m_targetColumn);
        settings.addBoolean("addFoldId", m_addFoldId);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predictionColumn = settings.getString("predictionColumn");
        m_targetColumn = settings.getString("targetColumn");
        /** @since 2.4 */
        m_addFoldId = settings.getBoolean("addFoldId", false);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_predictionColumn = settings.getString("predictionColumn", null);
        m_targetColumn = settings.getString("targetColumn", null);
        m_addFoldId = settings.getBoolean("addFoldId", false);
    }
}

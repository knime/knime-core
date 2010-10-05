/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class hold the settings for the Logistic Learner Node.
 *
 * @author Heiko Hofer
 */
public class LogRegLearnerSettings {
    /** Key for the included columns, used for dialog settings. */
    static final String CFG_VARIATES = "included_columns";

    /** Key for whether to include all appropriate columns by default. */
    static final String CFG_VARIATES_USE_ALL = "included_columns_use_all";

    /** Key for the target column, used for dialog settings. */
    static final String CFG_TARGET = "target";

    private String m_targetColumn = null;

    private String[] m_includedColumns = null;

    private boolean m_includeAll = true;

    /**
     * The target column which is the dependent variable.
     *
     * @return the targetColumn
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    /**
     * Set the target column which is the dependent variable.
     *
     * @param targetColumn the targetColumn to set
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    /**
     * The included columns which are the independent variables.
     *
     * @return the includedColumns
     */
    public String[] getIncludedColumns() {
        return m_includedColumns;
    }

    /**
     * Set the included columns which are the independent variables.
     *
     * @param includedColumns the includedColumns to set
     */
    public void setIncludedColumns(final String[] includedColumns) {
        m_includedColumns = includedColumns;
    }

    /**
     * Returns true, if all input columns except of the target should be used as
     * independent variables.
     *
     * @return the includeAll
     */
    public boolean getIncludeAll() {
        return m_includeAll;
    }

    /**
     * Pass true, when all input columns except of the target should be used as
     * independent variables.
     *
     * @param includeAll the includeAll to set
     */
    public void setIncludeAll(final boolean includeAll) {
        m_includeAll = includeAll;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColumn = settings.getString(CFG_TARGET);
        m_includedColumns = settings.getStringArray(CFG_VARIATES);
        m_includeAll = settings.getBoolean(CFG_VARIATES_USE_ALL);
    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_targetColumn = settings.getString(CFG_TARGET, null);
        m_includedColumns =
                settings.getStringArray(CFG_VARIATES, new String[0]);
        m_includeAll = settings.getBoolean(CFG_VARIATES_USE_ALL, true);
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_TARGET, m_targetColumn);
        settings.addStringArray(CFG_VARIATES, m_includedColumns);
        settings.addBoolean(CFG_VARIATES_USE_ALL, m_includeAll);
    }
}

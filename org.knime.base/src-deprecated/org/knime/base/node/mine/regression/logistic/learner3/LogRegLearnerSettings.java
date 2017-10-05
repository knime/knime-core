/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner3;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the Logistic Learner Node.
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 * @since 3.1
 */
class LogRegLearnerSettings {
    private static final String CFG_TARGET = "target";
    private static final String CFG_TARGET_REFERENCE_CATEGORY = "target_reference_category";
    private static final String CFG_SORT_TARGET_CATEGORIES = "sort_target_categories";
    private static final String CFG_SORT_INCLUDES_CATEGORIES = "sort_includes_categories";

    private String m_targetColumn;

    /** The selected learning columns configuration. */
    private DataColumnSpecFilterConfiguration m_includedColumns = LogRegLearnerNodeModel.createDCSFilterConfiguration();

    /** The target reference category, if not set it is the last category. */
    private DataCell m_targetReferenceCategory;
    /** True when target categories should be sorted. */
    private boolean m_sortTargetCategories;
    /** True when categories of nominal data in the include list should be sorted. */
    private boolean m_sortIncludesCategories;

    /**
     * Create default settings.
     */
    public LogRegLearnerSettings() {
        m_targetColumn = null;
        m_targetReferenceCategory = null;
        m_sortTargetCategories = true;
        m_sortIncludesCategories = true;
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
        m_includedColumns.loadConfigurationInModel(settings);
        m_targetReferenceCategory = settings.getDataCell(CFG_TARGET_REFERENCE_CATEGORY);
        m_sortTargetCategories = settings.getBoolean(CFG_SORT_TARGET_CATEGORIES);
        m_sortIncludesCategories = settings.getBoolean(CFG_SORT_INCLUDES_CATEGORIES);

    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     * @param inputTableSpec The input table's spec.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec inputTableSpec) {
        m_targetColumn = settings.getString(CFG_TARGET, null);
        m_includedColumns.loadConfigurationInDialog(settings, inputTableSpec);
        m_targetReferenceCategory = settings.getDataCell(CFG_TARGET_REFERENCE_CATEGORY, null);
        m_sortTargetCategories = settings.getBoolean(CFG_SORT_TARGET_CATEGORIES, true);
        m_sortIncludesCategories = settings.getBoolean(CFG_SORT_INCLUDES_CATEGORIES, true);
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_TARGET, m_targetColumn);
        m_includedColumns.saveConfiguration(settings);
        settings.addDataCell(CFG_TARGET_REFERENCE_CATEGORY, m_targetReferenceCategory);
        settings.addBoolean(CFG_SORT_TARGET_CATEGORIES, m_sortTargetCategories);
        settings.addBoolean(CFG_SORT_INCLUDES_CATEGORIES, m_sortIncludesCategories);
    }

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
     * @return the targetReferenceCategory
     */
    public DataCell getTargetReferenceCategory() {
        return m_targetReferenceCategory;
    }


    /**
     * @param targetReferenceCategory the targetReferenceCategory to set
     */
    public void setTargetReferenceCategory(final DataCell targetReferenceCategory) {
        m_targetReferenceCategory = targetReferenceCategory;
    }


    /**
     * @return the sortTargetCategories
     */
    public boolean getSortTargetCategories() {
        return m_sortTargetCategories;
    }


    /**
     * @param sortTargetCategories the sortTargetCategories to set
     */
    public void setSortTargetCategories(final boolean sortTargetCategories) {
        m_sortTargetCategories = sortTargetCategories;
    }


    /**
     * @return the sortIncludesCategories
     */
    public boolean getSortIncludesCategories() {
        return m_sortIncludesCategories;
    }


    /**
     * @param sortIncludesCategories the sortIncludesCategories to set
     */
    public void setSortIncludesCategories(final boolean sortIncludesCategories) {
        m_sortIncludesCategories = sortIncludesCategories;
    }

    /**
     * @return the includedColumns
     */
    public DataColumnSpecFilterConfiguration getIncludedColumns() {
        return m_includedColumns;
    }

    /**
     * @param includedColumns the includedColumns to set
     */
    public void setIncludedColumns(final DataColumnSpecFilterConfiguration includedColumns) {
        m_includedColumns = includedColumns;
    }
}


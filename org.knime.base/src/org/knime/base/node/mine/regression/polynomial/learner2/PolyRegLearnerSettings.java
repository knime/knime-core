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
 */
package org.knime.base.node.mine.regression.polynomial.learner2;

import java.util.Set;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * This class holds the settings for the polynomial regression learner node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.10
 */
public class PolyRegLearnerSettings {
    private int m_degree = 2;

    private int m_maxRowsForView = 10000;

    private String m_targetColumn;

    private final DataColumnSpecFilterConfiguration m_filterConfiguration = new DataColumnSpecFilterConfiguration("column filter", new DataTypeColumnFilter(DoubleValue.class), DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE | NameFilterConfiguration.FILTER_BY_NAMEPATTERN);

    private MissingValueHandling m_missingValueHandling = MissingValueHandling.fail;

    private static final String CFG_MISSING_VALUE_HANDLING = "missing_value_handling";

    /**
     * Returns the maximum degree that polynomial used for regression should
     * have.
     *
     * @return the maximum degree
     */
    public int getDegree() {
        return m_degree;
    }

    /**
     * Returns the name of the target column that holds the dependent variable.
     *
     * @return the target column's name
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings the node settings
     *
     * @throws InvalidSettingsException if one of the settings is missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_degree = settings.getInt("degree");
        m_targetColumn = settings.getString("targetColumn");
        m_maxRowsForView = settings.getInt("maxViewRows");
        boolean includeAll = settings.getBoolean("includeAll", false); // added v2.1
        // added in 2.10
        m_missingValueHandling =
            MissingValueHandling.valueOf(settings.getString(CFG_MISSING_VALUE_HANDLING,
                MissingValueHandling.ignore.name()));

        if (settings.containsKey("selectedColumns")) {
            //removed in 2.10
            String[] included = settings.getStringArray("selectedColumns");
            //we were able to load the old settings
            String[] excluded = new String[0];
            //but convert to new:
            NodeSettings fakeSettings =
                    createFakeSettings(m_filterConfiguration.getConfigRootName(), included, excluded, includeAll);
            //added in 2.10
            m_filterConfiguration.loadConfigurationInModel(fakeSettings);
        } else {
            //no previous config: we should use the new config
            //added in 2.10
            NodeSettingsRO filterSettings = settings.getNodeSettings(m_filterConfiguration.getConfigRootName());
            NodeSettingsRO dtSettings = filterSettings.getNodeSettings("datatype");
            NodeSettingsRO tlSettings = dtSettings.getNodeSettings("typelist");
            if (!tlSettings.keySet().contains(DoubleValue.class.getName())) {
                NodeSettings fakeSettings =
                    createFakeSettings(m_filterConfiguration.getConfigRootName(), new String[0], new String[0],
                        false);
                m_filterConfiguration.loadConfigurationInModel(fakeSettings);
            } else {
                m_filterConfiguration.loadConfigurationInModel(settings);
            }
        }
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings the node settings
     * @param spec The {@link DataTableSpec} of input table.
     *
     * @throws InvalidSettingsException if one of the settings is missing
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
            throws InvalidSettingsException {
        m_degree = settings.getInt("degree");
        m_targetColumn = settings.getString("targetColumn");
        m_maxRowsForView = settings.getInt("maxViewRows");
        // added in 2.10
        m_missingValueHandling =
            MissingValueHandling.valueOf(settings.getString(CFG_MISSING_VALUE_HANDLING,
                MissingValueHandling.ignore.name()));
        //added in 2.10, replacing includeAll and column names
        m_filterConfiguration.loadConfigurationInDialog(settings, spec);
    }

    /**
     * Creates a fake configuration to help migrating the old configuration.
     *
     * @param configName Name of the {@link NodeSettings} to create.
     * @param included The included column names.
     * @param excluded The excluded column names.
     * @param includeAll Should we include all columns that are not in the exclude list ({@code true}), or use the include list ({@code false})?
     * @return The fake {@link NodeSettings}.
     */
    static NodeSettings createFakeSettings(final String configName, final String[] included, final String[] excluded, final boolean includeAll) {
        NodeSettings fakeSettings = new NodeSettings("FakeRoot");
        NodeSettings filterSettings = (NodeSettings)fakeSettings.addNodeSettings(configName);
        filterSettings.addString("filter-type", "STANDARD");
        filterSettings.addStringArray("included_names", included);
        filterSettings.addStringArray("excluded_names", excluded);
        filterSettings.addString("enforce_option", (includeAll ? EnforceOption.EnforceExclusion : EnforceOption.EnforceInclusion).name());
        NodeSettings datatypeSettings = (NodeSettings)filterSettings.addNodeSettings("datatype");
        NodeSettingsWO typelist = datatypeSettings.addNodeSettings("typelist");
        typelist.addBoolean(DoubleValue.class.getName(), true);
        return fakeSettings;
    }

    /**
     * Saves the settings to the node settings object.
     *
     * @param settings the node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_targetColumn != null) {
            settings.addInt("degree", m_degree);
            settings.addString("targetColumn", m_targetColumn);
            settings.addInt("maxViewRows", m_maxRowsForView);
            settings.addString(CFG_MISSING_VALUE_HANDLING, m_missingValueHandling.name());
            m_filterConfiguration.saveConfiguration(settings);
        }
    }

    /**
     * Sets the maximum degree that polynomial used for regression should have.
     *
     * @param degree the maximum degree
     */
    public void setDegree(final int degree) {
        m_degree = degree;
    }

    /**
     * Sets the name of the target column that holds the dependent variable.
     *
     * @param targetColumn the target column's name
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    /**
     * Returns the maximum number of rows that are shown in the curve view.
     *
     * @return the maximum number of rows
     */
    public int getMaxRowsForView() {
        return m_maxRowsForView;
    }

    /**
     * Sets the maximum number of rows that are shown in the curve view.
     *
     * @param maxRowsForView the maximum number of rows
     */
    public void setMaxRowsForView(final int maxRowsForView) {
        m_maxRowsForView = maxRowsForView;
    }

    /**
     * Sets the names of the columns that should be used for the regression. The
     * target column name must not be among these columns!
     *
     * @param columnNames a set with the selected column names
     * @deprecated use {@link #getFilterConfiguration()}
     */
    @Deprecated
    public void setSelectedColumns(final Set<String> columnNames) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an (unmodifiable) set of the select column names.
     *
     * @return a set with the selected column names
     * @deprecated use {@link #getFilterConfiguration()}
     */
    @Deprecated
    public Set<String> getSelectedColumns() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the includeAll
     * @deprecated use {@link #getFilterConfiguration()}
     */
    @Deprecated
    public boolean isIncludeAll() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param includeAll the includeAll to set
     * @deprecated Use {@link #getFilterConfiguration()}
     */
    @Deprecated
    public void setIncludeAll(final boolean includeAll) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the missingValueHandling
     */
    public MissingValueHandling getMissingValueHandling() {
        return m_missingValueHandling;
    }

    /**
     * @param missingValueHandling the missingValueHandling to set
     */
    public void setMissingValueHandling(final MissingValueHandling missingValueHandling) {
        this.m_missingValueHandling = missingValueHandling;
    }

    /**
     * @return the filterConfiguration
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }
}

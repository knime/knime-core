/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * This class hold the settings for the Linear Learner Node.
 *
 * @author Heiko Hofer
 */
final class LinReg2LearnerSettings {
    private String m_targetColumn = null;

    private DataColumnSpecFilterConfiguration m_columnFilter;

    /** False when regression should go through the origin. */
    private boolean m_includeConstant;

    /** offset value (a user defined intercept). */
    private double m_offsetValue;

    /** first row used in scatter plot view. */
    private int m_scatterPlotFirstRow;

    /** row count used for scatter plot view. */
    private int m_scatterPlotRowCount;

    /** method how to deal with missing values in the input data. */
    private org.knime.base.node.mine.regression.MissingValueHandling m_missingValueHandling;

    /**
     * This enum holds all ways of handling missing values in the input table.
     *
     * @author Heiko Hofer
     * @deprecated Use {@link org.knime.base.node.mine.regression.MissingValueHandling} instead.
     */
    @Deprecated
    public enum MissingValueHandling {
        /** Ignore rows with missing values. */
        ignore,
        /** Fail when observing a missing value. */
        fail;
    }

    /**
     * Create a new instance.
     */
    @SuppressWarnings("unchecked")
    public LinReg2LearnerSettings() {
        m_columnFilter =
            new DataColumnSpecFilterConfiguration(CFG_COLUMN_FILTER, new DataTypeColumnFilter(DoubleValue.class,
                NominalValue.class));
        m_includeConstant = true;
        m_offsetValue = 0;
        m_scatterPlotFirstRow = 1;
        m_scatterPlotRowCount = 20000;
        m_missingValueHandling = org.knime.base.node.mine.regression.MissingValueHandling.fail;
    }

    private static final String CFG_TARGET = "target";
    private static final String CFG_COLUMN_FILTER = "column_filter";
    private static final String CFG_INCLUDE_CONSTANT = "include_constant";
    private static final String CFG_OFFSET_VALUE = "offset_value";
    private static final String CFG_SCATTER_PLOT_FIRST_ROW = "scatter_plot_first_row";
    private static final String CFG_SCATTER_PLOT_ROW_COUNT = "scatter_plot_row_count";
    private static final String CFG_MISSING_VALUE_HANDLING = "missing_value_handling";

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_targetColumn = settings.getString(CFG_TARGET);
        m_columnFilter.loadConfigurationInModel(settings);
        m_includeConstant = settings.getBoolean(CFG_INCLUDE_CONSTANT);
        m_offsetValue = settings.getDouble(CFG_OFFSET_VALUE);
        m_missingValueHandling = org.knime.base.node.mine.regression.MissingValueHandling.valueOf(settings.getString(CFG_MISSING_VALUE_HANDLING));
        // use default if not present (settings are only used in the scatter plot view).
        m_scatterPlotFirstRow = settings.getInt(CFG_SCATTER_PLOT_FIRST_ROW, 1);
        m_scatterPlotRowCount = settings.getInt(CFG_SCATTER_PLOT_ROW_COUNT, 20000);
    }

    /**
     * Loads the settings from the node settings object using default values if some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the spec of the input table
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_targetColumn = settings.getString(CFG_TARGET, null);
        m_columnFilter.loadConfigurationInDialog(settings, spec);
        m_includeConstant = settings.getBoolean(CFG_INCLUDE_CONSTANT, true);
        m_offsetValue = settings.getDouble(CFG_OFFSET_VALUE, 0.0);
        m_missingValueHandling = org.knime.base.node.mine.regression.MissingValueHandling.valueOf(
            settings.getString(CFG_MISSING_VALUE_HANDLING, org.knime.base.node.mine.regression.MissingValueHandling.ignore.toString()));
        m_scatterPlotFirstRow = settings.getInt(CFG_SCATTER_PLOT_FIRST_ROW, 1);
        m_scatterPlotRowCount = settings.getInt(CFG_SCATTER_PLOT_ROW_COUNT, 20000);
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_TARGET, m_targetColumn);
        m_columnFilter.saveConfiguration(settings);
        settings.addBoolean(CFG_INCLUDE_CONSTANT, m_includeConstant);
        settings.addDouble(CFG_OFFSET_VALUE, m_offsetValue);
        settings.addString(CFG_MISSING_VALUE_HANDLING, m_missingValueHandling.toString());
        settings.addInt(CFG_SCATTER_PLOT_FIRST_ROW, m_scatterPlotFirstRow);
        settings.addInt(CFG_SCATTER_PLOT_ROW_COUNT, m_scatterPlotRowCount);
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
     * Get filter for included columns (independent variables).
     *
     * @return the included columns
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_columnFilter;
    }

    /**
     * Returns true when the constant term (intercept) should be estimated.
     *
     * @return the include constant property
     */
    public boolean getIncludeConstant() {
        return m_includeConstant;
    }

    /**
     * Defines if the constant term (intercept) should be estimated.
     *
     * @param includeConstant the include constant property
     */
    public void setIncludeConstant(final boolean includeConstant) {
        m_includeConstant = includeConstant;
    }

    /**
     * Get offset value (a user defined intercept).
     *
     * @return offset value (a user defined intercept)
     */
    public double getOffsetValue() {
        return m_offsetValue;
    }

    /**
     * Set offset value (a user defined intercept).
     *
     * @param offsetValue offset value (a user defined intercept)
     */
    public void setOffsetValue(final double offsetValue) {
        m_offsetValue = offsetValue;
    }

    /**
     * @return the scatterPlotFirstRow
     */
    public int getScatterPlotFirstRow() {
        return m_scatterPlotFirstRow;
    }

    /**
     * @param scatterPlotFirstRow the scatterPlotFirstRow to set
     */
    public void setScatterPlotFirstRow(final int scatterPlotFirstRow) {
        this.m_scatterPlotFirstRow = scatterPlotFirstRow;
    }

    /**
     * @return the scatterPlotRowCount
     */
    public int getScatterPlotRowCount() {
        return m_scatterPlotRowCount;
    }

    /**
     * @param scatterPlotRowCount the scatterPlotRowCount to set
     */
    public void setScatterPlotRowCount(final int scatterPlotRowCount) {
        this.m_scatterPlotRowCount = scatterPlotRowCount;
    }

    /**
     * @return the missingValueHandling
     */
    @Deprecated
    public MissingValueHandling getMissingValueHandling() {
        return MissingValueHandling.values()[m_missingValueHandling.ordinal()];
    }

    /**
     * @param missingValueHandling the missingValueHandling to set
     */
    @Deprecated
    public void setMissingValueHandling(final MissingValueHandling missingValueHandling) {
        this.m_missingValueHandling = org.knime.base.node.mine.regression.MissingValueHandling.values()[missingValueHandling.ordinal()];
    }

    /**
     * @return the missingValueHandling
     */
    public org.knime.base.node.mine.regression.MissingValueHandling getMissingValueHandling2() {
        return m_missingValueHandling;
    }

    /**
     * @param missingValueHandling the missingValueHandling to set
     */
    public void setMissingValueHandling(final org.knime.base.node.mine.regression.MissingValueHandling missingValueHandling) {
        this.m_missingValueHandling = missingValueHandling;
    }


}

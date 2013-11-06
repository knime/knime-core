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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

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


    /**
     * Create a new instance.
     */
    public LinReg2LearnerSettings() {
        m_columnFilter = new DataColumnSpecFilterConfiguration(CFG_COLUMN_FILTER);
        m_includeConstant = true;
        m_offsetValue = 0;
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

    private static final String CFG_TARGET = "target";
    private static final String CFG_COLUMN_FILTER = "column_filter";
    private static final String CFG_INCLUDE_CONSTANT = "include_constant";
    private static final String CFG_OFFSET_VALUE = "offset_value";

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColumn = settings.getString(CFG_TARGET);
        m_columnFilter.loadConfigurationInModel(settings);
        m_includeConstant = settings.getBoolean(CFG_INCLUDE_CONSTANT);
        m_offsetValue = settings.getDouble(CFG_OFFSET_VALUE);
    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the spec of the input table
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_targetColumn = settings.getString(CFG_TARGET, null);
        m_columnFilter.loadConfigurationInDialog(settings, spec);
        m_includeConstant = settings.getBoolean(CFG_INCLUDE_CONSTANT, true);
        m_offsetValue = settings.getDouble(CFG_OFFSET_VALUE, 0.0);
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
    }

    /**
     * Get filter for included columns (independent variables).
     * @return the included columns
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_columnFilter;
    }

    /**
     * Returns true when the constant term (intercept) should be estimated.
     * @return the include constant property
     */
    public boolean getIncludeConstant() {
        return m_includeConstant;
    }

    /**
     * Defines if the constant term (intercept) should be estimated.
     * @param includeConstant the include constant property
     */
    public void setIncludeConstant(final boolean includeConstant) {
        m_includeConstant = includeConstant;
    }

    /**
     * Get offset value (a user defined intercept).
     * @return offset value (a user defined intercept)
     */
    public double getOffsetValue()  {
        return m_offsetValue;
    }

    /**
     * Set offset value (a user defined intercept).
     * @param offsetValue offset value (a user defined intercept)
     */
    public void setOffsetValue(final double offsetValue) {
        m_offsetValue = offsetValue;
    }

}

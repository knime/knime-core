/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation.function.column;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;

/**
 * Abstract class that allows the user to select a column from the input table.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public abstract class AbstractColumnDBAggregationFunction implements DBAggregationFunction {

    private ColumnFuntionSettingsPanel m_settingsPanel;
    private final ColumnFuntionSettings m_settings;
    private final String m_label;
    private final Class<? extends DataValue>[] m_classFilter;

    /**
     * @param label the label of the column option
     * @param defaultColName the column name to select as default or <code>null</code> for none
     *@param classFilter which classes are available for selection
     */
    @SafeVarargs
    protected AbstractColumnDBAggregationFunction(final String label, final String defaultColName,
        final Class<? extends DataValue>... classFilter) {
        m_label = label;
        m_classFilter = classFilter;
        m_settings = new ColumnFuntionSettings(defaultColName);
    }

    /**
     * {@inheritDoc}
     * @since 3.1
     */
    @Override
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName,
        final String columnName) {
        return getLabel() + "(" + manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(columnName)
                + ", " + manipulator.quoteIdentifier(tableName) + "."
                + manipulator.quoteIdentifier(getSelectedColumnName()) + ")";
    }

    /**
     * {@inheritDoc}
     * @since 3.1
     */
    @Override
    public String getSQLFragment4SubQuery(final StatementManipulator manipulator, final String tableName, final String subQuery) {
        return getLabel() + "((" + subQuery + "), " + manipulator.quoteIdentifier(tableName) + "."
                + manipulator.quoteIdentifier(getSelectedColumnName()) + ")";
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        return getLabel() + "_" + getSelectedColumnName();
    }

    /**
     * @return the selected second column name if available otherwise it returns <code>null</code>
     */
    protected String getSelectedColumnName() {
        return m_settings.getColumnName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnFuntionSettingsPanel getSettingsPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new ColumnFuntionSettingsPanel(m_settings, m_label, m_classFilter);
        }
        return m_settingsPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        getSettingsPanel().loadSettingsFrom(settings, spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        m_settings.validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        if (!spec.containsName(getSelectedColumnName())) {
            throw new InvalidSettingsException("Column '" + getSelectedColumnName() + "' not found in input table.");
        }
    }
}
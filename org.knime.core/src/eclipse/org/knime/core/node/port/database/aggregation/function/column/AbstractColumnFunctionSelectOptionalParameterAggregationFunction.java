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
 *   Jan 3, 2017 (oole): created
 */
package org.knime.core.node.port.database.aggregation.function.column;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.database.aggregation.function.parameter.StringParameterSettings;
import org.knime.core.node.port.database.aggregation.function.parameter.StringParameterSettingsPanel;

/**
 *
 * Abstract class on top of the {@link AbstractColumnFunctionSelectDBAggregationFunction} class. Allowing to set a parameter after selecting
 * another column. (Often used for statistics in databases such as Oracle.) And also allows entering a string parameter.
 * @author Ole Ostergaard, KNIME.com
 * @since 3.3
 */
public abstract class  AbstractColumnFunctionSelectOptionalParameterAggregationFunction extends AbstractColumnFunctionSelectDBAggregationFunction {
    private StringParameterSettingsPanel m_stringParameterSettingsPanel;
    private StringParameterSettings m_stringParameterSettings;
    private String m_stringParameterLabel;

    /**
     * @param selectionLabel The label for the parameter selection
     * @param defaultSelection The default selection parameter selection
     * @param functions a {@link List} of function parameters for the Aggregation Method
     * @param colLabel The label for the column selection
     * @param defaultColName The default selection for the column selection
     * @param classFilter Which classes are available for selection
     * @param stringParameterLabel The label for the parameter field
     * @param stringParameterDefault The default parameter for the parameter
     */
    @SafeVarargs
    protected AbstractColumnFunctionSelectOptionalParameterAggregationFunction(final String selectionLabel, final String defaultSelection,
        final List<String> functions, final String stringParameterLabel, final String stringParameterDefault, final String colLabel,
        final String defaultColName, final Class<? extends DataValue>... classFilter) {
        super(selectionLabel, defaultSelection, functions, colLabel, defaultColName, classFilter);

        m_stringParameterLabel = stringParameterLabel;
        m_stringParameterSettings = new StringParameterSettings(stringParameterDefault);
    }

    /**
     * @return Returns the String parameter from the StringParameterSettings
     */
    public String getStringParameter() {
        return m_stringParameterSettings.getParameter();
    }

    @Override
    public String getExtraFragment() {
        String extraFragment;
        extraFragment = super.getExtraFragment();
        if (!getStringParameter().isEmpty()) {
            extraFragment += ", '" + getStringParameter() + "'";
        }
        return extraFragment;
    }

    @Override
    public ColumnFuntionSettingsPanel getSettingsPanel() {
        ColumnFuntionSettingsPanel settingsPanel = super.getSettingsPanel();
        verifySettingsPanel();
        settingsPanel.add(m_stringParameterSettingsPanel);
        return settingsPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettings(settings);
        m_stringParameterSettings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        super.loadSettingsFrom(settings, spec);
        verifySettingsPanel();
        m_stringParameterSettingsPanel.loadSettingsFrom(settings, spec);
    }

    /**
     *
     */
    private void verifySettingsPanel() {
        if (m_stringParameterSettingsPanel == null) {
            m_stringParameterSettingsPanel = new StringParameterSettingsPanel(m_stringParameterSettings, m_stringParameterLabel);
        }
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_stringParameterSettings.saveSettingsTo(settings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_stringParameterSettings.validateSettings(settings);
    }
}

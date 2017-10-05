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
 * ---------------------------------------------------------------------
 *
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnominal.dic;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * Config proxy of node.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainDicConfiguration {

    /**
     *
     */
    private static final int DEFAULT_MAX_DOMAIN_VALUES = 1000;

    private static final String IGNORE_NOT_PRESENT_COLS = "ignore-not-present-col";

    private static final String IGNORE_NOT_MATHING_TYPES = "ignore-not-matching-types";

    private static final String NEW_DOMAIN_VALUES_FIRST = "new-domain-values-first";

    private static final String MAX_DOMAIN_VALUES = "maximal-domain-values";

    private static final String DATA_COLUMN_FILTER_SPEC_KEY = "datacolfilter";

    private boolean m_ignoreDomainColumns = false;

    private boolean m_ignoreWrongTypes = false;

    private boolean m_addNewValuesFirst = true;

    private int m_maxDomainValues = 1000;

    @SuppressWarnings("unchecked")
    private final DataColumnSpecFilterConfiguration m_filterConfiguration = new DataColumnSpecFilterConfiguration(
        DATA_COLUMN_FILTER_SPEC_KEY, new DataTypeColumnFilter(NominalValue.class));

    /** Sets default in the filter configuration: Columns in common (name & type) go to the include list, others to
     * the exclude list.
     * @param origSpec ...
     * @param valueSpec ...
     */
    void guessDefaultColumnFilter(final DataTableSpec origSpec, final DataTableSpec valueSpec) {
        // includes all column which are present in both DTS; also have to have the same type.
        InputFilter<DataColumnSpec> defaultGuessFilter = new InputFilter<DataColumnSpec>() {
            @Override
            public boolean include(final DataColumnSpec valueColumnSpec) {
                DataColumnSpec columnSpec = origSpec.getColumnSpec(valueColumnSpec.getName());
                return columnSpec == null ? false : columnSpec.getType().equals(valueColumnSpec.getType());
            }
        };
        m_filterConfiguration.loadDefault(valueSpec, defaultGuessFilter, true);
    }

    /**
     * Loads the configuration for the dialog with corresponding default values.
     *
     * @param settings the settings to load
     * @param origSpec ...
     * @param valueSpec the table specification
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec origSpec,
        final DataTableSpec valueSpec) {
        m_ignoreDomainColumns = settings.getBoolean(IGNORE_NOT_PRESENT_COLS, false);
        m_ignoreWrongTypes = settings.getBoolean(IGNORE_NOT_MATHING_TYPES, false);
        m_addNewValuesFirst = settings.getBoolean(NEW_DOMAIN_VALUES_FIRST, true);
        m_maxDomainValues = settings.getInt(MAX_DOMAIN_VALUES, DEFAULT_MAX_DOMAIN_VALUES);
        guessDefaultColumnFilter(origSpec, valueSpec);
        m_filterConfiguration.loadConfigurationInDialog(settings, valueSpec);
    }

    /**
     * Loads the configuration for the model.
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreDomainColumns = settings.getBoolean(IGNORE_NOT_PRESENT_COLS);
        m_ignoreWrongTypes = settings.getBoolean(IGNORE_NOT_MATHING_TYPES);
        m_addNewValuesFirst = settings.getBoolean(NEW_DOMAIN_VALUES_FIRST);
        m_maxDomainValues = settings.getInt(MAX_DOMAIN_VALUES);

        m_filterConfiguration.loadConfigurationInModel(settings);
    }

    /**
     * Called from dialog's and model's save method.
     *
     * @param settings Arg settings.
     */
    void saveConfiguration(final NodeSettingsWO settings) {
        settings.addBoolean(IGNORE_NOT_PRESENT_COLS, m_ignoreDomainColumns);
        settings.addBoolean(IGNORE_NOT_MATHING_TYPES, m_ignoreWrongTypes);
        settings.addBoolean(NEW_DOMAIN_VALUES_FIRST, m_addNewValuesFirst);
        settings.addInt(MAX_DOMAIN_VALUES, m_maxDomainValues);
        m_filterConfiguration.saveConfiguration(settings);
    }

    /**
     * @return the ignoreDomainColumns
     */
    boolean isIgnoreDomainColumns() {
        return m_ignoreDomainColumns;
    }

    /**
     * @return the ignoreWrongTypes
     */
    boolean isIgnoreWrongTypes() {
        return m_ignoreWrongTypes;
    }

    /**
     * @return the newValuesFirst
     */
    boolean isAddNewValuesFirst() {
        return m_addNewValuesFirst;
    }

    /**
     * @return the maxDomainValues
     */
    int getMaxDomainValues() {
        return m_maxDomainValues;
    }

    /**
     * @param newValuesFirst the newValuesFirst to set
     */
    void setNewValuesFirst(final boolean newValuesFirst) {
        m_addNewValuesFirst = newValuesFirst;
    }

    /**
     * @param ignoreDomainColumns the ignoreDomainColumns to set
     */
    void setIgnoreDomainColumns(final boolean ignoreDomainColumns) {
        m_ignoreDomainColumns = ignoreDomainColumns;
    }

    /**
     * @param ignoreWrongTypes the ignoreWrongTypes to set
     */
    void setIgnoreWrongTypes(final boolean ignoreWrongTypes) {
        m_ignoreWrongTypes = ignoreWrongTypes;
    }

    /**
     * @param maxDomainValues the maxDomainValues to set
     */
    void setMaxDomainValues(final int maxDomainValues) {
        m_maxDomainValues = maxDomainValues;
    }

    /**
     * @return the filterConfiguration (not null)
     */
    DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }

}

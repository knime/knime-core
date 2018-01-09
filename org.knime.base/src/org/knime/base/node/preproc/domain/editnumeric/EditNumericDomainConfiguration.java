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
 * ---------------------------------------------------------------------
 *
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnumeric;

import org.knime.base.node.preproc.domain.editnumeric.EditNumericDomainNodeModel.DomainOverflowPolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Config proxy of node.
 *
 * @author Marcel Hanser
 */
final class EditNumericDomainConfiguration {

    private static final String DOMAIN_OVERFLOW_POLICY_KEY = "out-of-domain-policy";

    private static final String MIN_KEY = "lowerBound";

    private static final String MAX_KEY = "upperBound";

    private static final String DATA_COLUMN_FILTER_SPEC_KEY = "datacolfilter";

    private double m_lowerBound = 0;

    private double m_upperBound = 1;

    private DomainOverflowPolicy m_domainOverflowPolicy = DomainOverflowPolicy.THROW_EXCEPTION;

    private DataColumnSpecFilterConfiguration m_columnspecFilterConfig;

    /**
     * Constructor.
     */
    public EditNumericDomainConfiguration() {
    }

    /**
     * Loads the configuration for the dialog with corresponding default values.
     *
     * @param settings the settings to load
     * @param spec the table specification
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {

        m_lowerBound = settings.getDouble(MIN_KEY, Double.NaN);
        m_upperBound = settings.getDouble(MAX_KEY, Double.NaN);

        m_domainOverflowPolicy =
            toDomainHandler(settings.getString(DOMAIN_OVERFLOW_POLICY_KEY,
                DomainOverflowPolicy.THROW_EXCEPTION.toString()));

        DataColumnSpecFilterConfiguration dataColumnSpecFilterConfiguration = createDoubleSpecFilterConfig();
        dataColumnSpecFilterConfiguration.loadConfigurationInDialog(settings, spec);
        m_columnspecFilterConfig = dataColumnSpecFilterConfiguration;

    }

    /**
     * Loads the configuration for the model.
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_lowerBound = settings.getDouble(MIN_KEY);
        m_upperBound = settings.getDouble(MAX_KEY);

        m_domainOverflowPolicy = toDomainHandler(settings.getString(DOMAIN_OVERFLOW_POLICY_KEY));

        DataColumnSpecFilterConfiguration dataColumnSpecFilterConfiguration = createDoubleSpecFilterConfig();
        dataColumnSpecFilterConfiguration.loadConfigurationInModel(settings);
        m_columnspecFilterConfig = dataColumnSpecFilterConfiguration;
    }

    /**
     * Determines the handler corresponding to the given name or returns {@link DomainOverflowPolicy#THROW_EXCEPTION} if
     * the name is <code>null</code> or unknown.
     *
     * @param name the name
     * @return the handler corresponding to the given name
     */
    private static DomainOverflowPolicy toDomainHandler(final String name) {
        try {
            return DomainOverflowPolicy.valueOf(name);
        } catch (Exception e) {
            return DomainOverflowPolicy.THROW_EXCEPTION;
        }
    }

    /**
     * @return
     */
    private DataColumnSpecFilterConfiguration createDoubleSpecFilterConfig() {
        return new DataColumnSpecFilterConfiguration(DATA_COLUMN_FILTER_SPEC_KEY, new InputFilter<DataColumnSpec>() {
            @Override
            public boolean include(final DataColumnSpec spec) {
                return LongCell.TYPE.equals(spec.getType()) || IntCell.TYPE.equals(spec.getType())
                    || DoubleCell.TYPE.equals(spec.getType());
            }
        });
    }

    /**
     * Called from dialog's and model's save method.
     *
     * @param settings Arg settings.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addDouble(MIN_KEY, m_lowerBound);
        settings.addDouble(MAX_KEY, m_upperBound);
        settings.addString(
            DOMAIN_OVERFLOW_POLICY_KEY,
            m_domainOverflowPolicy != null ? m_domainOverflowPolicy.toString() : DomainOverflowPolicy.THROW_EXCEPTION
                .toString());
        if (m_columnspecFilterConfig != null) {
            m_columnspecFilterConfig.saveConfiguration(settings);
        }
    }

    /**
     * @return the m_min
     */
    double getLowerBound() {
        return m_lowerBound;
    }

    /**
     * @return the m_max
     */
    double getUpperBound() {
        return m_upperBound;
    }

    /**
     * Sets the minimum.
     *
     * @param min the lower bound
     */
    void setMin(final double min) {
        m_lowerBound = min;
    }

    /**
     * Sets the maximum.
     *
     * @param max the upper bound
     */
    void setMax(final double max) {
        m_upperBound = max;
    }

    /**
     * @return the m_domainOverflowHandler
     */
    DomainOverflowPolicy getDomainOverflowPolicy() {
        return m_domainOverflowPolicy;
    }

    /**
     * @param handler the overflow handler
     */
    void setDomainOverflowPolicy(final DomainOverflowPolicy handler) {
        m_domainOverflowPolicy = handler;
    }

    /**
     * @return the m_columnspecFilterConfig
     */
    DataColumnSpecFilterConfiguration getColumnspecFilterConfig() {
        return m_columnspecFilterConfig;
    }

    /**
     * @param panel the DataColumnSpecFilterPanel to store the configuration from
     */
    void setColumnspecFilterCofig(final DataColumnSpecFilterPanel panel) {
        DataColumnSpecFilterConfiguration dataColumnSpecFilterConfiguration = createDoubleSpecFilterConfig();
        panel.saveConfiguration(dataColumnSpecFilterConfiguration);
        m_columnspecFilterConfig = dataColumnSpecFilterConfiguration;
    }

}

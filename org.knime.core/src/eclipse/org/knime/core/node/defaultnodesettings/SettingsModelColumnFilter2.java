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
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * Settings model for the column filter component {@link DialogComponentColumnFilter2}. It provides methods to
 * apply the settings to a current table spec.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public final class SettingsModelColumnFilter2 extends SettingsModel {

    private DataColumnSpecFilterConfiguration m_filterConfiguration;

    private int m_inputPortIndex = -1;

    /**
     * Accepts columns of all type. The dialog component to this settings model will also show a name pattern &amp;
     * type filter as no restrictions on the possible value classes is set.
     * @param configName the root config name.
     */
    @SuppressWarnings("unchecked")
    public SettingsModelColumnFilter2(final String configName) {
        this(new DataColumnSpecFilterConfiguration(configName));
    }

    /**
     * Accepts only columns of the specified type(s). The dialog component to this settings model will
     * show a name pattern filter but no type filter.
     * @param configName the root config name
     * @param allowedTypes The allowed data types
     */
    public SettingsModelColumnFilter2(final String configName, final Class<? extends DataValue>... allowedTypes) {
        this(new DataColumnSpecFilterConfiguration(configName, new DataTypeColumnFilter(allowedTypes)));
    }

    /**
     * Accepts only columns of the specified type(s). The dialog component to this settings model will
     * show a name pattern filter and type filter. This is useful if you want to restrict the initial selection to specific
     * types, but the user should still be able to further restrict the selection by type.
     *
     * @param configName the root config name
     * @param enableTypeFilter whether to enable the type filter in the dialog component
     * @param allowedTypes The allowed data types
     * @since 5.7
     */
    public SettingsModelColumnFilter2(final String configName, final boolean enableTypeFilter, final Class<? extends DataValue>... allowedTypes) {
        this(new DataColumnSpecFilterConfiguration(configName, new DataTypeColumnFilter(allowedTypes),
            enableTypeFilter
                ? DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE | NameFilterConfiguration.FILTER_BY_NAMEPATTERN
                : NameFilterConfiguration.FILTER_BY_NAMEPATTERN));
    }

    /**
     * Accepts only columns of the specified type(s). The flags argument enables or disables certain filter types.
     * This filter supports {@link DataColumnSpecFilterConfiguration#FILTER_BY_NAMEPATTERN} and
     * {@link DataColumnSpecFilterConfiguration#FILTER_BY_DATATYPE}. To enable both use
     * the bit-wise or ('|') operator (e.g. <code>FILTER_BY_NAMEPATTERN | FILTER_BY_DATATYPE</code>).
     * @param configName the root config name
     * @param filter A (type) filter applied to the input spec (null allowed)
     * @param filterEnableMask The mask to enable filters. 0 will disable all filters but the default in/exclude;
     * @since 2.9
     */
    public SettingsModelColumnFilter2(final String configName, final InputFilter<DataColumnSpec> filter,
        final int filterEnableMask) {
        this(new DataColumnSpecFilterConfiguration(configName, filter, filterEnableMask));
    }

    private SettingsModelColumnFilter2(final DataColumnSpecFilterConfiguration filterConf) {
        m_filterConfiguration = filterConf;
    }

    /** {@inheritDoc} */
    @Override
    protected SettingsModelColumnFilter2 createClone() {
        DataColumnSpecFilterConfiguration cloneConfig = m_filterConfiguration.clone();
        return new SettingsModelColumnFilter2(cloneConfig);
    }

    /** {@inheritDoc} */
    @Override
    protected String getModelTypeID() {
        return "SMID_columnfilter";
    }

    /** {@inheritDoc} */
    @Override
    protected String getConfigName() {
        return m_filterConfiguration.getConfigRootName();
    }

    /** Loads a default configuration by putting all appropriate columns
     * into the include list. Can be used during auto-configuration.
     * (that is, during NodeModel#configure() when no settings model has
     * been instantiated yet).
     * @param spec The spec to derive the default settings from. */
    public void loadDefaults(final DataTableSpec spec) {
        m_filterConfiguration.loadDefaults(spec, true);
    }

    /** Inits default values, usually used during first-time node configuration (auto-configure).
     * For a detailed description of the arguments, see
     * {@link DataColumnSpecFilterConfiguration#loadDefault(DataTableSpec, InputFilter, boolean)}.
     * @param spec ...
     * @param filter ...
     * @param filterDefinesIncludeList ...
     * @since 2.10
     * @see DataColumnSpecFilterConfiguration#loadDefault(DataTableSpec, InputFilter, boolean)
     */
    public void loadDefaults(final DataTableSpec spec,
        final InputFilter<DataColumnSpec> filter, final boolean filterDefinesIncludeList) {
        m_filterConfiguration.loadDefault(spec, filter, filterDefinesIncludeList);
    }

    /**
     * @return
     */
    protected DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration.clone();
    }

    /**
     * @return
     */
    protected InputFilter<DataColumnSpec> getColumnFilter() {
        return m_filterConfiguration.getFilter();
    }

    /**
     * Set the input port before {@link #loadSettingsForDialog(NodeSettingsRO, PortObjectSpec[])} is called. Because
     * the load method is fixed and final, the associated component must forward the port index into this model.
     * @param inPort port the associated component works with.
     */
    void setInputPortIndex(final int inPort) {
        m_inputPortIndex = inPort;
    }

    /**
     * @param conf
     */
    protected void setFilterConfiguration(final DataColumnSpecFilterConfiguration conf) {
        if (!conf.equals(m_filterConfiguration)) {
            m_filterConfiguration = conf.clone();
            notifyChangeListeners();
        }
    }

    /**
     * @param inSpec
     * @return the filter result of the configuration applied to the specified spec
     */
    public FilterResult applyTo(final DataTableSpec inSpec) {
        return m_filterConfiguration.applyTo(inSpec);
    }


    /** {@inheritDoc} */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        assert m_inputPortIndex >= 0; // must be set by associated the component
        if (m_inputPortIndex >= specs.length) {
            String msg = "Specified port index is out of bounds";
            NodeLogger.getLogger(SettingsModelColumnFilter2.class).coding(msg);
            throw new NotConfigurableException(msg);
        }
        DataColumnSpecFilterConfiguration clone = m_filterConfiguration.clone();
        m_filterConfiguration.loadConfigurationInDialog(settings, (DataTableSpec)specs[m_inputPortIndex]);
        if (!clone.equals(m_filterConfiguration)) {
            notifyChangeListeners();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filterConfiguration.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_filterConfiguration.clone().loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration clone = m_filterConfiguration.clone();
        m_filterConfiguration.loadConfigurationInModel(settings);
        if (!clone.equals(m_filterConfiguration)) {
            notifyChangeListeners();
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        m_filterConfiguration.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " - Port:" + m_inputPortIndex + "("
                + m_filterConfiguration.getConfigRootName() + ")";
    }

}

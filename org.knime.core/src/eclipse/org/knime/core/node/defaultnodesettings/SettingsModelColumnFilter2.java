/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.node.defaultnodesettings;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public final class SettingsModelColumnFilter2 extends SettingsModel {

    private DataColumnSpecFilterConfiguration m_filterConfiguration;

    private final int m_inputPortIndex;

    public SettingsModelColumnFilter2(final String configName, final int inPortIdx) {
        this(configName, null, inPortIdx);
    }

    public SettingsModelColumnFilter2(final String configName,
            final InputFilter<DataColumnSpec> filter, final int inPortIdx) {
        this(new DataColumnSpecFilterConfiguration(configName, filter), inPortIdx);
    }

    private SettingsModelColumnFilter2(final DataColumnSpecFilterConfiguration filterConfig, final int inPortIdx) {
        m_filterConfiguration = filterConfig;
        m_inputPortIndex = inPortIdx;
    }

    /** {@inheritDoc} */
    @Override
    protected SettingsModelColumnFilter2 createClone() {
        DataColumnSpecFilterConfiguration cloneConfig = m_filterConfiguration.clone();
        return new SettingsModelColumnFilter2(cloneConfig, m_inputPortIndex);

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

    /**
     * @return the port index this model keeps the filtered columns for
     */
    public int getInputPortIndex() {
        return m_inputPortIndex;
    }

    /**
     * @return
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration.clone();
    }

    /**
     * @param conf
     */
    public void setFilterConfiguration(final DataColumnSpecFilterConfiguration conf) {
        m_filterConfiguration = conf.clone();
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        if (m_inputPortIndex >= specs.length) {
            String msg = "Specified port index is out of bounds";
            NodeLogger.getLogger(SettingsModelColumnFilter2.class).coding(msg);
            throw new NotConfigurableException(msg);
        }
        m_filterConfiguration.loadConfigurationInDialog(settings, (DataTableSpec)specs[m_inputPortIndex]);
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
        m_filterConfiguration.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        m_filterConfiguration.saveConfiguration(settings);
    }

    public FilterResult applyTo(final DataTableSpec spec) {
        return m_filterConfiguration.applyTo(spec);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " - Port:" + m_inputPortIndex + "("
                + m_filterConfiguration.getConfigRootName() + ")";
    }

}

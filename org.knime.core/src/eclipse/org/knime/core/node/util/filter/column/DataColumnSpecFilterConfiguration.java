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
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.node.util.filter.column;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;

/**
 * Represents a column filtering. Classes of this object are used as member in the NodeModel and as underlying model to
 * a {@link DataColumnSpecFilterPanel}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 */
public class DataColumnSpecFilterConfiguration extends NameFilterConfiguration {

    private final InputFilter<DataColumnSpec> m_filter;

    private TypeFilterConfigurationImpl m_typeConfig;

    /**
     * Type filter is off by default.
     */
    private boolean m_typeFilterEnabled = false;

    /** Spec last passed into {@link #loadConfigurationInDialog(NodeSettingsRO, DataTableSpec)}. It's null
     * when not used in dialog but in model. */
    private DataTableSpec m_lastSpec;

    /**
     * New instance with hard coded root name. Consider to call {@link #setTypeFilterEnabled(boolean)} if
     * desired right after construction.
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     */
    public DataColumnSpecFilterConfiguration(final String configRootName) {
        this(configRootName, null);
    }

    /**
     * New instance with hard coded root name.
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     * @param filter A (type) filter applied to the input spec.
     */
    public DataColumnSpecFilterConfiguration(final String configRootName, final InputFilter<DataColumnSpec> filter) {
        super(configRootName);
        m_filter = filter;
        DataValueFilter valFilter = null;
        if (m_filter != null && m_filter instanceof DataTypeColumnFilter) {
            valFilter = new DataValueFilter(((DataTypeColumnFilter)m_filter).getFilterClasses());
        }
        m_typeConfig = new TypeFilterConfigurationImpl(valFilter);
    }

    /**
     * Enables or disables the type filter (default is false). This method should only be called right after
     * construction.
     *
     * @param enabled If the type filter should be enabled
     * @since 2.9
     */
    public void setTypeFilterEnabled(final boolean enabled) {
        m_typeFilterEnabled = enabled;
    }

    /**
     * Checks if the type filter is enabled.
     *
     * @return true if the type filter is enabled, false otherwise
     * @since 2.9
     */
    public boolean isTypeFilterEnabled() {
        return m_typeFilterEnabled;
    }

    /**
     * Loads the configuration in the dialog (no exception thrown) and maps it to the input spec.
     *
     * @param settings The settings to load from.
     * @param spec The non-null spec.
     */
    public void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        String[] names = toFilteredStringArray(spec);
        m_lastSpec = spec;
        super.loadConfigurationInDialog(settings, names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadConfigurationInDialogChild(final NodeSettingsRO settings, final String[] names) {
        super.loadConfigurationInDialogChild(settings, names);
        NodeSettingsRO configSettings;
        try {
            configSettings = settings.getNodeSettings(TypeFilterConfigurationImpl.TYPE);
        } catch (InvalidSettingsException e) {
            configSettings = new NodeSettings("empty");
        }
        m_typeConfig.loadConfigurationInDialog(configSettings, m_lastSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadConfigurationInModelChild(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationInModelChild(settings);
        try {
            NodeSettingsRO configSettings = settings.getNodeSettings(TypeFilterConfigurationImpl.TYPE);
            m_typeConfig.loadConfigurationInModel(configSettings);
        } catch (InvalidSettingsException e) {
            if (TypeFilterConfigurationImpl.TYPE.equals(getType())) {
                throw e;
            }
            // Otherwise leave at default settings as pattern matcher is not active (might be prior 2.9)
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean verifyType(final String type) {
        if (super.verifyType(type)) {
            return true;
        }
        if (isTypeFilterEnabled() && TypeFilterConfigurationImpl.TYPE.equals(type)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveConfigurationChild(final NodeSettingsWO settings) {
        super.saveConfigurationChild(settings);
        NodeSettingsWO configSettings = settings.addNodeSettings(TypeFilterConfigurationImpl.TYPE);
        m_typeConfig.saveConfiguration(configSettings);
    }

    /**
     * Applies the settings to the input spec and returns an object representing the included, excluded and unknown
     * names.
     *
     * @param spec The input spec.
     * @return The filter result object.
     */
    public FilterResult applyTo(final DataTableSpec spec) {
        if (getType().equals(TypeFilterConfigurationImpl.TYPE)) {
            Set<Class<? extends DataValue>> includes = m_typeConfig.applyTo(getDataValuesFromSpec(spec));
            List<String> incls = new ArrayList<String>();
            List<String> excls = new ArrayList<String>();
            for (DataColumnSpec colspec : spec) {
                if (includes.contains(colspec.getType().getPreferredValueClass())) {
                    incls.add(colspec.getName());
                } else {
                    excls.add(colspec.getName());
                }
            }
            return new FilterResult(incls, excls, new ArrayList<String>(), new ArrayList<String>());
        } else {
            String[] names = toFilteredStringArray(spec);
            return super.applyTo(names);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpecFilterConfiguration clone() {
        return (DataColumnSpecFilterConfiguration)super.clone();
    }

    /**
     * Guess default settings on the argument spec. If the flag is set all appropriate columns will be put into the
     * include list, otherwise they are put into the exclude list.
     *
     * @param spec To load from.
     * @param includeByDefault See above.
     */
    public void loadDefaults(final DataTableSpec spec, final boolean includeByDefault) {
        String[] names = toFilteredStringArray(spec);
        super.loadDefaults(names, includeByDefault);
    }

    /**
     * Returns the filter used by this configuration.
     *
     * @return a reference to the filter. Don't modify.
     */
    public InputFilter<DataColumnSpec> getFilter() {
        return m_filter;
    }

    /**
     * @return the typeConfig
     */
    TypeFilterConfigurationImpl getTypeConfig() {
        return m_typeConfig;
    }

    private String[] toFilteredStringArray(final DataTableSpec spec) {
        ArrayList<String> acceptedInNames = new ArrayList<String>();
        for (DataColumnSpec col : spec) {
            if (m_filter == null || m_filter.include(col)) {
                String name = col.getName();
                acceptedInNames.add(name);
            }
        }
        return acceptedInNames.toArray(new String[acceptedInNames.size()]);
    }

    private Set<Class<? extends DataValue>> getDataValuesFromSpec(final DataTableSpec spec) {
        Set<Class<? extends DataValue>> values = new HashSet<Class<? extends DataValue>>();
        for (DataColumnSpec colspec : spec) {
            values.add(colspec.getType().getPreferredValueClass());
        }
        return values;
    }

}

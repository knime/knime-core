/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.node.util.filter.column;

import java.util.ArrayList;
import java.util.List;

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
public final class DataColumnSpecFilterConfiguration extends NameFilterConfiguration {

    /** Mask passed in the constructor to enable a data type based filter
     * ({@value DataColumnSpecFilterConfiguration#FILTER_BY_DATATYPE}).
     * @since 2.9 */
    public static final int FILTER_BY_DATATYPE = 1 << 5; // shift 5 to allow four more filters in super class

    private final InputFilter<DataColumnSpec> m_filter;

    private TypeFilterConfigurationImpl m_typeConfig;

    /** Whether to use data type based filter. Controlled via bit mask in constructor. */
    private final boolean m_typeFilterEnabled;

    /** Spec last passed into {@link #loadConfigurationInDialog(NodeSettingsRO, DataTableSpec)}. It's null
     * when not used in dialog but in model. */
    private DataTableSpec m_lastSpec;

    /**
     * New instance with hard coded root name. This constructor enable a pattern and type filter.
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     */
    public DataColumnSpecFilterConfiguration(final String configRootName) {
        this(configRootName, null, FILTER_BY_NAMEPATTERN | FILTER_BY_DATATYPE);
    }

    /**
     * New instance with hard coded root name. This constructor enables only a name pattern filter (as a
     * data value class filter is applied).
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     * @param filter A (type) filter applied to the input spec (null allowed)
     */
    public DataColumnSpecFilterConfiguration(final String configRootName, final InputFilter<DataColumnSpec> filter) {
        this(configRootName, filter, FILTER_BY_NAMEPATTERN);
    }

    /**
     * New instance with hard coded root name. The flags argument enables or disables certain filter types. This filter
     * supports {@link #FILTER_BY_NAMEPATTERN} and {@link #FILTER_BY_DATATYPE}. To enable both use the bit-wise or ('|')
     * operator (e.g. <code>FILTER_BY_NAMEPATTERN | FILTER_BY_DATATYPE</code>).
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     * @param filter A (type) filter applied to the input spec (null allowed)
     * @param filterEnableMask The mask to enable filters. 0 will disable all filters but the default in/exclude;
     *        {@link #FILTER_BY_NAMEPATTERN} will enable the name pattern filter.
     * @since 2.9
     */
    public DataColumnSpecFilterConfiguration(final String configRootName, final InputFilter<DataColumnSpec> filter,
        final int filterEnableMask) {
        super(configRootName, filterEnableMask);
        m_filter = filter;
        m_typeConfig = new TypeFilterConfigurationImpl(filter);
        m_typeFilterEnabled = (filterEnableMask & FILTER_BY_DATATYPE) != 0;
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
            return m_typeConfig.applyTo(spec);
        } else {
            String[] names = toFilteredStringArray(spec);
            return super.applyTo(names);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpecFilterConfiguration clone() {
        DataColumnSpecFilterConfiguration clone = (DataColumnSpecFilterConfiguration)super.clone();
        clone.m_typeConfig = m_typeConfig.clone();
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataColumnSpecFilterConfiguration other = (DataColumnSpecFilterConfiguration)obj;
        if (!m_typeConfig.equals(other.m_typeConfig)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_typeConfig.hashCode();
        result = prime * result + super.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (getType().equals(TypeFilterConfigurationImpl.TYPE)) {
            return m_typeConfig.toString();
        } else {
            return super.toString();
        }
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
        loadDefaultTypeFilter(spec);
    }

    /** Applies default values and resets the current configuration (if any).
     * @param spec The actual input spec (must not be null).
     * @param filter A filter that defines which of the columns in <code>spec</code> go into the include list
     * (or exclude list if <code>filterDefinesIncludeList</code> is <code>false</code>). This argument may be null,
     * which is equivalent to a match-all filter object. Note, the filter as per the constructor is still used as
     * any column that's not suitable by this configuration is ignored.
     * @param filterDefinesIncludeList If <code>true</code> the columns for which {@link InputFilter#include(Object)}
     * returns true will be put in the include list (otherwise into the exclude list). Also, if <code>true</code> the
     * "enforce inclusion" option is set in the dialog
     * @since 2.9
     */
    public void loadDefault(final DataTableSpec spec,
        final InputFilter<DataColumnSpec> filter, final boolean filterDefinesIncludeList) {
        List<String> ins = new ArrayList<String>();
        List<String> excs = new ArrayList<String>();
        for (DataColumnSpec col : spec) {
            if (m_filter == null || m_filter.include(col)) {
                String name = col.getName();
                if ((filter == null || filter.include(col)) && filterDefinesIncludeList) {
                    ins.add(name);
                } else {
                    excs.add(name);
                }
            }
        }
        setIncludeList(ins.toArray(new String[ins.size()]));
        setExcludeList(excs.toArray(new String[excs.size()]));
        setEnforceOption(filterDefinesIncludeList ? EnforceOption.EnforceInclusion : EnforceOption.EnforceExclusion);

        loadDefaultTypeFilter(spec);
    }

    /**
     * Loads defaults in {@link TypeFilterConfigurationImpl}.
     */
    private void loadDefaultTypeFilter(final DataTableSpec spec) {
        final List<Class<? extends DataValue>> filteredSpec = new ArrayList<>();
        InputFilter<DataColumnSpec> filter = getFilter();
        if (filter instanceof DataTypeColumnFilter){
            for (final String include : getIncludeList()) {
                filteredSpec.add(spec.getColumnSpec(include).getType().getPreferredValueClass());
            }
            for (final String exclude : getExcludeList()) {
                filteredSpec.add(spec.getColumnSpec(exclude).getType().getPreferredValueClass());
            }
            for (final Class<? extends DataValue> value : ((DataTypeColumnFilter)filter).getFilterClasses()) {
                filteredSpec.add(value);
            }
            m_typeConfig.loadDefaults(filteredSpec);
        }
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

}

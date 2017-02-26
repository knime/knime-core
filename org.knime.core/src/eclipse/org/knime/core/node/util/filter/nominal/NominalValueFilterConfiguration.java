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
 *   Feb 8, 2017 (ferry): created
 */
package org.knime.core.node.util.filter.nominal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.PatternFilterConfiguration;

/**
 * Configuration for a nominal value filter that can include and exclude names and takes care on additional/missing names
 * using the enforce inclusion/exclusion option. It also supports filtering based on name patterns.
 *
 * @author Ferry Abt, KNIME.com AG, Zurich, Switzerland
 * @since 3.3
 */
public class NominalValueFilterConfiguration extends NameFilterConfiguration {

    /**
     *
     * @author Ferry Abt, KNIME.com AG, Zurich, Switzerland
     */
    public static class NominalValueFilterResult extends FilterResult {

        private final boolean m_includeMissing;

        /**
         * @param filter a FilterResult that should be extended by the missing value option
         * @param includeMissing whether missing values should be included
         */
        public NominalValueFilterResult(final FilterResult filter, final boolean includeMissing) {
            super(filter.getIncludes(), filter.getExcludes(), filter.getRemovedFromIncludes(),
                filter.getRemovedFromExcludes());
            m_includeMissing = includeMissing;
        }

        /**
         * @param incls list of included elements
         * @param excls list of excluded elements
         * @param removedFromIncludes see {@link #getRemovedFromIncludes()}
         * @param removedFromExcludes see {@link #getRemovedFromExcludes()}
         * @param includeMissing whether missing values should be included
         */
        public NominalValueFilterResult(final String[] incls, final String[] excls, final String[] removedFromIncludes,
            final String[] removedFromExcludes, final boolean includeMissing) {
            super(incls, excls, removedFromIncludes, removedFromExcludes);
            m_includeMissing = includeMissing;
        }

        /**
         * @param incls list of included elements
         * @param excls list of excluded elements
         * @param removedFromIncludes see {@link #getRemovedFromIncludes()}
         * @param removedFromExcludes see {@link #getRemovedFromExcludes()}
         * @param includeMissing whether missing values should be included
         */
        public NominalValueFilterResult(final List<String> incls, final List<String> excls, final List<String> removedFromIncludes,
            final List<String> removedFromExcludes, final boolean includeMissing) {
            super(incls, excls, removedFromIncludes, removedFromExcludes);
            m_includeMissing = includeMissing;
        }

        /**
         * @return whether missing values are included
         * @since 3.3
         */
        public boolean isIncludeMissing() {
            return m_includeMissing;
        }

    }

    /** Settings key for the enforce selection option. */
    private static final String KEY_INCLUDE_MISSING = "include_missing";

    private boolean m_includeMissing = false;

    /**
     * Creates a new nominal value filter configuration with the given settings name.
     *
     * @param configRootName the config name to used to store the settings
     * @throws IllegalArgumentException If config name is null or empty
     */
    public NominalValueFilterConfiguration(final String configRootName) {
        super(configRootName);
        setEnforceOption(EnforceOption.EnforceInclusion);
    }

    /**
     *
     * Load config in dialog, init defaults if necessary.
     *
     * @param settings to load from.
     * @param domain of the column to be filtered.
     *
     */
    public void loadConfigurationInDialog(final NodeSettingsRO settings, final Set<DataCell> domain) {
        ArrayList<String> names = new ArrayList<String>();
        if (domain != null) {
            for (DataCell dc : domain) {
                names.add(dc.toString());
            }
        }
        super.loadConfigurationInDialog(settings, names.toArray(new String[names.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadConfigurationInModelChild(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_includeMissing = settings.getBoolean(KEY_INCLUDE_MISSING, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveConfigurationChild(final NodeSettingsWO settings) {
        settings.addBoolean(KEY_INCLUDE_MISSING, m_includeMissing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        NominalValueFilterConfiguration other = (NominalValueFilterConfiguration)obj;
        return m_includeMissing == other.m_includeMissing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Boolean.valueOf(m_includeMissing).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getIncludeList() {
        return super.getIncludeList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getExcludeList() {
        return super.getExcludeList();
    }

    /**
     * Create and return a new name filter that contains a list of include and exclude names based on the
     * current configuration and domain provided as an argument.
     * @see NameFilterConfiguration#applyTo(String[])
     *
     * @param domain the domain to apply the current configuration on
     * @return a new name filter
     */
    public NominalValueFilterResult applyTo(final Set<DataCell> domain) {
        ArrayList<String> names = new ArrayList<String>();
        //get array of domain values
        if (domain != null) {
            for (DataCell dc : domain) {
                names.add(dc.toString());
            }
        }
        return new NominalValueFilterResult(super.applyTo(names.toArray(new String[names.size()])), m_includeMissing);
    }

    /**
     * @return true if missing values are included
     * @since 3.3
     */
    public boolean isIncludeMissing(){
        if (TYPE.equals(getType())) {
            return m_includeMissing;
        } else if (PatternFilterConfiguration.TYPE.equals(getType())) {
            return ((NominalValuePatternFilterConfiguration)getPatternConfig()).isIncludeMissing();
        } else {
            throw new IllegalStateException("Unsupported filter type: " + getType());
        }
    }

    /**
     * @param includeMissing whether missing values should be included
     * @since 3.3
     */
    protected final void setIncludeMissing(final boolean includeMissing) {
        m_includeMissing = includeMissing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadDefaults(final String[] names, final boolean includeByDefault) {
        super.loadDefaults(names, includeByDefault);
        m_includeMissing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadDefaults(final String[] includes, final String[] excludes, final EnforceOption enforceOption) {
        super.loadDefaults(includes, excludes, enforceOption);
        m_includeMissing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadConfigurationInDialogChild(final NodeSettingsRO settings, final String[] names) {
        m_includeMissing = settings.getBoolean(KEY_INCLUDE_MISSING, false);
    }

    /** {@inheritDoc} */
    @Override
    protected NameFilterConfiguration clone() {
        NominalValueFilterConfiguration clone = (NominalValueFilterConfiguration)super.clone();
        clone.m_includeMissing = m_includeMissing;
        return clone;
    }

    /**
     * @return the pattern config
     * @since 3.3
     */
    @Override
    protected NominalValuePatternFilterConfiguration createPatternConfig() {
        return new NominalValuePatternFilterConfiguration();
    }

}

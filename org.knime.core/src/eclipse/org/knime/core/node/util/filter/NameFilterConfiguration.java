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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for a generic filter that can includes and excludes names and
 * takes care on additional/missing names using the enforce inclusion/exclusion
 * option.
 *
 * <p>This class may contain additional functionality in future versions, e.g.
 * filters based on string expressions. Clients should not rely on the
 * enforce in/exclusion flags but only use the standard load/save routines and
 * the {@link #applyTo(String[])} method.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.6
 */
public class NameFilterConfiguration implements Cloneable {

    private String[] m_includeList = new String[0];
    private String[] m_excludeList = new String[0];

    private EnforceOption m_enforceOption = EnforceOption.EnforceInclusion;

    /** Enforce inclusion/exclusion options. */
    public enum EnforceOption {
        /** Option to enforce inclusion for all new, additional names. */
        EnforceInclusion,
        /** Option to enforce exclusion for all new, additional names. */
        EnforceExclusion;

        /**
         * Translates the given name into a {@link EnforceOption}.
         * @param name for a selection option
         * @return {@link EnforceOption} corresponding to the given name
         * @throws InvalidSettingsException if the name can't be parsed
         */
        public static EnforceOption parse(final String name)
            throws InvalidSettingsException {
            try {
                return EnforceOption.valueOf(name);
            } catch (Exception iae) {
                throw new InvalidSettingsException(
                        "Can't parse enforce option: " + name, iae);
            }
        }

        /**
         * Translates the given name into a {@link EnforceOption}.
         * @param name for a selection option
         * @param defaultOption returned if the name can't be parsed
         * @return {@link EnforceOption} corresponding to the given name, or
         *         the defaultOption is the name can't be parsed
         */
        public static EnforceOption parse(final String name,
                final EnforceOption defaultOption) {
            try {
                return EnforceOption.parse(name);
            } catch (InvalidSettingsException iae) {
                return defaultOption;
            }
        }
    }

    /** The result when a filtering is applied to an array of elements. */
    public static class FilterResult {
        private String[] m_incls;
        private String[] m_excls;
        private String[] m_removedFromIncludes;
        private String[] m_removedFromExcludes;
        /**
         * @param incls list of included elements
         * @param excls list of excluded elements
         * @param removedFromIncludes see {@link #getRemovedFromIncludes()}
         * @param removedFromExcludes see {@link #getRemovedFromExcludes()}
         */
        protected FilterResult(final List<String> incls, final List<String> excls,
                final List<String> removedFromIncludes,
                final List<String> removedFromExcludes) {
            m_incls = incls.toArray(new String[incls.size()]);
            m_excls = excls.toArray(new String[excls.size()]);
            m_removedFromIncludes = removedFromIncludes.toArray(
                    new String[removedFromIncludes.size()]);
            m_removedFromExcludes = removedFromExcludes.toArray(
                    new String[removedFromExcludes.size()]);
        }
        /** @return a list of names that were specifically included in the dialog
         * but which are no longer available in the list of actual values. Only used
         * in warning messages. */
        public String[] getRemovedFromIncludes() {
            return m_removedFromIncludes;
        }
        /** @return a list of names that were specifically excluded in the dialog
         * but which are no longer available in the list of actual values. Only used
         * in warning messages. */
        public String[] getRemovedFromExcludes() {
            return m_removedFromExcludes;
        }
        /** @return an array of included names. */
        public String[] getIncludes() {
            return m_incls;
        }
        /** @return an array of excluded names. */
        public String[] getExcludes() {
            return m_excls;
        }
    }

    /** Settings key for the excluded columns. */
    private static final String KEY_INCLUDED_NAMES = "included_names";

    /** Settings key for the excluded columns. */
    private static final String KEY_EXCLUDED_NAMES = "excluded_names";

    /** Settings key for the enforce selection option. */
    private static final String KEY_ENFORCE_OPTION = "enforce_option";

    /** Settings key for type of filter. */
    private static final String KEY_FILTER_TYPE = "filter-type";

    /** Enum of filter types. */
    enum FilterType {
        /** Default, new with KNIME 2.6. */
        STANDARD
    }

    private final String m_configRootName;

    /**
     * Creates a new name filter configuration with the given settings name.
     * @param configRootName the config name to used to store the settings
     * @throws IllegalArgumentException If config name is null or empty
     */
    public NameFilterConfiguration(final String configRootName) {
        if (configRootName == null || configRootName.length() == 0) {
            throw new IllegalArgumentException("Config name must not be null or empty.");
        }
        m_configRootName = configRootName;
    }

    /** @return the configRootName */
    public final String getConfigRootName() {
        return m_configRootName;
    }

    /**
     * Create and return a new name filter that contains a list of include,
     * exclude and unknown names based on the current configuration and names
     * provided as an argument.
     * @param names the names to validate the current configuration on
     * @return a new name filter
     */
    protected FilterResult applyTo(final String[] names) {

        final EnforceOption enforceOption = getEnforceOption();
        final List<String> nameList = Arrays.asList(names);
        final List<String> incls = new ArrayList<String>(
                Arrays.asList(getIncludeList()));
        final List<String> excls = new ArrayList<String>(
                Arrays.asList(getExcludeList()));

        for (String name : names) {
            if (!incls.contains(name) && !excls.contains(name)) {
                switch (enforceOption) {
                    case EnforceInclusion:
                        excls.add(name);
                        break;
                    case EnforceExclusion:
                        incls.add(name);
                        break;
                }
            }
            if (incls.contains(name) && excls.contains(name)) {
                // weird case, non-disjoint sets
                switch (enforceOption) {
                    case EnforceInclusion:
                        excls.remove(name);
                        break;
                    case EnforceExclusion:
                        incls.remove(name);
                        break;
                }
            }
        }

        final List<String> removedFromIncludes = new ArrayList<String>();
        final List<String> removedFromExcludes = new ArrayList<String>();

        for (Iterator<String> it = incls.iterator(); it.hasNext();) {
            String in = it.next();
            if (!nameList.contains(in)) {
                removedFromIncludes.add(in);
                it.remove();
            }
        }

        for (Iterator<String> it = excls.iterator(); it.hasNext();) {
            String ex = it.next();
            if (!nameList.contains(ex)) {
                removedFromExcludes.add(ex);
                it.remove();
            }
        }
        return new FilterResult(incls, excls,
                removedFromIncludes, removedFromExcludes);
    }

    /**
     * @return list of names in the include list
     */
    protected String[] getIncludeList() {
       return m_includeList;
    }

    /**
     * Set a new list of names to include.
     * @param includeList the new list of included names
     */
    protected void setIncludeList(final String[] includeList) {
        m_includeList = includeList;
    }

    /**
     * @return list of excluded names
     */
    protected String[] getExcludeList() {
        return m_excludeList;
    }

    /**
     * Set a new exclude list.
     * @param excludeList the names to be excluded
     */
    protected void setExcludeList(final String[] excludeList) {
        m_excludeList = excludeList;
    }

    /**
     * @return selected option to enabled inclusion or exclusion
     */
    protected EnforceOption getEnforceOption() {
        return m_enforceOption;
    }

    /**
     * Set a new enforce option.
     * @param enforceOption the new in-/exclusion option
     */
    protected void setEnforceOption(final EnforceOption enforceOption) {
        m_enforceOption = enforceOption;
    }

    /**
     * @return true, if inclusion is on
     */
    protected boolean isEnforceInclusion() {
        return m_enforceOption == EnforceOption.EnforceInclusion;
    }

    /**
     * @return true, if exclusion is on
     */
    protected boolean isEnforceExclusion() {
        return m_enforceOption == EnforceOption.EnforceExclusion;
    }

    /**  Save current config to argument.
     * @param settings */
    public void saveConfiguration(final NodeSettingsWO settings) {
        NodeSettingsWO subSettings = settings.addNodeSettings(m_configRootName);
        subSettings.addString(KEY_FILTER_TYPE, FilterType.STANDARD.name());
        subSettings.addStringArray(KEY_INCLUDED_NAMES, m_includeList);
        subSettings.addStringArray(KEY_EXCLUDED_NAMES, m_excludeList);
        subSettings.addString(KEY_ENFORCE_OPTION, m_enforceOption.name());
    }

    /** Load config from argument.
      * @param settings To load from.
      * @throws InvalidSettingsException If inconsistent/missing. */
    public void loadConfigurationInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettingsRO subSettings = settings.getNodeSettings(m_configRootName);
        String type = subSettings.getString(KEY_FILTER_TYPE);
        if (!type.equals(FilterType.STANDARD.name())) {
            throw new InvalidSettingsException("FilterType \"" + type
                + "\" not supported.");
        }
        m_enforceOption = EnforceOption.parse(
                subSettings.getString(KEY_ENFORCE_OPTION));
        m_includeList = subSettings.getStringArray(KEY_INCLUDED_NAMES);
        m_excludeList = subSettings.getStringArray(KEY_EXCLUDED_NAMES);
    }

    /** Load config in dialog, init defaults if necessary.
      * @param settings to load from.
      * @param names all names available for filtering
      */
    protected void loadConfigurationInDialog(final NodeSettingsRO settings,
            final String[] names) {
        NodeSettingsRO subSettings;
        try {
            subSettings = settings.getNodeSettings(m_configRootName);
            String type = subSettings.getString(KEY_FILTER_TYPE, null);
            if (type == null || !type.equals(FilterType.STANDARD.name())) {
                // no validation necessary
            }
            m_includeList = subSettings.getStringArray(KEY_INCLUDED_NAMES,
                    (String[]) null);
            m_excludeList = subSettings.getStringArray(KEY_EXCLUDED_NAMES,
                    (String[]) null);
            try {
                m_enforceOption = EnforceOption.parse(
                        subSettings.getString(KEY_ENFORCE_OPTION));
            } catch (InvalidSettingsException ise) {
                m_enforceOption = EnforceOption.EnforceExclusion;
            }
        } catch (InvalidSettingsException ise) {
            m_includeList = new String[0];
            m_excludeList = new String[0];
            m_enforceOption = EnforceOption.EnforceExclusion;
        }

        FilterResult applyTo = applyTo(names);
        m_includeList = applyTo.getIncludes();
        m_excludeList = applyTo.getExcludes();

    }

    /** Sets default names, used in auto-configure of a node when no settings
     * are available.
     * @param names The input names
     * @param includeByDefault If <code>true</code>, all elements will be put into the include list and the
     * "enforce exclusion" will be set. Otherwise all elements are put into the exclude list and the
     * "enforce inclusion" is set. If in doubt, pass <code>true</code> here. */
    protected void loadDefaults(final String[] names, final boolean includeByDefault) {
        String[] copy = Arrays.copyOf(names, names.length);
        if (includeByDefault) {
            m_includeList = copy;
            setEnforceOption(EnforceOption.EnforceExclusion);
        } else {
            m_excludeList = copy;
            setEnforceOption(EnforceOption.EnforceInclusion);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NameFilterConfiguration clone() {
        try {
            NameFilterConfiguration clone = (NameFilterConfiguration)super.clone();
            clone.m_excludeList = Arrays.copyOf(m_excludeList, m_excludeList.length);
            clone.m_includeList = Arrays.copyOf(m_includeList, m_includeList.length);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Object not clonable although it implements java.lang.Clonable", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_configRootName == null) ? 0 : m_configRootName.hashCode());
        result = prime * result + ((m_enforceOption == null) ? 0 : m_enforceOption.hashCode());
        result = prime * result + Arrays.hashCode(m_excludeList);
        result = prime * result + Arrays.hashCode(m_includeList);
        return result;
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
        NameFilterConfiguration other = (NameFilterConfiguration)obj;
        if (m_configRootName == null) {
            if (other.m_configRootName != null) {
                return false;
            }
        } else if (!m_configRootName.equals(other.m_configRootName)) {
            return false;
        }
        if (m_enforceOption != other.m_enforceOption) {
            return false;
        }
        if (!Arrays.equals(m_excludeList, other.m_excludeList)) {
            return false;
        }
        if (!Arrays.equals(m_includeList, other.m_includeList)) {
            return false;
        }
        return true;
    }

}

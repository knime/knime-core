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
 */
package org.knime.core.node.util.filter;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Configuration for a generic filter that can includes and excludes names and takes care on additional/missing names
 * using the enforce inclusion/exclusion option. It also supports filtering based on name patterns.
 *
 * <p>
 * This class may contain additional functionality in future versions. Clients should not rely on the enforce
 * in/exclusion flags but only use the standard load/save routines and the {@link #applyTo(String[])} method.
 *
 * @author Thomas Gabriel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 2.6
 */
public class NameFilterConfiguration implements Cloneable {

    /** Mask passed in the constructor to enable a name pattern filter
     * ({@value NameFilterConfiguration#FILTER_BY_NAMEPATTERN}).
     * @since 2.9 */
    public static final int FILTER_BY_NAMEPATTERN = 1 << 0;

    /** Identifier for the filter by include/exclude selection type.
     * @since 2.9 */
    protected static final String TYPE = "STANDARD";

    /** Settings key for the excluded columns. */
    private static final String KEY_INCLUDED_NAMES = "included_names";

    /** Settings key for the excluded columns. */
    private static final String KEY_EXCLUDED_NAMES = "excluded_names";

    /** Settings key for the enforce selection option. */
    private static final String KEY_ENFORCE_OPTION = "enforce_option";

    /** Settings key for type of filter. */
    private static final String KEY_FILTER_TYPE = "filter-type";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NameFilterConfiguration.class);

    private final String m_configRootName;

    private String[] m_includeList = new String[0];

    private String[] m_excludeList = new String[0];

    private String[] m_removedFromIncludeList = new String[0];

    private String[] m_removedFromExcludeList = new String[0];

    private EnforceOption m_enforceOption = EnforceOption.EnforceExclusion;

    /** Filter type identifier, see {@link #addType(String)} for details. */
    private String m_type = TYPE;

    /** A (final) patter filter, which is used when filtering is done based on name pattern (regex or wildcard).
     * It's not final as it's assigned in {@link #clone()}.
     */
    private PatternFilterConfiguration m_patternConfig;

    /**
     * Pattern filter is on by default.
     */
    private final boolean m_patternFilterEnabled;

    /** Enforce inclusion/exclusion options. */
    public enum EnforceOption {
        /** Option to enforce inclusion for all new, additional names. */
        EnforceInclusion,
        /** Option to enforce exclusion for all new, additional names. */
        EnforceExclusion;

        /**
         * Translates the given name into a {@link EnforceOption}.
         *
         * @param name for a selection option
         * @return {@link EnforceOption} corresponding to the given name
         * @throws InvalidSettingsException if the name can't be parsed
         */
        public static EnforceOption parse(final String name) throws InvalidSettingsException {
            try {
                return EnforceOption.valueOf(name);
            } catch (Exception iae) {
                throw new InvalidSettingsException("Can't parse enforce option: " + name, iae);
            }
        }

        /**
         * Translates the given name into a {@link EnforceOption}.
         *
         * @param name for a selection option
         * @param defaultOption returned if the name can't be parsed
         * @return {@link EnforceOption} corresponding to the given name, or the defaultOption is the name can't be
         *         parsed
         */
        public static EnforceOption parse(final String name, final EnforceOption defaultOption) {
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
         * @since 2.9
         */
        public FilterResult(final List<String> incls, final List<String> excls, final List<String> removedFromIncludes,
            final List<String> removedFromExcludes) {
            m_incls = incls.toArray(new String[incls.size()]);
            m_excls = excls.toArray(new String[excls.size()]);
            m_removedFromIncludes = removedFromIncludes.toArray(new String[removedFromIncludes.size()]);
            m_removedFromExcludes = removedFromExcludes.toArray(new String[removedFromExcludes.size()]);
        }

        /**
         * @param incls list of included elements
         * @param excls list of excluded elements
         * @param removedFromIncludes see {@link #getRemovedFromIncludes()}
         * @param removedFromExcludes see {@link #getRemovedFromExcludes()}
         * @since 3.4
         */
        protected FilterResult(final String[] incls, final String[] excls, final String[] removedFromIncludes,
            final String[] removedFromExcludes) {
            m_incls = incls;
            m_excls = excls;
            m_removedFromIncludes = removedFromIncludes;
            m_removedFromExcludes = removedFromExcludes;
        }

        /** A list of names that were specifically included in the dialog but which are no longer available in the list
         * of actual values. Only used in warning messages.  Note, this list is empty if a name pattern filter is used.
         * @return A non-null but possibly empty array.
         */
        public String[] getRemovedFromIncludes() {
            return m_removedFromIncludes;
        }

        /** A list of names that were specifically excluded in the dialog but which are no longer available in the list
         * of actual values. Only used in warning messages. Note, this list is empty if a name pattern filter is used.
         * @return A non-null but possibly empty array.
         */
        public String[] getRemovedFromExcludes() {
            return m_removedFromExcludes;
        }

        /**
         * @return an array of included names. The list is sorted according to the order provided in
         *         {@link NameFilterConfiguration#applyTo(String[])}.
         */
        public String[] getIncludes() {
            return m_incls;
        }

        /**
         * @return an array of excluded names. The list is sorted according to the order provided in
         *         {@link NameFilterConfiguration#applyTo(String[])}.
         */
        public String[] getExcludes() {
            return m_excls;
        }
    }

    /**
     * Creates a new name filter configuration with the given settings name. Also enables the name pattern filter.
     *
     * @param configRootName the config name to used to store the settings
     * @throws IllegalArgumentException If config name is null or empty
     * @since 2.9
     */
    public NameFilterConfiguration(final String configRootName) {
        this(configRootName, FILTER_BY_NAMEPATTERN);
    }

    /**
     * Creates a new name filter configuration with the given settings name. The flags argument enables or
     * disables certain filter types. Currently, this filter only supports {@link #FILTER_BY_NAMEPATTERN}.
     *
     * @param configRootName the config name to used to store the settings
     * @param filterEnableMask The mask to enable filters. 0 will disable all filters but the default in/exclude;
     *        {@link #FILTER_BY_NAMEPATTERN} will enable the name pattern filter.
     * @throws IllegalArgumentException If config name is null or empty
     * @since 2.9
     */
    public NameFilterConfiguration(final String configRootName, final int filterEnableMask) {
        if (configRootName == null || configRootName.length() == 0) {
            throw new IllegalArgumentException("Config name must not be null or empty.");
        }
        m_configRootName = configRootName;
        m_patternFilterEnabled = (filterEnableMask & FILTER_BY_NAMEPATTERN) != 0;
        m_patternConfig = createPatternConfig();
    }

    /**
     * Load config from argument.
     *
     * <p>Subclasses should not define another method with similar signature and then
     * call that new method to read from the passed NodeSettings object but instead overwrite
     * {@link #loadConfigurationInModelChild(NodeSettingsRO)}.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If inconsistent/missing.
     */
    public final void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO subSettings = settings.getNodeSettings(m_configRootName);
        String type = subSettings.getString(KEY_FILTER_TYPE);
        if (type == null || !verifyType(type)) {
            throw new InvalidSettingsException("Invalid type: " + type);
        }
        m_type = type;
        m_enforceOption = EnforceOption.parse(subSettings.getString(KEY_ENFORCE_OPTION));
        // addresses AP-20185 - this field should never be null (but let call deliberately fail if not present)
        m_includeList = requireNonNullElse(subSettings.getStringArray(KEY_INCLUDED_NAMES), EMPTY_STRING_ARRAY);
        m_excludeList = requireNonNullElse(subSettings.getStringArray(KEY_EXCLUDED_NAMES), EMPTY_STRING_ARRAY);
        try {
            NodeSettingsRO configSettings = subSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
            m_patternConfig.loadConfigurationInModel(configSettings);
        } catch (InvalidSettingsException e) {
            if (PatternFilterConfiguration.TYPE.equals(m_type)) {
                throw e;
            }
            // Otherwise leave at default settings as pattern matcher is not active (might be prior 2.9)
        }
        loadConfigurationInModelChild(subSettings);
    }

    /** Method call by {@link #loadConfigurationInModel(NodeSettingsRO)} to allow subclasses to read child elements.
     * @param settings The (sub)settings respecting the config root specified in the constructor
     * @throws InvalidSettingsException ...
     * @since 2.9
     */
    protected void loadConfigurationInModelChild(final NodeSettingsRO settings) throws InvalidSettingsException {
        // implementned in subclass
    }

    /**
     * Save current config to argument.
     *
     * <p>Subclasses should not define another method with similar signature and then
     * call that new method to read from the passed NodeSettings object but instead overwrite
     * {@link #loadConfigurationInDialogChild(NodeSettingsRO, String[])}.

     * @param settings The settings to save into
     */
    public final void saveConfiguration(final NodeSettingsWO settings) {
        NodeSettingsWO subSettings = settings.addNodeSettings(m_configRootName);
        subSettings.addString(KEY_FILTER_TYPE, m_type);
        // Only add removed list for selected enforce option
        String[] includes =
            m_enforceOption == EnforceOption.EnforceInclusion ? (String[])ArrayUtils.addAll(m_includeList,
                m_removedFromIncludeList) : m_includeList;
        subSettings.addStringArray(KEY_INCLUDED_NAMES, includes);
        String[] excludes =
            m_enforceOption == EnforceOption.EnforceExclusion ? (String[])ArrayUtils.addAll(m_excludeList,
                m_removedFromExcludeList) : m_excludeList;
        subSettings.addStringArray(KEY_EXCLUDED_NAMES, excludes);
        subSettings.addString(KEY_ENFORCE_OPTION, m_enforceOption.name());
        NodeSettingsWO configSettings = subSettings.addNodeSettings(PatternFilterConfiguration.TYPE);
        m_patternConfig.saveConfiguration(configSettings);
        saveConfigurationChild(subSettings);
    }

    /** Subclass hook to save to the sub settings object defined in the constructor.
     * Called from {@link #saveConfiguration(NodeSettingsWO)}.
     * @param settings The subsettings
     * @since 2.9
     */
    protected void saveConfigurationChild(final NodeSettingsWO settings) {
        // overwritten
    }

    /** @return the configRootName */
    public final String getConfigRootName() {
        return m_configRootName;
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
        if (!Arrays.equals(m_removedFromExcludeList, other.m_removedFromExcludeList)) {
            return false;
        }
        if (!Arrays.equals(m_removedFromIncludeList, other.m_removedFromIncludeList)) {
            return false;
        }
        if (!m_type.equals(other.m_type)) {
            return false;
        }
        if (!m_patternConfig.equals(other.m_patternConfig)) {
            return false;
        }
        return true;
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
        result = prime * result + m_type.hashCode();
        result = prime * result + Arrays.hashCode(m_excludeList);
        result = prime * result + Arrays.hashCode(m_includeList);
        result = prime * result + m_patternConfig.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_type.equals(PatternFilterConfiguration.TYPE)) {
            return m_patternConfig.toString();
        } else {
            StringBuilder includes = new StringBuilder();
            for (String include : m_includeList) {
                includes.append(", ").append(include);
            }
            StringBuilder excludes = new StringBuilder();
            for (String exclude : m_excludeList) {
                excludes.append(", ").append(exclude);
            }
            return "Includes: " + includes.toString().replaceFirst(", ", "") + "\nExcludes: "
                + excludes.toString().replaceFirst(", ", "");
        }
    }

    /**
     * Checks if the pattern filter is enabled in constructor.
     *
     * @return true if the pattern filter is enabled, false otherwise
     * @since 2.9
     */
    public final boolean isPatternFilterEnabled() {
        return m_patternFilterEnabled;
    }

    /**
     * Sets default names, used in auto-configure of a node when no settings are available.
     *
     * @param names The input names
     * @param includeByDefault If <code>true</code>, all elements will be put into the include list and the
     *            "enforce exclusion" will be set. Otherwise all elements are put into the exclude list and the
     *            "enforce inclusion" is set. If in doubt, pass <code>true</code> here.
     */
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

    /** Sets the include and exclude list and force the filter type to be "standard".
     * @param includes Include list, may be null but must not contain null, duplicates or elements in the exclude.
     * @param excludes Similar to include list... just for exclude.
     * @param enforceOption The new enforce option.
     * @since 2.11
     */
    public void loadDefaults(final String[] includes, final String[] excludes, final EnforceOption enforceOption) {
        CheckUtils.checkArgumentNotNull(enforceOption, "must not be null");
        String[] includesCopy = includes == null ? new String[0] : Arrays.copyOf(includes, includes.length);
        String[] excludesCopy = excludes == null ? new String[0] : Arrays.copyOf(excludes, excludes.length);
        Set<String> includesHash = new HashSet<String>(Arrays.asList(includesCopy));
        Set<String> excludesHash = new HashSet<String>(Arrays.asList(excludesCopy));
        CheckUtils.checkArgument(includesHash.size() == includesCopy.length,
                "Include list contains duplicates: %s", Arrays.toString(includesCopy));
        CheckUtils.checkArgument(!includesHash.contains(null), "Include list mut not contain null");
        CheckUtils.checkArgument(!excludesHash.contains(null), "Exclude list mut not contain null");
        includesHash.retainAll(Arrays.asList(excludesCopy));
        CheckUtils.checkArgument(includesHash.isEmpty(), "Arrays not disjoint: %s",
            ConvenienceMethods.getShortStringFrom(includesHash, 3));
        m_includeList = includesCopy;
        m_excludeList = excludesCopy;
        m_enforceOption = enforceOption;
        m_type = TYPE;
    }

    /**
     * Load config in dialog, init defaults if necessary.
     *
     * <p>Subclasses should not define another method with similar signature and then
     * call that new method to read from the passed NodeSettings object but instead overwrite
     * {@link #loadConfigurationInDialogChild(NodeSettingsRO, String[])}.
     *
     * @param settings to load from.
     * @param names all names available for filtering
     */
    protected final void loadConfigurationInDialog(final NodeSettingsRO settings, final String[] names) {
        NodeSettingsRO subSettings;
        try {
            subSettings = settings.getNodeSettings(m_configRootName);
        } catch (InvalidSettingsException ise) {
            subSettings = new NodeSettings("empty");
        }
        m_type = subSettings.getString(KEY_FILTER_TYPE, TYPE);
        if (m_type == null || !verifyType(m_type)) {
            m_type = TYPE;
        }
        m_includeList =
            requireNonNullElse(subSettings.getStringArray(KEY_INCLUDED_NAMES, EMPTY_STRING_ARRAY), EMPTY_STRING_ARRAY);
        m_excludeList =
            requireNonNullElse(subSettings.getStringArray(KEY_EXCLUDED_NAMES, EMPTY_STRING_ARRAY), EMPTY_STRING_ARRAY);
        try {
            m_enforceOption = EnforceOption.parse(subSettings.getString(KEY_ENFORCE_OPTION));
        } catch (InvalidSettingsException ise) {
            m_enforceOption = EnforceOption.EnforceExclusion;
        }
        NodeSettingsRO patternMatchSettings;
        try {
            patternMatchSettings = subSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
        } catch (InvalidSettingsException ise) {
            patternMatchSettings = new NodeSettings("empty");
        }
        m_patternConfig.loadConfigurationInDialog(patternMatchSettings);

        // applyTo() is used to fill the include and exclude lists and therefore the type needs to be switched to the
        // standard temporarily
        String type = m_type;
        m_type = TYPE;
        FilterResult applyTo = applyTo(names);
        m_type = type;

        m_includeList = applyTo.getIncludes();
        m_excludeList = applyTo.getExcludes();
        // Only keep removed names from enforced option
        if (m_enforceOption == EnforceOption.EnforceInclusion) {
            m_removedFromIncludeList = applyTo.getRemovedFromIncludes();
        } else if (m_enforceOption == EnforceOption.EnforceExclusion) {
            m_removedFromExcludeList = applyTo.getRemovedFromExcludes();
        }
        loadConfigurationInDialogChild(subSettings, names);
    }

    /** Subclass hook to read from the sub settings object defined in the constructor.
     * Called from {@link #loadConfigurationInDialog(NodeSettingsRO, String[])}
     * @param settings The subsettings
     * @param names As passed into the calling method
     * @since 2.9
     */
    protected void loadConfigurationInDialogChild(final NodeSettingsRO settings, final String[] names) {
        // overwritten
    }

    /**
     * Create and return a new name filter that contains a list of include, exclude and unknown names based on the
     * current configuration and names provided as an argument.
     *
     * @param names the names to validate the current configuration on
     * @return a new name filter
     */
    protected FilterResult applyTo(final String[] names) {
        if (TYPE.equals(m_type)) {
            final EnforceOption enforceOption = getEnforceOption();
            final LinkedHashSet<String> inclsHash =
                new LinkedHashSet<String>(Arrays.asList(ArrayUtils.addAll(getIncludeList(),
                    m_removedFromIncludeList)));
            final LinkedHashSet<String> exclsHash =
                new LinkedHashSet<String>(Arrays.asList(ArrayUtils.addAll(getExcludeList(),
                    m_removedFromExcludeList)));
            final List<String> incls = new ArrayList<String>();
            final List<String> excls = new ArrayList<String>();

            for (String name : names) {
                if (inclsHash.remove(name)) { // also remove from hash so that it contains only orphan items
                    incls.add(name);
                    if (exclsHash.remove(name)) {
                        LOGGER.coding("Item \"" + name + "\" appears in both the include and exclude list");
                    }
                } else if (exclsHash.remove(name)) {
                    excls.add(name);
                } else {
                    switch (enforceOption) {
                        case EnforceExclusion:
                            incls.add(name);
                            break;
                        case EnforceInclusion:
                            excls.add(name);
                            break;
                        default:
                            throw new IllegalStateException("Option not implemented: " + enforceOption);
                    }
                }
            }
            final List<String> orphanedIncludes = new ArrayList<String>(inclsHash);
            final List<String> orphanedExcludes = new ArrayList<String>(exclsHash);

            return new FilterResult(incls, excls, orphanedIncludes, orphanedExcludes);
        } else if (PatternFilterConfiguration.TYPE.equals(m_type)) {
            return m_patternConfig.applyTo(names);
        } else {
            throw new IllegalStateException("Unsupported filter type: " + m_type);
        }
    }

    /**
     * @return list of names in the include list
     */
    protected String[] getIncludeList() {
        return m_includeList;
    }

    /**
     * Set a new list of names to include.
     *
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
     *
     * @param excludeList the names to be excluded
     */
    protected void setExcludeList(final String[] excludeList) {
        m_excludeList = excludeList;
    }

    /**
     * @return list of names that were explicitly included but are no longer available
     * @since 2.10
     */
    protected String[] getRemovedFromIncludeList() {
        return m_removedFromIncludeList;
    }

    /**
     * @param removedFromIncludeList list of names that were explicitly included but are no longer available
     * @since 2.10
     */
    protected void setRemovedFromIncludeList(final String[] removedFromIncludeList) {
        m_removedFromIncludeList = removedFromIncludeList;
    }

    /**
     * @return list of names that were explicitly excluded but are no longer available
     * @since 2.10
     */
    protected String[] getRemovedFromExcludeList() {
        return m_removedFromExcludeList;
    }

    /**
     * @param removedFromExcludeList list of names that were explicitly excluded but are no longer available
     * @since 2.10
     */
    protected void setRemovedFromExcludeList(final String[] removedFromExcludeList) {
        m_removedFromExcludeList = removedFromExcludeList;
    }

    /**
     * @return selected option to enabled inclusion or exclusion
     */
    protected EnforceOption getEnforceOption() {
        return m_enforceOption;
    }

    /**
     * Set a new enforce option.
     *
     * @param enforceOption the new in-/exclusion option
     */
    protected void setEnforceOption(final EnforceOption enforceOption) {
        m_enforceOption = enforceOption;
    }

    /**
     * @return true, if inclusion is on
     * @since 2.11
     */
    public boolean isEnforceInclusion() {
        return m_enforceOption == EnforceOption.EnforceInclusion;
    }

    /**
     * @return true, if exclusion is on
     * @since 2.11
     */
    public boolean isEnforceExclusion() {
        return m_enforceOption == EnforceOption.EnforceExclusion;
    }

    /** {@inheritDoc} */
    @Override
    protected NameFilterConfiguration clone() {
        try {
            NameFilterConfiguration clone = (NameFilterConfiguration)super.clone();
            clone.m_excludeList = Arrays.copyOf(m_excludeList, m_excludeList.length);
            clone.m_includeList = Arrays.copyOf(m_includeList, m_includeList.length);
            clone.m_patternConfig = m_patternConfig.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Object not clonable although it implements java.lang.Clonable", e);
        }
    }

    /** The type identifier of the actual filter implementation. This can be the default include/exclude list, a
     * name pattern filter or something defined by the subclass. Default is {@linkplain #TYPE} ({@value #TYPE}) but
     * can also be {@link PatternFilterConfiguration#TYPE} or something else (subclasses can add more type filters).
     * @return the type, not null.
     * @since 2.9
     */
    protected final String getType() {
        return m_type;
    }

    /** Called by the load routines to check whether the argument type string is valid. This implementation
     * accepts {@link #TYPE} and {@link PatternFilterConfiguration#TYPE} (if pattern filter is enabled).
     * Subclasses override it to accept additional identifiers.
     * @param type The type identifier
     * @return if valid (if not valid a fallback is used)
     * @since 2.9
     */
    protected boolean verifyType(final String type) {
        if (TYPE.equals(type)) {
            return true;
        }
        if (isPatternFilterEnabled() && PatternFilterConfiguration.TYPE.equals(type)) {
            return true;
        }
        return false;
    }

    /** Setter for {@link #getType()}.
     * @param type the type to set, not null.
     * @throws InvalidSettingsException If the argument is null or invalid as per {@link #verifyType(String)}
     */
    final void setType(final String type) throws InvalidSettingsException {
        if (type == null || !verifyType(type)) {
            throw new InvalidSettingsException("Invalid type identifier: " + type);
        }
        m_type = type;
    }

    /**
     * @return the patternConfig
     * @since 3.4
     * @noreference This method is not intended to be referenced by clients outside KNIME core.
     */
    protected final PatternFilterConfiguration getPatternConfig() {
        return m_patternConfig;
    }

    /**
     * @return a new pattern config
     * @since 3.4
     * @noreference This method is not intended to be referenced by clients outside KNIME core.
     */
    protected PatternFilterConfiguration createPatternConfig() {
        return new PatternFilterConfiguration();
    }

}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.timeseries.node.timemissvaluehandler;

import static org.knime.core.node.util.CheckUtils.checkArgumentNotNull;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSAverageHandler;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSLastHandler;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSLinearHandler;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSMissVHandler;
import org.knime.timeseries.node.timemissvaluehandler.tshandler.TSNextHandler;

/**
 * An object that holds some the properties how to handle missing values in an time series columns (called individual)
 * or in columns of one type (called meta). This object holds all properties that can be set in one single component in
 * the missing value dialog, i.e.
 * <ul>
 * <li>name of column (or null for meta-column)</li>
 * <li>type {numerical or non-numerical}</li>
 * <li>method how to handle</li> where name and type are read only.
 * </ul>
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 */
@Deprecated
final class TimeMissingValueHandlingColSetting {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(TimeMissValueNodeModel.class);

    /** NodeSettings key: write method. */
    static final String CFG_METHOD = "miss_method";

    /** NodeSettings key: write column name (only for individual columns). */
    static final String CFG_COLNAME = "col_names";

    /** NodeSettings key: write column type. */
    static final String CFG_TYPE = "col_types";

    /** NodeSettings branch identifier for meta settings. */
    static final String CFG_META = "meta_colsetting";

    /**
     * NodeSettings branch identifier for individual settings.
     */
    static final String CFG_INDIVIDUAL = "individual_colsetting";

    /**
     * NodeSettings branch identifier for meta setting, String (Individual columns have their name as identifier).
     */
    static final String CFG_META_NUMERICAL = "meta_numeric";

    /**
     * NodeSettings branch identifier for meta setting, Double.
     */
    static final String CFG_META_NON_NUMERICAL = "meta_non_numeric";

    /** String array with names of the column or null if meta column. */
    private String[] m_names;

    /** Type of the column, e.g. NUMERICAL. */
    private ConfigType m_type;

    /** Method how to handle missing values, e.g. METHOD_MIN. */
    private HandlingMethod m_method;

    /** Private constructor, used by the load method. */
    private TimeMissingValueHandlingColSetting() {
        // no op
    }

    /**
     * Constructor for meta column setting.
     *
     * @param type the type of the meta column
     */
    TimeMissingValueHandlingColSetting(final ConfigType type) {
        m_names = null;
        m_type = checkArgumentNotNull(type, "No type given");
        setMethod(HandlingMethod.DO_NOTHING);
    }

    /**
     * Constructor for a list of columns.
     *
     * @param specs list of column specs
     */
    TimeMissingValueHandlingColSetting(final List<DataColumnSpec> specs) {
        this(TimeMissValueNodeModel.initType(specs.get(0)));
        m_names = new String[specs.size()];
        for (int i = 0; i < m_names.length; i++) {
            m_names[i] = specs.get(i).getName();
        }
    }

    /**
     * Constructor for individual column.
     *
     * @param spec the spec to the column
     */
    TimeMissingValueHandlingColSetting(final DataColumnSpec spec) {
        m_names = new String[]{spec.getName()};
        m_type = TimeMissValueNodeModel.initType(spec);
        setMethod(HandlingMethod.DO_NOTHING);
    }

    /**
     * @return the method
     */
    HandlingMethod getMethod() {
        return m_method;
    }

    /**
     * @param method the method to set
     */
    void setMethod(final HandlingMethod method) {
        m_method = method;
    }

    /**
     * @return returns the type
     */
    ConfigType getType() {
        return m_type;
    }

    /**
     * @return returns the display name or <code>null</code> if {@link #isMetaConfig()} returns <code>true</code>
     */
    String getDisplayName() {
        if (isMetaConfig()) {
            return null;
        }
        assert m_names.length > 0;
        final StringBuilder buf = new StringBuilder();
        for (String name : m_names) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(name);
        }
        return buf.toString();
    }

    /**
     * @return returns the name(s) or <code>null</code> if {@link #isMetaConfig()} returns <code>true</code>
     */
    final String[] getNames() {
        return m_names;
    }

    /**
     * Set a new list of column names.
     *
     * @param names a list of column names
     */
    final void setNames(final String[] names) {
        m_names = names;
    }

    /**
     * Set the type.
     *
     * @param type the new type
     */
    final void setType(final ConfigType type) {
        m_type = type;
    }

    /**
     * Is this config a meta-config?
     *
     * @return <code>true</code> if it is
     */
    boolean isMetaConfig() {
        return m_names == null;
    }

    /**
     * Loads settings from a NodeSettings object, used in {@link org.knime.core.node.NodeModel}.
     *
     * @param settings the (sub-) config to load from
     * @throws InvalidSettingsException if any setting is missing
     */
    void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // may be null to indicate meta config
        String[] names = null;
        if (settings.containsKey(CFG_COLNAME)) {
            try {
                names = settings.getStringArray(CFG_COLNAME);
            } catch (InvalidSettingsException ise) {
                // fallback to be compatible with <2.5
                String name = settings.getString(CFG_COLNAME);
                if (name != null) {
                    names = new String[]{name};
                }
            }
        }
        m_names = names;
        m_method = HandlingMethod.forString(settings.getString(CFG_METHOD));
        m_type = ConfigType.forString(settings.getString(CFG_TYPE));
    }

    /**
     * Save settings to config object.
     *
     * @param settings to save to
     */
    void saveSettings(final NodeSettingsWO settings) {
        if (!isMetaConfig()) {
            settings.addStringArray(CFG_COLNAME, m_names);
        }
        settings.addString(CFG_METHOD, m_method.name());
        settings.addString(CFG_TYPE, m_type.name());
    }

    /**
     * Helper that load meta settings from a config object, used in NodeModel.
     *
     * @param settings to load from
     * @return meta settings
     * @throws InvalidSettingsException if errors occur
     */
    static TimeMissingValueHandlingColSetting[] loadMetaColSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        NodeSettingsRO subConfig = settings.getNodeSettings(CFG_META);
        Map<String, TimeMissingValueHandlingColSetting> map = loadSubConfigs(subConfig);
        return map.values().toArray(new TimeMissingValueHandlingColSetting[0]);
    }

    /**
     * Helper that loads meta settings from a config object, used in NodeDialog.
     *
     * @param settings to load from
     * @param spec To be used for default init
     * @return meta settings
     */
    static TimeMissingValueHandlingColSetting[] loadMetaColSettings(final NodeSettingsRO settings,
        final DataTableSpec spec) {
        LinkedHashMap<String, TimeMissingValueHandlingColSetting> defaultsHash =
            new LinkedHashMap<String, TimeMissingValueHandlingColSetting>();
        if (spec.containsCompatibleType(IntValue.class) || spec.containsCompatibleType(DoubleValue.class)
            || spec.containsCompatibleType(DateAndTimeValue.class)) {
            defaultsHash.put(CFG_META_NON_NUMERICAL, new TimeMissingValueHandlingColSetting(ConfigType.NUMERICAL));
        }
        if (spec.containsCompatibleType(StringValue.class)) {
            defaultsHash.put(CFG_META_NUMERICAL, new TimeMissingValueHandlingColSetting(ConfigType.NON_NUMERICAL));
        }
        // lets see if the CFG_META branch is in the settings object
        if (settings.containsKey(CFG_META)) {
            NodeSettingsRO subConfig;
            try {
                subConfig = settings.getNodeSettings(CFG_META);
                Map<String, TimeMissingValueHandlingColSetting> subDefaults = loadSubConfigs(subConfig);
                defaultsHash.putAll(subDefaults);
            } catch (InvalidSettingsException ise) {
                LOGGER.debug("Error loading subconfig: " + CFG_META, ise);
            }

        }
        return defaultsHash.values().toArray(new TimeMissingValueHandlingColSetting[0]);
    }

    /**
     * Helper that load individual settings from a config object, used in NodeModel.
     *
     * @param settings to load from
     * @return individual settings
     * @throws InvalidSettingsException if errors occur
     */
    static TimeMissingValueHandlingColSetting[] loadIndividualColSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        NodeSettingsRO subConfig = settings.getNodeSettings(CFG_INDIVIDUAL);
        Map<String, TimeMissingValueHandlingColSetting> map = loadSubConfigs(subConfig);
        return map.values().toArray(new TimeMissingValueHandlingColSetting[0]);
    }

    /**
     * Helper that individual settings from a config object, used in NodeDialog.
     *
     * @param settings to load from
     * @param spec ignored, used here to differ from method that is used by {@link org.knime.core.node.NodeModel}
     * @return individual settings
     */
    static TimeMissingValueHandlingColSetting[] loadIndividualColSettings(final NodeSettingsRO settings,
        final DataTableSpec spec) {
        Map<String, TimeMissingValueHandlingColSetting> individHash =
            new LinkedHashMap<String, TimeMissingValueHandlingColSetting>();
        if (settings.containsKey(CFG_INDIVIDUAL)) {
            NodeSettingsRO subConfig;
            try {
                subConfig = settings.getNodeSettings(CFG_INDIVIDUAL);
                Map<String, TimeMissingValueHandlingColSetting> subDefaults = loadSubConfigs(subConfig);
                individHash.putAll(subDefaults);
            } catch (InvalidSettingsException ise) {
                LOGGER.debug("Error loading subconfig: " + CFG_INDIVIDUAL, ise);
            }
        }
        return individHash.values().toArray(new TimeMissingValueHandlingColSetting[0]);
    }

    /**
     * Get ColSetting objects in a map, mapping name to ColSetting.
     */
    private static Map<String, TimeMissingValueHandlingColSetting> loadSubConfigs(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        LinkedHashMap<String, TimeMissingValueHandlingColSetting> result =
            new LinkedHashMap<String, TimeMissingValueHandlingColSetting>();
        for (String key : settings.keySet()) {
            NodeSettingsRO subConfig = settings.getNodeSettings(key);
            TimeMissingValueHandlingColSetting local = new TimeMissingValueHandlingColSetting();
            try {
                local.loadSettings(subConfig);
            } catch (InvalidSettingsException ise) {
                throw new InvalidSettingsException("Unable to load sub config for key '" + key + "'", ise);
            }
            result.put(key, local);
        }
        return result;
    }

    /**
     * Saves the individual settings to a config object.
     *
     * @param colSettings the settings to write, may include meta settings (ignored)
     * @param settings to write to
     */
    static void saveIndividualsColSettings(final TimeMissingValueHandlingColSetting[] colSettings,
        final NodeSettingsWO settings) {
        NodeSettingsWO individuals = settings.addNodeSettings(CFG_INDIVIDUAL);
        for (int i = 0; i < colSettings.length; i++) {
            if (colSettings[i].isMetaConfig()) {
                continue;
            }
            String id = colSettings[i].getDisplayName();
            NodeSettingsWO subConfig = individuals.addNodeSettings(id);
            colSettings[i].saveSettings(subConfig);
        }
    }

    /**
     * Saves the meta settings to a config object.
     *
     * @param colSettings the settings to write, may include individual settings (ignored)
     * @param settings to write to
     */
    static void saveMetaColSettings(final TimeMissingValueHandlingColSetting[] colSettings,
        final NodeSettingsWO settings) {
        NodeSettingsWO defaults = settings.addNodeSettings(CFG_META);
        for (int i = 0; i < colSettings.length; i++) {
            if (!colSettings[i].isMetaConfig()) {
                continue;
            }
            ConfigType type = colSettings[i].getType();
            String id;
            switch (type) {
                case NON_NUMERICAL:
                    id = CFG_META_NUMERICAL;
                    break;
                case NUMERICAL:
                    id = CFG_META_NON_NUMERICAL;
                    break;
                default:
                    assert false;
                    id = CFG_META_NUMERICAL;
            }
            NodeSettingsWO subConfig = defaults.addNodeSettings(id);
            colSettings[i].saveSettings(subConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        if (isMetaConfig()) {
            buffer.append("META");
        } else {
            if (m_names.length == 1) {
                buffer.append(m_names[0].toString());
            } else {
                buffer.append(Arrays.toString(m_names));
            }
        }
        buffer.append(":");
        switch (m_type) {
            case NON_NUMERICAL:
                buffer.append(CFG_META_NUMERICAL);
                break;
            case NUMERICAL:
                buffer.append(CFG_META_NON_NUMERICAL);
                break;
            default:
                throw new RuntimeException();
        }
        buffer.append(":");
        buffer.append(m_method);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * The configuration type, i.e. numerical or non_numerical.
     *
     * @author Marcel Hanser
     */
    enum ConfigType {
        /**
         * Numerical values and date and time cells.
         */
        NUMERICAL,
        /**
         * All others.
         */
        NON_NUMERICAL;

        /**
         * @param string the name
         * @return ConfigType for the given string
         * @throws InvalidSettingsException if there exists no {@link ConfigType} for the given String
         */
        static ConfigType forString(final String string) throws InvalidSettingsException {
            ConfigType toReturn = null;
            try {
                toReturn = ConfigType.valueOf(string);
            } catch (IllegalArgumentException e) {
                // NOOP
            }
            return checkSettingNotNull(toReturn, "Config Type: '%s' not exist, use one of: '%s'", string,
                Arrays.toString(HandlingMethod.values()));
        }

        /**
         * @param type the type
         * @return a human readable string for the type
         */
        static String typeToString(final ConfigType type) {
            switch (type) {
                case NUMERICAL:
                    return "Numeric or date";
                case NON_NUMERICAL:
                    return "Non numeric";
                default:
                    break;
            }
            return null;
        }
    }

    /**
     * The handling method.
     *
     * @author Marcel Hanser
     */
    enum HandlingMethod {
        /** Method: Do nothing, leave untouched, available for all types. */
        DO_NOTHING("Do nothing") {
            @Override
            TSMissVHandler createTSMissVHandler() {
                return null;
            }
        },

        /** Method: Replace by next available Cell. */
        NEXT("Next") {
            @Override
            TSMissVHandler createTSMissVHandler() {
                return new TSNextHandler();
            }
        },

        /** Method: Replace by previous available Cell. */
        LAST("Last") {
            @Override
            TSMissVHandler createTSMissVHandler() {
                return new TSLastHandler();
            }
        },
        /**
         * Method: Replace by average between last and next proper value in column, available for Double and Int and
         * Date and Time.
         */
        AVERAGE("Average") {
            @Override
            TSMissVHandler createTSMissVHandler() {
                return new TSAverageHandler();
            }
        },
        /**
         * Method: Replace by linear interpolation between last and next proper value in column, available for Double
         * and Int and Date and Time.
         */
        LINEAR("Linear") {
            @Override
            TSMissVHandler createTSMissVHandler() {
                return new TSLinearHandler();
            }
        };

        /**
         * @return the handler implementation
         */
        abstract TSMissVHandler createTSMissVHandler();

        /**
         * The string to print on screen.
         */
        private final String m_toPrint;

        /**
         * @param toPrint string to print on screen
         */
        private HandlingMethod(final String toPrint) {
            this.m_toPrint = toPrint;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_toPrint;
        }

        /**
         * @param string a handler name as string
         * @return the handling method for the given string
         * @throws InvalidSettingsException if there exists no {@link HandlingMethod} for the given String
         */
        static HandlingMethod forString(final String string) throws InvalidSettingsException {
            HandlingMethod toReturn = null;
            try {
                toReturn = HandlingMethod.valueOf(string);
            } catch (IllegalArgumentException e) {
                // NOOP
            }
            return checkSettingNotNull(toReturn, "Handling method: '%s' not exist, use one of: '%s'", string,
                Arrays.toString(HandlingMethod.values()));
        }
    }
}

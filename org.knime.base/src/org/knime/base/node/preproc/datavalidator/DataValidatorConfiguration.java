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
 *   02.09.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.datavalidator;

import static org.knime.base.node.preproc.datavalidator.ConfigSerializationUtils.addEnum;
import static org.knime.base.node.preproc.datavalidator.ConfigSerializationUtils.getEnum;
import static org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.missingColumn;
import static org.knime.core.node.util.CheckUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ColumnExistenceHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The configuration object for the DataValidatorNodeModel.
 *
 * @author Marcel Hanser
 */
final class DataValidatorConfiguration {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataValidatorConfiguration.class);

    private static final String CFG_REJECTING_BEHAVIOR = "rejecting_behavior";

    /** NodeSettings which determines to remove unkown columns or not. */
    private static final String CFG_REMOVE_UNKOWN_COLUMNS = "unkown_columns_handling";

    /** NodeSettings branch identifier for meta settings. */
    private static final String CFG_INDIVDUALS = "individual_settings";

    /** Reference table spec. */
    private static final String CFG_SPEC = "data_table_spec";

    private DataTableSpec m_referenceTableSpec;

    private RejectBehavior m_failingBehavior;

    private UnknownColumnHandling m_removeUnkownColumns;

    private List<DataValidatorColConfiguration> m_individualConfigurations;

    private final boolean m_referenceSpecNeedet;

    /**
     * Constructor.
     *
     * @param referenceSpecNeedet <code>true</code> if a reference spec is expected on load and save
     */
    DataValidatorConfiguration(final boolean referenceSpecNeedet) {
        m_referenceSpecNeedet = referenceSpecNeedet;
    }

    /**
     * Default Constructor, reference spec enforced.
     */
    DataValidatorConfiguration() {
        this(true);
    }

    /**
     * @return the referenceTableSpec
     */
    DataTableSpec getReferenceTableSpec() {
        return m_referenceTableSpec;
    }

    /**
     * @param referenceTableSpec the referenceTableSpec to set
     */
    void setReferenceTableSpec(final DataTableSpec referenceTableSpec) {
        m_referenceTableSpec = referenceTableSpec;
    }

    /**
     * @return the individualConfigurations
     */
    List<DataValidatorColConfiguration> getIndividualConfigurations() {
        return m_individualConfigurations;
    }

    /**
     * @param individualConfigurations the individualConfigurations to set
     */
    void setIndividualConfigurations(final List<DataValidatorColConfiguration> individualConfigurations) {
        m_individualConfigurations = individualConfigurations;
    }

    /**
     * @return the failingBehavior
     */
    RejectBehavior getFailingBehavior() {
        return m_failingBehavior;
    }

    /**
     * @param failingBehavior the failingBehavior to set
     */
    void setFailingBehavior(final RejectBehavior failingBehavior) {
        m_failingBehavior = failingBehavior;
    }

    /**
     * @return the removeUnkownColumns
     */
    UnknownColumnHandling getUnkownColumnsHandling() {
        return m_removeUnkownColumns;
    }

    /**
     * @param removeUnkownColumns the removeUnkownColumns to set
     */
    void setRemoveUnkownColumns(final UnknownColumnHandling removeUnkownColumns) {
        m_removeUnkownColumns = removeUnkownColumns;
    }

    /**
     * Loads the configuration for the dialog with corresponding default values.
     *
     * @param settings the settings to read from
     * @param spec the specification
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        try {
            m_referenceTableSpec = DataTableSpec.load(settings.getNodeSettings(CFG_SPEC));
        } catch (InvalidSettingsException e) {
            m_referenceTableSpec = spec;
        }

        m_individualConfigurations = new ArrayList<>();

        try {
            NodeSettingsRO nodeSettings = settings.getNodeSettings(CFG_INDIVDUALS);
            for (String s : nodeSettings) {
                try {
                    m_individualConfigurations.add(DataValidatorColConfiguration.load(nodeSettings.getNodeSettings(s)));
                } catch (InvalidSettingsException e) {
                    LOGGER.info("Error while reading reading data validation settings of key: '" + s + "'", e);
                }
            }
        } catch (InvalidSettingsException e) {
            // NOOP - no settings could be found
        }

        m_removeUnkownColumns = getEnum(settings, CFG_REMOVE_UNKOWN_COLUMNS, UnknownColumnHandling.REJECT);
        m_failingBehavior = getEnum(settings, CFG_REJECTING_BEHAVIOR, RejectBehavior.FAIL_NODE);
    }

    /**
     * Loads the configuration for the model.
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_referenceSpecNeedet) {
            m_referenceTableSpec = DataTableSpec.load(settings.getNodeSettings(CFG_SPEC));
        }
        m_individualConfigurations = new ArrayList<>();

        if (settings.containsKey(CFG_INDIVDUALS)) {
            NodeSettingsRO nodeSettings = settings.getNodeSettings(CFG_INDIVDUALS);
            for (String s : nodeSettings) {
                m_individualConfigurations.add(DataValidatorColConfiguration.load(nodeSettings.getNodeSettings(s)));
            }
        }
        m_removeUnkownColumns = getEnum(settings, CFG_REMOVE_UNKOWN_COLUMNS, UnknownColumnHandling.class);
        m_failingBehavior = getEnum(settings, CFG_REJECTING_BEHAVIOR, RejectBehavior.class);
    }

    /**
     * Called from dialog's and model's save method.
     *
     * @param settings Arg settings.
     */
    void saveSettings(final NodeSettingsWO settings) {
        NodeSettingsWO individuals = settings.addNodeSettings(CFG_INDIVDUALS);
        int counter = 0;
        for (DataValidatorColConfiguration config : m_individualConfigurations) {
            NodeSettingsWO addNodeSettings = individuals.addNodeSettings("" + counter++);
            config.save(addNodeSettings);
        }
        addEnum(settings, CFG_REMOVE_UNKOWN_COLUMNS, m_removeUnkownColumns);
        addEnum(settings, CFG_REJECTING_BEHAVIOR, m_failingBehavior);
        if (m_referenceSpecNeedet) {
            m_referenceTableSpec.save(settings.addNodeSettings(CFG_SPEC));
        }
    }

    /**
     * Returns the column names mapped to the corresponding DataValidatorColConfiguration.
     *
     * @param in the input spec
     * @param conflicts conflicts collection
     * @return the column names mapped to the corresponding DataValidatorColConfiguration
     */
    Map<String, ConfigurationContainer> applyConfiguration(final DataTableSpec in,
        final DataValidatorColConflicts conflicts) {
        Map<String, ConfigurationContainer> toReturn = new LinkedHashMap<>();
        Map<String, ConfigurationContainer> directMatches = new LinkedHashMap<>();
        List<ConfigurationContainer> caseInsensitiveMatches = new ArrayList<>();

        for (DataValidatorColConfiguration config : m_individualConfigurations) {
            applyConfig(directMatches, caseInsensitiveMatches, config);
        }

        // try first to find direct matches.
        for (DataColumnSpec s : in) {
            String name = s.getName();
            ConfigurationContainer directColConfig = directMatches.remove(name);

            if (directColConfig != null) {
                directColConfig.setInputColName(name);
                toReturn.put(name, directColConfig);
            }
        }

        // now check the case insensitive matches
        for (DataColumnSpec s : in) {
            String name = s.getName();
            if (!toReturn.containsKey(name)) {
                ConfigurationContainer caseInsensitiveMatch =
                    removeFirstMatchingCaseInsensitiveConfig(name, caseInsensitiveMatches);
                if (caseInsensitiveMatch != null) {
                    caseInsensitiveMatch.setInputColName(name);
                    toReturn.put(name, caseInsensitiveMatch);
                }
            }
        }

        // check for not satisfied configurations
        for (Entry<String, ConfigurationContainer> configs : directMatches.entrySet()) {
            if (!configs.getValue().isSatisfied()
                && ColumnExistenceHandling.FAIL.equals(configs.getValue().getConfiguration()
                    .getColumnExistenceHandling())) {
                conflicts.addConflict(missingColumn(configs.getKey()));
            }
        }

        return toReturn;
    }

    /**
     * @return a set containing all configured colums
     */
    public Set<String> getConfiguredColumns() {
        Set<String> toReturn = new HashSet<>();
        for (DataValidatorColConfiguration config : m_individualConfigurations) {
            toReturn.addAll(Arrays.asList(config.getNames()));
        }
        return toReturn;
    }

    /**
     * @param directMatches
     * @param caseInsensitiveMatches
     * @param config
     */
    private void applyConfig(final Map<String, ConfigurationContainer> directMatches,
        final List<ConfigurationContainer> caseInsensitiveMatches, final DataValidatorColConfiguration config) {
        for (String name : config.getNames()) {
            ConfigurationContainer configurationContainer = new ConfigurationContainer(name, null, config);
            if (!directMatches.containsKey(name)) {
                directMatches.put(name, configurationContainer);

                if (config.isCaseInsensitiveNameMatching()) {
                    caseInsensitiveMatches.add(configurationContainer);
                }
            }
        }
    }

    /**
     * Removes all matching case insensitive matchings with the same configuration.
     *
     * @param name the input column name.
     * @param caseInsensitiveMatches the case insensitive matching possibilities
     */
    private static ConfigurationContainer removeFirstMatchingCaseInsensitiveConfig(final String name,
        final List<ConfigurationContainer> caseInsensitiveMatches) {
        Iterator<ConfigurationContainer> items = caseInsensitiveMatches.iterator();

        String upperName = name.toUpperCase();

        while (items.hasNext()) {
            ConfigurationContainer next = items.next();
            if (next.isSatisfied()) {
                items.remove();
            }
            if (!next.isSatisfied() && upperName.equals(next.getRefColName().toUpperCase())) {
                next.setInputColName(name);
                items.remove();
                return next;
            }
        }
        return null;
    }

    /**
     * A mapping from a reference column to an input column which also contains the configuration.
     *
     * @author Marcel Hanser
     */
    static final class ConfigurationContainer {
        private final String m_refColName;

        private final DataValidatorColConfiguration m_configuration;

        private String m_inputColName;

        /**
         * @param refColName
         * @param inputColName
         * @param configuration
         */
        private ConfigurationContainer(final String refColName, final String inputColName,
            final DataValidatorColConfiguration configuration) {
            super();
            this.m_refColName = checkNotNull(refColName);
            this.m_inputColName = inputColName;
            this.m_configuration = checkNotNull(configuration);
        }

        /**
         * @return the refColName
         */
        String getRefColName() {
            return m_refColName;
        }

        /**
         * @return the inputColName
         */
        String getInputColName() {
            return m_inputColName;
        }

        /**
         * @param inputColName the inputColName to set
         */
        void setInputColName(final String inputColName) {
            this.m_inputColName = inputColName;
        }

        /**
         * @return <code>true</code> if the configuration has a fitting column in the input spec
         */
        boolean isSatisfied() {
            return m_inputColName != null;
        }

        /**
         * @return the configuration
         */
        DataValidatorColConfiguration getConfiguration() {
            return m_configuration;
        }
    }

    /**
     * The general failing behavior.
     *
     * @author Marcel Hanser
     */
    enum RejectBehavior {
        /**
         * Fail node.
         */
        FAIL_NODE("Fail node (no data scan if structure differs)"),
        /**
         * Activate second port.
         */
        OUTPUT_TO_PORT_CHECK_DATA("Deactivate first output port (always scans data)");
        private final String m_description;

        private RejectBehavior(final String description) {
            m_description = description;
        }

        @Override
        public String toString() {
            return m_description;
        }
    }

    /**
     * Handling of unkown columns.
     *
     * @author Marcel Hanser
     */
    enum UnknownColumnHandling {
        /**
         * Unknown columns cause the node to fail.
         */
        REJECT("Don't allow unknown columns"),
        /**
         * Unknown columns are removed.
         */
        REMOVE("Remove unkown columns"),
        /**
         * Unknown columns are added to the end.
         */
        IGNORE("Sort them to the end");

        private final String m_description;

        private UnknownColumnHandling(final String description) {
            m_description = description;
        }

        @Override
        public String toString() {
            return m_description;
        }
    }
}

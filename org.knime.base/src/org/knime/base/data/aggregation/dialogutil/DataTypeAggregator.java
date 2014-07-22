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
 *   06.07.2014 (koetter): created
 */
package org.knime.base.data.aggregation.dialogutil;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethodDecorator;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

import com.sun.org.apache.xml.internal.security.encryption.AgreementMethod;

/**
 * {@link AggregationMethodDecorator} that stores a {@link DataType} in addition to the {@link AggregationMethod}
 * information.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DataTypeAggregator extends AggregationMethodDecorator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataTypeAggregator.class);

    private static final String CNFG_DATA_TYPE_AGGR_SECTION = "dataTypeAggregators";
    private static final String CNFG_COL_TYPES = "columnTypes";

    private final DataType m_type;

    /**
     * @param type {@link DataType}
     * @param method {@link AgreementMethod}
     * @param inclMissing <code>true</code> if missing values should be considered
     */
    public DataTypeAggregator(final DataType type, final AggregationMethod method, final boolean inclMissing) {
        super(method, inclMissing);
        m_type = type;
    }

    /**
     * @param type {@link DataType}
     * @param method {@link AggregationMethod}
     */
    public DataTypeAggregator(final DataType type, final AggregationMethod method) {
        super(method);
        m_type = type;
    }

    /**
     * @return the type
     */
    public DataType getDataType() {
        return m_type;
    }

    /**
     * @param dataType {@link DataType} to check
     * @return <code>true</code> if the given {@link DataType} is compatible to the user selected {@link DataType}
     * of this aggregator
     */
    public boolean isCompatibleType(final DataType dataType) {
        return m_type.equals(dataType) || m_type.isASuperTypeOf(dataType);
    }
    /**
     * Creates a {@link List} with all {@link DataTypeAggregator}s that were
     * stored in the settings.
     *
     * @param settings the settings object to read from
     * @param key the unique settings key
     * @return {@link List} with the {@link DataTypeAggregator}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<DataTypeAggregator> loadAggregators(final NodeSettingsRO settings, final String key)
            throws InvalidSettingsException {
        return loadAggregators(settings, key, null);
    }
    /**
     * Creates a {@link List} with all {@link DataTypeAggregator}s that were
     * stored in the settings.
     *
     * @param settings the settings object to read from
     * @param key the unique settings key
     * @param spec {@link DataTableSpec} of the input table if available
     * @return {@link List} with the {@link DataTypeAggregator}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<DataTypeAggregator> loadAggregators(final NodeSettingsRO settings, final String key,
        final DataTableSpec spec) throws InvalidSettingsException {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        final Config cnfg = settings.getConfig(key);
        final DataType[] colTypes = cnfg.getDataTypeArray(CNFG_COL_TYPES);
        final String[] aggrMethods = cnfg.getStringArray(CNFG_AGGR_METHODS);
        boolean[] inclMissingVals = null;
        inclMissingVals = cnfg.getBooleanArray(CNFG_INCL_MISSING_VALS);
        final List<DataTypeAggregator> aggrList = new LinkedList<>();
        if (aggrMethods.length != colTypes.length) {
            throw new InvalidSettingsException("Data type array and aggregation method array should be of equal size");
        }
        for (int i = 0, length = aggrMethods.length; i < length; i++) {
            final AggregationMethod method = AggregationMethods.getMethod4Id(aggrMethods[i]);
            final boolean inclMissingVal;
            if (inclMissingVals != null) {
                inclMissingVal = inclMissingVals[i];
            } else {
                //get the default behavior of the method
                inclMissingVal = method.inclMissingCells();
            }
            final DataTypeAggregator aggr = new DataTypeAggregator(colTypes[i], method, inclMissingVal);
            if (aggr.hasOptionalSettings()) {
                try {
                    final NodeSettingsRO operatorSettings = settings.getNodeSettings(
                                   createSettingsKey(aggr));
                    if (spec != null) {
                        //this method is called from the dialog
                        aggr.loadSettingsFrom(operatorSettings, spec);
                    } else {
                        //this method is called from the node model where we do not
                        //have the DataTableSpec
                        aggr.loadValidatedSettings(operatorSettings);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while loading settings for aggreation operator '"
                        + aggr.getId() + "', reason: " + e.getMessage());
                }
            }
            aggrList.add(aggr);
        }
        return aggrList;
    }

    /**
     * @param aggr
     * @return
     */
    private static String createSettingsKey(final DataTypeAggregator aggr) {
        //the method id and the data type are unique since
        return aggr.getId() + "_" + aggr.getDataType().toString();
    }

    /**
     * @param settings the settings object to write to
     * @param key the unique settings key
     * @param aggregators the {@link DataTypeAggregator} objects to save
     */
    public static void saveAggregators(final NodeSettingsWO settings, final String key,
        final Collection<DataTypeAggregator> aggregators) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (aggregators == null) {
            throw new NullPointerException("cols must not be null");
        }
        final String[] aggrMethods = new String[aggregators.size()];
        final boolean[] inclMissingVals = new boolean[aggregators.size()];
        final DataType[] types = new DataType[aggregators.size()];
        int i = 0;
        for (DataTypeAggregator aggr : aggregators) {
            final AggregationMethod method = aggr.getMethodTemplate();
            aggrMethods[i] = aggr.getId();
            types[i] = aggr.getDataType();
            inclMissingVals[i] = aggr.inclMissingCells();
            if (aggr.hasOptionalSettings()) {
                try {
                    final NodeSettingsWO operatorSettings = settings.addNodeSettings(createSettingsKey(aggr));
                    method.saveSettingsTo(operatorSettings);
                } catch (Exception e) {
                    LOGGER.error(
                        "Exception while saving settings for aggreation operator '"
                        + aggr.getId() + "', reason: " + e.getMessage());
                }
            }
            i++;
        }
        final Config cnfg = settings.addConfig(CNFG_DATA_TYPE_AGGR_SECTION);
        cnfg.addDataTypeArray(CNFG_COL_TYPES, types);
        cnfg.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
        cnfg.addBooleanArray(CNFG_INCL_MISSING_VALS, inclMissingVals);
    }

    /**
     * Validates the operator specific settings of all {@link DataTypeAggregator}s
     * that require additional settings.
     *
     * @param settings the settings to validate
     * @param aggregators the operators to validate
     * @throws InvalidSettingsException if the settings of an operator are not valid
     */
    public static void validateSettings(final NodeSettingsRO settings, final List<DataTypeAggregator> aggregators)
            throws InvalidSettingsException {
        for (int i = 0, length = aggregators.size(); i < length; i++) {
            final DataTypeAggregator aggr = aggregators.get(i);
            if (aggr.hasOptionalSettings()) {
                final NodeSettingsRO operatorSettings =
                    settings.getNodeSettings(createSettingsKey(aggr));
                try {
                    aggr.validateSettings(operatorSettings);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException(
                       "Invalid settings for aggreation operator '" + aggr.getLabel() + "' for datat type '"
                        + aggr.getDataType() + "', reason: " + e.getMessage());
                }
            }
        }
    }
}

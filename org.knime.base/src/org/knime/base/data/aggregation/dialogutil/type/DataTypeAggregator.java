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
package org.knime.base.data.aggregation.dialogutil.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethodDecorator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * {@link AggregationMethodDecorator} that stores a {@link DataType} in addition to the {@link AggregationMethod}
 * information.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DataTypeAggregator extends AggregationMethodDecorator {

    private static final String CNFG_DATA_TYPE = "dataType";

    private final DataType m_type;

    /**
     * @param type {@link DataType}
     * @param method {@link AggregationMethod}
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
        if (!settings.containsKey(key)) {
            return Collections.EMPTY_LIST;
        }
        final NodeSettingsRO root = settings.getNodeSettings(key);
        final Set<String> settingsKeys = root.keySet();
        final List<DataTypeAggregator> aggrList = new ArrayList<>(settingsKeys.size());
        for (String settingsKey : settingsKeys) {
            final NodeSettingsRO cfg = root.getNodeSettings(settingsKey);
            final DataType colType = cfg.getDataType(CNFG_DATA_TYPE);
            final boolean inclMissing = cfg.getBoolean(CNFG_INCL_MISSING_VALS);
            final AggregationMethod method = AggregationMethodDecorator.loadMethod(spec, cfg);
            aggrList.add(new DataTypeAggregator(colType, method, inclMissing));
        }
        return aggrList;
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
        final NodeSettingsWO root = settings.addNodeSettings(key);
        int idx = 0;
        for (DataTypeAggregator aggr : aggregators) {
            final NodeSettingsWO cfg = root.addNodeSettings("f_" + idx++);
            cfg.addDataType(CNFG_DATA_TYPE, aggr.getDataType());
            cfg.addBoolean(CNFG_INCL_MISSING_VALS, aggr.inclMissingCells());
            AggregationMethodDecorator.saveMethod(cfg, aggr.getMethodTemplate());
        }
    }
}

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
 *   20.08.2014 (koetter): created
 */
package org.knime.base.node.io.database.groupby.dialog.column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.base.node.io.database.groupby.dialog.AbstractDBAggregationFunctionRow;
import org.knime.base.node.io.database.groupby.dialog.DBAggregationFunctionProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
public class DBColumnAggregationFunctionRow extends AbstractDBAggregationFunctionRow<DBAggregationFunction> {

    private static final String CNFG_COL_NAMES = "columnName";
    private static final String CNFG_COL_TYPES = "columnType";

    private final DataColumnSpec m_col;

    /**
     * @param col the {@link DataColumnSpec} of the column to aggregate
     * @param function the {@link DBAggregationFunction} to use
     *
     */
    public DBColumnAggregationFunctionRow(final DataColumnSpec col, final DBAggregationFunction function) {
        super(function);
        m_col = col;
    }

    /**
     * @return the {@link DataColumnSpec} of the column to aggregate
     */
    public DataColumnSpec getColumnSpec() {
        return m_col;
    }

    /**
     * @param settings {@link NodeSettingsWO}
     * @param key the config key
     * @param rows the {@link DBColumnAggregationFunctionRow}s to save
     */
    public static void saveFunctions(final NodeSettingsWO settings, final String key,
        final List<DBColumnAggregationFunctionRow> rows) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (rows == null) {
            return;
        }
        final NodeSettingsWO root = settings.addNodeSettings(key);
        for (int i = 0, length = rows.size(); i < length; i++) {
            final NodeSettingsWO cfg = root.addNodeSettings("f_" + i);
            final DBColumnAggregationFunctionRow row = rows.get(i);
            DataColumnSpec spec = row.getColumnSpec();
            cfg.addString(CNFG_COL_NAMES, spec.getName());
            cfg.addDataType(CNFG_COL_TYPES, spec.getType());
            AbstractDBAggregationFunctionRow.saveFunction(cfg, row.getFunction());
        }
    }

    /**
     * Loads the functions and handles invalid aggregation functions graceful.
     * @param settings {@link NodeSettingsRO}
     * @param key the config key
     * @param dbIdentifier the {@link AggregationFunctionProvider}
     * @param tableSpec the input {@link DataTableSpec}
     * @return {@link List} of {@link DBColumnAggregationFunctionRow}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<DBColumnAggregationFunctionRow> loadFunctions(final NodeSettingsRO settings, final String key,
        final String dbIdentifier, final DataTableSpec tableSpec) throws InvalidSettingsException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (dbIdentifier == null || dbIdentifier.isEmpty()) {
            throw new IllegalArgumentException("dbIdentifier must not be empty");
        }
        if (settings == null || !settings.containsKey(key)) {
            return Collections.EMPTY_LIST;
        }
        final DatabaseUtility utility = DatabaseUtility.getUtility(dbIdentifier);
        DBAggregationFunctionProvider functionProvider = new DBAggregationFunctionProvider(utility);
        final NodeSettingsRO root = settings.getNodeSettings(key);
        final Set<String> settingsKeys = root.keySet();
        final List<DBColumnAggregationFunctionRow> colAggrList = new ArrayList<>(settingsKeys.size());
        for (String settingsKey : settingsKeys) {
            final NodeSettingsRO cfg = root.getNodeSettings(settingsKey);
            final String colName = cfg.getString(CNFG_COL_NAMES);
            final DataType colType = cfg.getDataType(CNFG_COL_TYPES);
            final DataColumnSpec colSpec = new DataColumnSpecCreator(colName, colType).createSpec();
            DBAggregationFunction function =
                    AbstractDBAggregationFunctionRow.loadFunction(tableSpec, functionProvider, cfg);
            final DBColumnAggregationFunctionRow aggrFunctionRow =
                    new DBColumnAggregationFunctionRow(colSpec, function);
            colAggrList.add(aggrFunctionRow);
        }
        return colAggrList;
    }

    /**
     * @param manipulator the {@link StatementManipulator}
     * @param tableName the name of the table the column belongs to
     * @return the sql fragment e.g. SUM(tableName.colname)
     */
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName) {
        return getFunction().getSQLFragment(manipulator, tableName, getColumnSpec().getName());
    }
}

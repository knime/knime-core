/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnominal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Config proxy of node.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainConfiguration {

    /**
     * The unknown cell.
     */
    static final StringCell UNKNOWN_VALUES_CELL = new StringCell(
        "---sabbel krababbel - new unkown column dummy do not touch! {}-)");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EditNominalDomainConfiguration.class);

    private static final String COLUMNS = "columns";

    private static final String COLUMN_ORDERING = "column-domain-value-ordering";

    private static final String COLUMN_CREATED_VALS = "created-domain-values";

    private Map<String, List<DataCell>> m_colMapping = new HashMap<String, List<DataCell>>();

    private Map<String, Set<DataCell>> m_createdDomainValues = new HashMap<String, Set<DataCell>>();

    private static final String IGNORE_NOT_PRESENT_COLS = "ignore-not-present-col";

    private static final String IGNORE_NOT_MATHING_TYPES = "ignore-not-matching-types";

    private boolean m_ignoreNotExistingColumns = false;

    private boolean m_ignoreWrongTypes = false;

    /**
     * Loads the configuration for the dialog with corresponding default values.
     *
     * @param settings the settings to load
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings) {
        m_ignoreWrongTypes = settings.getBoolean(IGNORE_NOT_MATHING_TYPES, false);
        m_ignoreNotExistingColumns = settings.getBoolean(IGNORE_NOT_PRESENT_COLS, false);

        if (settings.containsKey(COLUMNS)) {
            try {
                loadColumnMapping(settings);
            } catch (InvalidSettingsException e) {
                LOGGER.error("Error on loading settings", e);
            }
        }

    }

    /**
     * Loads the configuration for the model.
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreWrongTypes = settings.getBoolean(IGNORE_NOT_MATHING_TYPES);
        m_ignoreNotExistingColumns = settings.getBoolean(IGNORE_NOT_PRESENT_COLS);

        loadColumnMapping(settings);
    }

    /**
     * Called from dialog's and model's save method.
     *
     * @param settings Arg settings.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean(IGNORE_NOT_MATHING_TYPES, m_ignoreWrongTypes);
        settings.addBoolean(IGNORE_NOT_PRESENT_COLS, m_ignoreNotExistingColumns);

        NodeSettingsWO cols = settings.addNodeSettings(COLUMNS);

        for (Map.Entry<String, List<DataCell>> entry : m_colMapping.entrySet()) {
            NodeSettingsWO col = cols.addNodeSettings(entry.getKey());
            col.addDataCellArray(COLUMN_ORDERING, entry.getValue().toArray(new DataCell[entry.getValue().size()]));
            Set<DataCell> createdValues = m_createdDomainValues.get(entry.getKey());

            if (createdValues != null && !createdValues.isEmpty()) {
                col.addDataCellArray(COLUMN_CREATED_VALS, //
                    createdValues.toArray(new DataCell[createdValues.size()]));
            }
        }
    }

    /**
     * Loads the column settings.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    private void loadColumnMapping(final NodeSettingsRO settings) throws InvalidSettingsException {

        NodeSettingsRO nodeSettings = settings.getNodeSettings(COLUMNS);
        for (String cols : nodeSettings) {
            NodeSettingsRO colSettings = nodeSettings.getNodeSettings(cols);
            DataCell[] dataCellArray = colSettings.getDataCellArray(COLUMN_ORDERING, new DataCell[0]);
            DataCell[] createdSettings = colSettings.getDataCellArray(COLUMN_CREATED_VALS, new DataCell[0]);

            CheckUtils.checkSetting(dataCellArray != null, "invalid settings for column '%s'", cols);

            m_colMapping.put(cols, Arrays.asList(dataCellArray));
            m_createdDomainValues.put(cols, new HashSet<DataCell>(Arrays.asList(createdSettings)));
        }
    }

    /**
     * Returns an immutable set of columns for which a configuration exists. Never <code>null</code>.
     *
     * @return an immutable set of columns for which a configuration exists
     */
    Set<String> getConfiguredColumns() {
        return Collections.unmodifiableSet(m_colMapping.keySet());
    }

    /**
     * Returns an immutable list of the ordered DataCell of the given column or <code>null</code> if no sorting is
     * configured for the given column.
     *
     * @param columnName the column
     * @return an immutable list of the ordered DataCell of the given column or <code>null</code>
     */
    List<DataCell> getSorting(final String columnName) {
        List<DataCell> list = m_colMapping.get(columnName);
        return list == null ? null : Collections.unmodifiableList(m_colMapping.get(columnName));
    }

    /**
     * Returns <code>true</code> if the cell is a within this node created value.
     *
     * @param columnName the column
     * @param cell the cell
     * @return <code>true</code> if the cell is a within this node created value
     */
    boolean isCreatedValue(final String columnName, final DataCell cell) {
        return m_createdDomainValues.containsKey(columnName) ? m_createdDomainValues.get(columnName).contains(cell)
            : false;
    }

    /**
     * Adds a cell which is created within this node to the corresponding set.
     *
     * @param columnName the name
     * @param cell the cell
     */
    void addCreatedValue(final String columnName, final DataCell cell) {
        Set<DataCell> set = m_createdDomainValues.get(columnName);
        if (set == null) {
            set = new HashSet<DataCell>();
            m_createdDomainValues.put(columnName, set);
        }
        set.add(cell);
    }

    /**
     * Sets the sorting for the given column. Previous settings are overriden.
     *
     * @param columnName the column
     * @param cells the sorting
     */
    void setSorting(final String columnName, final List<DataCell> cells) {
        m_colMapping.put(columnName, new ArrayList<DataCell>(cells));
    }

    /**
     * Removes the stored sorting and created values for the given column.
     *
     * @param columnName the column
     */
    void removeSorting(final String columnName) {
        m_colMapping.remove(columnName);
        m_createdDomainValues.remove(columnName);
    }

    /**
     * Returns <code>true</code> if there is a sorting configuration for given column.
     *
     * @param columnName the column
     * @return <code>true</code> if there is a sorting configuration for given column.
     */
    boolean isConfiguredColumn(final String columnName) {
        return m_colMapping.containsKey(columnName);
    }

    /**
     * @return the ignoreNotExistingColumns
     */
    boolean isIgnoreNotExistingColumns() {
        return m_ignoreNotExistingColumns;
    }

    /**
     * @param ignoreNotExistingColumns the ignoreNotExistingColumns to set
     */
    void setIgnoreNotExistingColumns(final boolean ignoreNotExistingColumns) {
        m_ignoreNotExistingColumns = ignoreNotExistingColumns;
    }

    /**
     * @return the ignoreWrongTypes
     */
    boolean isIgnoreWrongTypes() {
        return m_ignoreWrongTypes;
    }

    /**
     * @param ignoreWrongTypes the ignoreWrongTypes to set
     */
    void setIgnoreWrongTypes(final boolean ignoreWrongTypes) {
        m_ignoreWrongTypes = ignoreWrongTypes;
    }
}

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.differ;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.knime.testing.internal.diffcheckers.CheckerUtil;
import org.knime.testing.internal.diffcheckers.EqualityChecker;

/**
 * Settings for the difference checker node.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class DifferenceCheckerSettings {
    private final Map<String, String> m_checkerPerColumn = new HashMap<String, String>();

    private final Map<String, NodeSettings> m_perColumnConfig = new HashMap<String, NodeSettings>();

    /**
     * Returns a collection with all configured columns.
     *
     * @return a collection with column names
     */
    public Collection<String> configuredColumns() {
        return m_checkerPerColumn.keySet();
    }

    /**
     * Returns the class name of the checker factory for the given column. If not factory is registered for the column
     * <code>null</code> is returned.
     *
     * @param columnName a column name
     * @return a class name or <code>null</code>
     */
    public String checkerFactoryClassName(final String columnName) {
        return m_checkerPerColumn.get(columnName);
    }

    /**
     * Returns the configured checker factory for the given column name. If no factory is configured, <code>null</code>
     * is returned.
     *
     * @param columnName a column name
     * @return a checker factory or <code>null</code>
     */
    public DifferenceCheckerFactory<? extends DataValue> checkerFactory(final String columnName) {
        String className = m_checkerPerColumn.get(columnName);
        if (className == null) {
            return null;
        } else {
            return CheckerUtil.instance.getFactory(className);
        }
    }

    /**
     * Sets the checker factory for the given column name. Both arguments must be non-<code>null</code>.
     *
     * @param columnName a column name
     * @param checkerFactory a checker factory
     */
    public void checkerFactory(final String columnName,
                               final DifferenceCheckerFactory<? extends DataValue> checkerFactory) {
        if (columnName == null) {
            throw new IllegalArgumentException("Column name must not be null");
        }
        if (checkerFactory == null) {
            throw new IllegalArgumentException("Checker factory must not be null");
        }
        m_checkerPerColumn.put(columnName, checkerFactory.getClass().getName());
        m_perColumnConfig.put(columnName, new NodeSettings("internals"));
    }

    /**
     * Returns the internal configuration for the checker for the given column. If no internals are available
     * <code>null</code> is returned.
     *
     * @param columnName a column name
     * @return internal settings or <code>null</code>
     */
    public NodeSettings internalsForColumn(final String columnName) {
        return m_perColumnConfig.get(columnName);
    }

    /**
     * Creates a configured checker for the given column. If no checker factory is registered for the column,
     * <code>null</code> is returned.
     *
     * @param columnName a column name
     * @return a checker or <code>null</code>
     * @throws InvalidSettingsException if the internal settings for the checker cannot be loaded
     */
    public DifferenceChecker<? extends DataValue> createCheckerForColumn(final String columnName)
            throws InvalidSettingsException {
        String className = m_checkerPerColumn.get(columnName);
        if (className == null) {
            return null;
        } else {
            DifferenceChecker<? extends DataValue> checker = CheckerUtil.instance.getFactory(className).newChecker();
            checker.loadSettings(internalsForColumn(columnName));
            return checker;
        }
    }

    /**
     * Loads the settings from the given settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_checkerPerColumn.clear();
        m_perColumnConfig.clear();

        for (String key : settings) {
            if (key.startsWith("columnConfig_")) {
                NodeSettingsRO columnConfig = settings.getNodeSettings(key);
                String columnName = key.substring("columnConfig_".length());
                String checkerFactoryClass = columnConfig.getString("checkerFactory");

                m_checkerPerColumn.put(columnName, checkerFactoryClass);
                if (columnConfig.containsKey("internals")) {
                    NodeSettings internals = new NodeSettings("internals");
                    columnConfig.getNodeSettings("internals").copyTo(internals);
                    m_perColumnConfig.put(columnName, internals);
                }
            }
        }
    }

    /**
     * Loads the settings from the given settings object, using default values for missing settings. By default the
     * {@link EqualityChecker} is used for unconfigured columns.
     *
     * @param settings a node settings object
     * @param spec the spec of the reference table
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_checkerPerColumn.clear();
        m_perColumnConfig.clear();

        for (DataColumnSpec dcs : spec) {
            String colName = dcs.getName();

            if (settings.containsKey("columnConfig_" + colName)) {
                try {
                    NodeSettingsRO columnConfig = settings.getNodeSettings("columnConfig_" + colName);
                    String checkerFactoryClass = columnConfig.getString("checkerFactory");

                    m_checkerPerColumn.put(colName, checkerFactoryClass);
                    if (columnConfig.containsKey("internals")) {
                        NodeSettings internals = new NodeSettings("internals");
                        try {
                            columnConfig.getNodeSettings("internals").copyTo(internals);
                            m_perColumnConfig.put(colName, internals);
                        } catch (InvalidSettingsException ex) {
                            m_perColumnConfig.put(colName, new NodeSettings("internals"));
                        }
                    } else {
                        m_perColumnConfig.put(colName, new NodeSettings("internals"));
                    }
                } catch (InvalidSettingsException ex) {
                    m_checkerPerColumn.put(colName, EqualityChecker.Factory.class.getName());
                    m_perColumnConfig.put(colName, new NodeSettings("internals"));
                }
            } else {
                m_checkerPerColumn.put(colName, EqualityChecker.Factory.class.getName());
                m_perColumnConfig.put(colName, new NodeSettings("internals"));
            }
        }
    }

    /**
     * Saves the settings into the given settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        for (String columnName : m_checkerPerColumn.keySet()) {
            NodeSettingsWO columnConfig = settings.addNodeSettings("columnConfig_" + columnName);
            columnConfig.addString("checkerFactory", m_checkerPerColumn.get(columnName));

            NodeSettings internals = m_perColumnConfig.get(columnName);
            if ((internals != null) && (internals.keySet().size() > 0)) {
                columnConfig.addNodeSettings(internals);
            }
        }
    }
}

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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.12.2009 (meinl): created
 */
package org.knime.base.node.meta.looper.columnlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the column list loop start node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnListLoopStartSettings {
    private final List<String> m_alwaysIncludeColumns = new ArrayList<String>();

    private final List<String> m_iterateOverColumns = new ArrayList<String>();

    private boolean m_iterateAllColumns;

    /**
     * Returns if always all columns should be iterated over.
     *
     * @return <code>true</code> if all columns should be iterated over
     *         (ignoring {@link #iterateOverColumns()}), <code>false</code> if
     *         the iterated columns should be set explicitly
     * @since 2.4
     */
    public boolean iterateAllColumns() {
        return m_iterateAllColumns;
    }

    /**
     * Sets if always all columns should be iterated over.
     *
     * @param b <code>true</code> if all columns should be iterated over
     *         (ignoring {@link #iterateOverColumns()}), <code>false</code> if
     *         the iterated columns should be set explicitly
     * @since 2.4
     */
    public void iterateAllColumns(final boolean b) {
        m_iterateAllColumns = b;
    }

    /**
     * Returns a collection with the columns that should always be included in
     * all iterations.
     *
     * @return a collection with column names
     */
    public Collection<String> alwaysIncludeColumns() {
        return m_alwaysIncludeColumns;
    }

    /**
     * Sets the collection of columns that should always be included in all
     * iterations.
     *
     * @param colNames a collection with column names
     */
    public void alwaysIncludeColumns(final Collection<String> colNames) {
        m_alwaysIncludeColumns.clear();
        m_alwaysIncludeColumns.addAll(colNames);
    }

    /**
     * Returns a list with the columns over which the loop should iterate.
     *
     * @return a list with column names
     */
    public List<String> iterateOverColumns() {
        return m_iterateOverColumns;
    }

    /**
     * Sets the collection with the columns over which the loop should iterate.
     *
     * @param colNames a list with column names
     */
    public void iterateOverColumn(final Collection<String> colNames) {
        m_iterateOverColumns.clear();
        m_iterateOverColumns.addAll(colNames);
    }

    /**
     * Saves the settings to the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray("alwaysIncludeColumns", m_alwaysIncludeColumns
                .toArray(new String[m_alwaysIncludeColumns.size()]));
        settings.addStringArray("iterateOverColumns", m_iterateOverColumns
                .toArray(new String[m_iterateOverColumns.size()]));
        settings.addBoolean("iterateAllColumns", m_iterateAllColumns);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if a setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_alwaysIncludeColumns.clear();
        for (String s : settings.getStringArray("alwaysIncludeColumns")) {
            m_alwaysIncludeColumns.add(s);
        }

        m_iterateOverColumns.clear();
        for (String s : settings.getStringArray("iterateOverColumns")) {
            m_iterateOverColumns.add(s);
        }

        /** @since 2.4 */
        m_iterateAllColumns = settings.getBoolean("iterateAllColumns", false);
    }

    /**
     * Loads the settings from the node settings object, using default values if
     * a setting is missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_alwaysIncludeColumns.clear();
        for (String s : settings.getStringArray("alwaysIncludeColumns",
                new String[0])) {
            m_alwaysIncludeColumns.add(s);
        }

        m_iterateOverColumns.clear();
        for (String s : settings.getStringArray("iterateOverColumns",
                new String[0])) {
            m_iterateOverColumns.add(s);
        }

        m_iterateAllColumns = settings.getBoolean("iterateAllColumns", false);
    }
}

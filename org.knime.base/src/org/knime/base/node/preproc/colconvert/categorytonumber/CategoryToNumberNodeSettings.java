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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.08.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings object of the Category2Number node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberNodeSettings {
    private static final String INCLUDED_COLUMNS = "included_columns";
    private static final String INCLUDE_ALL = "include_all";
    private static final String APPEND_COLUMN = "append_columns";
    private static final String COLUMN_SUFFIX = "column_suffix";
    private static final String START_INDEX = "start_index";
    private static final String INCREMENT = "increment";
    private static final String MAX_CATEGORIES = "max_categories";
    private static final int DEFAULT_MAX_CATEGORIES = 100;
    private static final String DEFAULT_VALUE = "default_value";
    private static final String MAP_MISSING_TO = "map_missing_to";

    /** columns used by the learner. */
    private String[] m_includedColumns;
    private boolean m_includeAll;
    /** Define names of new columns. */
    private boolean m_appendColumns;
    private String m_columnSuffix;
    /** controls content of output columns. */
    private int m_startIndex;
    private int m_increment;
    /** maximum number of allowed categories. */
    private int m_maxCategories;
    /** corner cases for the applier. */
    private DataCell m_defaultValue;
    private DataCell m_mapMissingTo;

    /**
     * Create an instance with default values.
     */
    public CategoryToNumberNodeSettings() {
        m_includedColumns = null;
        m_includeAll = false;
        m_appendColumns = true;
        m_columnSuffix = " (to number)";
        m_startIndex = 0;
        m_increment = 1;
        m_maxCategories = DEFAULT_MAX_CATEGORIES;
        m_defaultValue = DataType.getMissingCell();
        m_mapMissingTo = DataType.getMissingCell();
    }

    /**
     * @return the includedColumns
     */
    String[] getIncludedColumns() {
        return m_includedColumns;
    }

    /**
     * @param includedColumns the includedColumns to set
     */
    void setIncludedColumns(final String[] includedColumns) {
        m_includedColumns = includedColumns;
    }

    /**
     * @return the includeAll
     */
    boolean getIncludeAll() {
        return m_includeAll;
    }

    /**
     * @param includeAll the includeAll to set
     */
    void setIncludeAll(final boolean includeAll) {
        m_includeAll = includeAll;
    }


    /**
     * @return the appendColumns
     */
    boolean getAppendColumns() {
        return m_appendColumns;
    }

    /**
     * @param appendColumns the appendColumns to set
     */
    void setAppendColumns(final boolean appendColumns) {
        m_appendColumns = appendColumns;
    }

    /**
     * @return the columnSuffix
     */
    String getColumnSuffix() {
        return m_columnSuffix;
    }

    /**
     * @param columnSuffix the columnSuffix to set
     */
    void setColumnSuffix(final String columnSuffix) {
        m_columnSuffix = columnSuffix;
    }

    /**
     * @return the startIndex
     */
    int getStartIndex() {
        return m_startIndex;
    }

    /**
     * @param startIndex the startIndex to set
     */
    void setStartIndex(final int startIndex) {
        m_startIndex = startIndex;
    }

    /**
     * @return the increment
     */
    int getIncrement() {
        return m_increment;
    }

    /**
     * @param increment the increment to set
     */
    void setIncrement(final int increment) {
        m_increment = increment;
    }

    /**
     * @return the maxCategories
     */
    int getMaxCategories() {
        return m_maxCategories;
    }

    /**
     * @param maxCategories the maxCategories to set
     */
    void setMaxCategories(final int maxCategories) {
        m_maxCategories = maxCategories;
    }

    /**
     * @return the defaultValue
     */
    DataCell getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * @param dataCell the defaultValue to set
     */
    void setDefaultValue(final DataCell dataCell) {
        m_defaultValue = dataCell;
    }

    /**
     * @return the mapMissingTo
     */
    DataCell getMapMissingTo() {
        return m_mapMissingTo;
    }

    /**
     * @param mapMissingTo the mapMissingTo to set
     */
    void setMapMissingTo(final DataCell mapMissingTo) {
        m_mapMissingTo = mapMissingTo;
    }

    /** Called from dialog when settings are to be loaded.
     * @param settings To load from
     */
    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_includedColumns = settings.getStringArray(INCLUDED_COLUMNS,
                (String[])null);
        m_includeAll = settings.getBoolean(INCLUDE_ALL, false);
        /** Define names of new columns. */
        m_appendColumns = settings.getBoolean(APPEND_COLUMN, true);
        m_columnSuffix = settings.getString(COLUMN_SUFFIX, " (to number)");
        /** controls content of output columns. */
        m_startIndex = settings.getInt(START_INDEX, 0);
        m_increment = settings.getInt(INCREMENT, 1);
        /** maximum number of allowed categories. */
        m_maxCategories = settings.getInt(MAX_CATEGORIES,
                DEFAULT_MAX_CATEGORIES);
        /** corner cases for the applier. */
        m_defaultValue = settings.getDataCell(DEFAULT_VALUE,
                DataType.getMissingCell());
        m_mapMissingTo = settings.getDataCell(MAP_MISSING_TO,
                DataType.getMissingCell());
    }

    /** Called from model when settings are to be loaded.
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsForModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_includedColumns = settings.getStringArray(INCLUDED_COLUMNS);
        m_includeAll = settings.getBoolean(INCLUDE_ALL);
        /** Define names of new columns. */
        m_appendColumns = settings.getBoolean(APPEND_COLUMN);
        m_columnSuffix = settings.getString(COLUMN_SUFFIX);
        /** controls content of output columns. */
        m_startIndex = settings.getInt(START_INDEX);
        m_increment = settings.getInt(INCREMENT);
        /** maximum number of allowed categories. */
        m_maxCategories = settings.getInt(MAX_CATEGORIES);
        /** corner cases for the applier. */
        m_defaultValue = settings.getDataCell(DEFAULT_VALUE);
        m_mapMissingTo = settings.getDataCell(MAP_MISSING_TO);
    }

    /** Called from model and dialog to save current settings.
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray(INCLUDED_COLUMNS, m_includedColumns);
        settings.addBoolean(INCLUDE_ALL, m_includeAll);
        /** Define names of new columns. */
        settings.addBoolean(APPEND_COLUMN, m_appendColumns);
        settings.addString(COLUMN_SUFFIX, m_columnSuffix);
        /** controls content of output columns. */
        settings.addInt(START_INDEX, m_startIndex);
        settings.addInt(INCREMENT, m_increment);
        /** maximum number of allowed categories. */
        settings.addInt(MAX_CATEGORIES, m_maxCategories);
        /** corner cases for the applier. */
        settings.addDataCell(DEFAULT_VALUE, m_defaultValue);
        settings.addDataCell(MAP_MISSING_TO, m_mapMissingTo);
    }

}

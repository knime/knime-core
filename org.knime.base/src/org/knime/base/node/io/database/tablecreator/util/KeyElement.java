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
 *   Dec 3, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.tablecreator.DBColumn;
import org.knime.core.node.port.database.tablecreator.DBKey;

/**
 * Key row element of the KeysPanel
 *
 * @author Budi Yanto, KNIME.com
 */
class KeyElement extends RowElement {

    /** Default prefix of this element */
    static final String DEFAULT_PREFIX = "Key";

    /** Default primary key value */
    static final boolean DEFAULT_PRIMARY_KEY_VALUE = false;

    /** The column index of the key name in the table */
    static final int KEY_NAME_IDX = 0;

    /** The column index of the key columns in the table */
    static final int KEY_COLUMNS_IDX = 1;

    /** The column index of the primary key in the table */
    static final int PRIMARY_KEY_IDX = 2;

    /** The column names */
    static final String[] COLUMN_NAMES = new String[]{"Key Name", "Key Columns", "Primary Key"};

    /** The column classes */
    static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{String.class, String.class, Boolean.class};

    /** The configuration key for the key name */
    private static final String CFG_KEY_NAME = "keyName";

    /** The configuration key for the key columns */
    private static final String CFG_KEY_COLUMNS = "keyColumns";

    /** The configuration key for the primary key */
    private static final String CFG_KEY_PRIMARY = "primaryKey";

    /** The underlying DBKey element */
    private DBKey m_key;

    /**
     * Creates a new instance of KeyElement
     *
     * @param name name of the key
     * @param columns columns used to define the key
     * @param primaryKey true if this key is a primary key, otherwise false
     */
    KeyElement(final String name, final Set<ColumnElement> columns, final boolean primaryKey) {
        super(DEFAULT_PREFIX);
        m_key = new DBKey(name, convertColumnElementsToDBColumns(columns), primaryKey);
    }

    /**
     * Creates a new instance of KeyElement from the given NodeSettingsRO instance
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    KeyElement(final NodeSettingsRO settings) {
        super(DEFAULT_PREFIX, settings);
    }

    /**
     * Returns the name of the key
     *
     * @return the name
     */
    String getName() {
        return m_key.getName();
    }

    /**
     * Sets the name of the key
     *
     * @param name the name to set
     */
    void setName(final String name) {
        m_key.setName(name);
    }

    /**
     * Set column elements used to define this key
     *
     * @param elements column elements used to define this key
     */
    void setColumnElements(final Set<ColumnElement> elements) {
        m_key.setColumns(convertColumnElementsToDBColumns(elements));
    }

    /**
     * Set DBColumns used to define this key
     *
     * @param columns DBColumns used to define this key
     */
    void setColumns(final Set<DBColumn> columns) {
        m_key.setColumns(columns);
    }

    /**
     * Returns the DBColumns used to define this key
     *
     * @return the DBColumns used to define this key
     */
    Set<DBColumn> getColumns() {
        return m_key.getColumns();
    }

    /**
     * Returns true if this key is a primary key, otherwise returns false
     *
     * @return true if this key is a primary key, otherwise false
     */
    boolean isPrimaryKey() {
        return m_key.isPrimaryKey();
    }

    /**
     * Sets to true if this key is a primary key, otherwise sets to false
     *
     * @param primaryKey true if this key is a primary key, otherwise false
     */
    void setPrimaryKey(final boolean primaryKey) {
        m_key.setPrimaryKey(primaryKey);
    }

    /**
     * Returns the underlying DBKey instance
     *
     * @return the underlying DBKey instance
     */
    DBKey getDBKey() {
        return m_key;
    }

    /**
     * A helper method to convert the given ColumnElements to DBColumn
     *
     * @param elements the ColumnElements to convert
     * @return a list of DBColumns converted from the given ColumnElements
     */
    private Set<DBColumn> convertColumnElementsToDBColumns(final Set<ColumnElement> elements) {
        Set<DBColumn> columns = new LinkedHashSet<>();
        for (ColumnElement elem : elements) {
            columns.add(elem.getDBColumn());
        }
        return columns;
    }

    /**
     * Returns a string representation of the columns used to define this key
     *
     * @return a string representation of the columns used to define this key
     */
    String getColumnsString() {
        final StringBuilder builder = new StringBuilder();

        List<DBColumn> columns = new ArrayList<>(getColumns());
        Collections.sort(columns, new Comparator<DBColumn>() {

            @Override
            public int compare(final DBColumn o1, final DBColumn o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (DBColumn column : columns) {
            builder.append(column.getName() + ", "); // remove ', ' at the end of the string
        }
        final String columnsString = builder.toString();
        return columnsString.substring(0, columnsString.length() - 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            final String name = settings.getString(CFG_KEY_NAME);
            // Load all columns
            final NodeSettingsRO cfg = settings.getNodeSettings(CFG_KEY_COLUMNS);
            final Set<DBColumn> dbColumns = new LinkedHashSet<>();

            for (String settingsKey : cfg.keySet()) {
                final String colName = cfg.getString(settingsKey);
                DBColumn col = new DBColumn(colName, "", false);
                dbColumns.add(col);
            }

            final boolean primaryKey = settings.getBoolean(CFG_KEY_PRIMARY);

            m_key = new DBKey(name, dbColumns, primaryKey);
        } catch (InvalidSettingsException ex) {
            // Do nothing if no settings are found
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KEY_NAME, getName());
        NodeSettingsWO cfg = settings.addNodeSettings(CFG_KEY_COLUMNS);
        int idx = 0;
        for (DBColumn col : getColumns()) {
            cfg.addString(ColumnElement.DEFAULT_PREFIX + idx++, col.getName());
        }
        settings.addBoolean(CFG_KEY_PRIMARY, isPrimaryKey());
    }
}

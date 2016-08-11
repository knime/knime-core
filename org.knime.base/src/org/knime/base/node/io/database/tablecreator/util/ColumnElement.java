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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.tablecreator.DBColumn;

/**
 * Column row element of the ColumnsPanel
 *
 * @author Budi Yanto, KNIME.com
 */
class ColumnElement extends RowElement {

    static final String DEFAULT_PREFIX = "Column";

    static final boolean DEFAULT_NOT_NULL_VALUE = false;

    static final int COLUMN_NAME_IDX = 0;

    static final int COLUMN_TYPE_IDX = 1;

    static final int NOT_NULL_IDX = 2;

    static final String[] COLUMN_NAMES = new String[]{"Column Name", "Column Type", "Not Null"};

    static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{String.class, String.class, Boolean.class};

    private static final String CFG_COLUMN_NAME = "columnName";

    private static final String CFG_COLUMN_TYPE = "columnType";

    private static final String CFG_COLUMN_NOT_NULL = "notNull";

    private DBColumn m_column;

    /**
     * Creates a new instance of ColumnElement
     *
     * @param name name of the column
     * @param type SQL data type of the column
     * @param notNull true if the column cannot be null, otherwise false
     */
    ColumnElement(final String name, final String type, final boolean notNull) {
        super(DEFAULT_PREFIX);
        m_column = new DBColumn(name, type, notNull);
    }

    /**
     * Create a new instance of ColumnElement from the given NodeSettingsRO instance
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    ColumnElement(final NodeSettingsRO settings) {
        super(DEFAULT_PREFIX, settings);
    }

    /**
     * Returns the name of the column
     *
     * @return the name
     */
    String getName() {
        return m_column.getName();
    }

    /**
     * Set the name of the column
     *
     * @param name the name to set
     */
    void setName(final String name) {
        m_column.setName(name);
    }

    /**
     * Returns the SQL data type of the column
     *
     * @return the SQL data type
     */
    String getType() {
        return m_column.getType();
    }

    /**
     * Sets the SQL data type of the column
     *
     * @param type the SQL data type to set
     */
    void setType(final String type) {
        m_column.setType(type);
    }

    /**
     * Returns true if the column cannot be null, otherwise false
     *
     * @return true if the column cannot be null, otherwise false
     */
    boolean isNotNull() {
        return m_column.isNotNull();
    }

    /**
     * Sets to true if the column cannot be null, otherwise false
     *
     * @param notNull true if the column cannot be null, otherwise false
     */
    void setNotNull(final boolean notNull) {
        m_column.setNotNull(notNull);
    }

    /**
     * Returns the underlying DBColumn instance
     *
     * @return the underlying DBColumn instance
     */
    DBColumn getDBColumn() {
        return m_column;
    }

    /**
     * Creates a new instance of ColumnElement from the given DBColumn instance
     *
     * @param column the DBColumn instance used to create ColumnElement
     * @return a new instance of ColumnElement
     */
    static ColumnElement createColumnElement(final DBColumn column) {
        return new ColumnElement(column.getName(), column.getType(), column.isNotNull());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_COLUMN_NAME, getName());
        settings.addString(CFG_COLUMN_TYPE, getType());
        settings.addBoolean(CFG_COLUMN_NOT_NULL, isNotNull());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_column = new DBColumn(settings.getString(CFG_COLUMN_NAME), settings.getString(CFG_COLUMN_TYPE),
                settings.getBoolean(CFG_COLUMN_NOT_NULL));
        } catch (InvalidSettingsException ex) {
            // Do nothing if no settings are found
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_column.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_column == null) ? 0 : m_column.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ColumnElement)) {
            return false;
        }
        ColumnElement other = (ColumnElement)obj;
        if (m_column == null) {
            if (other.m_column != null) {
                return false;
            }
        } else if (!m_column.equals(other.m_column)) {
            return false;
        }
        return true;
    }

}

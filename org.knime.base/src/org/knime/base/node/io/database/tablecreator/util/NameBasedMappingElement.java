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
 *   Dec 11, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * NameBasedMapping row element of the NameBasedMappingPanel
 *
 * @author Budi Yanto, KNIME.com
 */
class NameBasedMappingElement extends RowElement {

    /** Default prefix of this element */
    static final String DEFAULT_PREFIX = "NameBasedMapping";

    /** The column index of the name pattern in the table */
    static final int NAME_PATTERN_IDX = 0;

    /** The column index of the isRegex in the table */
    static final int REGEX_IDX = 1;

    /** The column index of the SQL type in the table */
    static final int SQL_TYPE_IDX = 2;

    /** The column index of the not null in the table */
    static final int NOT_NULL_IDX = 3;

    /** The column names */
    static final String[] COLUMN_NAMES = new String[]{"Name Pattern", "RegEx", "SQL Type", "Not Null"};

    /** The column classes */
    static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{String.class, Boolean.class, String.class, Boolean.class};

    /** Default name pattern */
    static final String DEFAULT_NAME_PATTERN = ".*";

    /** Default isRegex value */
    static final boolean DEFAULT_IS_REGEX = true;

    /** Default not null value */
    static final boolean DEFAULT_NOT_NULL = true;

    /** The configuration key for the name pattern */
    private static final String CFG_NAME_PATTERN = "namePattern";

    /** The configuration key for the isRegex */
    private static final String CFG_IS_REGEX = "isRegex";

    /** The configuration key for the SQL type */
    private static final String CFG_SQL_TYPE = "sqlType";

    /** The configuration key for the not null */
    private static final String CFG_NOT_NULL = "notNull";

    private String m_namePattern;

    private boolean m_isRegex;

    private String m_sqlType;

    private boolean m_isNotNull;

    /**
     * Creates a new instance of NameBasedMappingElement
     *
     * @param namePattern name pattern of the columns used to map
     * @param isRegex true if the name pattern is regex, otherwise false
     * @param sqlType SQL data type to be mapped to
     * @param notNull default not null value of the generated column
     */
    NameBasedMappingElement(final String namePattern, final boolean isRegex, final String sqlType,
        final boolean notNull) {
        super(DEFAULT_PREFIX);
        m_namePattern = namePattern;
        m_isRegex = isRegex;
        m_sqlType = sqlType;
        m_isNotNull = notNull;
    }

    /**
     * Creates a new instance of NameBasedMappingElement
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    NameBasedMappingElement(final NodeSettingsRO settings) {
        super(DEFAULT_PREFIX, settings);
    }

    /**
     * Returns the name pattern
     *
     * @return the namePattern
     */
    String getNamePattern() {
        return m_namePattern;
    }

    /**
     * Sets the name pattern
     *
     * @param namePattern the namePattern to set
     */
    void setNamePattern(final String namePattern) {
        m_namePattern = namePattern;
    }

    /**
     * Returns true if the name pattern is regex, otherwise returns false
     *
     * @return true if the name pattern is regex, otherwise false
     */
    boolean isRegex() {
        return m_isRegex;
    }

    /**
     * Sets to true if the name pattern is regex, otherwise sets to false
     *
     * @param isRegex true if the name pattern is regex, otherwise false
     */
    void setRegex(final boolean isRegex) {
        m_isRegex = isRegex;
    }

    /**
     * Returns the SQL data type
     *
     * @return the SQL data type
     */
    String getSqlType() {
        return m_sqlType;
    }

    /**
     * Sets the SQL data type
     *
     * @param sqlType the SQL data type to set
     */
    void setSqlType(final String sqlType) {
        m_sqlType = sqlType;
    }

    /**
     * Returns true if the generated column cannot be null, otherwise returns false
     *
     * @return true if the generated column cannot be null, otherwise false
     */
    boolean isNotNull() {
        return m_isNotNull;
    }

    /**
     * Sets to true if the generated column cannot be null, otherwise sets to false
     *
     * @param isNotNull true if the generated column cannot be null, otherwise false
     */
    void setNotNull(final boolean isNotNull) {
        m_isNotNull = isNotNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_namePattern = settings.getString(CFG_NAME_PATTERN);
            m_isRegex = settings.getBoolean(CFG_IS_REGEX);
            m_sqlType = settings.getString(CFG_SQL_TYPE);
            m_isNotNull = settings.getBoolean(CFG_NOT_NULL);
        } catch (InvalidSettingsException ex) {
            // Do nothing if no settings are found
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_NAME_PATTERN, getNamePattern());
        settings.addBoolean(CFG_IS_REGEX, isRegex());
        settings.addString(CFG_SQL_TYPE, getSqlType());
        settings.addBoolean(CFG_NOT_NULL, isNotNull());
    }
}

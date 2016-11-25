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
 * NameBasedKey row element of the NameBasedKeysPanel
 *
 * @author Budi Yanto, KNIME.com
 */
class NameBasedKeysElement extends RowElement {

    /** Default prefix of this element */
    static final String DEFAULT_PREFIX = KeyElement.DEFAULT_PREFIX;

    /** The column index of the name pattern in the table */
    static final int NAME_PATTERN_IDX = 0;

    /** The column index of the isRegex in the table */
    static final int REGEX_IDX = 1;

    /** The column index of the key name in the table */
    static final int KEY_NAME_IDX = 2;

    /** The column index of the primary key in the table */
    static final int PRIMARY_KEY_IDX = 3;

    /** The column names */
    static final String[] COLUMN_NAMES = new String[]{"Name Pattern", "RegEx", "Key Name", "Primary Key"};

    /** The column classes */
    static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{String.class, Boolean.class, String.class, Boolean.class};

    /** Default name pattern */
    static final String DEFAULT_NAME_PATTERN = ".*";

    /** Default isRegex value */
    static final boolean DEFAULT_IS_REGEX = true;

    /** Default key name value */
    static final String DEFAULT_KEY_NAME = "";

    /** Default primary key value */
    static final boolean DEFAULT_PRIMARY_KEY = false;

    /** The configuration key for the name pattern */
    private static final String CFG_NAME_PATTERN = "namePattern";

    /** The configuration key for the isRegex */
    private static final String CFG_IS_REGEX = "isRegex";

    /** The configuration key for the key name */
    private static final String CFG_KEY_NAME = "keyName";

    /** The configuration key for the primary key */
    private static final String CFG_PRIMARY_KEY = "primaryKey";

    private String m_namePattern;

    private boolean m_isRegex;

    private String m_keyName;

    private boolean m_PrimaryKey;

    /**
     * Creates a new instance of NameBasedKeysElement
     *
     * @param namePattern name pattern of the columns used to define the key
     * @param isRegex true if the name pattern is regex, otherwise false
     * @param keyName name of the key
     * @param primaryKey true if this key is a primary key, otherwise false
     */
    NameBasedKeysElement(final String namePattern, final boolean isRegex, final String keyName,
        final boolean primaryKey) {
        super(DEFAULT_PREFIX);
        m_namePattern = namePattern;
        m_isRegex = isRegex;
        m_keyName = keyName;
        m_PrimaryKey = primaryKey;
    }

    /**
     * Creates a new instance of NameBasedKeysElement
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    NameBasedKeysElement(final NodeSettingsRO settings) {
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
     * Returns the name of the key
     *
     * @return the keyName
     */
    String getKeyName() {
        return m_keyName;
    }

    /**
     * Sets the name of the key
     *
     * @param keyName the keyName to set
     */
    void setKeyName(final String keyName) {
        m_keyName = keyName;
    }

    /**
     * Returns true if this key is a primary key, otherwise returns false
     *
     * @return true if this key is a primary key, otherwise false
     */
    boolean isPrimaryKey() {
        return m_PrimaryKey;
    }

    /**
     * Sets to true if this key is a primary key, otherwise sets to false
     *
     * @param primaryKey true if this key is a primary key, otherwise false
     */
    void setPrimaryKey(final boolean primaryKey) {
        m_PrimaryKey = primaryKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_namePattern = settings.getString(CFG_NAME_PATTERN);
            m_isRegex = settings.getBoolean(CFG_IS_REGEX);
            m_keyName = settings.getString(CFG_KEY_NAME);
            m_PrimaryKey = settings.getBoolean(CFG_PRIMARY_KEY);
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
        settings.addString(CFG_KEY_NAME, getKeyName());
        settings.addBoolean(CFG_PRIMARY_KEY, isPrimaryKey());
    }
}

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

import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * KnimeBasedMapping row element of the KnimeBasedMappingPanel
 *
 * @author Budi Yanto, KNIME.com
 */
class KNIMEBasedMappingElement extends RowElement {

    /** Default prefix of this element */
    static final String DEFAULT_PREFIX = "KnimeBasedMapping";

    /** The column index of the knime type in the table */
    static final int KNIME_TYPE_IDX = 0;

    /** The column index of the SQL type in the table */
    static final int SQL_TYPE_IDX = 1;

    /** The column index of the not null in the table */
    static final int NOT_NULL_IDX = 2;

    /** The column names */
    static final String[] COLUMN_NAMES = new String[]{"KNIME Type", "SQL Type", "Not Null"};

    /** The column classes */
    static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{DataType.class, String.class, Boolean.class};

    /** Default KNIME type */
    static final DataType DEFAULT_KNIME_TYPE = StringCell.TYPE;

    /** Default SQL type */
    static final String DEFAULT_SQL_TYPE = "varchar (255)";

    /** Default not null value*/
    static final boolean DEFAULT_NOT_NULL = true;

    /** The configuration key for the KNIME type */
    private static final String CFG_KNIME_TYPE = "knimeType";

    /** The configuration key for the SQL type */
    private static final String CFG_SQL_TYPE = "sqlType";

    /** The configuration key for the not null */
    private static final String CFG_NOT_NULL = "notNull";

    private DataType m_knimeType;

    private String m_sqlType;

    private boolean m_notNull;

    /**
     * Creates a new instance of KnimeBasedMappingElement
     *
     * @param knimeType the Knime data type to map
     * @param sqlType the SQL data type to be mapped to
     * @param notNull default not null value of the generated column
     */
    KNIMEBasedMappingElement(final DataType knimeType, final String sqlType, final boolean notNull) {
        super(DEFAULT_PREFIX);
        m_knimeType = knimeType;
        m_sqlType = sqlType;
        m_notNull = notNull;
    }

    /**
     * Creates a new instance of KnimeBasedMappingElement
     *
     * @param settings the NodeSettingsRO instance to load from
     */
    KNIMEBasedMappingElement(final NodeSettingsRO settings) {
        super(DEFAULT_PREFIX, settings);
    }

    /**
     * Returns the Knime data type
     *
     * @return the knimeType
     */
    public DataType getKnimeType() {
        return m_knimeType;
    }

    /**
     * Sets the Knime data type
     *
     * @param knimeType the knimeType to set
     */
    void setKnimeType(final DataType knimeType) {
        m_knimeType = knimeType;
    }

    /**
     * Returns the SQL data type
     *
     * @return the sqlType
     */
    public String getSqlType() {
        return m_sqlType;
    }

    /**
     * Sets the SQL data type
     *
     * @param sqlType the sqlType to set
     */
    void setSqlType(final String sqlType) {
        m_sqlType = sqlType;
    }

    /**
     * Returns true if the generated column cannot be null, otherwise returns false
     *
     * @return true if the generated column cannot be null, otherwise false
     */
    public boolean isNotNull() {
        return m_notNull;
    }

    /**
     * Sets to true if the generated column cannot be null, otherwise sets to false
     *
     * @param notNull true if the generated column cannot be null, otherwise false
     */
    void setNotNull(final boolean notNull) {
        m_notNull = notNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_knimeType = settings.getDataType(CFG_KNIME_TYPE);
            m_sqlType = settings.getString(CFG_SQL_TYPE);
            m_notNull = settings.getBoolean(CFG_NOT_NULL);
        } catch (InvalidSettingsException ex) {
            // Do nothing if no setting is found
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDataType(CFG_KNIME_TYPE, getKnimeType());
        settings.addString(CFG_SQL_TYPE, getSqlType());
        settings.addBoolean(CFG_NOT_NULL, isNotNull());
    }

}

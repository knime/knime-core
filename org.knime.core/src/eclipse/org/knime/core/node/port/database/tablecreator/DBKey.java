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
 *   Dec 23, 2015 (budiyanto): created
 */
package org.knime.core.node.port.database.tablecreator;

import java.util.Set;

/**
 * Class to represent a key in database
 *
 * @author Budi Yanto, KNIME.com
 * @since 3.3
 */
public class DBKey {

    /** Name of the key **/
    private String m_name;

    /** Columns used to define the key **/
    private Set<DBColumn> m_columns;

    /** Attribute to indicate whether the key is primary key or not **/
    private boolean m_primaryKey;

    /**
     * Creates a new instance of DBKey
     *
     * @param name name of the key
     * @param columns columns used to define the key
     * @param primaryKey true if the key is a primary key, otherwise false
     */
    public DBKey(final String name, final Set<DBColumn> columns, final boolean primaryKey) {
        m_name = name;
        m_columns = columns;
        m_primaryKey = primaryKey;
    }

    /**
     * Returns the name of the key
     *
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the name of the key
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * Returns the columns that define the key
     *
     * @return the columns
     */
    public Set<DBColumn> getColumns() {
        return m_columns;
    }

    /**
     * Sets the columns to define the key
     *
     * @param columns the columns to set
     */
    public void setColumns(final Set<DBColumn> columns) {
        m_columns = columns;
    }

    /**
     * Returns true if the key is a primary key, otherwise returns false
     *
     * @return true if the key is a primary key, otherwise false
     */
    public boolean isPrimaryKey() {
        return m_primaryKey;
    }

    /**
     * Sets to true if the key is a primary key, otherwise sets to false
     *
     * @param primaryKey true if the key is a primary key, otherwise false
     */
    public void setPrimaryKey(final boolean primaryKey) {
        m_primaryKey = primaryKey;
    }

}

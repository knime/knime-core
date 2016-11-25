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

/**
 * Class to represent a column in database
 *
 * @author Budi Yanto, KNIME.com
 * @since 3.3
 */
public class DBColumn {

    /** Name of the column **/
    private String m_name;

    /** SQL data type of the column **/
    private String m_type;

    /** Attribute to indicate whether the column can have null value or not **/
    private boolean m_notNull;

    /**
     * Creates a new instance of DBColumn
     *
     * @param name name of the column
     * @param type SQL data type of the column
     * @param notNull true if the column cannot have null value, otherwise false
     */
    public DBColumn(final String name, final String type, final boolean notNull) {
        m_name = name;
        m_type = type;
        m_notNull = notNull;
    }

    /**
     * Returns the name of the column
     *
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the name of the column
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * Returns the SQL data type of the column
     *
     * @return the SQL data type
     */
    public String getType() {
        return m_type;
    }

    /**
     * Sets the SQL data type of the column
     *
     * @param type the SQL data type to set
     */
    public void setType(final String type) {
        m_type = type;
    }

    /**
     * Returns true if the column cannot have null value, otherwise returns false
     *
     * @return true if the column cannot have null value, otherwise false
     */
    public boolean isNotNull() {
        return m_notNull;
    }

    /**
     * Sets to true if the column cannot have null value, otherwise sets to false
     *
     * @param notNull true if the column cannot have null value, otherwise false
     */
    public void setNotNull(final boolean notNull) {
        m_notNull = notNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
        result = prime * result + (m_notNull ? 1231 : 1237);
        result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
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
        if (!(obj instanceof DBColumn)) {
            return false;
        }
        DBColumn other = (DBColumn)obj;
        if (m_name == null) {
            if (other.m_name != null) {
                return false;
            }
        } else if (!m_name.equals(other.m_name)) {
            return false;
        }
        if (m_notNull != other.m_notNull) {
            return false;
        }
        if (m_type == null) {
            if (other.m_type != null) {
                return false;
            }
        } else if (!m_type.equals(other.m_type)) {
            return false;
        }
        return true;
    }

}

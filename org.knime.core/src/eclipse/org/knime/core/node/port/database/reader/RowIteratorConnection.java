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
 *   07.11.2015 (koetter): created
 */
package org.knime.core.node.port.database.reader;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.port.database.DatabaseConnectionSettings;

/**
 * A row iterator that holds an open database connection and allows to create an iterator that iterates through the
 * database entries and return them as a data row.
 * IMPORTANT: the RowIteratorConnection needs to be closed after use in order to close the database connection.
 *
 * @author Martin Horn, University of Konstanz
 * @since 3.1
 */
@SuppressWarnings("javadoc")
public class RowIteratorConnection implements DBRowIterator {

    protected Connection m_conn2;

    protected Statement m_stmt;

    protected boolean m_autoCommit;

    protected DataTableSpec m_spec2;

    private RowIterator m_iterator;

    /**
     * @throws SQLException
    *
    */
    public RowIteratorConnection(final Connection conn, final Statement stmt, final DataTableSpec spec,
        final RowIterator iterator) throws SQLException {
        m_conn2 = conn;
        m_stmt = stmt;
        m_spec2 = spec;
        m_iterator = iterator;
        m_autoCommit = conn.getAutoCommit();
    }

    /**
     * @return the database row iterator
     */
    @Override
    public RowIterator iterator() {
        return m_iterator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec2;
    }

    /**
     * Closes the database connection.
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        if (m_stmt != null) {
            if (!m_conn2.getAutoCommit()) {
                m_conn2.commit();
            }
            DatabaseConnectionSettings.setAutoCommit(m_conn2, m_autoCommit);
            m_stmt.close();
        }
    }

}
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node.port.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Wraps an <code>java.sql.Driver</code> object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseWrappedDriver implements Driver {
    private final Driver m_d;

    /**
     * Create wrapper.
     *
     * @param d For this <code>Driver</code>.
     */
    DatabaseWrappedDriver(final Driver d) {
        m_d = d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection connect(final String url, final Properties info)
            throws SQLException {
        return m_d.connect(url, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return m_d.acceptsURL(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url,
            final Properties info) throws SQLException {
        return m_d.getPropertyInfo(url, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMajorVersion() {
        return m_d.getMajorVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinorVersion() {
        return m_d.getMinorVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean jdbcCompliant() {
        return m_d.jdbcCompliant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_d.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_d.hashCode();
    }
    
    /**
     * Added with Java 1.7, needs to be flagged with @Override.
     * @since KNIME v2.5
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return m_d.getParentLogger();
    }
}

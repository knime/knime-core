/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.io.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Wraps a Driver object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class WrappedDriver implements Driver {
    private final Driver m_d;

    /**
     * Create wrapper.
     * 
     * @param d For this <code>Driver</code>.
     */
    WrappedDriver(final Driver d) {
        m_d = d;
    }

    /**
     * {@inheritDoc}
     */
    public Connection connect(final String url, final Properties info)
            throws SQLException {
        return m_d.connect(url, info);
    }

    /**
     * {@inheritDoc}
     */
    public boolean acceptsURL(final String url) throws SQLException {
        return m_d.acceptsURL(url);
    }

    /**
     * {@inheritDoc}
     */
    public DriverPropertyInfo[] getPropertyInfo(final String url,
            final Properties info) throws SQLException {
        return m_d.getPropertyInfo(url, info);
    }

    /**
     * {@inheritDoc}
     */
    public int getMajorVersion() {
        return m_d.getMajorVersion();
    }

    /**
     * {@inheritDoc}
     */
    public int getMinorVersion() {
        return m_d.getMinorVersion();
    }

    /**
     * {@inheritDoc}
     */
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
}

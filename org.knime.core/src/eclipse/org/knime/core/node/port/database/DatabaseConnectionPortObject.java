/*
 * ------------------------------------------------------------------------
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
 *   24.04.2014 (thor): created
 */
package org.knime.core.node.port.database;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectRegistry;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Port object that holds a database connection (without any query).
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DatabaseConnectionPortObject implements PortObject {

    /**
     * The spec for this port object.
     */
    protected final DatabaseConnectionPortObjectSpec m_spec;

    /**
     * Database port type.
     */
    public static final PortType TYPE = PortObjectRegistry.getInstance().getPortType(DatabaseConnectionPortObject.class);

    /**
     * Database type for optional ports.
     */
    public static final PortType TYPE_OPTIONAL =
        PortObjectRegistry.getInstance().getPortType(DatabaseConnectionPortObject.class, true);

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseConnectionPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        String jdbcUrl = "<unknown>";
        String dbId = null;
        try {
            DatabaseConnectionSettings cs = getConnectionSettings(null);
            jdbcUrl = cs.getJDBCUrl();
            dbId = cs.getDatabaseIdentifier();
        } catch (InvalidSettingsException ex) {
            // jo mei...
        }
        StringBuilder buf = new StringBuilder();
        if (dbId != null) {
            buf.append("DB: ").append(dbId).append(" ");
        }
        buf.append("URL: ").append(jdbcUrl);
        return buf.toString();
    }

    /**
     * Creates a new database port object.
     *
     * @param spec database port object spec, must not be <code>null</code>
     */
    public DatabaseConnectionPortObject(final DatabaseConnectionPortObjectSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("DatabaseConnectionPortObjectSpec must not be null!");
        }
        m_spec = spec;
    }

    /**
     * Returns the connection settings for this object.
     *
     * @param credProvider a credentials provider, may be <code>null</code>
     * @return a connection settings object
     * @throws InvalidSettingsException if the spec is missing required information for the connection
     */
    public DatabaseConnectionSettings getConnectionSettings(final CredentialsProvider credProvider)
        throws InvalidSettingsException {
        return m_spec.getConnectionSettings(credProvider);
    }

    /**
     * Serializer used to save {@link DatabaseConnectionPortObject}s.
     *
     * @return a new serializer
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.0
     */
    public static final class Serializer extends PortObjectSerializer<DatabaseConnectionPortObject> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObject(final DatabaseConnectionPortObject portObject,
            final PortObjectZipOutputStream out, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
            // nothing to save
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DatabaseConnectionPortObject loadPortObject(final PortObjectZipInputStream in,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            return new DatabaseConnectionPortObject((DatabaseConnectionPortObjectSpec)spec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return m_spec.getViews();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DatabaseConnectionPortObject)) {
            return false;
        }
        DatabaseConnectionPortObject dbPort = (DatabaseConnectionPortObject) obj;
        return m_spec.equals(dbPort.m_spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_spec.hashCode();
    }
}

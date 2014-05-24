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
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Specification for a database connection port.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DatabaseConnectionPortObjectSpec implements PortObjectSpec {
    /**
     * A serializer for {@link DatabaseConnectionPortObjectSpec}s.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     */
    protected static class ConnectionSpecSerializer extends PortObjectSpecSerializer<DatabaseConnectionPortObjectSpec> {
        @Override
        public DatabaseConnectionPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in)
            throws IOException {
            ModelContentRO modelContent = loadModelContent(in);
            return new DatabaseConnectionPortObjectSpec(modelContent);
        }

        @Override
        public void savePortObjectSpec(final DatabaseConnectionPortObjectSpec portObjectSpec,
            final PortObjectSpecZipOutputStream out) throws IOException {
            saveModelContent(out, portObjectSpec);
        }

        /**
         * Reads the model content from the input stream.
         * @param in an input stream
         * @return the model content containing the spec information
         * @throws IOException if an I/O error occurs
         */
        protected ModelContentRO loadModelContent(final PortObjectSpecZipInputStream in) throws IOException {
            ZipEntry ze = in.getNextEntry();
            if (!ze.getName().equals(KEY_DATABASE_CONNECTION)) {
                throw new IOException("Key \"" + ze.getName() + "\" does not " + " match expected zip entry name \""
                    + KEY_DATABASE_CONNECTION + "\".");
            }
            return ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));
        }

        /**
         * Saves the given spec object into the output stream.
         * @param os an output stream
         * @param portObjectSpec the port spec
         * @throws IOException if an I/O error occurs
         */
        protected void saveModelContent(final PortObjectSpecZipOutputStream os,
            final DatabaseConnectionPortObjectSpec portObjectSpec) throws IOException {
            os.putNextEntry(new ZipEntry(KEY_DATABASE_CONNECTION));
            portObjectSpec.m_conn.saveToXML(new NonClosableOutputStream.Zip(os));
        }
    }

    private final ModelContentRO m_conn;

    /**
     * Creates a new spec for a database connection port.
     *
     * @param conn connection model
     */
    protected DatabaseConnectionPortObjectSpec(final ModelContentRO conn) {
        if (conn == null) {
            throw new IllegalArgumentException("Database connection model must not be null.");
        }
        m_conn = conn;
    }

    /**
     * Creates a new spec for a database connection port.
     *
     * @param connSettings the connection settings
     */
    public DatabaseConnectionPortObjectSpec(final DatabaseConnectionSettings connSettings) {
        if (connSettings == null) {
            throw new IllegalArgumentException("Database connection settings must not be null.");
        }
        ModelContent temp = new ModelContent(this.getClass().getName());
        connSettings.saveConnection(temp);
        m_conn = temp;
    }

    /**
     * returns the actual model content. The actual content is defined by the {@link DatabaseConnectionSettings}
     * class (and its potential subclasses).
     *
     * @return a model content
     */
    protected ModelContentRO getConnectionModel() {
        return m_conn;
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
        return new DatabaseConnectionSettings(m_conn, credProvider);
    }

    /**
     * Serializer used to save {@link DatabaseConnectionPortObjectSpec}s.
     *
     * @return a new serializer
     */
    public static PortObjectSpecSerializer<DatabaseConnectionPortObjectSpec> getPortObjectSpecSerializer() {
        return new ConnectionSpecSerializer();
    }

    private static final String KEY_DATABASE_CONNECTION = "database_connection.zip";

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[]{new DatabaseConnectionView(m_conn)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DatabaseConnectionPortObjectSpec)) {
            return false;
        }
        DatabaseConnectionPortObjectSpec dbSpec = (DatabaseConnectionPortObjectSpec)obj;
        return m_conn.equals(dbSpec.m_conn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_conn.hashCode();
    }
}
